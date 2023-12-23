import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.1"
    id("io.spring.dependency-management") version "1.1.4"

    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:2.5.0")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("org.apache.kafka:kafka-streams:2.5.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.github.microutils:kotlin-logging:1.5.9")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.kafka:spring-kafka:2.5.17.RELEASE")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

//jar {
//    from {
//        configurations.compile.collect { it.isDirectory() ? it : zipTree(it) }
//    }
//}