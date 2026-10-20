import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.4" // Matches the version from pom.xml
    id("io.spring.dependency-management") version "1.1.7" // Standard Spring plugin
    kotlin("jvm") version "2.1.20" // Update to a compatible Kotlin version for Spring Boot 3.x
    kotlin("plugin.spring") version "2.1.20" // Update for Spring support in Kotlin
    application // To define the main class
}

group = "com.absolutecinema"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.pebbletemplates:pebble-spring-boot-starter:3.2.2") // Add Pebble starter

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Database
    runtimeOnly("org.postgresql:postgresql") // Use runtimeOnly as it's needed at runtime

    // .env file support
    implementation("io.github.cdimascio:dotenv-java:2.3.2") // Or latest version

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("com.stripe:stripe-java:22.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("com.absolutecinema.Application") // Set your main application class
}
