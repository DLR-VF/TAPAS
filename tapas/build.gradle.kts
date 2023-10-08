/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java project to get you started.
 * For more details take a look at the Java Quickstart chapter in the Gradle
 * User Manual available at https://docs.gradle.org/5.6.3/userguide/tutorial_java_projects.html
 */

// Do not forget to set the PROJECT_NUMBER in the tapas.doxyfile
// also create a release tag "1.0.1" or similar afterwards
project.version = "1.0.1"

plugins {
    // Apply the java plugin to add support for Java
    java

    // Apply the application plugin to add support for building a CLI application
    application

    //plugin for creating a fat jar (called shadow jar) with all dependencies included
    //id("com.github.johnrengelman.shadow") version "7.0.0"

    id("org.javamodularity.moduleplugin") version "1.8.12"

    //`maven-publish`
    idea
}




repositories {
    // You can declare any Maven/Ivy/file repository here.
    //use mavenCentral repository
    mavenCentral()
    maven{
        url = uri("https://repository.jboss.org/nexus/content/repositories/thirdparty-releases")
    }

    //local directory
   // flatDir { dirs("ext") }
}

dependencies {

    implementation("org.postgresql:postgresql:42.5.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("net.sourceforge.javacsv:javacsv:2.0")
    implementation("com.lmax:disruptor:3.4.2")

    //lombok
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor ("org.projectlombok:lombok:1.18.30")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    //local dependency
    implementation(project(":tapas-logger"))
    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-util"))
    implementation(project(":tapas-model"))
    implementation(project(":converter"))
    implementation(project(":io"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}



// This is for publishing a package to GitHub
//publishing {
//    repositories {
//        maven {
//            name = "GitHubPackages"
//            url = uri("https://maven.pkg.github.com/DLR-VF/TAPAS")
//            credentials {
//                // set your environment variables GITHUB_USER and GITHUB_TOKEN
//                // don't forget to create an access token in GitHub
//                // for more information see the documentation
//                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER")
//                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
//            }
//        }
//    }
//    publications {
//        create<MavenPublication>("TAPAS") {
//            groupId = "de.dlr.ivf"
//            artifactId = "tapas"
//            version = project.version.toString()
//            from(components["java"])
//        }
//    }
//}

application {
    // Define the main class for the application
    mainModule.set("de.dlr.ivf.tapas")
    mainClass.set("de.dlr.ivf.tapas.TapasLauncher")
}


task("TapasLauncherTest", JavaExec::class) {

    mainModule.set("de.dlr.ivf.tapas")
    mainClass.set("de.dlr.ivf.tapas.TapasLauncher")
    //classpath = sourceSets.main.1runtimeClasspath
    group = "runnables"
//    description = "Starts Tapas"
//    // Define the main class for the application
//    mainModule.set("de.dlr.ivf.tapas")
//    mainClass.set("de.dlr.ivf.tapas.TapasLauncher")
//   // modularity.inferModulePath.set(false)
//    classpath = sourceSets["main"].runtimeClasspath
//    args = listOf("D:/TAPAS_modularity/tapas/src/main/resources/tapasconfig.json")
//    jvmArgs = listOf("-Xmx8g")
    //jvmArgs = listOf("-Ddebug=true", "-Xmx8g")
}
idea {
    module {
        inheritOutputDirs = true
    }
}


// Task for writing the version to the buildnumber.properties file.
// Below it is set to run during a build task.
//task("buildnumber") {
//    doLast {
//        val date = Date()
//        val version = project.version
//        val versionString = "#TAPAS buildnumber properties\n" +
//                "#" + date + "\n" +
//                "version=" + version + "\n" +
//                "builddate=" + SimpleDateFormat("yyyy/MM/dd hh:mm").format(date) + "\n" +
//                "buildnumber=1a"
//        File(projectDir, "src/main/resources/buildnumber.properties").writeText(versionString)
//    }
//}
//set versionText to run during the build task
//alternative: rootProject.tasks.getByName("build").dependsOn("versionText")
//tasks.build.get().dependsOn("buildnumber")

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// If you want to add more executable tasks (like main() runs) then add
// similar tasks like the ones below
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


//task("SimulationDaemon", JavaExec::class) {
//    group = "runnables"
//    description = "Runs the SimulationDaemon"
//    // Define the main class for the application
//    getMainClass().set("de.dlr.ivf.tapas.runtime.server.SimulationDaemon")
//    classpath = sourceSets["main"].runtimeClasspath
//    args = listOf("T:\\Simulations")
////    jvmArgs = listOf("-Xmx8g")
//    jvmArgs = listOf("-Ddebug=true", "-Xmx8g")
//}
//
//task("SimulationControl", JavaExec::class) {
//    group = "runnables"
//    description = "Starts the SimulationControl app"
//    // Define the main class for the application
//    getMainClass().set("de.dlr.ivf.tapas.runtime.client.SimulationControl")
//    classpath = sourceSets["main"].runtimeClasspath
//}
//
//
//task("Installer", JavaExec::class) {
//    group = "runnables"
//    description = "Starts the installation script"
//    classpath = sourceSets["main"].runtimeClasspath
//    // Define the main class for the application
//    getMainClass().set("de.dlr.ivf.tapas.installer.Installer")
////    set arguments like in the line below
////    args = mutableListOf("--dbserver=localhost","--dbname=my_tapas_db_name","--dbuser=my_already_existing_tapas_db_user","--dbpassword=my_db_password")
//}


tasks.test {
    useJUnitPlatform()
}