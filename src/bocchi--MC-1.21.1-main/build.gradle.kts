plugins {
    java
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("org.spongepowered.mixin") version "0.7-SNAPSHOT" apply false
    id("fabric-loom") version "1.10-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.78" apply false
}


group = "me.baier"
version = "0.1.0"

val archives_base_name: String by rootProject
val mod_version: String by rootProject
val maven_group: String by rootProject


allprojects {
    apply {
        plugin("java")
    }

    base {
        archivesName = archives_base_name
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
        options.release = 21
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    java {
        withSourcesJar()
    }
}
