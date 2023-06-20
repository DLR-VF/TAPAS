package de.dlr.ivf.tapas.environment.gui.fx.model.implementation;

import de.dlr.ivf.tapas.environment.TapasEnvironment;
import de.dlr.ivf.tapas.environment.dto.ServerEntry;
import de.dlr.ivf.tapas.environment.gui.fx.model.ServersModel;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ServersModelManager implements ServersModel {
    private final TapasEnvironment tapasEnvironment;

    private final Map<String, ServerEntry> serverEntries;

    private final ReadLock readLock;

    private final WriteLock writeLock;
    public ServersModelManager(TapasEnvironment tapasEnvironment) {
        this.serverEntries = new HashMap<>();
        this.tapasEnvironment = tapasEnvironment;

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public Collection<ServerEntry> getServerData() {
        try{
            readLock.lock();
            return serverEntries.values();
        }finally {
            readLock.unlock();
        }
    }

    @Override
    public ServerEntry getServer(String serverIp) {
        return this.serverEntries.get(serverIp);
    }

    @Override
    public Collection<ServerEntry> reload() throws IOException {
        try{
            writeLock.lock();

            Map<String, ServerEntry> servers = tapasEnvironment.loadServers();

            this.serverEntries.clear();
            this.serverEntries.putAll(servers);

            return serverEntries.values();
        } finally{
            writeLock.unlock();
        }
    }

    @Override
    public void remove(Collection<ServerEntry> serversToRemove) throws IOException {

    }

    @Override
    public void insert(ServerEntry server) {
        //dao.
    }
}
