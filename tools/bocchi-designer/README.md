# Bocchi Designer

Bocchi Client 的 **Design Editor**: 在浏览器里 1:1 复刻三个游戏界面舞台
(加载页 / misayos 主菜单 / poulsen 主菜单), 编辑文案 / 配色 / 布局微调 /
替换纹理与字体, 并导出可直接使用的材质包 (zip) 或 `design.json`。

**零运行时依赖**: 纯原生 ES Module + CSS, 无打包器、无 npm 依赖。
`package.json` 仅承载脚本别名。

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
├─ design.js      design.json 数据模型 + localStorage 持久化
│   └─ core(state)
├─ fonts.js       字体度量 (Skia 约定换算) + FontFace 注册
│   └─ design(S)
├─ history.js     撤销/重做栈 (纯逻辑: 时间窗合并 + 手势括号 + 回放守卫)
├─ preview.js     预览资源刷新 (纹理/SVG/唱片配色)
├─ layout.js      三舞台布局计算 (全部魔法数字取自 facts.js)
├─ render.js      重排编排 (relayout / scheduleRelayout / 重排后回调注入点)
├─ ov.js          布局微调值域 (滑杆注册表 / setOV / 复位, 经 history 入栈)
├─ status.js      状态栏写入器
├─ panels.js      右侧控制面板 DOM 构建
├─ interactions.js 舞台切换 / 入场动画 / 选中拖拽 / 键盘微调 / 缩放
└─ io.js          材质包导入导出 (applyDesignJSON 为纯模型变更; 导入清空历史)
```

模块间无环。跨层协作通过 **main.js 组装期依赖注入** (替代全局服务定位器):

| 注入点 | 说明 |
| --- | --- |
| `interactions.hooks.focusText` | 双击舞台文本 -> 面板输入框定位 |
| `render.setAfterRelayout()` | 重排后选中框跟随 |
| `io.onModelImported()` | 导入 zip/design.json 后的 UI 全量同步 |

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
- ⚠️ 回退解析器对 facts.js 格式敏感: 组头行必须形如 `name: {` 且以 `{` 结尾,
  fact 行需含 `expr:` 与 `java:` 字段。解析到 0 条常量会以退出码 2 报错 (防静默绿灯)。

## 撤销 / 重做

布局微调（滑杆/画布拖拽/方向键/复位）、文本、配色、主题、资源替换均入撤销栈：
`Ctrl+Z` 撤销，`Ctrl+Y` / `Ctrl+Shift+Z` 重做（输入框聚焦期间交给浏览器原生撤销，
失焦后走模型栈）。连续拖动按同键 700ms 时间窗合并为一步；画布拖拽以手势为单位合并；
导入 zip/design.json 会整体替换模型并清空历史。资源行与配色行的「内置」按钮一键还原
mod 内置默认（同样可撤销）。核心逻辑在 `js/history.js`
（纯模块，`test/history.test.mjs` 覆盖）。

## 测试

```bash
npm test                      # node --test test/
```

零依赖 (Node >= 18 内置 test runner), 覆盖: 表达式求值器与循环引用、
design.json 组装与原型污染防护、ZIP 往返、文案导出转换、debounce 等。

## 行为快照门禁 (重构必备)

改结构不改行为时, 用 `dev/verify.mjs` + `dev/compare.mjs` 做等价性验证:

```bash
# 终端 1: 本地服务
python -m http.server 8833 --bind 127.0.0.1

# 改动前采集基线, 改动后采集新快照并对比
node dev/verify.mjs --out dev/dumps/baseline.json
node dev/verify.mjs --out dev/dumps/head.json
node dev/compare.mjs dev/dumps/baseline.json dev/dumps/head.json   # 退出码 0 = 等价
```

快照内容: 三舞台全部带 id 元素的几何(行内样式, 免疫动画)/关键计算样式/
design.json 内容/console 报错, 外加键盘微调与滑杆真实事件烟测。
定时器驱动的不稳定属性在 `EXCLUDE` 表中维护。

## 导出产物

材质包 zip = `pack.mcmeta`(pack_format 46, supported_formats 33-9999) +
`assets/minecraft/client/design.json` + 全部引用资源; 未上传的内置资源缺失时
跳过并在 toast 提示, 游戏端回退 mod 内置默认。`design.json` 键语义见根 README
「资源包定制」一节, 与 Java 端 `Design.java` 的累加覆盖语义一致。
