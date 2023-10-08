plugins {
    java
    application
    idea
    // javafx plugin to handle javafx dependencies
    id("org.openjfx.javafxplugin") version "0.0.9"

    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf.tapas.environment"
version = "unspecified"

repositories {
    mavenCentral()

}

dependencies {
    implementation(project(":tapas-tools"))
    implementation(project(":tapas-environment"))
    implementation(project(":io"))
    implementation(project(":converter"))
    implementation(project(":tapas-matrixtool"))
    implementation(project(":tapas-logger"))
    implementation(project(":tapas-analyzer"))
    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-util"))
    implementation(project(":tapas-model"))

    implementation(files("D:/scenicview/lib/scenicview.jar"))


    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    implementation("org.jfree:jfreechart:1.5.4")
    implementation("org.openjfx:javafx:19.0.2.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-cli:commons-cli:1.5.0")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    //lombok
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor ("org.projectlombok:lombok:1.18.30")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")
}

javafx {
    version = "19"
    modules("javafx.controls", "javafx.fxml", "javafx.swing","javafx.graphics", "javafx.web")
}

application {
    // Define the main class for the application
    mainModule.set("de.dlr.ivf.tapas.environment.gui")
    mainClass.set("de.dlr.ivf.tapas.environment.gui.fx.StartSimulationMonitor")
}

tasks.test {
    useJUnitPlatform()
}