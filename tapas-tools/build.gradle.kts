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
    implementation("org.apache.commons:commons-lang3:3.12.0");
    //jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")

    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-logger"))
    implementation(project(":io"))
    implementation(project(":tapas-model"))
}

tasks.test {
    useJUnitPlatform()
}