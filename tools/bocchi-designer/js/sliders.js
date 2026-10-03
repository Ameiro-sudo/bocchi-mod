/* ============================================================================
 * sliders.js - misayos 布局微调滑杆的唯一定义处
 *
 * 16 组 min/max/def/step 此前直接写死在 panels.js 的 build() 里, 于是:
 *   - 没有任何一处能列出「当前有哪些滑杆、各自值域是什么」;
 *   - 改错一个默认值只有肉眼比对能发现 (改了 684 变 685, 舞台只是「看起来有点怪」);
 *   - 「默认值等于 mod 内置常量」这个本该由测试守住的不变量, 只能靠注释声明 ——
 *     原 test/facts.test.mjs 里那句「滑杆默认值同源」的注释其实断言的是这里的字面量,
 *     改的是字面量、测的也是字面量, 于是它把重复常量这件事掩盖了。
 *
 * 现在把定义搬到这里: panels.js 只负责按数据建行, 测试可以直接 import 并逐项比对
 * facts.js 里由 Java 源码反推出来的内置值 (fact 字段)。
 *
 * fact:  该滑杆默认值应当等于哪个 fact (只在有内置值对应时给; 偏移类缺省 0,
 *        预览类游戏端不读, 都没有 fact)
 * webOnly: 游戏端不读, 不导出 design.json —— 与 design.js LAYOUT_SPEC 的口径一致
 * ==========================================================================*/
export const SLIDER_SPEC = [
  { key: "tachieX",    label: "立绘 X 偏移", min: -200, max: 200, def: 0 },
  { key: "tachieY",    label: "立绘 Y 偏移", min: -200, max: 200, def: 0 },
  { key: "tachieH",    label: "立绘高度", min: 300, max: 800, def: 684, fact: "misayos.tachieH" },
  { key: "tachieRot",  label: "立绘旋转 · 仅预览", min: -10, max: 10, def: 0, step: 0.1, webOnly: true },
  { key: "tachieOp",   label: "立绘透明度 · 仅预览", min: 0, max: 100, def: 100, webOnly: true },
  { key: "recordSize", label: "唱片大小", min: 200, max: 600, def: 468, fact: "misayos.recordSize" },
  { key: "recordX",    label: "唱片 X 偏移", min: -200, max: 200, def: 0 },
  { key: "recordY",    label: "唱片 Y 偏移", min: -200, max: 200, def: 0 },
  { key: "titleSize",  label: "标题字号 · 仅预览", min: 30, max: 90, def: 53, webOnly: true },
  { key: "titleX",     label: "标题 X 偏移 · 仅预览", min: -300, max: 300, def: 0, webOnly: true },
  { key: "titleY",     label: "标题 Y 位置 · 仅预览", min: -100, max: 100, def: 0, webOnly: true },
  { key: "panelW",     label: "面板宽度 · 仅预览", min: 60, max: 140, def: 95, webOnly: true },
  { key: "panelX",     label: "面板 X 偏移 · 仅预览", min: -100, max: 100, def: 0, webOnly: true },
  { key: "block1",     label: "block1 大小", min: 150, max: 400, def: 290, fact: "misayos.block1Size" },
  { key: "blockX",     label: "block1 X 偏移", min: -200, max: 200, def: 0 },
  { key: "blockY",     label: "block1 Y 偏移", min: -200, max: 200, def: 0 },
];

/** 面板一次构建需要的形状; 保持纯数据, 本模块不碰 DOM (因此可在 Node 下直接测) */
export const WEB_ONLY_KEYS = SLIDER_SPEC.filter(s => s.webOnly).map(s => s.key);