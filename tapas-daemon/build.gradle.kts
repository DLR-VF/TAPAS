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
    implementation(project(":tapas-util"))
    implementation(project(":tapas-model"))

    //junit
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    //lombok
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor ("org.projectlombok:lombok:1.18.30")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
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