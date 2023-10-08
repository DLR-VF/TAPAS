plugins {
    id("java")
    idea
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf.tapas"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    implementation("net.lingala.zip4j:zip4j:2.11.4")
    implementation("commons-cli:commons-cli:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    // Define the main class for the application
    mainModule.set("de.dlr.ivf.tapas.installer")
    mainClass.set("de.dlr.ivf.tapas.installer.Installer")
}