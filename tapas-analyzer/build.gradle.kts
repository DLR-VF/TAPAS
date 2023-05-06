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

    implementation("org.jfree:jfreechart:1.5.4")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.jgoodies:jgoodies-forms:1.9.0")
    implementation("net.sourceforge.javacsv:javacsv:2.0")

    implementation(project(":tapas-parameter"))
    implementation(project(":tapas-logger"))
    implementation(project(":io"))
    implementation(project(":tapas-util"))
    implementation(project(":tapas-tools"))
}

tasks.test {
    useJUnitPlatform()
}