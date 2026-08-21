plugins {
    java
    id("multiloader-common")
    id("net.neoforged.moddev")
}


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

    // 测试源集完整继承 main 的 classpath (MC 系传递依赖 gson/slf4j/commons-io 由
    // moddev 插件注入 main 的 compile/runtimeClasspath, 默认不传导到 test)
    test {
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
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

// 持久化往返测试: cfg/mod/setting 包为纯 Java (Gson+Lombok+slf4j), 不依赖 MC 运行时
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}