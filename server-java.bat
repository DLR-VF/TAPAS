set MAIN=de.dlr.ivf.tapas.runtime.server.SimulationDaemon
set JAR=build/libs/TAPAS-all.jar
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

start /BELOWNORMAL %JAVA_EXE% -Xmx32g -cp "%JAR%" %MAIN% %CD%\data\Simulations
rem java -Xmx4g -cp "%jar%;%ext_jar%;." %main% %CD%

@rem pause