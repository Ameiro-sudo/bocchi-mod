# Bocchi Designer

Bocchi Client 的 **Design Editor**: 在浏览器里 1:1 复刻三个游戏界面舞台
(加载页 / misayos 主菜单 / poulsen 主菜单), 编辑文案 / 配色 / 布局微调 /
替换纹理与字体, 并导出可直接使用的材质包 (zip) 或 `design.json`。

**零运行时依赖**: 纯原生 ES Module + CSS, 无打包器、无 npm 依赖 ——
浏览器直接加载 `index.html`, 不经过任何构建步骤。`devDependencies` 里的
`puppeteer-core` **只服务于门禁脚本**, 与页面运行无关; `package.json` 的
`scripts` 仅仅是别名。

## 快速开始

**Windows**: 双击 `start-designer.bat` —— 自动起本地服务(最小化窗口, 关掉即停)
并打开浏览器; 已在运行则直接复用。

命令行方式:

```bash
cd tools/bocchi-designer
python -m http.server 8833 --bind 127.0.0.1   # 或: npm run serve
# 打开 http://127.0.0.1:8833/
```

> 为什么不能直接双击 index.html: 页面是 ES Module, 浏览器禁止模块脚本跑在
> `file://` 下(CORS); 且资源内嵌 fetch、字体上传、zip 导入/导出本就依赖网络 API。
> 需要任意静态服务器即可, 无其他依赖。

## 架构 (ES Module 依赖图)

```
main.js (组装根 + 启动)
├─ core.js        应用状态 / DOM 助手 / toast / ZIP 读写(零依赖) / 节流
├─ facts.js       布局常量单一数据源 (纯模块, 浏览器+Node 双端可导入)
├─ sliders.js     16 组滑杆值域与默认值 (纯数据, 不碰 DOM, 可直接单测)
├─ design.js      design.json 数据模型 + layout 段换算 + localStorage 持久化
│   ├─ core(state)
│   └─ facts(布局比例的基准值)
├─ fonts.js       字体度量 (Skia 约定换算) + FontFace 注册
│   └─ design(S)
├─ history.js     撤销/重做栈 (纯逻辑: 时间窗合并 + 手势括号 + 回放守卫)
├─ preview.js     预览资源刷新 (纹理/SVG/唱片配色)
├─ layout.js      三舞台布局计算 (全部魔法数字取自 facts.js)
├─ render.js      重排编排 (relayout / scheduleRelayout / 重排后回调注入点)
├─ ov.js          布局微调值域 (滑杆注册表 / setOV / 复位, 经 history 入栈)
├─ status.js      状态栏写入器
├─ panels.js      控制面板的组装根 (只负责按顺序建段 + 转出出口)
│   └─ ui/        一种关注点一个文件, 拆分的理由见下
│       ├─ section.js       折叠分区骨架 + 网格小标题
│       ├─ slider.js        布局微调滑杆行
│       ├─ preview-color.js 预览配色 (只调设计器自己的主题)
│       ├─ res.js           纹理/SVG/字体行 + 内置资源可达性探针
│       ├─ color.js         design.json colors 段行
│       ├─ text.js          文案行 (与舞台元素双向关联)
│       ├─ theme.js         menu.theme 选择
│       ├─ exportbar.js     design.json 预览 + 导出区
│       ├─ dirty.js         「已改 N 项」标记
│       └─ baseline.js      撤销基准登记 (导入后一次全量对齐)
├─ interactions.js 舞台切换 / 入场动画 / 选中拖拽 / 键盘微调 / 缩放
└─ io.js          材质包导入导出 (applyDesignJSON 为纯模型变更; 导入清空历史)
```

模块间无环 (25 个模块 / 94 条依赖边)。跨层协作通过 **main.js 组装期依赖注入**
(替代全局服务定位器):

| 注入点 | 说明 |
| --- | --- |
| `interactions.hooks.focusText` | 双击舞台文本 -> 面板输入框定位 |
| `render.setAfterRelayout()` | 重排后选中框跟随 |
| `io.onModelImported()` | 导入 zip/design.json 后的 UI 全量同步 |

> **`panels.js` 为什么被拆开**：它一度是 630 行的上帝模块 —— 分区骨架、滑杆、预览
> 配色、资源上传、colors 段、文本行、主题下拉、json 预览、导出按钮，八类关注点挤在
> 一个 170 行的 `build()` 里。改配色行要读文本行的撤销基准怎么维护，删一个导出按钮
> 要在四百行里找。拆开不是为了行数好看，是为了让「一种关注点一个文件」之后，每种改动
> 只需要读一个文件。拆分是**纯结构**：`js/ui/*` 的每个文件都是原函数原样搬过去，
> `npm run verify` 在不重建基线的前提下直接等价。

## 面板上的三件新增事

- **「已改 N 项」**: 顶栏计数 + 行上粉色标记。与默认值不同的配色 / 文本 /
  资源路径 / 主题会被标出来 —— 60 多个可编辑项, 靠肉眼比对既漏改也看不出
  「已经改回去了」。标记跟撤销走, 撤回来就灭。
- **资源路径可编辑**: 资源行的路径是输入框而非只读文本, 改完回车生效、可撤销。
  换命名空间 / 换文件名不必再绕开工具手改 JSON。空路径会被拒绝并回填。
- **内置资源缺失标记**: `assets/fonts/meiryo-bold.ttf` 没有随工具分发 (它在 mod 里
  是 9.3MB)。面板对每个内置资源做一次 HEAD 探针, 该在而不存在的路径显示为
  **虚线橙框**, 不会让人以为它会进包。

## 布局常量同步 (facts)

`js/facts.js` 是游戏端 Java 布局魔法数字的**唯一记录点**
(`expr` 公式 + `java` 源文件行号)。修改 Java 布局后运行:

```bash
npm run check-layout          # = python sync/check-layout.py
python sync/check-layout.py --tree bocchi-1.21.5   # 或 bocchi-1.21.1
```

- 求值走 **Node 直读 facts.js** (`sync/facts-dump.mjs`, 与浏览器预览同一份代码);
  Node 缺失时回退内置正则解析器。
- Java 逻辑画布是 480x270, facts 默认帧是预览的 1280x720 —— 同一组公式成比例,
  检查器通过 `valueIn(name, 480, 270)` 对齐参考系。
- ⚠️ **Java 赋值必须写成单行**: 求值器按行匹配 `^(\w+)\s*=\s*(.+)$`, 一条赋值
  被格式化工具折成两行, 绑定就整条消失, 连带一批 fact 误报「Java 已重构」。
- `Design.num(key, fallback)` 在求值器里被替换成 `fallback` —— 因为它未覆盖时
  就等于 fallback, 这正是要检验的语义。

## 布局微调 ⇄ design.json

面板 16 个滑杆里有 **9 个进产物** (`design.json` 的 `layout` 段), 另外 7 个
标「仅预览」—— 游戏端不读它们 (Skia 旋转 / 描边字号之类), 导出去只会让人误以为
改了生效。两种口径由 `test/sliders.test.mjs` 钉死为「恰好划分 16 个、无重叠」。

- **尺寸类键**写绝对比例 (`misayos.tachieH` / `recordSize` / `block1SizeW/H`),
  缺省 = mod 内置值;
- **偏移类键**写相对内置位置的偏移比例 (`…Offset`, 缺省 0), 于是游戏端
  `screenWidth * Design.num("layout.misayos.tachieXOffset", 0f)` 即可, 不需要知道
  基准值, 少一次抄错常量的机会。

Java 侧只多了一个 `Design.num()` —— `Design.merge()` 早就把任意段落的标量键
收进 `VALUES`, 查 `layout.<键>` 直接可用, 不覆盖时行为与改动前逐字节一致。

## 撤销 / 重做

布局微调（滑杆/画布拖拽/方向键/复位）、文本、配色、主题、预览配色、资源替换与
资源路径修改均入撤销栈：`Ctrl+Z` 撤销，`Ctrl+Y` / `Ctrl+Shift+Z` 重做（输入框聚焦
期间交给浏览器原生撤销，失焦后走模型栈）。连续拖动按同键 700ms 时间窗合并为一步；
画布拖拽以手势为单位合并；导入 zip/design.json 会整体替换模型并清空历史。资源行与
配色行的「内置」按钮一键还原 mod 内置默认（同样可撤销）。核心逻辑在 `js/history.js`
（纯模块，`test/history.test.mjs` 覆盖）。

导入前会列出将被丢弃的内容（文案/配色/主题、布局微调、上传的 n 个资源）并要求确认
—— 上传的 blob 只存在内存里，刷新即还原；**确有未导出的上传资源时**关页还会拦一道。

## 测试

```bash
npm test                      # node --test test/  —— 49 个用例，纯 Node，无需浏览器
```

覆盖: 表达式求值器与绑定词边界、循环引用、design.json 组装与原型污染防护、
滑杆值域完整性 + 「默认值 == mod 内置常量」跨模块断言、layout 段往返、
ZIP 往返、`download` 的 URL 生命周期、字体度量、debounce / rAF 节流、撤销栈。

## 门禁

```bash
npm test                # 单元测试
npm run check-layout    # facts ⇄ Java 漂移 (双树)
npm run verify          # 行为快照 + 四组断言探针, 与已入库基线比对
npm run verify:baseline # 确认差异都是预期的之后, 重建基线
```

`dev/verify.mjs` 自起随机端口静态服务、自找浏览器, **零环境变量可跑**。它做两件事:

1. **快照比对** —— 采 `<header>` + `#controls` 的结构化节点表 (tag/class/id/text/
   attrs/hidden/rect)、`#jsonPreview` 指纹、内置资源探针结果, 与
   `dev/dumps/baseline.json` 比对。`npm run verify` 退出码 0 = 等价。
2. **断言探针** —— 快照看不见的地方用它补, 违反即 exit 1:
   - `uiProbe` — 舞台不应整块可 hover 描虚线; Esc 后状态栏不得为空。
   - `sliderProbe` — 滑杆真实 input 事件必须联动舞台几何。
   - `historyProbe` — 走用户路径派发 input/change, 断言「改完能撤回来」:
     入栈条数、回放后基准是否刷新、撤销后再失焦**不产生伪造条目**、
     预览配色回放是否同时还原 CSS 变量、「已改」标记是否跟撤销走。
   - `responsiveProbe` — 压到 1000/640 两宽, 断言无横向溢出、面板仍在视口内、
     「适应」后预览区无横竖滚动。

> **重构守则**: 行为不变的重构应让 `npm run verify` 直接等价通过, **不需要重建基线**。
> 需要重建基线 = 这次改动确实改了可见行为, 得逐条核对差异后再重建。
>
> **重建基线后必须连跑两次 `npm run verify`**: 静态服务端口是随机的, 只跑一次
> 无法排除「第二次采集才出现的差异」。

## 导出产物

材质包 zip = `pack.mcmeta`(pack_format 46, supported_formats 33-9999) +
`assets/minecraft/client/design.json` + 全部引用资源; 未上传的内置资源缺失时
跳过并在 toast 提示, 游戏端回退 mod 内置默认。`design.json` 键语义见根 README
「资源包定制」一节, 与 Java 端 `Design.java` 的累加覆盖语义一致。