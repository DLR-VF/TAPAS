package de.dlr.ivf.tapas.daemon.task;

import de.dlr.ivf.tapas.environment.dao.ServersDao;
import de.dlr.ivf.tapas.environment.dao.exception.DaoReadException;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import lombok.Getter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.System.Logger.Level;

@Getter
public class ServerStateMonitor implements Runnable{

    private final Lock lock = new ReentrantLock();
    private final Condition serverShutDownSignal = lock.newCondition();
    private final Condition serverStartSignal = lock.newCondition();
    private final Condition serverSleepingSignal = lock.newCondition();

    private final ServersDao serversDao;
    private final ServerEntry serverEntry;

    private final System.Logger logger = System.getLogger(ServerStateMonitor.class.getName());

    public ServerStateMonitor(ServersDao serversDao, ServerEntry serverEntry){
        this.serversDao = serversDao;
        this.serverEntry = serverEntry;
    }


    @Override
    public void run() {
        try {
            serversDao.getByIp(serverEntry.getServerIp()).ifPresentOrElse(
                    this::updateState,
                    () -> logger.log(Level.WARNING,"Server {0} not present in the environment.",serverEntry.getServerIp())
            );


        } catch (DaoReadException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void updateState(ServerEntry newServerEntry){

        //the state has been externally modified, from the gui or daemon for example
        if(this.serverEntry.getServerState() != newServerEntry.getServerState()){
            try{
                lock.lock();
                logger.log(Level.INFO,"Server state has changed");
                serverEntry.setServerState(newServerEntry.getServerState());

            }finally {
                serverStartSignal.signalAll();
                lock.unlock();
            }
        }
    }
}
