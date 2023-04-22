set MAIN=de.dlr.de.dlr.ivf.server.runtime.tapas.ivf.SimulationDaemon
set JAR=build/libs/TAPAS-0.2.0-SNAPSHOT-all.jar
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
set JAVA_ARGS=-Xmx8g
set RUNTIME_DIR=D:\TAPAS\Simulations

%JAVA_EXE% %JAVA_ARGS% -cp "%JAR%" %MAIN% %RUNTIME_DIR%
