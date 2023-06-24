plugins {
    java
    idea
    id("org.javamodularity.moduleplugin") version "1.8.12"
}

group = "de.dlr.ivf.tapas"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")
    implementation("ch.qos.logback:logback-core:1.4.8")

    //lombok
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor ("org.projectlombok:lombok:1.18.24")
    testCompileOnly ("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.24")

    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")

    implementation(project(":tapas-util"))
}

tasks.test {
    useJUnitPlatform()
}