package de.dlr.ivf.tapas.util;

public class VirtualThreadFactory {

    private static final Thread.Builder.OfVirtual threadFactory = Thread.ofVirtual().name("VirtualThread-",0);
    public static Thread startVirtualThread(Runnable task){
        return threadFactory.start(task);
    }
}
