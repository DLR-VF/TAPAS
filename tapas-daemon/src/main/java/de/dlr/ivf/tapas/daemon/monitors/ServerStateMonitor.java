package de.dlr.ivf.tapas.daemon.monitors;

import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.model.ServerState;
import lombok.Getter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

@Getter
public class ServerStateMonitor implements Runnable{

    private final Lock lock = new ReentrantLock();
    private final Condition serverStopSignal = lock.newCondition();
    private final Condition serverStartSignal = lock.newCondition();

    private final ServersDao serversDao;
    private final ServerEntry serverEntry;

    private final Logger logger = System.getLogger(ServerStateMonitor.class.getName());

    public ServerStateMonitor(ServersDao serversDao, ServerEntry serverEntry){
        this.serversDao = serversDao;
        this.serverEntry = serverEntry;
    }


    @Override
    public void run() {
        try {
            serversDao.getByIp(serverEntry.getServerIp()).ifPresentOrElse(
                    this::updateServerEntry,
                    () -> logger.log(Level.WARNING,"Server {0} not present in the environment.",serverEntry.getServerIp())
            );

        } catch (DaoReadException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void updateServerEntry(ServerEntry newServerEntry){

        ServerState newServerState = newServerEntry.getServerState();

        //the state has been changed externally
        if(this.serverEntry.getServerState() != newServerState){
            try{
                lock.lock();
                logger.log(Level.INFO,"Server state has changed from {0} to {1}",
                        serverEntry.getServerState(), newServerState);

                updateState(serverEntry, newServerState);
                fireStateChanges(newServerState);

            }finally {
                lock.unlock();
            }
        }
    }

    private void fireStateChanges(ServerState newServerState) {
        switch (newServerState){
            case STOP -> serverStopSignal.signalAll();
            case RUN -> serverStartSignal.signalAll();
        }
    }

    private void updateState(ServerEntry serverEntry, ServerState newServerState) {
        serverEntry.setServerState(newServerState);
    }

    public ServerState getServerState(){
        return this.serverEntry.getServerState();
    }
}
