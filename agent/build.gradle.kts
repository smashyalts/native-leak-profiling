plugins {
    `java-library`
    `kotlin-dsl`
}

group = "net.skullian.nativeleaks"
version = libs.versions.plugin.version.get()

allprojects {
    repositories {
        mavenCentral()
    }
}