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
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.4")

    //lombok
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor ("org.projectlombok:lombok:1.18.30")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    implementation(project(":converter"))
}

tasks.test {
    useJUnitPlatform()
}