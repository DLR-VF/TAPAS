package de.dlr.ivf.tapas.environment.gui.fx.model.implementation;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.dto.SimulationEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.SimulationsModel;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class SimulationsModelManager implements SimulationsModel {

    private final TapasEnvironment tapasEnvironment;

    private final Map<Integer, SimulationEntry> simulationEntries;

    private final ReadLock readLock;

    private final WriteLock writeLock;

    public SimulationsModelManager(TapasEnvironment tapasEnvironment){
        this.simulationEntries = new HashMap<>();
        this.tapasEnvironment = tapasEnvironment;

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public Collection<SimulationEntry> getSimulations() {

        try{
            readLock.lock();
            return simulationEntries.values();
        }finally {
            readLock.unlock();
        }
    }

    @Override
    public SimulationEntry getSimulation(int simId) {
        return this.simulationEntries.get(simId);
    }

    @Override
    public Collection<SimulationEntry> reload() throws IOException{

        try{
            writeLock.lock();

            Map<Integer, SimulationEntry> simulations = tapasEnvironment.loadSimulations();

            this.simulationEntries.clear();
            this.simulationEntries.putAll(simulations);
        } finally{
            writeLock.unlock();
        }

        return simulationEntries.values();
    }

    @Override
    public void insert(File simulationFile) throws IOException{
        try {
            writeLock.lock();
            SimulationEntry simulationEntry = tapasEnvironment.addSimulation(simulationFile);
            this.simulationEntries.put(simulationEntry.getId(), simulationEntry);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(Collection<SimulationEntry> simulationsToRemove) throws IOException {
        try{
            writeLock.lock();
            for(SimulationEntry simulationEntry : simulationsToRemove){
                simulationEntries.remove(simulationEntry.getId());
                tapasEnvironment.removeSimulation(simulationEntry);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
