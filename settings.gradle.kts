pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/snapshots") }
    }
}

rootProject.name = "HadolintPlugin"
