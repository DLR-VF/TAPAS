plugins {
    java
    idea
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf.tapas.parameter"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("net.sourceforge.javacsv:javacsv:2.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":tapas-util"))
    implementation(project(":tapas-logger"))
}

tasks.test {
    useJUnitPlatform()
}