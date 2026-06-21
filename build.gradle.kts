plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Strategy A: depend + extend CPSolver (LGPL-3.0). См. docs/ADR-0001-форк-cpsolver.md
    // Тянет транзитивно log4j-core 2.25.4 и dom4j 2.1.5.
    implementation("org.unitime:cpsolver:1.4.91")
}

application {
    // Спайк недели 1: прогнать решатель на игрушечном блоке.
    mainClass.set("org.uniplan.spike.BlockSpikeKt")
}

kotlin {
    jvmToolchain(21)
}
