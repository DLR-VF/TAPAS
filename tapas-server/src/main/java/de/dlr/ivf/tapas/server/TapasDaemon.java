package de.dlr.ivf.tapas.server;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.atomic.AtomicBoolean;

import de.dlr.ivf.tapas.server.services.StateMonitor;
import de.dlr.ivf.tapas.environment.model.ServerState;
import lombok.Builder;

/**
 * The TAPAS daemon is responsible for handling a {@link TapasServer} based on {@link ServerState} changes obtained
 * by a background task.
 */
@Builder
public class TapasDaemon implements Runnable{

    private final Logger logger = System.getLogger(TapasDaemon.class.getName());

    private final StateMonitor<ServerState> stateMonitor;
    private final TapasServer tapasServer;
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);

    @Override
    public void run() {

        Thread shutdownThread = new Thread(new DaemonShutDownStrategy(stateMonitor, tapasServer));
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        logger.log(Level.INFO,"Running...");
        stateMonitor.start();

        while (keepRunning.get()) {

            try {
                ServerState serverState = stateMonitor.awaitStateChange(); //blocking call
                switch (serverState) {
                    case STOP -> stopServer();
                    case RUN -> startServer();
                }

            } catch (InterruptedException ignored) {
                //nothing to do
            }
        }
    }

    private void startServer() {
        logger.log(Level.INFO,"Starting Tapas server.");
        tapasServer.start();
    }

    private void stopServer() {
        logger.log(Level.INFO,"Stopping Tapas server.");
        tapasServer.stop();
    }
}
