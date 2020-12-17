/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.installer;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Installer scripts for a minimal and exemplary install of a TAPAS database installation
 */
public class Installer {

    private static Connection DBCONNECTION;
    private static String DBSERVER;
    private static String DBNAME;
    private static String DBUSER;
    private static String DBPASSWORD;

    // this must be set to true here otherwise existsDestinationDirectory does not work
    private static boolean DESTINATION_ALREADY_EXISTS = true;

    /**
     * Closes the database connection
     */
    private static void closeConnection() {
        try {
            DBCONNECTION.close();
            System.out.println("Close connection to DB");
        } catch (SQLException e) {
            System.err.println("Could not close connection to DB");
            exitAndCleanUp(e, false);
        }
    }

    /**
     * Takes a directory and concatenates all files in it to one string
     *
     * @param dir string representing a directory
     * @return string composed of all files in the given directory dir
     */
    private static String composeFilesInDir(String dir) {
        StringBuilder bar = new StringBuilder();
        try {
            Files.walk(Paths.get(dir)).filter(Files::isRegularFile).forEach(file -> {
                try {
                    bar.append(Files.readString(file).replaceAll("%DBUSER%", DBUSER));
                } catch (IOException e) {
                    exitAndCleanUp(e, true);
                }
            });
        } catch (IOException e) {
            exitAndCleanUp(e, true);
        }
        return bar.toString();
    }

    /**
     * Connects to a postgres database
     *
     * @param dbname name of the database one wants connect to
     */
    private static void connectToDB(String dbname) {
        try {
            Class.forName("org.postgresql.Driver");
            DBCONNECTION = DriverManager.getConnection("jdbc:postgresql://" + DBSERVER + "/" + dbname, DBUSER,
                    DBPASSWORD);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            exitAndCleanUp(e, false);
        }
        System.out.println("Opened database " + dbname + " successfully");
    }

    /**
     * Creates a new database on the server
     *
     * @param dbname name of the database to be created
     */
    private static void createDB(String dbname) {
        try {
            Statement statement = DBCONNECTION.createStatement();
            statement.executeUpdate("CREATE DATABASE " + dbname);
        } catch (SQLException e) {
            System.err.println("DB already exists? Abort and leave the DB alone");
            exitAndCleanUp(e, false);
        }
    }

    /**
     * Deletes a directory recursively, i.e.
     * at first each file and subfolder in the directory is deleted first
     * This is a necessary condition for the standard file deletion procedure in Java
     *
     * @param directoryToBeDeleted File pointing to the directory to be deleted
     * @return true if deletion was successful
     */
    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    /**
     * Drops newly created database DBNAME if it failed during the process
     */
    private static void dropDBIfFailed() {
        closeConnection();
        connectToDB("postgres");
        Statement stmt;
        try {
            System.out.println("Try to drop newly created database " + DBNAME);
            stmt = DBCONNECTION.createStatement();
            stmt.execute("DROP DATABASE IF EXISTS " + DBNAME);
        } catch (SQLException e) {
            System.err.println("Could not delete database " + DBNAME);
            exitAndCleanUp(e, false);
        }
    }

    /**
     * Execute an sql command on a database
     *
     * @param command sql command to execute
     */
    private static void executeCommand(String command) {
        try {
            Statement stmt = DBCONNECTION.createStatement();
            stmt.execute(command);
        } catch (SQLException e) {
            exitAndCleanUp(e, true);
        }
    }

    /**
     * Executes an array of postgres commands as strings
     *
     * @param commands string array of postgres commands
     */
    private static void executeCommands(String[] commands) {
        try {
            Statement stmt = DBCONNECTION.createStatement();
            Arrays.stream(commands).forEach(it -> {
                try {
                    stmt.addBatch(it);
                } catch (SQLException e) {
                    exitAndCleanUp(e, true);
                }
            });
            stmt.executeBatch();
        } catch (SQLException e) {
            exitAndCleanUp(e, true);
        }
    }

    /**
     * Reads all necessary sql files and executes the command to the database
     *
     * @param destination directory of sql-datasets
     */
    private static void executeCompleteQuery(String destination) {
        executeFile(destination + "/sql_dumps/tapas_deployment.sql");

        //create tapas groups if they do not exist
        System.out.println("Create TAPAS groups if they do not exist yet");
        try {
            Statement stmt = DBCONNECTION.createStatement();
            stmt.execute("DO $$ begin create group tapas_user_group; exception when DUPLICATE_OBJECT then raise " +
                    "notice 'note creating group tapas_user_group'; end $$;");
            stmt.execute("DO $$ begin create group tapas_admin_group; exception when DUPLICATE_OBJECT then raise " +
                    "notice 'note creating group tapas_admin_group'; end $$;");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // core tables
        executeDir(destination + "/sql_dumps/core_tables/core_tables_1");
        executeDir(destination + "/sql_dumps/core_tables/core_tables_2");
        // types
        executeDir(destination + "/sql_dumps/types/core");
        executeDir(destination + "/sql_dumps/types/public");
        // functions, they have to be handled separately
        String functionFilesComposed = composeFilesInDir(destination + "/sql_dumps/functions");
        List<String> functionStringList = parseFunctionFile(functionFilesComposed);
        executeCommands(functionStringList.toArray(new String[0]));

        // second core tables, because they need the functions first
        executeDir(destination + "/sql_dumps/core_tables/core_tables_3");

        // public tables
        executeDir(destination + "/sql_dumps/public_tables");

        executeCommands(
                new String[]{"set search_path = \"public\";", "select core.create_region_based_tables('berlin', 'core');"});

        // berlin sample data
        executeDir(destination + "/sql_dumps/berlin/berlin_1");
        executeDir(destination + "/sql_dumps/berlin/berlin_2");
        executeDir(destination + "/sql_dumps/berlin/berlin_3");
    }

    /**
     * Walks over each sql-file in a directory and executes all sql statements
     *
     * @param dir string representing a directory
     */
    private static void executeDir(String dir) {
        try {
            Files.walk(Paths.get(dir)).filter(Files::isRegularFile).forEach(Installer::executeFile);
        } catch (IOException e) {
            exitAndCleanUp(e, true);
        }
    }

    /**
     * Reads an sql-file and executes each command. For most files the commands will be executed in batches through
     * the executeCommands method. For big files like core_berlin_matrices.sql each command is executed separately
     * because of memory overhead.
     *
     * @param file file file name as a string
     */
    private static void executeFile(String file) {
        executeFile(Paths.get(file));
    }

    /**
     * Reads an sql-file and executes each command. For most files the commands will be executed in batches through
     * the executeCommands method. For big files like core_berlin_matrices.sql each command is executed separately
     * because of memory overhead.
     *
     * @param path path object pointing to a file
     */
    private static void executeFile(Path path) {
        System.out.println("Executing: " + path.getFileName());
        try {
            executeCommands(Files.readString(path).replaceAll("%DBUSER%", DBUSER).split(";"));
        } catch (IOException e) {
            exitAndCleanUp(e, true);

        }
    }

    /**
     * Method to exit the program as cleanly as possible when an exception occurred,
     * i.e. it prints the stack trace and drops the newly created database if necessary
     *
     * @param e       exception whose stack trace is printed
     * @param cleanUp if true it drops the newly created database
     */
    private static void exitAndCleanUp(Exception e, boolean cleanUp) {
        e.printStackTrace();
        if (cleanUp) dropDBIfFailed();
        System.exit(1);
    }

    /**
     * Prints the help message and exits the program afterwards.
     *
     * @param options commandline options
     */
    private static void help(Options options) {
        String header = "This script installs a basic data set for TAPAS.\nIt expects a postgres database with " +
                "an installed postgis extension. You have to state a DB name, e.g. 'tapas', a DB server address, " +
                "e.g. localhost or an ip address like 192.168.0.1, an already existing postgres database user with " +
                "sufficient rights, e.g. 'postgres' or 'tapas_user, and its password. You can supply these arguments " +
                "through the commandline parameters or you will be prompted. The password prompt is masked for " +
                "security.";

        String footer = "Example: java -cp TAPAS-all.jar Installer -n tapas -s localhost -u tapas_user";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TAPAS Installer", header, options, footer, false);
        System.exit(0);
    }

    public static void main(String[] args) {
        String source = "sql_dumps.zip";
        String destination = "tmp_sql_dumps";

        Options options = new Options();
        options.addOption("n", "dbname", true, "Database Name");
        options.addOption("u", "dbuser", true, "Database User with sufficient rights");
        options.addOption("s", "dbserver", true, "Database Server Address");
        options.addOption("p", "dbpassword", true, "Database Password of DBUSER");
        options.addOption(Option.builder("h").longOpt("help").desc("Show help").build());

        //Create a parser
        CommandLineParser parser = new DefaultParser();

        //parse the options passed as command line arguments
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption('h')) {
                help(options);
            }
            DBSERVER = promptCmdArgument(cmd, options, "dbserver", false);
            DBNAME = promptCmdArgument(cmd, options, "dbname", false);
            DBUSER = promptCmdArgument(cmd, options, "dbuser", false);
            DBPASSWORD = promptCmdArgument(cmd, options, "dbpassword", true);
        } catch (ParseException e) {
            exitAndCleanUp(e, false);
        }
        connectToDB("postgres");
        createDB(DBNAME);
        // reconnect to new database
        closeConnection();
        connectToDB(DBNAME);

        //unzip necessary data
        unzip(source, destination);

        executeCompleteQuery(destination);
        //delete directory
        if (!DESTINATION_ALREADY_EXISTS) deleteDirectory(new File(destination));
    }

    /**
     * Parses an sql string containing one or more sql function definitions and other sql commands.
     * Uses the property that all *our* functions start with "CREATE OR REPLACE FUNCTION" and end with "COST 100;"
     * Returns a list of strings, where each string represents a command for a proper execution of sql commands
     *
     * @param functionString content
     * @return list of sql commands (one function is one command) as strings
     */
    private static List<String> parseFunctionFile(String functionString) {
        //            String functionString = Files.readString(Paths.get(file));
        String[] bar = functionString.split("CREATE OR REPLACE FUNCTION");
        //first basic sql statements which are not a function
        ArrayList<String> stringlist = Arrays.stream(bar[0].split(";")).collect(
                Collectors.toCollection(ArrayList::new));
        for (int i = 0; i < bar.length - 1; i++) {
            String[] baz = bar[i].split("COST 100;");
            if (baz.length > 1) {
                stringlist.add("CREATE OR REPLACE FUNCTION" + baz[0] + "COST 100;");
                stringlist.addAll(Arrays.stream(baz[1].split(";")).collect(Collectors.toList()));
            } else stringlist.addAll(Arrays.stream(baz[0].split(";")).collect(Collectors.toList()));
        }
        //last basic sql statements
        stringlist.addAll(Arrays.stream(bar[0].split(";")).collect(Collectors.toList()));
        return stringlist;
    }

    /**
     * Either evaluates the given command line arguments or prompt for missing arguments
     *
     * @param cmd            commandline object with parsed arguments
     * @param opts           options
     * @param argumentOpt    specific option to read
     * @param passwordPrompt boolean value if the argument is either a password or not
     * @return string containing the argument value
     */
    private static String promptCmdArgument(CommandLine cmd, Options opts, String argumentOpt, boolean passwordPrompt) {
        String val = cmd.getOptionValue(argumentOpt, null);
        if (val == null) {
            if (!passwordPrompt) val = System.console().readLine(
                    "Enter " + opts.getOption(argumentOpt).getDescription() + ": ");
            if (passwordPrompt) val = String.valueOf(
                    System.console().readPassword("Enter " + opts.getOption(argumentOpt).getDescription() + ": "));
        }
        return val;
    }

    /**
     * Checks if the destination directory (where the sql_dumps.zip will extracted to) already exists or not
     * Sets the global DESTINATION_ALREADY_EXISTS variable to true if it exists.
     * Normally we delete the tmp_sql_dumps folder after the Installer run. But we do not want to do this if this
     * folder has been created already (who knows what is in there).
     *
     * @param destination string oath to destination folder
     */
    private static void existsDestinationDirectory(String destination) {
        File f = new File(destination);
        // we use !DESTINATION_ALREADY_EXISTS because we do not want to run into this condition again
        // unzip will be called recursively and there existsDestinationDirectory too
        // There are four possibilities here
        // 1. destination already exists (and is a directory) at the beginning of the Installer script
        //  a) first call of existsDestinationDirectory -> it goes into the if
        //  b) later call of existsDestinationDirectory -> it goes into the if
        // 2. destination does not exist already at the beginning of the Installer script
        //  a) first call of existsDestinationDirectory -> it goes into the else
        //  b) later call of existsDestinationDirectory (dir is created through the attempt of zip extraction) ->
        //      -> it goes into the else
        if (f.exists() && f.isDirectory() && DESTINATION_ALREADY_EXISTS) {
            System.out.println(destination + " already exists. It will not be deleted afterwards.");
        } else {
            DESTINATION_ALREADY_EXISTS = false;
            System.out.println(destination + " does not exist already. It will be created and deleted afterwards. ");
        }
    }

    /**
     * Extracts zip archive into a destination folder
     *
     * @param source      zip archive
     * @param destination directory to which is archive is extracted
     */
    private static void unzip(String source, String destination) {
        System.out.println("Unzip necessary sql-data archive '" + source + "' to '" + destination + "/'");
        existsDestinationDirectory(destination);
        try {
            ZipFile zipFile = new ZipFile(source);
            zipFile.extractAll(destination);
        } catch (ZipException e) {
            source = System.console().readLine("Zip archive not found. Enter path to zip archive: ");
            unzip(source, destination);
        }
    }

}
