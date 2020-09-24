set MAIN=de.dlr.ivf.tapas.runtime.client.SimulationControl
set JAR=build/libs/TAPAS-0.2.0-SNAPSHOT-all.jar
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

echo %JAVA_EXE% -Dfile.encoding=UTF-8 -cp "%JAR%" %MAIN%

D:\Java\jdk-12\bin\java -Dfile.encoding=UTF-8 -cp "%JAR%" %MAIN%
