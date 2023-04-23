plugins {
    java
    idea
}

group = "de.dlr.ivf.util"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
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

tasks.test {
    useJUnitPlatform()
}