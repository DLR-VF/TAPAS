plugins {
    id("java")
    id("org.javamodularity.moduleplugin") version "1.8.12"
    idea
}

group = "de.dlr.ivf"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(":io"))
    implementation(project(":converter"))

    //lombok
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor ("org.projectlombok:lombok:1.18.30")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.opencsv:opencsv:5.7.1")


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}