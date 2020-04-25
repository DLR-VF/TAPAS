#!/bin/sh
# client.sh

JAVA_HOME=/bin/java
TAPAS_JAR=build/libs/TAPAS-all.jar

$JAVA_HOME -cp $TAPAS_JAR de.dlr.ivf.tapas.runtime.server.SimulationControl