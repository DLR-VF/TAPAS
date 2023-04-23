plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(":tapas"))
    implementation(project(":io"))
    implementation(project(":tapas-environment"))

    //junit
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    //lombok
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor ("org.projectlombok:lombok:1.18.24")
    testCompileOnly ("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.24")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")
}

application{
    mainModule.set("de.dlr.ivf.tapas.daemon")
    mainClass.set("de.dlr.ivf.tapas.daemon.TapasDaemonLauncher")
}

idea {
    module {
        inheritOutputDirs = true
    }
}

tasks.test {
    useJUnitPlatform()
}