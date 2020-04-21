#TAPAS (Travel Activity Pattern Simulation)

for more information see https://wiki.dlr.de/display/MUD/TAPAS

## Requirements: 
 - Installed and running Postgres 11 server with the Postgis 3 extension
 - Java 11+
 
### Basic Postgres DB Setup:

This only applies if you don't have an already running Postgres/Postgis system. 
 
 Get [Postgres 11+](https://www.postgresql.org/) for your operating system and install it. 
 Also get and install [Postgis](https://postgis.net/install/) for your system where installed the postgres server. 


Initialize local DB Server:

  `initdb[.exe] init -D path/to/data/dir -E UTF8 -U DEFAULTSUPERUSER --pwprompt`
  'DEFAULTSUPERUSER' should probably be 'postgres' but you can set whatever you want. 
  
Start DB server: 

    pg_ctl[.exe] -D path/to/data/dir -l /path/to/log/file start
    


##Execution  

There are two ways to execute the Installer, SimulationControl and the SimulationDaemon: Through Gradle (if you
 cloned the git repository) or via the command line (terminal):
 
### Command Line
 
The Installer creates a minimal functioning install of a TAPAS database on your Postgres server. It needs four
parameters to work, _dbserver_, _dbname_, _dbuser_, and _dbpassword_. See 

    java -cp tapas-all.jar de.dlr.ivf.tapas.installer.Installer --help

   
for more information. The _dbuser_ must be a superuser or a user with
sufficient rights on the postgres server. 
 
Additionally, there must be an sql_dumps.zip archive in your
current active directory. If not, you will be asked for the archived directory via prompt.
 
Start Installer:

    java -cp tapas-all.jar de.dlr.ivf.tapas.installer.Installer [--dbserver=localhost --dbname=tapas_db --dbuser=postgres --dbpassword=postgres]

The commandline arguments are optional. You can omit them, but then you will be prompted during the Installer run.
 

Start SimulationControl:

    `java -jar TAPAS-all.jar`
    
or

    java -cp TAPAS-all.jar de.dlr.ivf.tapas.runtime.client.SimulationControl
 
Start SimulationDaemon: 

    java -cp TAPAS-all.jar de.dlr.ivf.tapas.runtime.server.SimulationDaemon ..\tdatadir\Simulationen

### Gradle

If you have cloned the git repository you can execute the java programs through gradle (tasks).

Execute Installer with Gradle: 

    gradlew[.bat] Installer --args="--dbserver=localhost --dbname=tapas_db --dbuser=postgres --dbpassword=postgres"
 
The commandline arguments must be supplied through the Gradle properties via -P, e.g. -Pmyproperty. You cannot enter
 the arguments during the Installer run because Gradle (and other IDEs) do not work with the standard input.
 
SimulationControl: `gradlew[.bat] SimulationControl`
 
SimulationDaemon:  `gradlew[.bat] SimulationDaemon --args=""`
 