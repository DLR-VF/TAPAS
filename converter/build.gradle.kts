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
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor ("org.projectlombok:lombok:1.18.30")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}