# Bocchi Client

基于 [bocchi-template](https://github.com/baier233/bocchi-template-1.21.5) 的 Minecraft 客户端模组：

<img width="1919" height="1032" alt="m主界面" src="https://github.com/user-attachments/assets/feb2c740-d3d9-4809-a364-29de31527f2b" />
<img width="1915" height="1033" alt="p主界面" src="https://github.com/user-attachments/assets/5cde75b6-09f8-41fa-bb07-dfac8cd543f1" />
<img width="1919" height="1031" alt="开屏" src="https://github.com/user-attachments/assets/4277ce86-ada4-4930-ba8a-15239c15ebe7" />
<img width="1919" height="995" alt="编辑器" src="https://github.com/user-attachments/assets/db250c4d-bbc7-40d7-9e0e-ca4de57c9303" />

## 版本

支持1.21.1与1.21.5的Fabric与Neoforge端

## 原版

本仓库基于 [baier233/bocchi-template-1.21.5](https://github.com/baier233/bocchi-template-1.21.5)，

**新增的能力**

- Design 设计系统：全部资源/配色/主题路径集中到 `design.json`，材质包可覆盖、可热重载
- Theme 主题系统：misayos / poulsen 双主题运行时切换（`theme/` 包 + `menu.theme` 键）
- neoforge 加载画面适配（fabric/neoforge 行为一致）
- 一键构建全部变体：`tools/build-all.ps1`
- 详细差异对照见 `bocchi--MC-1.21.5-main-与原版差异.md`

## 构建

需要 JDK 21。

```bash
# 1.21.5
cd src/bocchi--MC-1.21.5-main
gradlew :fabric:build :neoforge:build

# 1.21.1
cd src/bocchi--MC-1.21.1-main
gradlew :fabric:build :neoforge:build
```

产物位置：
- Fabric：`fabric/build/libs/bocchi-fabric-<version>-<ver>.jar`
- NeoForge：`neoforge/build/libs/bocchi-neoforge-<version>-<ver>-all.jar`

## 安装

- **Fabric**：需要 fabric-loader + fabric-api（放到 `mods/`）
- **NeoForge**：直接放入 `mods/` 即可

## 主题切换

主题是运行时选项，无需换 jar：

1. 复制 `成品/bocchi-design-template-<version>.zip`（或 `design.json`）到材质包
   `assets/minecraft/client/design.json`
2. 把 `menu.theme` 改成 `"misayos"` 或 `"poulsen"`
3. 游戏内装载材质包（或重载资源）即时生效

## 材质包定制

1. 复制 `成品/bocchi-design-template-<version>.zip` 到资源包文件夹并启用（包内附 README.txt 用法）
2. 或只复制 `design.json` 到材质包 `assets/minecraft/client/design.json`，改哪项覆盖哪项，`_` 开头的键是注释，自动回退 mod 内置默认

可定制项（详见包内 `design.json`）：

| 段 | 内容 |
| --- | --- |
| `textures` | 立绘 `bocchi` / `gotoh`（poulsen 主题）、加载图 `bocchi_loading`、Logo `logo`、图片块 `gotoh_image_1/2` |
| `svgs` | 主菜单按钮图标：`single/multi/option/quit/lang/theme`（主题切换按钮） |
| `fonts` | Skia 字体，键 = FontSet 字体名，值 = ttf 文件路径（换字体需重启） |
| `colors` | 唱片配色 `vinyl_*`，`#RRGGBB` 或 `#AARRGGBB` |
| `menu` | `theme`: `"misayos"`（默认）/ `"poulsen"`，材质包覆盖即切主题（优先于游戏内按钮的 `~/.bocchi/theme.json`） |

**加载动画图 `bocchi_loading.png` 规格**（遮挡动画）：1800×90 横向雪碧图，20 帧、每帧 90×90、20 FPS（左→右）。模板 zip 内含带网格标注的 `loading-sprite-template.png` 供参考。

