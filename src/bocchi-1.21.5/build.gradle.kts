plugins {
    java
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("fabric-loom") version "1.10-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.78" apply false
}


val mod_version: String by rootProject
val maven_group: String by rootProject


allprojects {
    apply {
        plugin("java")
    }

    version = mod_version
    group = maven_group

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.fabricmc.net/")
        maven("https://jitpack.io")
        maven("https://maven.luna5ama.dev/")
    }

    tasks.javadoc {
        enabled = false
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
        options.release = (project.property("java_version") as String).toInt()
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    java {
        withSourcesJar()
    }
}
