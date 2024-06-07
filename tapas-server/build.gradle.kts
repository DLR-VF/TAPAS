plugins {
    java
    application
    idea
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "de.dlr.ivf"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {

    //project
    implementation(project(":tapas"))
    implementation(project(":io"))
    implementation(project(":service"))
    implementation(project(":tapas-environment"))
    implementation(project(":tapas-util"))
    implementation(project(":tapas-model"))

    //junit
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    //lombok
    implementation("org.projectlombok:lombok:1.18.32")
    annotationProcessor ("org.projectlombok:lombok:1.18.32")
    testCompileOnly ("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.32")

    //spring boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


}

application{
    mainModule.set("de.dlr.ivf.tapas.server")
    mainClass.set("de.dlr.ivf.tapas.server.TapasDaemonLauncher")
}

idea {
    module {
        inheritOutputDirs = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.test {
    useJUnitPlatform()
}