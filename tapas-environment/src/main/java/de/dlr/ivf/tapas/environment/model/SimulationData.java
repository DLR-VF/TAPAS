/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.model;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * In instances of this class all simulation data can be stored. These are the same as in the database. There exist an unique identifier, the run properties file, three falgs to determine the
 * simulation state, two timestamps for start and end of the simulation and two values for the completed and total amount of households to simulate.<br>
 * <br>
 * Behaviour of flags and state:<br>
 * The states have a logical order: INSERTED -> READY -> STARTED -> FINISHED. Only when a simulation is interrupted it gets the STOPPED state which is equal to the INSERTED state. With this logical
 * order you can detect whether a simulation is at least e.g started or ready. The state is detected by the flags. They are not reset when the state rises, i.e. when the state is READY the ready flag
 * is true. Then the next state is STARTED and the started and the ready flags are true. This behaviour leads to the following table:<br>
 * <table border="1" cellspacing="5" cellpadding="5">
 * <tr>
 * <th align="left">State</th>
 * <th align="left">Flags ready / started / finished</th>
 * <th align="left">progress</th>
 * </tr>
 * <tr>
 * <th align="left">INSERTED</th>
 * <th align="left">false / false / false</th>
 * <th align="right">0</th>
 * </tr>
 * <tr>
 * <th align="left">STOPPED</th>
 * <th align="left">false / false / false</th>
 * <th>0 < progress < total</th>
 * </tr>
 * <tr>
 * <th align="left">READY</th>
 * <th align="left">true / false / false</th>
 * <th align="right">0</th>
 * </tr>
 * <tr>
 * <th align="left">STARTED</th>
 * <th align="left">true / true / false</th>
 * <th align="right">0 < progress < total</th>
 * </tr>
 * <tr>
 * <th align="left">FINISHED</th>
 * <th align="left">true / true / true</th>
 * <th align="right">total</th>
 * </tr>
 * </table>
 *
 * @author mark_ma
 */
public class SimulationData {

    /**
     * This format is used to print dates in the format dd.MM.yyyy kk:mm:ss, e.g. 12.05.2011 12:01:12.
     */
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss");

    /**
     * a description field
     */
    private final String description;
    /**
     * Flag if the simulation is finished
     */
    private boolean sim_finished;
    /**
     * Unique simulation identifier
     */
    private final String sim_key;
    /**
     * Amount of completely simulated households
     */
    private long sim_progress;
    /**
     * Flag if the simulation is ready. This flag is not reset when the simulation is started or finished.
     */
    private boolean sim_ready;
    /**
     * Flag if simulation is started. This flag is not reset when simulation is finished.
     */
    private boolean sim_started;
    /**
     * Total amount of households to simulate.
     */
    private long sim_total;
    /**
     * Timestamp when simulation finished
     */
    private Timestamp timestamp_finished;
    /**
     * Timestamp when simulation starts
     */
    private Timestamp timestamp_started;
    /**
     * a link to ther insance which calculates the progress
     */
    private final ProgressCalculator progressCalc;

    /**
     * Internal constructor to set all member values.
     *
     * @param sim_key      unique identifier
     * @param sim_progress amount of simulated households
     * @param sim_total    total amount of households
     * @param sim_ready    ready flag
     * @param sim_started  started flag
     * @param sim_finished finished flag
     */
    private SimulationData(String sim_key, String description, long sim_progress, long sim_total, boolean sim_ready, boolean sim_started, boolean sim_finished) {
        this.description = description;
        this.sim_finished = sim_finished;
        this.sim_key = sim_key;
        this.sim_progress = sim_progress;
        this.sim_ready = sim_ready;
        this.sim_started = sim_started;
        this.sim_total = sim_total;
        this.progressCalc = new ProgressCalculator(this);
    }

    /**
     * This method formats the given duration in milliseconds into the format *hh:mm:ss.
     *
     * @param duration duration in milliseconds
     * @return formatted string
     */
    private String format(long duration) {
        String s = "";
        int hour, minute, seconds;

        // convert from miliseconds to seconds
        duration = duration / 1000;


        // hours
        hour = (int) (duration / 3600); //3600 seconds are one hour
        s += hour < 10 ? "0" + hour : Integer.toString(hour);
        s += ":";
        duration = duration % 3600; // remainer

        //minutes
        minute = (int) (duration / 60);
        s += minute < 10 ? "0" + minute : Integer.toString(minute);
        s += ":";
        duration = duration % 60; // remainer

        // seconds
        seconds = (int) duration;
        s += seconds < 10 ? "0" + seconds : Integer.toString(seconds);

        return s;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * This method return the elapsed time of the simulation if it is already started and not stopped or finished.
     *
     * @param current current database timestamp
     * @return elapsed time of the simulation
     */
    public String getElapsedTime(Timestamp current) {
        if (timestamp_started != null) {
            long duration = 0;
            if (isFinished()) {
                duration = timestamp_finished.getTime() - timestamp_started.getTime();
            } else if (isStarted()) {
                duration = current.getTime() - timestamp_started.getTime();
            }
            return format(duration);
        }
        return "";
    }

    /**
     * This method return the estimated time of the simulation if it is already started and not stopped or finished by:<br>
     * <br>
     * duration = [(sim_total - sim_progress)/sim_progress] * (current - timestamp_started)
     *
     * @param current current database timestamp
     * @return elapsed time of the simulation
     */
    public String getEstimatedTime(Timestamp current) {
        if (isStarted() && !isFinished()) {
            return format(this.progressCalc.getEstimatedTime());
        }
        return "";
    }

    /**
     * @return unique identifer
     */
    public String getKey() {
        return sim_key;
    }

    /**
     * @return amount of simulated households
     */
    public long getProgress() {
        return sim_progress;
    }

    /**
     * @return relative path to the run properties file
     */
    public String getRelativeFileName() {
        return "";
    }

    /**
     * @return current state of the simulation
     */
    public SimulationState getState() {
        return SimulationState.getState(this);
    }

    /**
     * @return formatted finished timestamp
     */
    public String getTimestampFinished() {
        if (timestamp_finished != null) return SDF.format(timestamp_finished);
        return "";
    }

    /**
     * @return formatted start timestamp
     */
    public String getTimestampStarted() {
        if (timestamp_started != null) return SDF.format(timestamp_started);
        return "";
    }

    /**
     * @return total amount of households to simulate
     */
    public long getTotal() {
        return sim_total;
    }


    /**
     * @return true if state is FINISHED
     */
    public boolean isFinished() {
        return getState() == SimulationState.FINISHED;
    }

    /**
     * @return true if state is READY
     */
    public boolean isReady() {
        return getState() == SimulationState.STOPPED;
    }

    /**
     * @return true if state is STARTED
     */
    public boolean isStarted() {
        return getState() == SimulationState.STARTED;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + getKey() + ", state=" + getState().name() + ", ready=" +
                sim_ready + " started=" + sim_started + " finished=" + sim_finished + "]";
    }

    /**
     * This method updates all members from the given ResultSet of a database select statement.
     *
     * @param rs ResultSet from a database select
     * @throws SQLException This exception is thrown if anything with the ResultSet is wrong, e.g. closed or too few values
     */
    public void update(ResultSet rs) throws SQLException {
        this.sim_finished = rs.getBoolean("sim_finished");
        this.sim_progress = rs.getLong("sim_progress");
        this.sim_ready = rs.getBoolean("sim_ready");
        this.sim_started = rs.getBoolean("sim_started");
        this.sim_total = rs.getLong("sim_total");
        this.timestamp_started = rs.getTimestamp("timestamp_started");
        this.timestamp_finished = rs.getTimestamp("timestamp_finished");
        this.progressCalc.update();
    }



    private class ProgressCalculator {

        private long estimatedTime;
        private long processedHouseholds;
        private long startTime;
        private long lastTimeUpdate;
        private final SimulationData simData;
        private boolean lastStateStarted;
        private final double[] slidingAverage;
        private int indexSlidingAverage = 0;

        public ProgressCalculator(SimulationData sim) {
            this.simData = sim;
            this.lastStateStarted = this.simData.isStarted();
            slidingAverage = new double[12];
            Arrays.fill(this.slidingAverage, -1);
            stateUpdate();
        }

        public long getEstimatedTime() {
            if (this.lastStateStarted) return Math.max(0,
                    this.estimatedTime - System.currentTimeMillis() + this.lastTimeUpdate);
            else return 0;
        }

        private void stateUpdate() {
            this.estimatedTime = 0;
            if (this.lastStateStarted) {
                this.processedHouseholds = this.simData.sim_progress;
                this.startTime = System.currentTimeMillis();
                this.lastTimeUpdate = this.startTime;
            } else {
                this.processedHouseholds = -1;
                this.startTime = -1;
                this.lastTimeUpdate = 0;
            }
        }

        public void update() {
            long currentTime = System.currentTimeMillis();

            if (this.simData.isStarted() != this.lastStateStarted) {
                this.lastStateStarted = this.simData.isStarted();
                stateUpdate();
            } else {
                if (this.lastStateStarted && this.simData.sim_progress != this.processedHouseholds) {
                    //calc delta
                    double number = this.simData.sim_progress - this.processedHouseholds;
                    double duration = currentTime - this.lastTimeUpdate;
                    double mSecPerHH = duration / number;
                    //store new values
                    this.lastTimeUpdate = currentTime;
                    this.processedHouseholds = this.simData.sim_progress;
                    //insert new delta
                    this.slidingAverage[this.indexSlidingAverage] = mSecPerHH;
                    //move cursor with wrap around
                    this.indexSlidingAverage = (this.indexSlidingAverage + 1) % this.slidingAverage.length;
                    //sum valid measurements
                    mSecPerHH = 0;
                    int elements = 0;
                    for (double v : this.slidingAverage) {
                        if (v > 0) {
                            mSecPerHH += v;
                            elements++;
                        }
                    }
                    mSecPerHH /= elements;
                    //calc estimate
                    this.estimatedTime = (long) ((double) (this.simData.sim_total - this.simData.sim_progress) *
                            mSecPerHH);
                }
            }
        }
    }
}
