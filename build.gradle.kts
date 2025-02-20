plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.vrdc"
version = "0.0.2"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val jacksonDatabindVersion = "2.18.2"
val kotlinxCoroutinesCoreVersion = "1.8.0"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesCoreVersion")
}

intellij {
    version.set("2024.1.7")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }
}
