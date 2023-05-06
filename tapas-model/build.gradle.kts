plugins {
    id("java")
    id("org.javamodularity.moduleplugin") version "1.8.12"

    //`maven-publish`
    idea
}

group = "de.dlr.ivf.tapas"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("net.sourceforge.javacsv:javacsv:2.0")

    implementation(project(":tapas-logger"))
    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-util"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}