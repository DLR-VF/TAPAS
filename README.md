#TAPAS (Travel Activity Pattern Simulation)


for more information see https://wiki.dlr.de/display/MUD/TAPAS

## Table of Contents

- [Download](#download)
- [Install](#install)
    - [Requirements](#requirements)
    - [Basic Postgres DB Setup](#basic-postgres-db-setup)
    - [Basic Files and Folders](#basic-files-and-folders)
    - [Installer](#installer)
- [Usage](#usage)
    - [Execution through the Command-Line](#execution-through-the-command-line)
        - [SimulationControl](#simulationcontrol)
        - [SimulationDaemon](#simulationdaemon)
    - [Using Script Files .sh and .bat (TODO)](#using-script-files-sh-and-bat)
    - [Developer Guide with Gradle](#developer-guide-with-gradle)
        - [Gradle Command-Line](#gradle-command-line)
        - [Import Project via IntelliJ](#import-project-via-intellij)
        - [Import Project via Eclipse (TODO)](#import-project-via-eclipse)
- [License](#license)
- [Contributors](#contributors)
- [Publications](#publications)
- [Contact](#contact)
    
    
## Download

## Install

### Requirements 
 - Installed and running Postgres 11+ server with 
    - Postgis 3 extension
    - Database Drivers: pgJDBC and pgODBC (32 and/or 64 bit depending on your build target) 
 - Java 11+ (for development options you need the JDK of course)
 - Git if you want to clone this repository


### Basic Postgres DB Setup:

This only applies if you don't have an already running Postgres/Postgis system. 
 
 Get [Postgres 11](https://www.postgresql.org/) for your operating system and install it. Also, add the [Postgis](https://postgis.net/install/) extension to your postgres server. 


Initialize local DB Server (if not happened already):

  `initdb[.exe] -D path/to/data/dir -E UTF8 -U DEFAULTSUPERUSER --pwprompt`
  
  `DEFAULTSUPERUSER` should probably be 'postgres', but you can set whatever you want. 
  
Start DB server: 

    pg_ctl[.exe] -D path/to/data/dir -l /path/to/log/file start
    
### Basic Files and Folders

Clone (or fork) the repository from GitHub

    git clone https://github.com/DLR-VF/TAPAS.git

or download the specific files you want.  

Some basic information about important files of the repository:  
- [TAPAS-\<version\>-all.jar](https://github.com/DLR-VF/TAPAS/packages/202073): the main jar for TAPAS. Important is the
 ...-_all_.jar because it includes all dependencies
- [sql_dumps.zip](https://github.com/DLR-VF/TAPAS/blob/master/sql_dumps.zip): Minimal database dump: leave the zip as is
 for the Installer
- [data/Simulations](https://github.com/DLR-VF/TAPAS/tree/master/data/Simulations): Runtime data archive: exemplary set 
of scenario and runtime files (needed for SimulationDaemon and SimulationControl); these files contain the login and 
database parameter settings
- [src](https://github.com/DLR-VF/TAPAS/tree/master/src): Source folder
- [gradle/wrapper](https://github.com/DLR-VF/TAPAS/tree/master/gradle/wrapper): Gradle wrapper folder for executing task
 through gradle; should not be changed 
- [build.gradle.kts](https://github.com/DLR-VF/TAPAS/blob/master/build.gradle.kts): Main Gradle file for defining the 
gradle tasks and more

Recommendation: Place the still compressed `sql_dumps.zip` archive, in your current working directory with your `TAPAS-<version>-all.jar `

### Installer 
The Installer creates a minimal functioning install of a TAPAS database on your Postgres server. It needs four
parameters to work, _`dbserver`_, _`dbname`_, _`dbuser`_, and _`dbpassword`_. See 

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.installer.Installer --help

   
for more information. The _`dbuser`_ must be a superuser or a user with
sufficient rights on the postgres server. 
 
Additionally, there must be an sql_dumps.zip archive in your
current active directory. If not, you will be asked for the archive through a prompt.
 
Start the Installer with  

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.installer.Installer
or 

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.installer.Installer --dbserver=localhost --dbname=tapas --dbuser=postgres --dbpassword=postgres

The commandline arguments are optional. You can omit them, but then you will be prompted during the Installer run.

Note: This is the recommended way to running the Installer because in this case you can omit the password argument in
 the command line. You will be then prompted during the run of the script with a secret input of the db password. If
  you run this script through [Gradle](#developer-guide-with-gradle) or an [IDE](#import-project-via-intellij) it is necessary to deliver the argument beforehand because Java does
   not recognize their standard command line input.  
 

## Usage 

### Execution through the Command-Line 

There are several ways to execute the Installer, SimulationControl and the SimulationDaemon. 
The most basic one is through the command-line. Other options are via Gradle through the command-line or through IntelliJ IDEA or Eclipse. 

#### SimulationControl

The SimulationControl center is a gui tool for managing your simulations and your simulation processes.
If not already present it asks for a [`runtime.csv`](https://github.com/DLR-VF/TAPAS/blob/master/data/Simulations/runtime.csv) which contains the db server login information and more. 
An exemplary file is located in the [`data/Simulations`](https://github.com/DLR-VF/TAPAS/blob/master/data/Simulations) folder.   

Start SimulationControl:

    `java -jar TAPAS-<version>-all.jar`
    
or

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.runtime.client.SimulationControl


#### SimulationDaemon 

The SimulationDaemon connects to the db server and starts a SimulationServer which simulates the scenarios. 
You need to specify the path to a simulation folder (in the TAPAS project is an example folder [`data
/Simulations`](https://github.com/DLR-VF/TAPAS/blob/master/data/Simulations)) where a [`runtime.csv`](https://github.com/DLR-VF/TAPAS/blob/master/data/Simulations/runtime.csv) is present. 

Start SimulationDaemon: 

    java -cp TAPAS-<version>-all.jar de.dlr.ivf.tapas.runtime.server.SimulationDaemon path/to/simulations/folder

### Using Script Files .sh and .bat

ToDo

### Developer Guide with Gradle 

Clone (or fork) the whole repository from GitHub

    git clone https://github.com/DLR-VF/TAPAS.git

#### Gradle Command-Line
 
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

#### Import Project via Eclipse

TODO

## License

[MIT](LICENSE) 

## Contributors

[Contributors](https://github.com/DLR-VF/TAPAS/graphs/contributors)

## Publications

## Contact
DLR-VF
- Homepage: [TAPAS-Homepage (in German)](https://www.dlr.de/vf/desktopdefault.aspx/tabid-12751/22270_read-29381/)
- e-mail: 
- Youtube: [VF-MUD-G3](https://www.youtube.com/channel/UC1kdpWq3RDO4MAh5UREwyQw/)
- Twitter (for general transport topics): [@DLR_Verkehr](https://twitter.com/DLR_Verkehr)