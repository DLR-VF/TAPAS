plugins {
    id("java")
}

group = "de.dlr.ivf.tapas"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-logger"))
}

tasks.test {
    useJUnitPlatform()
}