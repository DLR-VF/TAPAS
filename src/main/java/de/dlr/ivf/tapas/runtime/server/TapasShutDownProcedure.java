package de.dlr.ivf.tapas.runtime.server;

import java.util.List;

public class TapasShutDownProcedure extends Thread {

    private final List<ShutDownable> shutDownables;
    public TapasShutDownProcedure(List<ShutDownable> shutdownable_services) {
        this.shutDownables = shutdownable_services;
    }

    @Override
    public void run() {
        shutDownables.forEach(ShutDownable::shutdown);
    }
}
