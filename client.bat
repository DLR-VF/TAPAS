set MAIN=de.dlr.ivf.tapas.runtime.client.SimulationControl
set JAR=build/libs/TAPAS-all.jar
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

echo %JAVA_EXE% -Dfile.encoding=UTF-8 -cp "%JAR%" %MAIN%

%JAVA_EXE% -Dfile.encoding=UTF-8 -cp "%JAR%" %MAIN%
