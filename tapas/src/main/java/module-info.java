module de.dlr.ivf.tapas {

    requires java.sql;
    requires java.desktop;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires disruptor;
    requires commons.math3;

    requires org.postgresql.jdbc;
    requires java.rmi;
    requires java.management;
    requires lombok;
    requires com.fasterxml.jackson.databind;

    requires de.dlr.ivf.tapas.logger;
    requires de.dlr.ivf.tapas.parameter;
    requires de.dlr.ivf.tapas.util;
    requires de.dlr.ivf.tapas.model;
    requires de.dlr.ivf.api.io;
    requires de.dlr.ivf.api.converter;
    requires de.dlr.ivf.api.service;
    requires javacsv;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    opens de.dlr.ivf.tapas to com.fasterxml.jackson.databind;
    opens de.dlr.ivf.tapas.dto to de.dlr.ivf.api.io;
    opens de.dlr.ivf.tapas.configuration.spring to spring.core;
    exports de.dlr.ivf.tapas.configuration.spring to spring.beans, spring.context;
    exports de.dlr.ivf.tapas.simulation.implementation to spring.beans;
    exports de.dlr.ivf.tapas.simulation.runner to spring.beans;
    exports de.dlr.ivf.tapas;
    exports de.dlr.ivf.tapas.simulation;
    opens de.dlr.ivf.tapas.simulation to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json;
    opens de.dlr.ivf.tapas.configuration.json to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.runner to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.trafficgeneration to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.locationchoice to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.modechoice to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.agent to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.region to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.region.matrix to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.acceptance to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.configuration.json.util to com.fasterxml.jackson.databind;
    exports de.dlr.ivf.tapas.simulation.choice.location to spring.beans, spring.context;
    exports de.dlr.ivf.tapas.choice to spring.beans, spring.context;
    opens de.dlr.ivf.tapas.simulation.choice.location to spring.core;
}