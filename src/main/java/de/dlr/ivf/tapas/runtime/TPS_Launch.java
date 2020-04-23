package de.dlr.ivf.tapas.runtime;

import de.dlr.ivf.tapas.TPS_Main;

import java.io.File;

public final class TPS_Launch {

    /**
     * @param args - [0] contains TAPAS network directory
     *             [1] contains relative path and filename of sim_file
     *             [2] contains sim_key
     */
    public static void main(String[] args) {

        File file = new File(args[0], args[1]);
        TPS_Main main = new TPS_Main(file, args[2]);
        main.run(Runtime.getRuntime().availableProcessors());
        main.getPersistenceManager().close();
        TPS_Main.STATE.setFinished();

    }

}
