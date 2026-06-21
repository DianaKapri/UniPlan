plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "org.uniplan"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Strategy A: depend + extend CPSolver (LGPL-3.0). См. docs/ADR-0001-форк-cpsolver.md
    implementation("org.unitime:cpsolver:1.4.91")

    runtimeOnly("org.postgresql:postgresql")   // основной таргет (SaaS)
    runtimeOnly("com.h2database:h2")            // dev/тесты без Postgres (профиль dev)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Спайк-0 (Неделя 1): прогон движка CPSolver. Запуск: ./gradlew runSpike
tasks.register<JavaExec>("runSpike") {
    group = "application"
    description = "Прогон движка IFS из CPSolver (спайк недели 1)."
    mainClass.set("org.uniplan.spike.BlockSpikeKt")
    classpath = sourceSets["main"].runtimeClasspath
}
