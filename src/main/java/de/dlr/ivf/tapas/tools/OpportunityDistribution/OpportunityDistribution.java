package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.File;
import java.io.IOException;

public class OpportunityDistribution {

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();
        parameterClass.loadRuntimeParameters(new File("T:\\Simulationen\\runtime.csv"));
        TPS_DB_Connector dbCon = new TPS_DB_Connector(parameterClass);
        //GroupToTAZ grouper = new GroupToTAZ(dbCon);
        //grouper.importData("12 Bezirke", "berlin", "D:\\tmp\\OpportunityDistribution\\bezirkeToTAZ.csv");
        GroupToDistribute worker = new GroupToDistribute(dbCon, args[0]);
        worker.initRegionsFromTazGrouper(args[1]);
        worker.processOpportunities(args[1]);
        worker.exportOpportunityOutputToCSV(args[2]);
        worker.adaptCapacities();
        worker.exportUpdatedCapacitiesToCSV(args[3]);

    }

}
