#!/bin/sh
# daemon.sh

JAVA_HOME=/bin/java
TAPAS_JAR=build/libs/TAPAS-all.jar
SIM_DIR=data/Simulations

$JAVA_HOME -cp $TAPAS_JAR de.dlr.ivf.tapas.runtime.server.SimulationDaemon $SIM_DIR