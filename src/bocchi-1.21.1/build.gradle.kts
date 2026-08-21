import java.util.Properties

plugins {
    java
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("fabric-loom") version "1.10.5" apply false
    id("net.neoforged.moddev") version "2.0.78" apply false
}


val mod_version: String by rootProject
val maven_group: String by rootProject

// .properties 规范以 ISO-8859-1 解码 gradle.properties，中文值会被逐字节误读成双重编码乱码
// （v1.0 发布 jar 的 NeoForge 模组描述乱码即此因）。显式按 UTF-8 重读并覆盖 description。
val utf8Description = Properties().apply {
    rootProject.file("gradle.properties").reader(Charsets.UTF_8).use { load(it) }
}.getProperty("description")


allprojects {
    apply {
        plugin("java")
    }

    version = mod_version
    group = maven_group
    description = utf8Description

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
