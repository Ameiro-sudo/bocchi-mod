# Bocchi Client

基于 [bocchi-template](https://github.com/baier233/bocchi-template-1.21.5) 的 Minecraft 客户端 UI 模组，提供全新主菜单、加载画面与可定制主题系统。

<img width="1919" height="1032" alt="misayos 主题" src="https://github.com/user-attachments/assets/feb2c740-d3d9-4809-a364-29de31527f2b" />
<img width="1915" height="1033" alt="poulsen 主题" src="https://github.com/user-attachments/assets/5cde75b6-09f8-41fa-bb07-dfac8cd543f1" />
<img width="1919" height="1031" alt="加载画面" src="https://github.com/user-attachments/assets/4277ce86-ada4-4930-ba8a-15239c15ebe7" />
<img width="1919" height="995" alt="Design Editor" src="https://github.com/user-attachments/assets/db250c4d-bbc7-40d7-9e0e-ca4de57c9303" />

## 支持版本

| MC 版本 | Fabric | NeoForge |
|---------|--------|----------|
| 1.21.5  | ✅     | ✅       |
| 1.21.1  | ✅     | ✅       |

## 相对原版新增能力

- **Design 设计系统**：所有纹理 / SVG / 字体 / 配色路径集中管理于 `design.json`，材质包可覆盖、可热重载
- **Theme 主题系统**：`misayos` / `poulsen` 双主题运行时切换，无需重新构建
- **NeoForge 加载画面适配**：Fabric / NeoForge 行为一致
- **一键构建全部变体**：`tools/build-all.py`（跨平台，仅需 Python 3）

---

## 环境要求

- **JDK 21**（推荐 [Eclipse Temurin](https://adoptium.net/)）
- **Gradle**（项目自带 Gradle Wrapper，无需手动安装）

## 构建

```bash
# ---------- 1.21.5 ----------
cd src/bocchi-1.21.5
./gradlew :fabric:build :neoforge:build

# ---------- 1.21.1 ----------
cd src/bocchi-1.21.1
./gradlew :fabric:build :neoforge:build
```

> Windows 下将 `./gradlew` 替换为 `.\gradlew.bat`。

### 一键构建全部变体

```bash
python tools/build-all.py
```

可选参数：

| 参数 | 说明 |
|------|------|
| `--only-1215` | 仅构建 1.21.5 |
| `--only-1211` | 仅构建 1.21.1 |

构建矩阵：版本 (1.21.1 / 1.21.5) × 加载器 (Fabric / NeoForge)

### 产物位置

`tools/build-all.py` 构建后输出：

```
release/
├── 1.21.5/
│   ├── fabric/
│   │   └── vanilla/  bocchi-fabric-1.21.5-1.0.1.jar
│   └── neoforge/
│       └── vanilla/  bocchi-neoforge-1.21.5-1.0.1-all.jar
├── 1.21.1/
│   └── ...
```

> 上面 jar 名里的 `1.0.1` 是当前 `mod_version`。改版本后请同步更新此处 ——
> `tools/check-version.py` 会拦截遗漏（历史上因此漏改过两次）。

直接用 `./gradlew` 构建时产物落在 `src/<版本>/<加载器>/build/libs/`，CI 发布用的就是这个位置。

### 提交前自检

```bash
python tools/check-version.py     # 两树 mod_version 一致 + 文档无旧版本残留
python tools/check-sync.py        # 双树共享区域逐字节一致（例外须登记）
```

两条都是秒级的，CI 的 `build-pr.yml` 也会跑。完整清单见 [`docs/VERIFY.md`](docs/VERIFY.md)。

---

## 安装

1. 安装对应版本的 [Fabric Loader](https://fabricmc.net/) 或 [NeoForge](https://neoforged.net/)
2. 将构建好的 jar 放入 Minecraft 的 `mods/` 文件夹
3. **Fabric** 用户还需放入 [fabric-api](https://modrinth.com/mod/fabric-api)

| 加载器 | 额外依赖 |
|--------|----------|
| Fabric | fabric-loader + fabric-api |
| NeoForge | 无 |

---

## 主题切换

主题是运行时选项，无需重新构建 jar：

1. 从 mod jar 中提取 `assets/minecraft/client/design.json`，放入资源包同路径
2. 修改 `menu.theme` 字段为 `"misayos"` 或 `"poulsen"`
3. 游戏内装载/重载资源包即可生效

也可在游戏内通过主菜单的主题切换按钮操作（存储于 `~/.bocchi/theme.json`）。

---

## 资源包定制

### 快速开始

1. 从 mod jar 中提取 `assets/minecraft/client/` 到资源包（含完整设计模板）
2. 或只复制 `design.json` 到 `assets/minecraft/client/design.json`，按需覆盖字段

`_` 开头的键为注释，未覆盖的字段自动回退 mod 内置默认值。

### 可定制项

| 段 | 内容 |
|----|------|
| `textures` | 立绘 `bocchi` / `gotoh`（poulsen 主题）、加载图 `bocchi_loading`、Logo `logo`、图片块 `gotoh_image_1/2` |
| `svgs` | 主菜单按钮图标：`single` / `multi` / `option` / `quit` / `lang` / `theme` |
| `fonts` | Skia 字体，键 = FontSet 字体名，值 = ttf 文件路径（更换需重启） |
| `colors` | 唱片配色 `vinyl_*`，格式 `#RRGGBB` 或 `#AARRGGBB` |
| `texts` | 界面文案：主菜单大标题/姓名框/介绍（`mInfoLine1~3`）、面板标题与版权行、poulsen 姓名假名别名、信息条等，键见 Bocchi Designer 文本面板 |
| `menu` | `theme`: `"misayos"`（默认）/ `"poulsen"` |

### 加载动画规格

`bocchi_loading.png`：1800×90 横向雪碧图，20 帧，每帧 90×90，20 FPS，从左到右播放。模板 zip 内含 `loading-sprite-template.png` 供参考。

---

## 项目结构

```
bocchi-mod/
├── src/
│   ├── bocchi-1.21.5/
│   │   ├── common/src/main/
│   │   │   ├── java/me/baier/
│   │   │   │   ├── design/Design.java       # 设计资源管理器
│   │   │   │   ├── client/                  # 主菜单 / 启动页 UI
│   │   │   │   ├── graphics/                # Skia 渲染 / 字体 / Shader
│   │   │   │   ├── skui/                    # 自研 UI 组件库
│   │   │   │   └── utils/ResPack.java       # 资源读取工具
│   │   │   └── resources/assets/minecraft/client/
│   │   │       ├── design.json              # 设计模板
│   │   │       └── textures/ svgs/ fonts/
│   │   ├── fabric/                          # Fabric 入口 + 构建
│   │   └── neoforge/                        # NeoForge 入口 + 构建
│   └── bocchi-1.21.1/              # 同上结构
├── tools/
│   ├── build-all.py                        # 全变体一键构建
│   ├── check-version.py                    # 版本一致性(两树 mod_version + 文档无旧版本)
│   ├── check-sync.py                       # 双树源码同步(fail-closed)
│   ├── sync-allowlist.txt                  #   └ 双树差异例外登记处
│   └── bocchi-designer/                     # Design Editor (Web UI)
├── docs/
│   ├── VERIFY.md                           # 改代码前该跑什么(验证清单)
│   └── UNATTENDED_LOG.md                   # 无人值守任务登记
├── release/
│   └── 1.21.x/{fabric,neoforge}/vanilla/   # 预构建 jar（build-all.py 生成）
└── .github/workflows/
    ├── build-pr.yml                        # PR 门禁: 版本/同步一致性 + 双树单测与构建
    ├── build-release.yml                   # CI: 构建 + Release 发布
    └── web-tool.yml                        # 门禁: designer 单测 + 布局漂移检查
```

---

## CI / CD

三条 workflow，职责不重叠：

| workflow | 触发 | 做什么 |
|---|---|---|
| `build-pr.yml` | `src/**`、`README.md`、`docs/**`、校验脚本 | 快门禁：版本一致性 + 双树同步；改动 Java 源码时追加双树 `:common:test` 与构建 |
| `web-tool.yml` | `src/**`、`tools/**` | Designer 单测（`npm test`）+ 布局漂移检查（`check-layout.py` 双树） |
| `build-release.yml` | 推 `v*` tag / 手动 dispatch | preflight 校验 → 双树构建与发布 |

### 发布

```bash
git tag v1.0.2 && git push origin v1.0.2
```

> 推 tag 后产生的 Release 名是 **`v1.0.2-1.21.5`** 与 **`v1.0.2-1.21.1`**，不是 `v1.0.2`。
> 手动触发（Actions → Build & Release → Run workflow）则发到滚动 tag `1.21.5` / `1.21.1`，
> 每次触发都会用最新产物覆盖这两个 Release 的附件。

发布前的 preflight 会拦下两类问题，不通过就不会开始构建：

- **版本割裂**：两棵树 `mod_version` 不一致。只升一棵树时，两个发布步骤各自用本树的版本号
  拼 jar 路径，都会命中、都会绿，于是发出「1.21.5=1.0.2 / 1.21.1=1.0.1」而两个 Release
  同名的割裂组合 —— 只有这里能拦住。
- **双树漂移**：共享区域出现未登记的差异。

### 改代码前该跑什么

见 [`docs/VERIFY.md`](docs/VERIFY.md)。本地校验项比 CI 多（双树哈希核对、截图人眼验收），
以那份清单为准。

---

## 致谢

| 项目 | 作者 | 许可证 | 用途 |
|------|------|--------|------|
| [bocchi-template-1.21.5](https://github.com/baier233/bocchi-template-1.21.5) | baier233 | CC0-1.0 | 代码基础 |


## 许可证

本项目基于 [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/)（公有领域）的 [bocchi-template](https://github.com/baier233/bocchi-template-1.21.5) 构建，按 **CC0-1.0** 授权（见根目录 `LICENSE`）。
