# 验证清单

改代码前该跑什么。**本地能跑的项比 CI 多**，以本文档为准；CI 只跑其中可自动化的部分。

命令均在仓库根目录执行，Windows 下 Gradle 用 `.\gradlew.bat`。

---

## 快速自检（秒级，任何改动都该跑）

```bash
python tools/check-version.py     # 两棵树 mod_version 一致 + 文档无旧版本残留
python tools/check-sync.py        # 双树共享区域逐字节一致
```

这两条同时是 CI `build-pr.yml` 的 `consistency` 门禁。`check-sync.py` 报差异时，
先问「这是 MC 版本适配的必要产物，还是漏改」：前者登记进 `tools/sync-allowlist.txt`
并写明理由，后者把另一棵树也同步过来。

---

## 改了 `src/**`（Java / Gradle / 资源）

```bash
# 两棵树都要跑，且都要过
cd src/bocchi-1.21.5 && .\gradlew.bat :common:test :fabric:build :neoforge:build
cd src/bocchi-1.21.1 && .\gradlew.bat :common:test :fabric:build :neoforge:build
```

> **`:common:test` 必须显式点名。** `:fabric:build` 不会传递触发它 —— fabric/neoforge
> 经 `commonJava` / `commonResources` artifact 依赖 common，而那两个 artifact 只
> `builtBy(compileJava)` / `builtBy(processResources)`；common 的 `jar` 又是禁用的。
> 这是历史遗留的盲区，CI 侧已一并补上。

**改了 UI 后还要做**（无自动化替代）：

1. 启动对应版本的客户端，人眼过一遍双主题主菜单 + 加载页
2. 改动涉及设置面板时，确认四类设置项（Boolean / Number / Ranged / Enum）读写对称
3. 若改动涉及 Skia 渲染（`graphics/`），重点看有无 native 资源泄漏

**改了 Java 布局数字后还要做**：

```bash
cd tools/bocchi-designer
python sync/check-layout.py --tree bocchi-1.21.5
python sync/check-layout.py --tree bocchi-1.21.1
```

若同时改了 `js/facts.js` 的表达式，记得两边一起改 —— 这个检查器比对的正是两者的对应关系。

---

## 改了 `tools/bocchi-designer/**`（Designer）

```bash
cd tools/bocchi-designer
npm test                                    # 33 个用例，纯 Node，无需浏览器
python sync/check-layout.py --tree bocchi-1.21.5
python sync/check-layout.py --tree bocchi-1.21.1
```

**改了布局 / 交互等「结构改、行为不该变」的部分**，还要做行为快照比对：

```bash
# 终端 1：起服务
python -m http.server 8833 --bind 127.0.0.1

# 终端 2：改动前采基线，改动后采新快照并比对
node dev/verify.mjs --out dev/dumps/before.json
#   ... 做改动 ...
node dev/verify.mjs --out dev/dumps/after.json
node dev/compare.mjs dev/dumps/before.json dev/dumps/after.json   # 退出码 0 = 等价
```

> `dev/verify.mjs` 需要 `puppeteer-core`，本项目刻意保持零 npm 依赖，跑不起来时
> 脚本会列出所有尝试过的来源和四种安装方式，照着做即可。
>
> **`dev/dumps/` 不在版本控制内**，基线只在你自己机器上存在。别把某个 dump
> 文件当成"官方基线" —— `review-final` 那类过期快照拿来做比对会直接报假红。

**已知盲区**（改动落在这里时快照门禁帮不上忙，需手动确认）：

- `panels.js` 产出的右侧控制面板 —— 它在舞台容器之外，快照采集不到
- 舞台内没有 `id` 的元素（黑胶 `.m-record` 子层、主菜单背景层等）
- `<header>` 上的撤销 / 重做 / 导入按钮状态

---

## 改版本号 / 发版

见 [`README.md` 的「CI / CD」](../README.md)。要点：

1. **两棵树一起改** `gradle.properties` 的 `mod_version`
2. 跑 `python tools/check-version.py` —— 它同时会告诉你 README 里哪些位置需要跟着改
3. `python tools/build-all.py` 全量构建并确认 `release/` 下 4 个文件名
4. 打 tag 推送；**Release 名是 `v<X.Y.Z>-1.21.5` / `v<X.Y.Z>-1.21.1`**

---

## 各验证项与 CI 的对应关系

| 验证项 | 本地 | CI |
| --- | :---: | :---: |
| `check-version.py` | ✅ | ✅ `build-pr` |
| `check-sync.py` | ✅ | ✅ `build-pr` + `build-release` preflight |
| 双树 `:common:test` | ✅ | ✅ `build-pr` + `build-release` |
| 双树 `:fabric:build` / `:neoforge:build` | ✅ | ✅ `build-pr` + `build-release` |
| 双树逐文件哈希核对 | ✅ `check-sync.py` 已代劳 | ✅ 同左 |
| `npm test` | ✅ | ✅ `web-tool` |
| `check-layout.py` 双树 | ✅ | ✅ `web-tool` |
| `verify.mjs` + `compare.mjs` 快照 | ✅ | ❌ 未进 CI（需 puppeteer） |
| 视觉验收（人眼） | ✅ | ❌ 无法自动化 |

---

## 相关文档

- [`PROCESS_AUDIT.md`](PROCESS_AUDIT.md) —— 流程诊断报告，本清单的由来
- [`UNATTENDED_LOG.md`](UNATTENDED_LOG.md) —— 无人值守任务登记与回填
