import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "org.capturecoop"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.wollinger:Zipper:v0.0.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "org.capturecoop.ccupdater.MainKt" }
    dependsOn(configurations.runtimeClasspath)
    from({
        sourceSets.main.get().output
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}