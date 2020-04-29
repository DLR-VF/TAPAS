#TAPAS (Travel Activity Pattern Simulation)

for more information see https://wiki.dlr.de/display/MUD/TAPAS

## Requirements: 
 - Installed and running Postgres 11 server with the Postgis 3 extension
 - Java 11
 
Furthermore from the Github repository you need to download from the packages section: 
- [TAPAS-version-all.jar](https://github.com/DLR-VF/TAPAS/packages/202073) (the main jar for TAPAS. Important is the
 ...-_all_.jar because it includes all dependencies )
- [sql_dumps.zip](https://github.com/DLR-VF/TAPAS/blob/master/sql_dumps.zip) (Minimal database dump: leave the zip as
 is for the Installer)
- [runtime_data.zip](https://github.com/DLR-VF/TAPAS/blob/master/runtime_data.zip) (Runtime data archive: decompress the archive
 for an exemplary set of scenario and runtime files (needed for SimulationDaemon and SimulationControl))

 Recommendation: Place both, the still compressed `sql_dumps.zip` archive and the `data` folder (extracted from runtime_data
 .zip), in your current working directory with your `TAPAS-<version>-all.jar `
 
### Basic Postgres DB Setup:

This only applies if you don't have an already running Postgres/Postgis system. 
 
 Get [Postgres 11](https://www.postgresql.org/) for your operating system and install it. 
 Also get and install [Postgis](https://postgis.net/install/) for your system where installed the postgres server. 


Initialize local DB Server:

  `initdb[.exe] -D path/to/data/dir -E UTF8 -U DEFAULTSUPERUSER --pwprompt`
  
  `DEFAULTSUPERUSER` should probably be 'postgres', but you can set whatever you want. 
  
Start DB server: 

    pg_ctl[.exe] -D path/to/data/dir -l /path/to/log/file start
    

 
##Execution  

There are several ways to execute the Installer, SimulationControl and the SimulationDaemon
 
### Command-Line
 
#### Installer 
The Installer creates a minimal functioning install of a TAPAS database on your Postgres server. It needs four
parameters to work, _`dbserver`_, _`dbname`_, _`dbuser`_, and _`dbpassword`_. See 

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.installer.Installer --help

   
for more information. The _`dbuser`_ must be a superuser or a user with
sufficient rights on the postgres server. 
 
Additionally, there must be an sql_dumps.zip archive in your
current active directory. If not, you will be asked for the archive through a prompt.
 
Start Installer:

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.installer.Installer
or

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.installer.Installer --dbserver=localhost --dbname=tapas --dbuser=postgres --dbpassword=postgres

The commandline arguments are optional. You can omit them, but then you will be prompted during the Installer run.

Note: This is the recommended way to running the Installer because in this case you can omit the password argument in
 the command line. You will be then prompted during the run of the script with a secret input of the db password. If
  you run this script through Gradle or an IDE it is necessary to deliver the argument beforehand because Java does
   not recognize their standard command line input.  
 

#### SimulationControl

The SimulationControl center is a gui tool for managing your simulations and your simulation processes.
If not already present it asks for a `runtime.csv` which contains the db server login information and more.  

Start SimulationControl:

    `java -jar TAPAS-<version>-all.jar`
    
or

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.runtime.client.SimulationControl


#### SimulationDaemon 

The SimulationDaemon connects to the db server and starts a SimulationServer which simulates the scenarios. 
You need to specify the path to a simulations folder (in the TAPAS project is an example folder can be found under `data
/Simulations`) where a `runtime.csv` is present. 

Start SimulationDaemon: 

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.runtime.server.SimulationDaemon path/to/simulations/folder

#### Using Script Files .sh and .bat

ToDo

### Developer Guide with Gradle 

#### Clone (or fork) the Repository from GitHub

    git clone https://github.com/DLR-VF/TAPAS.git

#### Import Project via IntelliJ

File -> Open... -> Either choose the cloned TAPAS folder or the build.gradle.kts file to import the project. The
 dependencies should be downloaded automatically.
 
In the Gradle menu in IntelliJ (either on the right side or press double shift -> type in Gradle -> Under Actions -> 
 Gradle) you find the most important tasks: 
- Build project: `TAPAS -> Tasks -> build -> build` (the `TAPAS-<version>-all.jar` jar is located in `build/libs/`)
- Clean project: `TAPAS -> Tasks -> build -> clean`  
- Run Installer script : `TAPAS -> Tasks -> runnables -> Installer` (set arguments in build.gradle.kts in the Installer section) 
- Run SimulationControl: `TAPAS -> Tasks -> runnables -> SimulationControl` 
- Run SimulationDaemon: `TAPAS -> Tasks -> runnables -> SimulationDaemon` (expects the simulation directory to be in
 `data/Simulations`)

#### Eclipse

TODO


#### Command-Line
 
Note: The gradle executable is called `gradlew`. On Linux one uses `./gradlew` to execute it in a terminal, on Windows
 it is `gradlew.bat`. Examples are given in Linux command style.  

+ You can compile the project and build the `TAPAS.jar` (without dependencies)/`TAPAS-<version>-all.jar` (with deps) through

    `./gradlew build`
        
+ Cleanup the project: `./gradlew clean`     

+ Execute Installer with Gradle: `./gradlew Installer --args="--dbserver=localhost --dbname=tapas --dbuser=postgres --dbpassword=postgres"`
  
  The commandline arguments must be supplied through the `--args="..."` parameter. You cannot enter
 the arguments during the Installer run because Gradle (and other IDEs) do not work with the standard input.
 
+ SimulationControl: `./gradlew SimulationControl`
 
+ SimulationDaemon:  `./gradlew SimulationDaemon --args="path/to/simulations/folder"`
 
#### Publication to Github with Gradle

ToDo
