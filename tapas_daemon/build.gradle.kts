plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(":tapas"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application{
    mainModule.set("de.dlr.ivf.tapas.daemon")
    mainClass.set("de.dlr.ivf.tapas.daemon.TestMain")
}

tasks.test {
    useJUnitPlatform()
}