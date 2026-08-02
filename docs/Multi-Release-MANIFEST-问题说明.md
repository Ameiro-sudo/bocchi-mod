# Multi-Release Manifest 丢失问题说明（sun.misc.Cleaner 崩溃）

面向开发者的技术文档。本文记录了 Skija 库在 Java 21 下启动崩溃
（`NoClassDefFoundError: sun.misc.Cleaner`）的根因与修复方案。

## 1. 现象

游戏启动时（渲染上下文初始化阶段）直接崩溃：

```
java.lang.NoClassDefFoundError: sun.misc.Cleaner
	at io.github.humbleui.skija.impl.Cleanable.<clinit>(Cleanable.java:19)
	at io.github.humbleui.skija.impl.Managed.<init>(Managed.java:20)
	at io.github.humbleui.skija.impl.RefCnt.<init>(RefCnt.java:7)
	at io.github.humbleui.skija.DirectContext.<init>(DirectContext.java:109)
	at me.baier.graphics.SkiaContext.<init>(SkiaContext.java:41)
	...
```

仅发生在打包后的 jar 上；开发环境 `runClient` 不受影响。

## 2. 根因

### 2.1 Skija 是 multi-release jar

`io.github.humbleui:skija-shared:0.116.4`（common/build.gradle.kts 依赖）
为了同时支持 Java 8 与 Java 9+，打成了 multi-release jar，内含两套实现：

| 位置 | 目标 JVM | 使用的清理 API |
| --- | --- | --- |
| `io/github/humbleui/skija/impl/Cleanable.class`（根版本） | Java 8 | `sun.misc.Cleaner` |
| `META-INF/versions/9/.../Cleanable.class` | Java 9+ | `java.lang.ref.Cleaner` |

`sun.misc.Cleaner` 是 Java 8 的内部 API，JDK 9 起被 `java.lang.ref.Cleaner`
取代并逐步移除，Java 21 中已不存在。

### 2.2 Multi-Release 标记丢失

JVM 加载 multi-release jar 时，**只有在 Manifest 中看到 `Multi-Release: true`
才会按运行版本选择 `META-INF/versions/9/` 下的类**；没有该标记则一律加载根版本。

问题出在打包环节：

- **Fabric**：`remapJar`（fabric-loom）会重新生成 Manifest，导致原依赖 jar
  合并进来的 `Multi-Release: true` 属性丢失 → Java 21 加载了 Java 8 版
  `Cleanable` → 引用不存在的 `sun.misc.Cleaner` → 崩溃。
- **NeoForge**：同样需要在 `shadowJar` 合并 Manifest 时显式声明。

## 3. 修复

### 3.1 fabric/build.gradle.kts

`remapJar` 完成后重写 jar，把 `Multi-Release: true` 写回 Manifest：

```kotlin
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
```

### 3.2 neoforge/build.gradle.kts

`shadowJar` 直接声明 Manifest 属性：

```kotlin
tasks.shadowJar {
    configurations = listOf(modLibrary)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Multi-Release"] = true
    }
}
```

## 4. 验证方法

构建后检查产物 jar：

```bash
# 1. Manifest 必须包含 Multi-Release: true
unzip -p bocchi-fabric-1.21.5-0.1.0.jar META-INF/MANIFEST.MF | grep Multi-Release

# 2. versions/9 下的类必须存在
unzip -l bocchi-fabric-1.21.5-0.1.0.jar | grep "META-INF/versions/9/.*/Cleanable"
```

两条都满足即修复正确。

## 5. 影响范围与注意事项

- 受影响构建：**全部**（1.21.5 / 1.21.1 × fabric / neoforge），只要打进 skija 就有此问题。
- 开发环境 `runClient` 不经过 remapJar/shadowJar，不会复现，**必须在打包产物上验证**。
- 若后续升级 skija 或更换依赖，需重新确认其 multi-release 结构是否仍存在。
- 修改构建脚本后记得重新构建并覆盖 mods 目录中的 jar。

## 6. 相关文件

- `common/build.gradle.kts` —— skija 依赖声明（`library(...)` 配置）
- `fabric/build.gradle.kts` —— remapJar Manifest 重写
- `neoforge/build.gradle.kts` —— shadowJar Manifest 属性
