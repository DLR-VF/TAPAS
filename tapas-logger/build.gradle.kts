plugins {
    java
    idea
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf.tapas"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")

    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-util"))
}

tasks.test {
    useJUnitPlatform()
}