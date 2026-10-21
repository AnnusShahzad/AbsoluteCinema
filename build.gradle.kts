plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.absolutecinema"
// Release builds pass -Pversion=x.y.z (see .github/workflows/release.yml)
version = (findProperty("version") as String?)?.takeIf { it != "unspecified" } ?: "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.pebbletemplates:pebble-spring-boot-starter:3.2.2")
    implementation("io.github.cdimascio:dotenv-java:2.3.2")
    implementation("com.stripe:stripe-java:22.0.0")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<JavaCompile> {
    // Java 17 bytecode, whatever JDK (17+) runs the build
    options.release = 17
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName = "absolute-cinema.jar"
}

tasks.jar {
    enabled = false
}
