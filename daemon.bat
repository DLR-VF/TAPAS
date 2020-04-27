set MAIN=de.dlr.ivf.tapas.runtime.server.SimulationDaemon
set JAR=build/libs/TAPAS-all.jar
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
set JAVA_ARGS=-Xmx64g
set RUNTIME_DIR=%CD%\data\Simulations

%JAVA_EXE% %JAVA_ARGS% -cp "%JAR%" %MAIN% %RUNTIME_DIR%
