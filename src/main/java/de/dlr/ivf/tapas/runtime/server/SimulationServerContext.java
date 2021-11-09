package de.dlr.ivf.tapas.runtime.server;

import de.dlr.ivf.tapas.runtime.util.IPInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

public class SimulationServerContext {

    private final String hostname;
    private final InetAddress ip;
    private final int core_count;
    private SimulationServer running_server = null;

    private SimulationServerContext(String hostname, InetAddress ip, int core_count){
        this.hostname = hostname;
        this.ip = ip;
        this.core_count = core_count;
    }


    public static SimulationServerContext newLocalServerContext() {

        //resolve hostname
        String host_name = "Unknown";

        try {
             host_name = IPInfo.getHostname();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //resolve ip
        InetAddress ip = null;
        try {
            ip =  IPInfo.getEthernetInetAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int core_count = Runtime.getRuntime().availableProcessors();

        return new SimulationServerContext(host_name, ip, core_count);
    }

    public String getHostname(){
        return this.hostname;
    }

    public InetAddress getIp(){
        return this.ip;
    }

    public int getCoreCount(){
        return this.core_count;
    }

    public Optional<SimulationServer> getRunningServer(){
        return Optional.ofNullable(this.running_server);
    }

    public void setRunningServer(SimulationServer server){
        this.running_server = server;
    }
}
