plugins {
    java
    id("multiloader-common")
    id("net.neoforged.moddev")
}


group = "me.baier"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

neoForge {
    neoFormVersion = property("neo_form_version")!!.toString()
    // Automatically enable AccessTransformers if the file exists
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
    parchment {
        minecraftVersion = property("parchment_minecraft")!!.toString()
        mappingsVersion = property("parchment_version")!!.toString()
    }
}

lateinit var library: Configuration

configurations {
    library = create("library") {
        isCanBeResolved = true
    }
}

dependencies {
    implementation("org.spongepowered:mixin:0.8.5")
    implementation("io.github.llamalad7:mixinextras-common:0.3.5")
    implementation("org.ow2.asm:asm-tree:9.6")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    library("io.github.humbleui:skija-shared:0.116.4")

    library("io.github.humbleui:skija-windows-x64:0.116.4")

    library("io.github.humbleui:types:0.2.0")

    library("org.jetbrains:annotations:26.0.2")

    library("org.bytedeco:ffmpeg:7.1-1.5.11")
    library("org.bytedeco:ffmpeg:7.1-1.5.11:windows-x86_64")

    library("com.mojang:brigadier:1.0.18")
}

configurations {
    create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }

    create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile) {
        builtBy(tasks.compileJava)
    }

    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile) {
        builtBy(tasks.processResources)
    }
}

sourceSets {
    main {
        compileClasspath += library
    }
}


tasks {
    jar {
        enabled = false
    }

    javadocJar {
        enabled = false
    }
}