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
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.jfree:jfreechart:1.5.4")
    implementation("org.openjfx:javafx:17")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-cli:commons-cli:1.5.0")

    implementation(project(":tapas-analyzer"))
    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-util"))
}

javafx {
    version = "17"
    modules("javafx.controls", "javafx.fxml", "javafx.swing","javafx.graphics")
}

tasks.test {
    useJUnitPlatform()
}