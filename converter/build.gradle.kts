plugins {
    id("java")
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf.api"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {

    //lombok
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor ("org.projectlombok:lombok:1.18.24")
    testCompileOnly ("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.24")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}