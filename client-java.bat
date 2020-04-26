set MAIN=de.dlr.ivf.tapas.runtime.client.SimulationControl
set JAR=build/libs/TAPAS-all.jar
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

echo java -Dfile.encoding=UTF-8 -cp "%JAR%" %MAIN% %CD%

%JAVA_EXE% -Dfile.encoding=UTF-8 -cp "%JAR%" %MAIN% %CD%
