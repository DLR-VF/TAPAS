/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java project to get you started.
 * For more details take a look at the Java Quickstart chapter in the Gradle
 * User Manual available at https://docs.gradle.org/5.6.3/userguide/tutorial_java_projects.html
 */
import java.text.SimpleDateFormat
import java.util.*

plugins {
    // Apply the java plugin to add support for Java
    java

    // Apply the application plugin to add support for building a CLI application
    application

    //plugin for creating a fat jar (called shadow jar) with all dependencies included
    id("com.github.johnrengelman.shadow") version "5.1.0"

    // javafx plugin to handle javafx dependencies
    id("org.openjfx.javafxplugin") version "0.0.8"

    `maven-publish`

    // git versioning plugin
    id("org.ajoberstar.reckon") version "0.12.0"
}




repositories {
    // You can declare any Maven/Ivy/file repository here.
    //use mavenCentral repository
    mavenCentral()

    //local directory
    flatDir { dirs("ext") }
}

dependencies {
    // These dependencies are used by the application.
    implementation("net.sourceforge.javacsv:javacsv:2.0")
    implementation("org.apache.logging.log4j:log4j:2.13.1")
    implementation("org.postgresql:postgresql:42.2.8.jre7")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.apache.poi:poi:4.1.1")
    implementation("org.jfree:jfreechart:1.5.0")
    implementation("com.jgoodies:jgoodies-forms:1.9.0")
    implementation("com.miglayout:miglayout-swing:5.2")
    implementation("net.sourceforge.jexcelapi:jxl:2.6.12")
    implementation("net.lingala.zip4j:zip4j:2.5.1")

    //local dependency
    implementation("simon_w:simon_w:")

    //test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

javafx {
    //set javafx module version and dependencies; it is this way because javafx is...special
    version = "13"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

// One can change the reckoned version through
// -Preckon.scope=TAPAS_SCOPE -Preckon.stage=TAPAS_STAGE
// where TAPAS_COPE is one of major, minor or patch (defaults to minor)
// and TAPAS_STAGE is one of snapshot and final (defaults to snapshot)
// Example: ./gradlew build -Preckon.scope=major -Preckon.stage=final
reckon {
    scopeFromProp()
    snapshotFromProp()
}


// This is for publishing a package to GitHub
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/DLR-VF/TAPAS")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("TAPAS") {
            groupId = "de.dlr.ivf"
            artifactId = "TAPAS"
            version = project.version.toString()
            from(components["java"])
            artifact(tasks["shadowJar"])
        }
    }
}

application {
    // Define the main class for the application
    mainClassName = "de.dlr.ivf.tapas.runtime.client.SimulationControl"
}


// Task for writing the version to the buildnumber.properties file.
// Below it is set to run during a build task.
task("buildnumber") {
    doLast {
        val date = Date()
        val version = project.version
        val versionString = "#TAPAS buildnumber properties\n" +
                "#" + date + "\n" +
                "version=" + version + "\n" +
                "builddate=" + SimpleDateFormat("yyyy/MM/dd hh:mm").format(date) + "\n" +
                "buildnumber=1a"
        File(projectDir, "src/main/resources/buildnumber.properties").writeText(versionString)
    }
}
//set versionText to run during the build task
//alternative: rootProject.tasks.getByName("build").dependsOn("versionText")
tasks.build.get().dependsOn("buildnumber")

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// If you want to add more executable tasks (like main() runs) then add
// similar tasks like the ones below
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


task("SimulationDaemon", JavaExec::class) {
    group = "runnables"
    description = "Runs the SimulationDaemon"
    // Define the main class for the application
    main = "de.dlr.ivf.tapas.runtime.server.SimulationDaemon"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("data/Simulations")
//    jvmArgs = listOf("-Xmx8g")
    jvmArgs = listOf("-Ddebug=true", "-Xmx8g")
}

task("SimulationControl", JavaExec::class) {
    group = "runnables"
    description = "Starts the SimulationControl app"
    // Define the main class for the application
    main = "de.dlr.ivf.tapas.runtime.client.SimulationControl"
    classpath = sourceSets["main"].runtimeClasspath
}


task("Installer", JavaExec::class) {
    group = "runnables"
    description = "Starts the installation script"
    classpath = sourceSets["main"].runtimeClasspath
    // Define the main class for the application
    main = "de.dlr.ivf.tapas.installer.Installer"
//    set arguments like in the line below
//    args = mutableListOf("-s localhost -n my_tapas_db_name -u my_already_existing_tapas_db_user -p my_db_password")
}

tasks {
    // Use the built-in JUnit support of Gradle.
    "test"(Test::class) {
        useJUnitPlatform()
    }
}