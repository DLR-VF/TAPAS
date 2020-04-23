package de.dlr.ivf.tapas.tools;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.TPS_Geometrics;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TPS_SpeedAndDistanceValidator extends TPS_BasicConnectionClass {

    Map<Integer, TAZ> tazes = new HashMap<>();
    TravelData[][] data = new TravelData[0][0];

    public static void main(String[] args) {

        //bad misuse of this class to fix the veu2-calculation for egress times.
        //I hope Ill never look for this piece of crap eeeh code again!

//		TPS_SpeedAndDistanceValidator worker = new TPS_SpeedAndDistanceValidator();
//		double [][]egt = worker.loadMatrix("core.berlin_matrices", "PT_VISUM_1193_CALCULATED_AUG_2015_ACT");
//		final double threshold = 300;
//		final double summand=60;
//		final double factor = 0.5;
//		for(int i=0; i< egt.length;++i){
//			for(int j=0; j<egt[i].length;++j){
//				if(egt[i][j]>threshold)
//					egt[i][j]=egt[i][j]*factor+summand;
//			}
//		}
//
//		System.out.println("update core.berlin_matrices set matrix_values="+
//				TPS_BasicConnectionClass.matrixToStringWriterSQL(egt, 0)+" where matrix_name='PT_VISUM_1193_CALCULATED_AUG_2015_ACT_INTERMOBILE'");


        //String region = "braunschweig";
        //String region = "main-roehn";
        String region = "berlin";
        //String distMatrix = "MIV_braunschweig_rnb3_DIST";
        //String distMatrix = "RW_Braunschweig_2030";
        //String distMatrix = "CAR_1193TAZ_DIST_NEW";
        String distMatrix = "PT_VISUM_1223_BASIS_2017_NTR";
        //String timeMatrix = "CAR_1193_2030_ANA3_T0_TT_TOP3";
        //String timeMatrix = "CAR_1193_2010_T0_TT_TOP3";
        String timeMatrix = "PT_VISUM_1223_2030_HF60_1_SUM_TT";
        //String accessName = "OEV_Zugang_BS_2030_Basis_HDB";
        //String egressName = "OEV_Abgang_BS_2030_Basis_HDB";
        String accessName = null;
        String egressName = null;
        //String distMatrix = "T:\\Alte Stände\\Inputdateien\\Braunschweig\\Dist_MIV_BS_2030_HDB.csv";
        //String timeMatrix = "T:\\Alte Stände\\Inputdateien\\Braunschweig\\MIV_TT_BS_2030_HDB.csv";

        TPS_SpeedAndDistanceValidator worker = new TPS_SpeedAndDistanceValidator();
        worker.loadTAZInfos("core." + region + "_taz_1223", "core." + region + "_matrices", distMatrix, timeMatrix,
                accessName, egressName);
        double[] speedStat = worker.calcStatistics(true, 0, 10e9);
        double[] distStat = worker.calcStatistics(false);

        System.out.format("Dist:\t%6.2f\t%6.2f\t%6.2f\t%6.2f\n", distStat[STATVAL.MIN.ordinal()],
                distStat[STATVAL.MAX.ordinal()], distStat[STATVAL.AVG.ordinal()], distStat[STATVAL.STDDEV.ordinal()]);

        System.out.format("Speed:\t%6.2f\t%6.2f\t%6.2f\t%6.2f\n", speedStat[STATVAL.MIN.ordinal()],
                speedStat[STATVAL.MAX.ordinal()], speedStat[STATVAL.AVG.ordinal()],
                speedStat[STATVAL.STDDEV.ordinal()]);
        Collection<ScatterData> data = worker.calchScatterPlotData(10, 10, false);
        List<OutlierEntry> outliers = worker.findOutliers(2);

        for (OutlierEntry o : outliers) {
            System.out.println("TAZ: " + o.taz + " num: " + o.count + " ratio: " + o.ratio);
        }

        //Collection<ScatterData> data = worker.calchAVGSpeedScatterPlotData(100);
        worker.writeScatterData("T:\\temp\\" + region + "_" + timeMatrix + "_" + distMatrix + ".csv", data);
        //worker.writeScatterData("T:\\temp\\"+region+"_bs_txt_time_bs_txt_dist_MIV2030.csv", 1, 500);

//		double min= 1;
//		double max = 300;
//		List<TravelData> strange = worker.findStrangePairs(false, max, min);
//		System.out.println("Thresholds: Min: "+min+" Max: "+max);
//		System.out.println("Num of Strnage pairs: "+strange.size());
//		for (TravelData e: strange){
//			System.out.println(e.toString());
//		}

    }

    public void addMatrix(double[][] a, double[][] b) {
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < a[i].length; ++j) {
                a[i][j] += b[i][j];
            }
        }
    }

    public double[] calcStatistics(boolean speed) {
        return this.calcStatistics(speed, 0, Double.MAX_VALUE);

    }

    public double[] calcStatistics(boolean speed, double distMin, double distMax) {
        double[] stats = new double[4];
        double val;
        stats[STATVAL.MIN.ordinal()] = Double.POSITIVE_INFINITY;
        stats[STATVAL.MAX.ordinal()] = Double.NEGATIVE_INFINITY;
        stats[STATVAL.AVG.ordinal()] = 0;
        stats[STATVAL.STDDEV.ordinal()] = 0;
        int count = 0;
        for (int i = 0; i < this.data.length; ++i) {
            for (int j = 0; j < this.data[i].length; ++j) {
                if (i == j | this.data[i][j].yVal <= 0) continue;
                if (this.data[i][j].xVal > distMax | this.data[i][j].xVal < distMin) continue;
                if (speed) val = this.data[i][j].getKMH();
                else val = this.data[i][j].xVal / this.data[i][j].beeLine;
                stats[STATVAL.MIN.ordinal()] = Math.min(val, stats[STATVAL.MIN.ordinal()]);
                stats[STATVAL.MAX.ordinal()] = Math.max(val, stats[STATVAL.MAX.ordinal()]);
                stats[STATVAL.AVG.ordinal()] += val;
                count++;
            }
        }
        stats[STATVAL.AVG.ordinal()] /= count;
        for (int i = 0; i < this.data.length; ++i) {
            for (int j = 0; j < this.data[i].length; ++j) {
                if (i == j | this.data[i][j].yVal <= 0) continue;
                if (this.data[i][j].xVal > distMax | this.data[i][j].xVal < distMin) continue;
                if (speed) val = this.data[i][j].getKMH();
                else val = this.data[i][j].xVal / this.data[i][j].beeLine;
                stats[STATVAL.STDDEV.ordinal()] += Math.pow((val - stats[STATVAL.AVG.ordinal()]), 2);
            }
        }
        stats[STATVAL.STDDEV.ordinal()] /= count - 1;
        stats[STATVAL.STDDEV.ordinal()] = Math.sqrt(stats[STATVAL.STDDEV.ordinal()]);
        return stats;
    }

    /**
     * Calc the average speed per distance bin.
     *
     * @param distBin
     * @return
     */

    public Collection<ScatterData> calchAVGSpeedScatterPlotData(double distBin) {
        Map<String, ScatterData> data = new HashMap<>();
        String key;
        int distCell;
        ScatterData tmp;
        for (int i = 0; i < this.data.length; ++i) {
            for (int j = 0; j < this.data[i].length; ++j) {
                if (i == j | this.data[i][j].yVal <= 0) {
                    continue;
                }

                distCell = (int) (this.data[i][j].xVal / distBin);
                key = String.format("%d", distCell);
                if (data.containsKey(key)) {
                    tmp = data.get(key);
                } else {
                    tmp = new ScatterData();
                    tmp.speed = 0;
                    tmp.dist = distCell * distBin;
                    tmp.num = 0;
                }
                tmp.speed += this.data[i][j].getKMH();
                tmp.num += 1;
                data.put(key, tmp);
            }
        }
        for (ScatterData d : data.values()) {
            d.speed /= d.num; //calc the average
        }
        return data.values();
    }

    public Collection<ScatterData> calchScatterPlotData(double speedBin, double distBin, boolean speed) {
        Map<String, ScatterData> data = new HashMap<>();
        String key;
        int speedCell;
        int distCell;
        ScatterData tmp;
        for (int i = 0; i < this.data.length; ++i) {
            for (int j = 0; j < this.data[i].length; ++j) {
                if (i == j | this.data[i][j].yVal <= 0) {
                    continue;
                }
                if (speed) {
                    speedCell = (int) (this.data[i][j].getKMH() / speedBin);
                } else {
                    speedCell = (int) (this.data[i][j].yVal / speedBin);
                }

                distCell = (int) (this.data[i][j].xVal / distBin);
                if ((distCell * distBin == 11560 || distCell * distBin == 11910) && speedCell * speedBin == 3850) {
                    System.out.println("Arg");
                }
                key = String.format("%d-%d", speedCell, distCell);
                if (data.containsKey(key)) {
                    tmp = data.get(key);
                } else {
                    tmp = new ScatterData();
                    tmp.speed = speedCell * speedBin;
                    if (tmp.speed > 1000) {
                        tmp.speed = tmp.speed; //TODO
                        speedCell = (int) this.data[i][j].getKMH();
                    }
                    tmp.dist = distCell * distBin;
                    tmp.num = 0;
                }
                tmp.num += 1;
                data.put(key, tmp);
            }
        }
        return data.values();
    }

    /**
     * Method to build an ordered list of outliers. An outlier deviates by a given Threshold between the x and y values.
     *
     * @param maxThreshold
     * @return
     */
    public List<OutlierEntry> findOutliers(double maxThreshold) {

        Map<Integer, OutlierEntry> outliers = new HashMap<>();
        double ratio;
        for (int i = 0; i < this.data.length; ++i) {
            for (int j = 0; j < this.data[i].length; ++j) {
                if (i == j) //skip diagonal!
                    continue;
                if (data[i][j].xVal > data[i][j].yVal) {
                    ratio = data[i][j].xVal / data[i][j].yVal;
                } else {
                    ratio = data[i][j].yVal / data[i][j].xVal;
                }

                if (Double.isInfinite(ratio)) continue;

                if (ratio > maxThreshold) {
                    OutlierEntry from = outliers.get(i);
                    OutlierEntry to = outliers.get(j);
                    if (from == null) {
                        from = new OutlierEntry(i);
                        outliers.put(i, from);
                    }
                    if (to == null) {
                        to = new OutlierEntry(j);
                        outliers.put(j, to);
                    }
                    from.addOutlier(ratio);
                    to.addOutlier(ratio);
                }
            }
        }
        List<OutlierEntry> resultSet = new ArrayList<>(outliers.values());
        Collections.sort(resultSet);
        return resultSet;
    }

    public List<TravelData> findStrangePairs(boolean speed, double maxThreshold, double minThreshold) {
        List<TravelData> returnVal = new LinkedList<>();
        double val;
        for (int i = 0; i < this.data.length; ++i) {
            for (int j = 0; j < this.data[i].length; ++j) {
                if (i == j | this.data[i][j].yVal <= 0) continue;
                if (speed) val = this.data[i][j].getKMH();
                else val = this.data[i][j].xVal / this.data[i][j].beeLine;
                if (val < minThreshold || val > maxThreshold) {
                    returnVal.add(this.data[i][j]);
                }
            }
        }
        return returnVal;
    }

    public double[][] loadMatrix(String table, String name) {
        String query = "";
        ResultSet rs;
        double[][] returnVal = new double[0][0];
        try {
            query = "SELECT matrix_values from " + table + " where matrix_name = '" + name + "'";
            rs = this.dbCon.executeQuery(query, this);
            if (rs.next()) {
                int[] val = TPS_DB_IO.extractIntArray(rs, "matrix_values");
                returnVal = array1Dto2D(val);
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnVal;
    }

    public double[][] loadMatrixFromFile(String name) {
        double[][] retVal = new double[0][0];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(name));
            int[] values = new int[0];
            int k = 0;
            String bla = "start";
            do {
                bla = reader.readLine();
                if (bla != null) {
                    String[] blub = bla.split(",");
                    if (k == 0) {
                        values = new int[blub.length * blub.length];
                    }
                    for (String s : blub) {
                        values[k++] = (Integer.parseInt(s));
                    }
                }
            } while (bla != null);
            retVal = array1Dto2D(values);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public void loadTAZ(String table) {
        String query = "";
        ResultSet rs;
        try {
            query = "SELECT taz_id, st_x(taz_coordinate), st_y(taz_coordinate) from " + table + " order by taz_id";
            rs = this.dbCon.executeQuery(query, this);
            int num = 0;
            while (rs.next()) {
                TAZ newTaz = new TAZ();
                newTaz.id = rs.getInt("taz_id");
                newTaz.x = rs.getDouble("x");
                newTaz.y = rs.getDouble("y");
                tazes.put(num, newTaz);
                num++;
            }
            rs.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void loadTAZInfos(String tazTable, String matrixTable, String distName, String ttName, String accessName, String egressName) {
        this.tazes.clear();
        this.loadTAZ(tazTable);
        double[][] times = this.loadMatrix(matrixTable, ttName);
        if (accessName != null) {
            double[][] acc = this.loadMatrix(matrixTable, accessName);
            this.addMatrix(times, acc);
        }
        if (egressName != null) {
            double[][] egr = this.loadMatrix(matrixTable, egressName);
            this.addMatrix(times, egr);
        }
        double[][] dists = this.loadMatrix(matrixTable, distName);
        //double times[][] = this.loadMatrixFromFile(ttName);
        //double dists[][] = this.loadMatrixFromFile(distName);
        this.data = new TravelData[this.tazes.size()][this.tazes.size()];
        for (int i = 0; i < this.data.length; ++i) {
            TAZ from = this.tazes.get(i);
            for (int j = 0; j < this.data[i].length; ++j) {
                TAZ to = this.tazes.get(j);
                this.data[i][j] = new TravelData();
                this.data[i][j].from = i;
                this.data[i][j].to = j;
                this.data[i][j].yVal = times[i][j];
                this.data[i][j].xVal = dists[i][j];
                this.data[i][j].beeLine = TPS_Geometrics.getDistance(from.x, from.y, to.x, to.y);
            }
        }
    }

    public void writeScatterData(String filename, Collection<ScatterData> data) {
        FileWriter fw;
        try {
            fw = new FileWriter(new File(filename));
            for (ScatterData d : data) {
                fw.write(d.toString());
            }

            fw.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }


    enum STATVAL {MIN, MAX, AVG, STDDEV}

    class TravelData {
        int from = -1, to = -1;
        double xVal = 0, //usually this is distance
                beeLine = 0, yVal = 0; //usually this is time

        public double getKMH() {
            return xVal * 3.6 / yVal;
        }

        public String toString() {
            return "from: " + from + " to: " + to + " dist: " + xVal + " travel time: " + yVal + " beeline: " +
                    beeLine + " speed: " + this.getKMH() + " ratio: " + (xVal / beeLine);
        }
    }

    class TAZ {
        int id = 0;
        double x = 0, y = 0;
    }

    class OutlierEntry implements Comparable<OutlierEntry> {
        public int taz = 0, count = 0;
        public double ratio = 1;

        public OutlierEntry(int id) {
            this.taz = id;
        }

        public void addOutlier(double ratio) {
            this.ratio = ((this.ratio * this.count) + ratio) / (this.count + 1);
            this.count++;
        }

        public int compareTo(OutlierEntry arg0) {
            return this.count - arg0.count;
        }

    }

    class ScatterData {
        double speed, dist;
        int num = 0;

        public String toString() {
            return String.format("%d\t%d\t%d\n", (int) dist, (int) speed, num);
        }
    }

}
