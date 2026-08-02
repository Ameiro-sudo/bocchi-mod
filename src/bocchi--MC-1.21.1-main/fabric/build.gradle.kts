import java.io.BufferedOutputStream
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    java
    id("multiloader-loader")
    id("fabric-loom")
    id("com.gradleup.shadow")
}

group = "me.baier"
version = "0.1.0"

repositories {
    mavenCentral()
}

loom {
    val aw = project(":common").file("src/main/resources/${property("mod_id")}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath.set(aw)
    }
    mixin {
        defaultRefmapName.set("${property("mod_id")}.refmap.json")
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val modLibrary by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("parchment_minecraft")}:${property("parchment_version")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modLibrary(project(path = ":common", configuration = "library")) {
        exclude("org.apache.commons", "commons-lang3")
        exclude("org.slf4j", "slf4j-api")
    }

    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

sourceSets {
    main {
        resources { srcDir("src/generated/resources") }
        compileClasspath += modLibrary
        runtimeClasspath += modLibrary
    }
}

tasks.shadowJar {
    archiveClassifier = "named"
    configurations = listOf(modLibrary)
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile = tasks.shadowJar.get().archiveFile
    outputs.upToDateWhen { false }
    doLast {
        val f = archiveFile.get().asFile
        val tmp = File.createTempFile("bocchi-mr", ".jar", f.parentFile)
        JarFile(f).use { src ->
            ZipOutputStream(BufferedOutputStream(tmp.outputStream())).use { out ->
                val man = src.manifest ?: Manifest()
                man.mainAttributes.putValue("Multi-Release", "true")
                out.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
                man.write(out)
                out.closeEntry()
                val it = src.entries()
                while (it.hasMoreElements()) {
                    val e = it.nextElement()
                    if (e.name == "META-INF/MANIFEST.MF") continue
                    out.putNextEntry(ZipEntry(e.name))
                    src.getInputStream(e).copyTo(out)
                    out.closeEntry()
                }
            }
        }
        f.delete()
        tmp.renameTo(f)
    }
}

tasks.javadocJar { enabled = false }
tasks.sourcesJar { enabled = false }
tasks.remapSourcesJar { enabled = false }