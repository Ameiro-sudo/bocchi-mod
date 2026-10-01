# 流程诊断报告

> 基线：`main` @ `ba93c8a`，工作区干净。诊断全程只读，未修改任何被跟踪文件。
> 方法：源码通读 + 哈希量化 + 专项实测（实跑 `npm test` / `check-layout.py` / `verify.mjs` / `compare.mjs`）+ git 历史取证。
> 覆盖范围：升版发布、双树同步、本地验证↔CI 门禁、Designer 验证流程。

---

## 摘要

流程**执行纪律很好，但没有一道自动门禁**。四条主线上共 19 个问题，其中 4 个会造成「绿灯假象」或「静默错误发布」：

| 级别 | 数量 | 代表问题 |
| --- | --- | --- |
| P0 | 4 | CI 静默发出割裂版本组合；JUnit 测试从不在 CI 跑；快照门禁在干净环境跑不起来；`check-layout.py` 不用行号 |
| P1 | 6 | README CI 章节过期；CI 失败语义与本地脚本不等价；`verify.mjs` 只采 id 元素 |
| P2 / P3 | 9 | 滚动 tag 冻结在旧提交；基线管理缺位；编码事故三处前例；文档漂移 |

**最高性价比的三件事**（均为零代码或极小改动）：

1. `build-pr.yml` 加两行 `mod_version` 一致性断言 → 堵住静默割裂发版
2. `build-pr.yml` / `build-release.yml` 补 `:common:test` → 让已有测试产生门禁价值
3. `check-layout.py` 纳入行号判定（或删掉行号字段并修正文档）→ 停止提供虚假安心

---

## 一、升版与发布流程

### 事实

**版本传播链**（两树同构）：

```
gradle.properties:30  mod_version=1.0.1
  → build.gradle.kts:11  val mod_version by rootProject
  → build.gradle.kts:26  allprojects { version = mod_version }
     ├─→ multiloader-common.gradle:7   archivesName = "${mod_id}-${project.name}-${minecraft_version}"
     └─→ multiloader-common.gradle:87  expandProps['version']
           ├─→ fabric.mod.json:4
           ├─→ neoforge.mods.toml:7
           └─→ jar MANIFEST Specification/Implementation-Version
```

产物命名规则：`<mod_id>-<子项目>-<mc版本>-<mod版本>[-<classifier>].jar`
→ `bocchi-fabric-1.21.5-1.0.1.jar` / `bocchi-neoforge-1.21.5-1.0.1-all.jar`

**两棵 `gradle.properties` 结构完全同构**：20 个键，**仅 7 个值不同**，且全部是 MC 版本相关（`minecraft_version`、`minecraft_version_range`、`neo_form_version`、`parchment_minecraft`、`parchment_version`、`fabric_version`、`neoforge_version`）。`mod_version` 两侧当前均为 `1.0.1`。

**硬编码 `1.0.1` 字面量，全仓库仅 4 处**：两棵 `gradle.properties:30` + `README.md:68` / `README.md:70`。
Java 源码、`tools/**`、`docs/**` grep `1.0.1|1.0.0|0.1.0` **零命中**。

### P0-1　两棵树 `mod_version` 无任何一致性校验，可静默发出割裂版本组合

只把 1.21.5 树升到 1.0.2、1.21.1 树留在 1.0.1：

- `Publish release 1.21.5` 用 `mod_version_1215=1.0.2` → 命中，正常发布
- `Publish release 1.21.1` 用 `mod_version_1211=1.0.1` → 命中，正常发布
- **两个 step 全绿、两个 Release 全部发出，用户拿到 1.21.5=1.0.2 / 1.21.1=1.0.1，而两个 Release 都叫「Bocchi Client」**

`fail_on_unmatched_files: true`（`build-release.yml:87`、`:103`）**抓不到** —— 它只校验「路径是否存在」，而每个 step 用的是自己那棵树的版本号，路径永远自洽。workflow 注释「两棵树的 mod_version 分别解析, 避免一侧升版后另一侧发布 glob 落空」解决的是路径匹配，不是一致性。

全仓库无一处比对两树版本号：`build-pr.yml:30-42` 只做双树构建不比对；`web-tool.yml` 的 `check-layout.py` 只校验 web 布局常量，与版本无关；`build-all.py:52` 只读单棵树版本。

> **当前唯一能发现割裂的环节是人眼核对 Release 页面上的 jar 名**（`build-release.yml:81-82` / `:98-99` 的 body 会打出实际版本号）。

### P0-2　CI 的失败语义与本地脚本不等价

workflow 里所有自定义 `if:`（`:44`、`:51`、`:73`、`:90`）**不含** `always()` / `failure()`，按 GitHub Actions 规则隐式前置 `success()`：

- `Build 1.21.5`（`:43`）失败 → `Build 1.21.1`（`:50`）被跳过 → 两个 Publish 全被跳过
- `Publish release 1.21.5`（`:72`）因 `fail_on_unmatched_files` 失败 → `Publish release 1.21.1`（`:89`）被跳过

而 `build-all.py:101-102` 是 `for mc,label in trees: all_ok &= build_tree(mc,label)`，一棵失败（`:48-50` return False）**仍继续构建另一棵**，最后汇总退出码。

后果：一侧的版本/构建问题会让另一侧也发不出去（健康产物被无辜扣押），而本地永远能看到两侧真实状态。`README.md:22` / `:58` 的「一键构建全部变体」让人以为两条链路等价。

### P1-3　README「CI / CD」章节已过期，且与同文件自相矛盾

| README 表述 | 实际 |
| --- | --- |
| `:159`「workflow 支持手动触发」 | 漏掉 `push: tags: v*`（`build-release.yml:10-12`，`636eb32` 加入后未更新）。作者本人在 `docs/UNATTENDED_LOG.md:17` 写「此前 build-release 只认 tag/dispatch」 |
| 未说明发布 tag 命名 | tag 触发 → `<tag>-<mcver>`；手动 → 滚动 tag `1.21.5` / `1.21.1`。推 `v1.0.2` **不会**得到名为 `v1.0.2` 的 Release，而是 `v1.0.2-1.21.5` 和 `v1.0.2-1.21.1` |
| 未说明 CI 产物落点 | CI 发的是 `src/**/build/libs/` 直出物，与 `:60-73` 的 `release/` 布局**不是同一套** |
| `:150-151` `release/ └── 1.21.x/{fabric,neoforge}/` | **少了 `vanilla/` 层**，与同文件 `:64-73` 自相矛盾（`build-all.py:77` 总是创建该层） |

远端 tag 实测印证触发行为：

```
fe4c22e  refs/tags/1.21.1          ← 手动触发创建的滚动 tag
fe4c22e  refs/tags/1.21.5          ← 同上
04ff471  refs/tags/v1.0.1           ← 人工 base tag，触发 CI
04ff471  refs/tags/v1.0.1-1.21.1    ← CI 用 GITHUB_TOKEN 创建
04ff471  refs/tags/v1.0.1-1.21.5    ← 同上
```

### P1-4　`if: inputs.version != …` 是排除式判断，非法输入静默退化为全量发布

`workflow_dispatch` 的 input 是**自由文本**（`:4-9`，`required:false`，**无 `type: choice`**）。任何不精确等于 `'1.21.1'` / `'1.21.5'` 的值（`1.21.2`、`All`、`'1.21.5 '` 带尾空格、空串）都让两个 `!=` 同时为真 → **构建双树 + 发布双树 + 覆盖滚动 tag**。拼写错误不报错，而是静默做了影响更大的事。

tag push 时 `inputs` 为空，两个 `!=` 也为真 → 双树发布。**这是期望行为**（已被 `v1.0.1-1.21.*` 实证），但它靠「null 恰好不等于任何字符串」成立，workflow 里没有一行显式写出 `if: github.ref_type == 'tag' || inputs.version == 'all'`。任何人后来把 `!=` 改成 `==`，tag 触发会静默变成「什么都不发」。

### P1-5　jar 完整文件名在 CI 里被重写，形成第二真相源

`build-release.yml:85-86`、`:101-102` 直接写死完整 jar 路径字面量，把 `multiloader-common.gradle:7` 的命名规则在 YAML 里复制了一份；`build-all.py:56-57` 是第三份。任何对 `archivesName` / `mod_id` / 子项目名 / classifier 的改动会同时让 3 份副本失配。

缓解：`fail_on_unmatched_files:true` 与 `build-all.py:73` 精确匹配会硬失败 → **不会发错文件，只会发不出去**。风险是可用性与维护成本，非正确性事故。

### P2-6　手动发布的滚动 tag 冻结在旧提交，历史 jar 无限累积

`refs/tags/1.21.1` 与 `refs/tags/1.21.5` 都指向 `fe4c22e`（2026-08-21「升级 CI actions 至 v5」），而 main 现为 `ba93c8a`。workflow 未设 `target_commitish`，softprops 对已存在 tag 只更新 Release 记录与 assets，不动 tag 指向的 ref：

1. Release 页 1.21.5 的「源码」是 8-21 的代码，assets 是最新产物 —— 对不上
2. `overwrite_files` 默认 true，但**升版后文件名变了**（`…-1.0.1.jar` → `…-1.0.2.jar`），每次手动触发都在同一 Release 上**永久堆积上一版 jar**

### P3-7　README 版本号是第四份手工副本，历史上已漏改两次

当前值正确，但纯手抄。git 铁证：

- `305b658 chore(release): 双树升版 1.0.0→1.0.1` —— 只改 2 个 `gradle.properties`
- `b46ca80` —— 专为此事的 README 补提交 #1
- `ba93c8a docs(readme): 产物 jar 版本 1.0.0 -> 1.0.1，与 gradle.properties 对齐` —— 补提交 #2，**距升版 33 天**

升到下版是第三次手工同步，**无任何校验**。

### P3-8　其他

- **artifact glob 会把 Fabric 中间产物一并上传**：`:65` 用 `…/fabric/build/libs/*.jar`，会连带匹配未 remap 的 `…-named.jar`（`fabric/build.gradle.kts:64`）。不影响 Release（用精确路径），只污染 workflow artifact
- **版本读取失败时机两链路相反**：CI `grep` 在 build 前（`:37-41`），快速失败；`build-all.py` 的 `read_mod_version()` 在 `:52`，即**完整跑完几分钟构建之后**才检查
- **concurrency 用静态组**（`:17-19`，不含 `${{ github.ref }}`）：手动触发会排在 tag 发布之后串行，同 commit 可能产生两个 Release

### 升版操作路径（当前唯一可执行的流程）

| # | 动作 | 文件 | 能否自动发现漏改 |
| --- | --- | --- | --- |
| 1 | 确认双树同步升版 | — | 无，**必须人工** `git diff src/*/gradle.properties` |
| 2 | 改版本号 | 两棵 `gradle.properties:30` | Gradle 侧动态取用，不需改任何 `.kts` / 资源模板 |
| 3 | 验证展开值 | `:fabric:processResources` 后查 `fabric.mod.json` | 强校验 |
| 4 | 本地全量构建 | `python tools/build-all.py` | 强校验（精确名匹配，不符则退出码 1） |
| 5 | 确认产物 | `release/<mc>/<loader>/vanilla/` 下 4 个文件名 | `build-all.py:55` 的 rmtree 保证旧 jar 不残留 |
| 6 | **改 README** | `README.md:68`、`:70` | **无**，已漏改两次 |
| 7 | 提 PR 合并 | 触发 `build-pr.yml` | 只验证两树能构建，**不验证版本一致性、不验证 README** |
| 8 | 打 tag | `git tag v<X.Y.Z> && git push origin v<X.Y.Z>` | 触发发布 |
| 9 | 核对发版 | 打开 `v<X.Y.Z>-1.21.5`（**不是 `v<X.Y.Z>`**） | 人工，**当前唯一能发现割裂的环节** |

**关键结论**：真正「不漏改」只需盯 3 个文件。其中 README **完全无校验**，两个 `gradle.properties` 之间的**一致性也无校验** —— 这两处是整条链路上最脆弱的点。全仓库 grep「升版 / 发布流程 / release / tag」只命中 README 4 行 + `docs/UNATTENDED_LOG.md:17`，**无任何成文 checklist**，流程散落在 git 提交信息里。

---

## 二、双树同步流程

### 量化事实

**common 层 Java 源码**：

| 指标 | 值 |
| --- | --- |
| 1.21.5 文件数 | 137 |
| 1.21.1 文件数 | 113 |
| 共同路径 | 113 |
| **其中字节完全相同** | **102（90%）** |
| 已分化 | 11 |
| 仅 1.21.5 有 | 24（全部是 `graphics/{shader,pipeline,util}` 新渲染管线，未回移） |
| 仅 1.21.1 有 | 0 |

**已分化的 11 个文件及规模**：

| 文件 | 1.21.5 行 | 1.21.1 行 | 差异行数 |
| --- | --- | --- | --- |
| `mixins/transformers/MixinLocalPlayer.java` | 115 | 110 | 27 |
| `mixins/transformers/MixinMinecraftClient.java` | 78 | 73 | 5 |
| `mixins/transformers/MixinMouseHandler.java` | 38 | 38 | 8 |
| `client/ui/mainmenu/misayos/MainMenuMisayosScreen.java` | 209 | 209 | 2 |
| `client/ui/mainmenu/poulsen/MainMenuScreen.java` | 213 | 213 | 2 |
| `client/ui/splash/SplashUI.java` | 251 | 251 | 8 |
| `client/ui/settings/SettingsScreen.java` | 121 | 121 | 2 |
| `graphics/SkiaRenderEngine.java` | 700 | 696 | 10 |
| `graphics/SkiaContext.java` | 246 | 251 | 37 |
| `graphics/font/SkiaFont.java` | 72 | 70 | 16 |
| `utils/ColorUtil.java` | 350 | 350 | 4 |

分化性质清晰：**跨 MC API 版本的必要适配**（mixin 签名、`SkiaContext` 差异），而非各写各的。

**资源**：`assets/` 23 vs 23，路径集合完全相同，21 个字节相同，仅 `bocchi.mixins.json` 与 `pack.mcmeta` 分化（内含版本号，预期）。**`design.json` 两树一致** —— 这对 Designer 工具很重要。

**构建配置与测试**：两树 `settings.gradle.kts`、两个 `multiloader-*.gradle`、4 个测试文件**字节完全一致**。gradle 脚本唯一差异是纯语法写法：`neoforge/build.gradle.kts:76` 1.21.5 用 `attributes["Multi-Release"] = true`，1.21.1 用 `attributes("Multi-Release" to "true")`。

### 纪律执行：良好

近 30 个提交中触及 `src/` 的 14 个，**13 个严格成对修改**（1:1、3:3、2:2 数量完全一致），提交信息里直接写「— 双树」作为标记。同步不是靠运气，是有意识的习惯。

### P1-9　同步纪律全靠人工，无任何自动校验

一旦某次只改一树，**没有任何环节会发现**：CI 两树都能构建（各自通过）、`mod_version` 不一致不会被比对（见 P0-1）、designer 门禁与 Java 无关。漂移会静默积累，直到用户报告「1.21.1 版没有新功能」。

成本量化：一次跨树改动 = 改 2 份 + 手工确认。`docs/UNATTENDED_LOG.md:34` 记录的做法是「双树逐文件哈希核对 SAME」—— 即**用一次性的人工哈希比对替代本该常驻的门禁**。

**注意**：90% 的相同率意味着存在明确的「应该同步」集合（`common/` 下除 11 + 24 之外的文件）。这个集合是可机器判定的 —— 目前没人写这个判定。

---

## 三、本地验证与 CI 门禁对齐

### P0-10　JUnit 测试从不在 CI 里跑

**证据链**（完整）：

1. `build-pr.yml:40` / `:55` 与 `build-release.yml:48` / `:55` 全部只跑 `./gradlew :fabric:build :neoforge:build`
2. `fabric` / `neoforge` 通过 **artifact 配置**依赖 `:common`（`fabric/build.gradle.kts:45`；`buildSrc/…/multiloader-loader.gradle:20-21` 的 `commonJava` / `commonResources`）
3. 这两个 artifact 定义在 `common/build.gradle.kts:65-73`，分别是 `builtBy(tasks.compileJava)` 和 `builtBy(tasks.processResources)` —— **只有编译，没有 `test`**
4. `common/build.gradle.kts:90-92` 的 `tasks.jar { enabled = false }` 进一步切断了 `:common:build` 的可能

→ `:common:test` **不在任何构建的依赖图内**。

而测试基建是齐的：`common/build.gradle.kts:100-107` 已接 JUnit 5（`useJUnitPlatform()`），两树各 2 个测试文件共 4 个（`CfgsRoundtripTest.java`、`SettingsTest.java`）。`docs/UNATTENDED_LOG.md:33` 记录本地「双树 `:common:test` 5/5 绿」跑了 2 轮 —— **但这 5 个断言从未在 CI 执行过一次**。

### P1-10　三条 workflow 触发条件互斥，README 类改动零覆盖

| workflow | 触发路径 | 跑什么 |
| --- | --- | --- |
| `build-pr.yml` | `src/**` | 双树 `:fabric:build :neoforge:build` |
| `web-tool.yml` | `tools/**` | `npm test` + 双树 `check-layout.py` |
| `build-release.yml` | `workflow_dispatch` + tag `v*` | 双树构建 + 发 Release |

`README.md`、`docs/**`、`.gitignore`、`.gitattributes`、workflow 自身 —— **没有任何 workflow 覆盖**。这正是 `ba93c8a` 那类文档漂移能存活 33 天的机制性原因。

跨 `src/` + `tools/` 的 PR 会在 PR 上产生两个独立 check，失败原因需分别排查。

### P3-11　本地验证项远多于 CI，但无成文对照表

| 验证项 | 本地 | CI |
| --- | --- | --- |
| 双树 `:common:test` | 有（`UNATTENDED_LOG.md:33`，跑 2 轮） | **无** |
| 双树 `:fabric:build` / `:neoforge:build` | 有 | 有 |
| 双树逐文件哈希核对 | 有（`UNATTENDED_LOG.md:34`） | 无 |
| `npm test`（33 用例） | 有 | 有 |
| `check-layout.py` 双树 | 有 | 有 |
| `verify.mjs` + `compare.mjs` 快照 | 有（但当前跑不起来，见 P0-12） | 无 |
| 视觉验收（截图人眼） | 有（`UNATTENDED_LOG.md:35`） | 无（无法自动化） |

`docs/UNATTENDED_LOG.md` 是目前唯一的「本地跑了什么」记录，但它写在**任务回填**语境下，不是检查清单 —— 换个人接手无从得知该跑什么。

### P3-12　编码事故已有三处前例

1. `build.gradle.kts:14-15` 注释记载：「v1.0 发布 jar 的 NeoForge 模组描述乱码即此因」（`gradle.properties` 按 ISO-8859-1 解码），已用显式 UTF-8 重读修复
2. **`.gitattributes:1` 的中文注释已退化成字面 `?`** —— 字节实测为 `23 20 57 69 6E 64 6F 77 73 20 3F 3F 3F 3F 3F 3F 3F 20 43 52 4C 46 …`，即 7 个 `0x3F` + 8 个 `0x3F`；同目录 `.gitignore` 的中文注释却完好。是某次有损写入造成的
3. `build-all.py:19-22` 主动做了 `sys.stdout.reconfigure(errors="replace")` 防护

前两处说明仓库缺一个「编辑器 / 工具默认 UTF-8」的约束（无 `.editorconfig`，`.gitattributes` 也没管文本文件编码）。

---

## 四、Designer 验证流程

### P0-12　快照门禁在当前机器和任何干净 checkout 上一步都跑不起来

1. `dev/verify.mjs:33` 硬编码 `const requireFrom = process.env.PUPPETEER_REQUIRE || "D:/project/blog/package.json";` —— **指向另一个项目的 package.json**。该路径在本机**不存在**，实跑报 `Error: Cannot find module 'puppeteer-core'`，在 `:111` 启动浏览器之前就 exit 1
2. `dev/verify.mjs:32` 还硬编码本机 Edge 路径 `C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe`
3. `.gitignore:14` 把 `dev/dumps/` 整个排除，`git ls-files tools/bocchi-designer/dev/` 显示**只有 `compare.mjs` 和 `verify.mjs` 被跟踪** —— 任何 clone 后手上是 **0 份基线**
4. `web-tool.yml:25-30` 只跑 `npm test` 和两条 `check-layout.py`，**完全没有引用 verify / compare**

`README.md:91-107` 用整节篇幅称这条链路是「重构必备」的门禁，实际既不能在 CI 跑、也不能在干净环境复现，只在装过 `D:/project/blog` 的那台机器上可用过。

### P0-13　`check-layout.py` 完全不使用 `facts.js` 里的 Java 行号

grep 证实 `line` 变量在 `check-layout.py` 中出现 9 处，**唯一的消费点是 `:556` 的 DRIFT 提示文案**。真正的匹配走 `:546` **整文件全文扫描** + `:547` `if expected in found`。

实测反例：`facts.js:157` 记 `poulsen.btnFont → MainMenuScreen.java:84`，而真实表达式 `frame.getBgFontSize() * 0.221f * 0.3f` 在**第 93 行**（差 9 行），检查器照样打勾并 `EXIT=0`。`btnX`(86→95)、`btnY0`(87→96) 同理。

更隐蔽的问题：OK 消息里打印的行号是**扫描命中行**而非 `facts.js` 记录的行 —— 实测输出 `iLogoW  ui/splash/SplashUI.java:77 → 94`，而真实 `iLogoW = 94.f;` 在**第 78 行**。

**后果**：直接否定该工具的核心卖点。`README.md:58-59` 和 `facts.js:4-6` 都宣称 `java` 字段是「源文件**与行号**」「唯一记录点」，但行号纯属装饰 —— **Java 文件插删行这一 README 点名的场景根本检测不到**。工具实际回答的是「这个值在这个文件的某处还能算出来吗」，不是「还是不是同一处」。

### P1-13　匹配是「全文件值存在性」+ 两级宽松兜底，宽松结果也算通过

`check-layout.py:547-553` 三档都计入通过数，`:574` 退出码只看 `drift or missing`：

| 档位 | 条件 | 实测输出标记 |
| --- | --- | --- |
| `found` | 全文件求值命中期望值 | 勾 |
| `literals`（`:396-397`） | 期望值作为**裸数字字面量**出现在某片段 | `OK~` |
| `found_loose`（`:126-137`） | **所有未知标识符一律按 1 估**后命中 | `OK?`（提示「建议人工复核」） |

实测输出里 `poulsen.btnGap`、`splash.barW` 就是 `OK?`，**但退出码仍是 0，且 CI 不会有人去看这一行**。

具体假绿场景：把 `scaledWidth * 0.1076` 改成 `* 0.1077`，同时别处新增一个恰好等于旧值的字面量 → 全绿。

### P1-14　回退正则解析器部分解析假绿，`if total == 0` 只挡极端

守卫在 `:505-507` 是 `if total == 0`。实测扰动后只剩 2 条 fact（共 75 条）→ `total=2` → **守卫不触发**，流程继续，输出 `2 / 2 通过` 绿灯。

**已存在的静默丢失**：Node 路径解析 **75** 条，正则回退解析 **74** 条，差 `splash.loadingFrameW`（该 fact 无 `java` 字段，`FACT_RE`(`:406`) 强制要求 `java:`，故被跳过）。模拟 node 缺失实跑 → `74 / 74 通过`，**无任何 warning**。

`facts.js:12-13` 自己写了「正则解析依赖本文件的缩进形状…调整格式时须同步修改解析器」。任何人给条目加个 `note:` 挪到 `java:` 前面，或改缩进 → 回退路径大面积漏解析但仍报绿。`README.md:71` 的「防静默绿灯」只防住了最极端情况。

### P1-15　快照只采集「带 id 且在舞台容器内」的元素，`panels.js` 几乎全盲

- `verify.mjs:61-62` scope = `#misayosStage` / `#poulsenStage` / `#splashStage`
- `verify.mjs:85` `scope.querySelectorAll("[id]")`

`index.html:143` 的 `<div class="controls" id="controls">` **不在任何舞台容器内** → `panels.js` 产出的整个右侧面板（21KB，第二大模块）不在快照里。唯一的面板断言是 `rangeInputCount`（一个数字）和 `exportBtnExists`（一个布尔）。

同样全盲的还有：舞台内无 id 的元素（`index.html:66-75` 的 `.m-record` 子层 —— 黑胶渲染的全部视觉）、`index.html:12-38` 整个 `<header>`（`btnUndo` / `btnRedo` / `btnImport`，撤销重做禁用态检测不到）、`#selBox`。

**这正是「只比了子集」式假绿**：改 `preview.js`（黑胶配色 / 纹理）、`layout.js`（无 id 视觉层）、`panels.js` 大部分 —— 门禁全绿。

### P1-16　`compare.mjs` 的多处结构性假绿

| 问题 | 位置 | 后果 |
| --- | --- | --- |
| `viewport` 是硬编码字面量 | `verify.mjs:118` 真设 1600×900，`:128` 写死 `"1600x900@1"` | 改了真实尺寸，两份不同视口的快照 compare 仍绿 |
| offset 兜底是整数精度 | `verify.mjs:72-79` 用 `offsetLeft/Top/Width/Height`（浏览器返回四舍五入整数） | `tol=0.05` 意味着 **小于 1px 的布局变化完全不可见** |
| `attrs` 不接入排除机制 | `verify.mjs:90/96/97` 的 `inlineTransform` / `baseRot` / `naturalW/H` 从不走 `excluded()` | 动画 / 图片加载时序差异 → **误报假红** |
| 两份快照来源不可校验 | `compare.mjs` 不校验是否同一版 `verify.mjs` 同一张 `EXCLUDE` 表 | 改宽 `EXCLUDE` 后拿旧基线比 → 假绿 |

### P2-17　基线管理完全缺位，8 个 dump 的真实关系

`.gitignore:14` 明确放弃基线管理。现场 8 个 dump 经 SHA256 分组与互比：

| 文件 | 关系 |
| --- | --- |
| `baseline` / `baseline2` / `refactored` / `review` / `review-after` | **字节完全相同**（同 hash） |
| `hist-base` | 仅 `$.url` 为 `…:8845/` |
| `hist-feat` | 仅 `$.url` 为 `…:8844/` |
| `review-final` | **实质不同且已过期**：`splashLogo` rect 4 项全变（60×19.02 → 94×29.798） |

`review-final` 采自 splash logo 改版**之前** —— 当前 `js/layout.js:28` 与 `facts.js:176-177` 都用 60/19.02，而 94/29.798 = `facts.js:167-168` 的 `iLogoW/iLogoH`。拿它当基线比当前代码**必然报假红**。

另外：8 个文件都恰好 48924 字节是**巧合**（url 4 字符等长 + 数值位数增减抵消），不是同源证据。3 个只差 url 端口说明采集时手工换过端口 —— 正是「无自起服务能力」（`verify.mjs:28` 硬编码端口、无 `createServer`）导致的流程摩擦痕迹。

无基线命名规范、无更新时机、无删除机制、无 CI 链路。

### P2-18　`start-designer.bat` 静默失败面较大

- `:12` / `:17` 把输出重定向 `>nul 2>&1` —— **端口占用时 "Address already in use" 被完全吞掉**
- `:26` 固定等 1 秒、**无就绪轮询** —— 冷启动（Defender 扫描 Python 目录）可能 ERR_CONNECTION_REFUSED 且不重试
- `:7` 探活用 `-match 'Bocchi'`，PowerShell **大小写不敏感** —— 若 8833 已被「从仓库根起的 http.server」占用，目录列表含 `bocchi-mod` → 误判「已在运行」→ 打开错误页面，直接违反 `README.md:12-13`
- 无 Python 版本探测：旧机器上 `python` 可能是 2.7（`-m http.server` 不存在），同样被 `>nul` 吞掉

做得好的一面：`:5` `cd /d "%~dp0"` 路径含空格安全；`:10-19` `where py` 优先于 `python` 合理。

### P3-19　测试覆盖现状

`npm test` 纯 Node 可跑（本机无浏览器，**33/33 通过**，6 文件）：

| 文件 | 用例 | 覆盖 |
| --- | --- | --- |
| `core.test.mjs` | 6 | crc32、zip 往返、escapeHtml、debounce、rafThrottle |
| `design.test.mjs` | 5 | 原型污染防护、hexToCss、textsForExport、buildDesignJSON |
| `facts.test.mjs` | 7 | 表达式求值器、循环引用报错、480×270 帧 |
| `fonts.test.mjs` | 2 | 字体键映射、度量元数据（不触发 canvas） |
| `history.test.mjs` | 8 | LIFO、分叉、时间窗合并、applying 守卫 |
| `io.test.mjs` | 5 | zipEntry、applyDesignJSON 合并、原型污染 |

**8 个模块完全无测试**：`main.js`、`interactions.js`、`layout.js`、`ov.js`、`panels.js`、`preview.js`、`render.js`、`status.js`。

`io.js` 的 8 个导出只测了 `applyDesignJSON` —— `exportPack` / `exportJson` / `importPack` / `importJsonFile` / `bind` 全无测试，而 `README.md:109-113` 专门用一节描述了 zip 导出规格。

### P3-20　文档漂移

- `README.md:7-8`「零运行时依赖…无 npm 依赖」未加限定；`verify.mjs` 实际依赖 `puppeteer-core` + 本机 Edge，正文零处提及
- `README.md:85` `npm test  # node --test test/` 与 `package.json:7` 的 `node --test` 不符（后者递归发现，范围更大）
- `README.md:71`「防静默绿灯」被 P1-14 证伪
- `README.md:58-59` 强调行号，但行号不参与判定（P0-13）
- `verify.mjs:36-37` 的 `EXCLUDE` 注释只有概括理由，**无逐条说明**，无法回答「为什么只有这 5 条」

---

## 五、顺带发现

`docs/UNATTENDED_LOG.md:4` 声明流程对齐 IReckon 惯例并引用共享黑板 `D:\project\AGENT_HANDOFF.md 八½节`，但该文件**已不存在**（`D:\project` 下现只有 `AGENT.md` 与 `SnowBlock-Brand.zip`）。无人值守登记 / 回填链路已断。

---

## 六、改进方向（按性价比排序，不含代码）

### 第一梯队 —— 零代码或极小改动，堵住真实风险

1. **`build-pr.yml` 加两树 `mod_version` 一致性断言**（`grep | diff`）→ 堵住 P0-1 静默割裂发版。**唯一能自动发现该问题的手段**
2. **`build-pr.yml` / `build-release.yml` 的 gradle 命令补 `:common:test`** → 让已有的 5 个断言产生门禁价值（P0-10）
3. **README CI 章节重写**（补 tag 触发 + 发布 tag 命名 + CI 产物落点；`:150-151` 补 `vanilla/`）→ 纯文档零风险
4. **`workflow_dispatch` 的 version 改 `type: choice`** → 消除 P1-4 静默全量发布

### 第二梯队 —— 小改动，修复语义不等价

5. **两个 Publish step 用 `if: !cancelled()` 或拆 job** → 恢复与 `build-all.py` 一致的失败隔离（P0-2）
6. **`check-layout.py` 纳入行号判定**（要求命中行集合含 `facts.js` 记录的行，或 ±N 容差窗）；若不改，就**删掉行号字段并修正文档**（P0-13）—— 二选一，不能维持现状
7. **`found_loose` 在 `--strict` 下判失败**；`literals` 兜底限制在命中行附近（P1-13）
8. **回退解析器守卫从 `total == 0` 改为「解析条数下限」**；Node 与正则两条路径条数不一致时告警（P1-14）
9. **`verify.mjs` 的 puppeteer 定位改相对 `import.meta.url` + 浏览器多候选探测**；基线目录移出 `.gitignore` 并定唯一文件名（P0-12）—— 目标只是「能跑、能复现」

### 第三梯队 —— 结构性，需要专门设计

10. **建立「应同步文件集」并加机器校验**（P1-9）。90% 相同率意味着这个集合是可判定的：common 下排除 11 个已分化 + 24 个 1.21.5 独有即为应同步集
11. **`verify.mjs` 采集面扩到 `#controls` + `header`**，或对无 id 元素用结构路径定位（P1-15）
12. **`compare.mjs` 从实际 `page.viewport()` 取值；`EXCLUDE` 表带版本号写进快照并在 compare 时校验**（P1-16）
13. **补一份成文的验证清单 + 升版 checklist**（当前全仓库无），明确「改 src 要跑什么、改 tools 要跑什么」
14. **加 `.editorconfig`（charset = utf-8）**（P3-12）

### 长期

15. 让 `build-all.py` 成为产物命名的唯一真相源，CI 复用同一套 glob；README 示例改用 `<mod_version>` 占位而非字面量（P1-5、P3-7）

---

## 附：无法确认的点

诚实标注，未猜测填充：

1. `fabric/build/libs` 与 `neoforge/build/libs` 的完整文件集合（Loom 是否还产无 classifier 的 dev jar）—— 本检出无构建产物，`build/` 与 `release/` 均不存在
2. 线上 GitHub Release `1.21.5` / `1.21.1` 上实际累积了多少个历史版本 jar —— 需联网查 Release 页
3. `softprops/action-gh-release@v3` 是浮动 tag，实际解析到哪个 commit —— 未 pin SHA
4. 是否真发生过 P2-6 描述的「同 commit 两个 Release」—— 需查 Actions run 历史
5. `D:/project/blog` 是否曾真实存在并装有 `puppeteer-core`。若它只是个人机器上的临时路径，则快照门禁**从未在第二台机器上被验证过**
6. `dev/dumps/` 8 个基线的采集时序与对应 commit（已 gitignore，无元数据）
7. `EXCLUDE` 那 5 条当初排除的具体观察依据（无注释、无 commit 说明）
8. `review-final` 的过期是「回退了 splashLogo 改动」还是「采自未完成分支」
