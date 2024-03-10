module de.dlr.ivf.tapas.model {

    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires lombok;


    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;
    requires javacsv;
    requires java.sql;
    exports de.dlr.ivf.tapas.model;
    exports de.dlr.ivf.tapas.model.person;
    exports de.dlr.ivf.tapas.model.constants;
    exports de.dlr.ivf.tapas.model.mode;
    exports de.dlr.ivf.tapas.model.location;
    exports de.dlr.ivf.tapas.model.scheme;
    exports de.dlr.ivf.tapas.model.distribution;
    exports de.dlr.ivf.tapas.model.parameter;
    exports de.dlr.ivf.tapas.model.plan;
    exports de.dlr.ivf.tapas.model.vehicle;
    exports de.dlr.ivf.tapas.model.plan.acceptance;
    exports de.dlr.ivf.tapas.model.choice;

}