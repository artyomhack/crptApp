plugins {
    id("java-library")
}

group = "com.artyom"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1");


    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.test {
    useJUnitPlatform()
}