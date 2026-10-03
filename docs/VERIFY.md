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
npm test                                    # 49 个用例，纯 Node，无需浏览器
python sync/check-layout.py --tree bocchi-1.21.5
python sync/check-layout.py --tree bocchi-1.21.1
```

**改了布局 / 交互等「结构改、行为不该变」的部分**，还要过行为门禁：

```bash
npm run verify        # 快照 + 四组断言探针，与已入库基线比对；退出码 0 = 等价
```

`dev/verify.mjs` 自起随机端口静态服务、自找浏览器，**零环境变量可跑**
（`puppeteer-core` 是 devDependency，与页面运行无关）。它做两件事：

1. **快照比对** —— 采 `<header>` + `#controls` 的结构化节点表（tag / class / id /
   text / attrs / hidden / rect）、`#jsonPreview` 指纹、内置资源探针结果、三舞台
   几何与 design.json 内容；与入库的 `dev/dumps/baseline.json` 比对。
2. **断言探针** —— 快照看不见的地方用它补，违反即退出码 1：
   - `uiProbe` —— 舞台不应整块可 hover 描虚线；Esc 后状态栏不得为空。
   - `sliderProbe` —— 滑杆真实 input 事件必须联动舞台几何。
   - `historyProbe` —— 走用户路径派发 input/change，断言「改完能撤回来」：入栈
     条数、回放后基准是否刷新、撤销后再失焦**不产生伪造条目**、预览配色回放是否
     同时还原 CSS 变量、「已改」标记是否跟撤销走。
   - `responsiveProbe` —— 压到 1000 / 640 两宽，断言无横向溢出、面板仍在视口内、
     「适应」后预览区无横竖滚动。

> **重构守则**：行为不变的重构应让 `npm run verify` 直接等价通过，**不需要重建
> 基线**。需要重建基线 = 这次改动确实改了可见行为，得逐条核对差异后再重建。
>
> ```bash
> npm run verify:baseline   # 确认差异都是预期的之后，重建基线
> npm run verify           # 再跑一次 —— 端口随机，只跑一次排除不掉偶发差异
> npm run verify           # 第二次也必须等价
> ```

**残留盲区**（改动落在这里时门禁帮不上忙，需手动确认）：

- 舞台内没有 `id` 的元素（黑胶 `.m-record` 子层、主菜单背景层等）
- 字体渲染结果本身 —— 只度量盒尺寸，不比对像素
- 跨平台数值差异：CI 跑 `npm run verify:ci`（`compare.mjs --struct`，只比结构与
  字符串不比数值），因为同一组浮点公式在不同平台有尾数差异

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
| `verify.mjs` + `compare.mjs` 快照 | ✅ | ✅ `web-tool`（`--struct`） |
| 四组断言探针（ui / slider / history / responsive） | ✅ | ✅ `web-tool` |
| 视觉验收（人眼） | ✅ | ❌ 无法自动化 |

---

## 相关文档

- [`PROCESS_AUDIT.md`](PROCESS_AUDIT.md) —— 流程诊断报告，本清单的由来
- [`UNATTENDED_LOG.md`](UNATTENDED_LOG.md) —— 无人值守任务登记与回填
