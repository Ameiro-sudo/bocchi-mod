# bocchi--MC-1.21.5-main 与原版差异说明

对比对象：
- 原版：`C:\Users\Administrator\Downloads\bocchi--MC-1.21.5-main.zip`
- 当前：`D:\bocchi-mod\src\bocchi--MC-1.21.5-main`

统计：新增 3 个文件，修改 21 个文件，无删除文件（排除 `.gradle/`、`build/` 构建产物后）。

---

## 一、新增文件（3 个）— Design 设计系统

| 文件 | 作用 |
| --- | --- |
| `common/src/main/java/me/baier/design/Design.java` | 设计资源集中管理器：所有纹理/SVG/字体/视频/动画/配色的路径与默认值都收在这里，材质包可通过 `assets/minecraft/client/design.json` 覆盖 |
| `common/src/main/java/me/baier/utils/ResPack.java` | 资源读取工具：优先走 Minecraft ResourceManager（材质包可覆盖），失败回退 classpath |
| `common/src/main/resources/assets/minecraft/client/design.json` | 设计模板：内置默认值 + 材质包覆盖模板（7 段：textures/svgs/fonts/media/animations/shaders/colors） |

## 二、修改文件（21 个）

### 1. Design 迁移（硬编码路径/颜色 → `Design.resource` / `Design.color`）

| 文件 | 改动 |
| --- | --- |
| `client/Bocchi.java` | `DEFAULT_VIDEO = Design.resource("media.default")`，视频改用 `ResPack.open` 读取 |
| `client/ui/splash/SplashUI.java` | 加载图 `Design.resource("textures.bocchi_loading")` |
| `client/ui/common/components/LogoRenderer.java` | Logo `Design.resource("textures.logo")` |
| `client/ui/common/components/AlbumRenderer.java` | 唱片纹理 + 唱片配色全部改为 `Design.color("colors.vinyl_*")` |
| `client/ui/mainmenu/misayos/BgRectsComponent.java` | 背景 `Design.resource("textures.bocchi")` |
| `client/ui/mainmenu/misayos/MainTachieComponent.java` | 立绘 `Design.resource("textures.bocchi")` |
| `client/ui/mainmenu/misayos/childs/ButtonChild.java` | 按钮图标 `Design.resource("svgs." + icon)` |
| `client/ui/mainmenu/poulsen/BgImageComponent.java` | `Design.resource("textures.gotoh")` |
| `client/ui/mainmenu/poulsen/ImagesBlockComponent.java` | `textures.gotoh_image_1/2` |
| `client/ui/mainmenu/poulsen/MainTachieComponent.java` | `Design.resource("textures.gotoh")` |
| `graphics/font/SkiaFont.java` | 字体路径 `Design.resource("fonts." + name, fallback)`，fallback/classpath 路径小写化（原版字体文件名含大写，`ResourceLocation` 不允许） |
| `graphics/pipeline/PassTest.java` | shader 路径 `Design.resource("shaders.*")` |

### 2. 资源包热重载支持

| 文件 | 改动 |
| --- | --- |
| `mixins/transformers/MixinMinecraftClient.java` | 新增 `reloadResourcePacks` 钩子：资源包重载完成后 `Design.reload()` + 清空纹理缓存，装/卸材质包即时生效（原版无此钩子） |
| `graphics/SkiaRenderEngine.java` | 新增 `clearTextureCache()` 方法（原版只有纹理缓存没有清理入口） |

### 3. 其他功能改动（非 Design 迁移）

| 文件 | 改动 |
| --- | --- |
| `client/ui/mainmenu/misayos/GuiComponent.java` | 原版按钮无点击行为；现为 single/multi/option/lang/quit 按钮接上 `setOnClick`（进入单人/多人/设置/语言界面、退出游戏），移除原版的 "alt"（Alt Manager）按钮 |
| `client/ui/mainmenu/misayos/MainMenuMisayosScreen.java` | `renderAsBackground` 时跳过第 5 个组件；ESC 关闭时返回原版 `TitleScreen` 并实现 `onClose` |
| `graphics/media/SKVideoDecoder.java` | 视频解码输出从 AWT `BufferedImage`（每帧分配）改为 Skija `Bitmap` 复用 + 行拷贝，性能优化 |
| `mixins/transformers/MixinInGameHUD.java` | 移除原版的 `PassTest.init()/test()` 后处理测试调用 |

### 4. 构建脚本（Multi-Release 修复）

| 文件 | 改动 |
| --- | --- |
| `fabric/build.gradle.kts` | `remapJar` 后重写 Manifest 补 `Multi-Release: true`（skija 是 multi-release jar，缺失会导致 Java 21 加载旧版 `sun.misc.Cleaner` 崩溃） |
| `neoforge/build.gradle.kts` | `shadowJar` 的 Manifest 加 `Multi-Release: true` |

---

## 三、目录结构（当前 = 原版 + 上述新增）

```
bocchi--MC-1.21.5-main/
├── common/src/main/
│   ├── java/aka/bocchi/injection/          # Mixin 系列
│   ├── java/me/baier/
│   │   ├── animation/                      # Bezier 动画系统
│   │   ├── client/                         # 主菜单/启动页 UI
│   │   ├── design/Design.java              # [新增] 设计资源管理器
│   │   ├── event/                          # 事件系统
│   │   ├── graphics/                       # Skia 渲染/视频/字体/shader 管线
│   │   ├── manager/
│   │   ├── platform/
│   │   ├── skui/                           # 自研 UI 组件库
│   │   └── utils/ResPack.java              # [新增] 资源读取工具
│   └── resources/assets/minecraft/client/
│       ├── design.json                     # [新增] 设计模板
│       ├── textures/ svgs/ fonts/ media/ animations/ shaders/
├── fabric/                                 # Fabric 入口 + 构建
├── neoforge/                               # NeoForge 入口 + 构建
├── buildSrc/ gradle/ build.gradle.kts gradle.properties settings.gradle.kts
```

## 四、注意

- 字体文件已全部重命名为小写（`Kranky-Regular.ttf` → `kranky-regular.ttf` 等），因为 `ResourceLocation` 只允许 `[a-z0-9/._-]`，大写文件名会导致启动崩溃
- 字体与背景视频是静态加载，更换材质包需重启游戏；立绘/背景/Logo/SVG/配色可热重载
- 材质包用法：把 `design.json` 复制到材质包 `assets/minecraft/client/design.json`，改哪项覆盖哪项
