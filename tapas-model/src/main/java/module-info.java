module de.dlr.ivf.tapas.model {

    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;


    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;
    requires javacsv;
    exports de.dlr.ivf.tapas.model;
    exports de.dlr.ivf.tapas.model.person;
    exports de.dlr.ivf.tapas.model.constants;
    exports de.dlr.ivf.tapas.model.mode;
    exports de.dlr.ivf.tapas.model.location;
    exports de.dlr.ivf.tapas.model.scheme;
    exports de.dlr.ivf.tapas.model.implementation.utilityfunction;
    exports de.dlr.ivf.tapas.model.distribution;
    exports de.dlr.ivf.tapas.model.parameter;
    exports de.dlr.ivf.tapas.model.plan;

}