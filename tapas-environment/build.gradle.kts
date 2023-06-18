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
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor ("org.projectlombok:lombok:1.18.24")
    testCompileOnly ("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.24")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")
    implementation("com.opencsv:opencsv:5.7.1")


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}