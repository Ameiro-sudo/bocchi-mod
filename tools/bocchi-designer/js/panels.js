/* ============================================================================
 * panels.js - 右侧控制面板的组装根
 *
 * 这里只做两件事: 按固定顺序把各个分段建出来, 以及把分段模块的少数几个出口
 * 转出去 (main.js 只需要这几个)。每一行的 DOM 构建、值域、撤销、脏标记都在
 * js/ui/ 下的分段模块里 —— 拆开的理由不是行数好看, 而是「一种关注点一个文件」
 * 之后, 改配色行不必去读文本行的撤销基准怎么维护。
 *
 * 依赖方向: ui/* 依赖 ov/design/render/history 等下层, 只往上依赖彼此的
 * dirty.js (脏标记) 与 baseline.js (撤销基准登记); panels.js 依赖全部。
 * 无环。
 * ==========================================================================*/
import { SLIDER_SPEC } from "./sliders.js";
import { resetAll, flushClamped } from "./ov.js";
import { addSection, addGroupLabel } from "./ui/section.js";
import { addSlider } from "./ui/slider.js";
import { addPreviewColor } from "./ui/preview-color.js";
import { addResRow } from "./ui/res.js";
import { addColorRow } from "./ui/color.js";
import { addTextRow } from "./ui/text.js";
import { addThemeRow } from "./ui/theme.js";
import { addJsonPreview, addExportBar } from "./ui/exportbar.js";
import { refreshDirtyMarks } from "./ui/dirty.js";

/* ---------- 分段模块的出口 ----------
 * 只有 main.js 消费它们, 在这里转出去而不是让 main.js 直接 import js/ui/*,
 * 是为了「面板对外接口」保持在文件头部一目了然。 */
export { resyncBaselines } from "./ui/baseline.js";
export { focusTextInput, applyAllTexts } from "./ui/text.js";
export { updateResNames, probeBundledAssets } from "./ui/res.js";
export { refreshDirtyMarks } from "./ui/dirty.js";

/* ---------- 组装面板 ---------- */
export function build() {
  /* 预览配色 */
  let body = addSection("预览配色（仅预览，不导出）", "colors-preview");
  const cg = document.createElement("div");
  cg.className = "grid";
  body.appendChild(cg);
  addPreviewColor(cg, "主色 accent", "--accent");
  addPreviewColor(cg, "深粉 accent-deep", "--accent-deep");
  addPreviewColor(cg, "背景顶", "--bg-top");
  addPreviewColor(cg, "背景底", "--bg-bottom");
  addPreviewColor(cg, "加载页背景", "--splash-bg");
  addPreviewColor(cg, "按钮底色", "--btn-bg");

  /* misayos 布局微调 */
  body = addSection("misayos 布局微调（画布上可直接拖拽）· 标「仅预览」的滑杆不进 design.json", "layout-misayos", {
    tool: { label: "全部复位", onClick: () => resetAll() },
    badge: "↑↓←→ 微调 · Shift×10 · 双击滑杆标签复位 · Ctrl+Z 撤销",
  });
  const lg = document.createElement("div");
  lg.className = "grid";
  body.appendChild(lg);
  // 16 组值域来自 sliders.js (纯数据), 理由见那里的注释: 定义埋在 build() 里时,
  // 「默认值等于 mod 内置常量」这个不变量没有任何地方能守住。
  for (const s of SLIDER_SPEC) addSlider(lg, s.label, s.key, s.min, s.max, s.def, s.step);
  const hint = document.createElement("div");
  hint.className = "hint";
  hint.innerHTML = "画布上: 点击选中 → 拖拽移动 / 拖角缩放; 双击文字定位到编辑框; Esc 取消选中; 方向键微调。布局/文本/配色/资源替换均可 Ctrl+Z 撤销、Ctrl+Y 重做。导出时写入 design.json 的 layout 段 —— 标「仅预览」的 7 个滑杆游戏端不读, 不导出。";
  body.appendChild(hint);

  /* 文本内容 */
  body = addSection("文本内容 texts（导出 design.json）", "texts", {
    badge: "游戏内已接入 texts 段",
  });
  const t2 = document.createElement("div");
  t2.className = "grid";
  body.appendChild(t2);
  addTextRow(t2, "大标题 BOCCHI", "mBocchi");
  addTextRow(t2, "副标题 THE ROCK", "mRock");
  addTextRow(t2, "姓名框 Gotoh Hitori", "mBoxGotoh");
  addTextRow(t2, "描述框 A reclusive girl", "mBoxGirl");
  addTextRow(t2, "标语 SOCIAL PHOBIA", "mPhobia");
  addTextRow(t2, "介绍 (支持<br>)", "mInfo");
  addTextRow(t2, "面板标题", "pTitle");
  addTextRow(t2, "版本号", "pVer");
  addTextRow(t2, "分支", "pBranch");
  addTextRow(t2, "版权行 1", "pCopy1");
  addTextRow(t2, "版权行 2", "pCopy2");
  addTextRow(t2, "poulsen 姓 HITORI (上)", "pHitoriTop");
  addTextRow(t2, "poulsen 姓 HITORI (下)", "pHitoriBottom");
  addTextRow(t2, "poulsen 名 GOTO (上)", "pGoto1");
  addTextRow(t2, "poulsen 名 GOTO (下)", "pGoto2");
  addTextRow(t2, "poulsen 姓名", "pJName");
  addTextRow(t2, "poulsen 假名", "pJKana");
  addTextRow(t2, "poulsen 别名", "pAliasText");
  addTextRow(t2, "信息条 1", "pAdd1");
  addTextRow(t2, "信息条 2", "pAdd2");
  addTextRow(t2, "信息条 3", "pAdd3");
  addGroupLabel(t2, "设置界面 (游戏内 CFGS 面板, 舞台上无对应元素, 不预览)");
  addTextRow(t2, "设置面板标题", "sTitle");
  addTextRow(t2, "完成按钮", "sDone");

  /* 纹理 */
  body = addSection("纹理 textures（上传即预览，导出时打包）", "res-textures");
  const textureLabels = {
    bocchi: "立绘 bocchi（misayos）", gotoh: "立绘 gotoh（poulsen）", gotoh_image_1: "poulsen 方块图 1",
    gotoh_image_2: "poulsen 方块图 2", bocchi_loading: "加载动画雪碧图", logo: "Logo",
  };
  for (const key of Object.keys(textureLabels)) addResRow(body, textureLabels[key], "textures", key);

  /* SVG */
  body = addSection("按钮图标 svgs", "res-svgs");
  const svgLabels = { single: "单人游戏", multi: "多人游戏", option: "选项", lang: "语言", quit: "退出", theme: "主题切换" };
  for (const key of Object.keys(svgLabels)) addResRow(body, svgLabels[key], "svgs", key);

  /* 字体 */
  body = addSection("字体 fonts（键 = FontSet 字体名，换字体需重启游戏）", "res-fonts");
  const fontLabels = {
    "Radikal-Black": "Radikal-Black", "Radikal-Regular": "Radikal-Regular", "meiryo-bold": "meiryo-bold",
    "SourceHanSansSC-Light": "思源黑体 Light", "SourceHanSansSC-Regular": "思源黑体 Regular",
    "SourceHanSansSC-Heavy": "思源黑体 Heavy", "SourceHanSansSC-Normal": "思源黑体 Normal", "SourceHanSansSC-Bold": "思源黑体 Bold",
  };
  for (const key of Object.keys(fontLabels)) addResRow(body, fontLabels[key], "fonts", key);

  /* 配色 */
  body = addSection("配色 colors（design.json 的 colors 段，#RRGGBB / #AARRGGBB）", "res-colors");
  const colorLabels = {
    vinyl_edge: "唱片外缘 vinyl_edge", vinyl_base: "唱片盘面 vinyl_base", vinyl_groove: "音轨 vinyl_groove",
    vinyl_shine_1: "高光 1 vinyl_shine_1", vinyl_shine_2: "高光 2 vinyl_shine_2",
    vinyl_shine_3: "高光 3 vinyl_shine_3", vinyl_label: "中心标签 vinyl_label",
  };
  for (const key of Object.keys(colorLabels)) addColorRow(body, colorLabels[key], key);

  /* menu 主题 */
  body = addSection("menu（主菜单主题）", "menu-theme");
  addThemeRow(body);

  /* design.json 预览 */
  body = addSection("design.json 实时预览", "json");
  addJsonPreview(body);

  /* 导出 */
  body = addSection("导出", "export", { badge: "pack.mcmeta + design.json + 全部资源" });
  addExportBar(body);

  // 滑块建完才知道哪些持久化值被夹过, 此时一次性落盘 (见 ov.flushClamped)
  flushClamped();
  // 所有行都建完了才能数「已改 N 项」—— 注册期只有行元素, 比较要等状态就位
  refreshDirtyMarks();
}