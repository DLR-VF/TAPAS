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
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

}

tasks.test {
    useJUnitPlatform()
}