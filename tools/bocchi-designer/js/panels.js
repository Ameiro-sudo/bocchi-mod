/* ============================================================================
 * panels.js - 右侧控制面板 (可折叠分区 / 滑杆 / 文本 / 资源 / 配色 / 导出)
 *
 * OV 滑杆的值域逻辑在 ov.js (注册表/设值/复位); 本模块只负责行 DOM 的创建与文案。
 * ==========================================================================*/
import { $, toast } from "./core.js";
import { state } from "./core.js";
import { escapeHtml } from "./core.js";
import {
  S, DEFAULT_DESIGN, DEFAULT_TEXTS, buildDesignJSON, hexToCss, saveState, setBlob, localAsset,
} from "./design.js";
import { refreshPreviews, refreshVinyl } from "./preview.js";
import { FONT_SET_NAME, replaceFace } from "./fonts.js";
import { registerSlider, clampSaved, onSliderInput, resetAll, setFill, setOV } from "./ov.js";
import { relayout, scheduleRelayout } from "./render.js";
import { exportPack, exportJson, copyJson } from "./io.js";
import { push as pushHistory } from "./history.js";

const OV = state.OV;

/* ---------- 折叠分区 ---------- */
const controls = $("controls");
function addSection(title, id, opts) {
  opts = opts || {};
  const sec = document.createElement("div");
  sec.className = "sec" + (state.openSections.includes(id) ? " open" : "");
  sec.id = "sec-" + id;
  const head = document.createElement("div");
  head.className = "sec-head";
  const caret = document.createElement("span");
  caret.className = "caret";
  const h = document.createElement("h2");
  h.textContent = title;
  head.append(caret, h);
  if (opts.tool) {
    const t = document.createElement("button");
    t.className = "sec-tool";
    t.textContent = opts.tool.label;
    t.addEventListener("click", opts.tool.onClick);
    head.appendChild(t);
  }
  const badge = document.createElement("span");
  badge.className = "badge";
  badge.textContent = opts.badge || "";
  head.appendChild(badge);
  const body = document.createElement("div");
  body.className = "sec-body";
  head.addEventListener("click", (e) => {
    if (e.target.closest(".sec-tool")) return;
    sec.classList.toggle("open");
    const i = state.openSections.indexOf(id);
    if (sec.classList.contains("open")) { if (i < 0) state.openSections.push(id); }
    else if (i >= 0) state.openSections.splice(i, 1);
    saveState();
  });
  sec.append(head, body);
  controls.appendChild(sec);
  return body;
}

/* ---------- 滑杆 (双击标签复位) ---------- */
function addSlider(body, label, key, min, max, def, step) {
  const row = document.createElement("div");
  row.className = "slider-row";
  const lab = document.createElement("label");
  lab.textContent = label;
  lab.title = "双击复位到默认值 " + def;
  lab.style.cursor = "pointer";
  const input = document.createElement("input");
  input.type = "range"; input.min = min; input.max = max; input.step = step || 1;
  // L8: 持久化值可能越界, 载入时收敛到滑块范围
  input.value = clampSaved(key, min, max, def);
  const val = document.createElement("span");
  val.className = "val" + (input.value == def ? " is-default" : "");
  val.textContent = input.value;
  lab.addEventListener("dblclick", () => setOV(key, +def));   // 复位单项 (可撤销)
  input.addEventListener("input", () => onSliderInput(key, +input.value));
  setFill(input);
  row.append(lab, input, val);
  body.appendChild(row);
  registerSlider(key, def, input, val);
}

/* ---------- 预览配色 ---------- */
function addPreviewColor(body, label, key) {
  const wrap = document.createElement("div");
  wrap.className = "field";
  const lab = document.createElement("label");
  lab.textContent = label;
  const input = document.createElement("input");
  input.type = "color";
  input.value = state.PREVIEW_COLORS[key];
  input.addEventListener("input", () => {
    document.documentElement.style.setProperty(key, input.value);
    state.PREVIEW_COLORS[key] = input.value;
    if (key === "--btn-bg") document.querySelectorAll(".btn, .btn-icon").forEach(el => el.style.background = input.value);
    saveState();
  });
  wrap.append(lab, input);
  body.appendChild(wrap);
}

/* ---------- 资源上传行 ----------
 * applyRes 是"资源态落地"的唯一入口 (上传/撤销/恢复内置共用):
 * blob + path 一起写回, 字体段同步重注册 FontFace, 再刷新预览。 */
function applyRes(sec, key, blob, path) {
  const entry = S[sec][key];
  setBlob(sec, key, blob);
  if (entry) entry.path = path;
  if (sec === "fonts") {
    const cssFam = FONT_SET_NAME[key];
    // blob 与内置文件二选一作为字体源; 注册完成后再重排 (度量准确)
    if (cssFam && (blob || path)) replaceFace(cssFam, blob || localAsset(path)).then(() => relayout()).catch(() => {});
  }
  updateResName(sec, key);
  refreshPreviews();
  relayout();
}

function addResRow(body, label, sec, key) {
  const row = document.createElement("div");
  row.className = "res-row";
  const lab = document.createElement("div");
  lab.className = "r-label"; lab.textContent = label;
  const name = document.createElement("div");
  name.className = "r-name"; name.id = `rn_${sec}_${key}`;
  const rst = document.createElement("button");
  rst.className = "row-reset"; rst.textContent = "内置";
  rst.title = "恢复 mod 内置默认 (上传/导入路径一并还原, Ctrl+Z 可撤销)";
  const btn = document.createElement("button");
  btn.className = "r-btn"; btn.textContent = "选择文件...";
  const input = document.createElement("input");
  input.type = "file"; input.style.display = "none";
  input.addEventListener("change", () => {
    const f = input.files[0];
    if (!f) return;
    // 记录可撤销的资源替换 (blob 引用互换, 无拷贝开销)
    const prevBlob = S[sec][key].blob, prevPath = S[sec][key].path;
    applyRes(sec, key, f, prevPath);
    pushHistory({
      label: `替换资源 ${label}`,
      undo: () => applyRes(sec, key, prevBlob, prevPath),
      redo: () => applyRes(sec, key, f, prevPath),
    });
    toast(`已上传 ${f.name} (仅本次会话生效, 刷新后还原; Ctrl+Z 可撤销)`);
    input.value = "";
  });
  btn.addEventListener("click", () => input.click());
  // 「内置」: 还原 mod 内置默认路径 + 清除已上传 blob (可撤销)
  rst.addEventListener("click", () => {
    const entry = S[sec][key];
    const defPath = DEFAULT_DESIGN[sec][key];
    if (!entry.blob && entry.path === defPath) { toast(`${label} 已是内置默认`); return; }
    const prevBlob = entry.blob, prevPath = entry.path;
    applyRes(sec, key, null, defPath);
    pushHistory({
      label: `还原资源 ${label}`,
      undo: () => applyRes(sec, key, prevBlob, prevPath),
      redo: () => applyRes(sec, key, null, defPath),
    });
    toast(`已还原内置默认: ${label}`);
  });
  row.append(lab, name, rst, btn, input);
  body.appendChild(row);
}

function updateResName(sec, key) {
  const el = $(`rn_${sec}_${key}`);
  if (!el) return;
  const f = S[sec][key];
  const has = !!(f.blob);
  el.textContent = has ? "已上传: " + f.blob.name : f.path;
  el.classList.toggle("uploaded", has);
}
export function updateResNames() {
  for (const sec of ["textures", "svgs", "fonts"])
    for (const key of Object.keys(S[sec])) updateResName(sec, key);
}

/* ---------- design.json colors 段编辑 ---------- */
function addColorRow(body, label, key) {
  const row = document.createElement("div");
  row.className = "color-row";
  const lab = document.createElement("div");
  lab.className = "c-label"; lab.textContent = label;
  const text = document.createElement("input");
  text.type = "text"; text.value = S.colors[key];
  const pick = document.createElement("input");
  pick.type = "color";
  const m8 = /^#?([0-9a-fA-F]{8})$/.exec(S.colors[key]);
  const m6 = /^#?([0-9a-fA-F]{6})$/.exec(S.colors[key]);
  if (m8) pick.value = "#" + m8[1].slice(2);
  else if (m6) pick.value = "#" + m6[1];
  /** 落地函数 (入栈与回放共用) */
  const applyColor = (value) => {
    S.colors[key] = value;
    text.value = value;
    refreshVinyl();
    relayout();
    saveState();
  };
  let committedColor = S.colors[key];   // 最近一次入栈的值 (时间窗合并的基准)
  /** 值变化后调用: 与 committedColor 比对入栈; 同键连续拖取色器自动合并 */
  const commitColor = () => {
    const cur = S.colors[key];
    if (cur === committedColor) return;
    const from = committedColor, to = cur;
    pushHistory({
      label: `配色 ${key}`,
      undo: () => applyColor(from),
      redo: () => applyColor(to),
    }, "color:" + key);
    committedColor = cur;
  };
  text.addEventListener("change", () => {
    applyColor(text.value.trim() || DEFAULT_DESIGN.colors[key]);
    commitColor();
  });
  pick.addEventListener("input", () => {
    const old = S.colors[key];
    const a = /^#?([0-9a-fA-F]{2})([0-9a-fA-F]{6})$/.exec(old);
    applyColor(a ? "#" + a[1] + pick.value.slice(1) : pick.value);
    commitColor();
  });
  pick.addEventListener("change", commitColor);   // 松手收尾 (合并窗已覆盖, 兜底)
  // 「内置」: 还原 design.json 默认色板 (可撤销)
  const rst = document.createElement("button");
  rst.className = "row-reset"; rst.textContent = "内置";
  rst.title = "恢复默认色值";
  rst.addEventListener("click", () => {
    const def = DEFAULT_DESIGN.colors[key];
    if (S.colors[key] === def) { toast(`配色 ${key} 已是默认值`); return; }
    const prev = S.colors[key];
    applyColor(def);
    committedColor = def;
    pushHistory({
      label: `还原配色 ${key}`,
      undo: () => { applyColor(prev); committedColor = prev; },
      redo: () => { applyColor(def); committedColor = def; },
    });
  });
  row.append(lab, text, pick, rst);
  body.appendChild(row);
}

/* ---------- 文本编辑 (仅预览) ----------
 * input 与舞台元素双向关联: 点舞台元素可定位到对应输入框
 */
const TEXT_INPUTS = {};
/** 文本模型落地函数 (输入/入栈/回放共用); 输入框聚焦时不回写 value 防光标跳动 */
function setTextModel(elId, v, input) {
  state.TEXTS[elId] = v;
  if (document.activeElement !== input) input.value = v;
  applyText(elId, v);
  relayout();
  saveState();
}
function addTextRow(body, label, elId) {
  const row = document.createElement("div");
  row.className = "text-row";
  const lab = document.createElement("div");
  lab.className = "t-label"; lab.textContent = label;
  const dot = document.createElement("span");
  dot.className = "t-dot"; dot.title = "在舞台上双击对应元素可定位到此处";
  const input = document.createElement("input");
  input.type = "text";
  input.value = state.TEXTS[elId] != null ? state.TEXTS[elId] : DEFAULT_TEXTS[elId];
  // 撤销基准: 最近入栈值 -> change (失焦/回车) 时与最新值比对入栈
  const rec = { input, row, committed: input.value };
  input.addEventListener("change", () => {
    const to = input.value;
    if (to === rec.committed) return;
    const from = rec.committed;
    pushHistory({
      label: `文本 ${label}`,
      undo: () => setTextModel(elId, from, input),
      redo: () => setTextModel(elId, to, input),
    });
    rec.committed = to;
  });
  input.addEventListener("input", () => {
    state.TEXTS[elId] = input.value;
    applyText(elId, input.value);
    relayout();
    saveState();
  });
  row.append(dot, lab, input);
  body.appendChild(row);
  TEXT_INPUTS[elId] = rec;
}
const INNER_HTML_IDS = new Set(["mPhobia", "mInfo", "pJKana", "pCopy1", "pCopy2"]);
// L7: 仅放行 <br>, 其余标签/脚本转义 (escapeHtml 见 core.js), 消除自我 XSS 面
function applyText(elId, html) {
  const el = $(elId);
  if (!el) return;
  if (INNER_HTML_IDS.has(elId)) {
    el.innerHTML = String(html).split(/\s*<br\s*\/?>\s*/i).map(escapeHtml).join("<br>");
  } else el.textContent = html;
}
/** 从舞台元素反查文本输入框并高亮 (双击舞台文本时调用) */
export function focusTextInput(elId) {
  const rec = TEXT_INPUTS[elId];
  if (!rec) return false;
  rec.row.scrollIntoView({ behavior: "smooth", block: "center" });
  rec.input.classList.add("flash");
  setTimeout(() => rec.input.classList.remove("flash"), 1200);
  rec.input.focus();
  return true;
}
export function applyAllTexts() {
  for (const elId of Object.keys(DEFAULT_TEXTS)) {
    const v = state.TEXTS[elId] != null ? state.TEXTS[elId] : DEFAULT_TEXTS[elId];
    applyText(elId, v);
    const rec = TEXT_INPUTS[elId];
    if (rec) {
      if (document.activeElement !== rec.input) rec.input.value = v;
      rec.committed = v;   // 模型被导入整体替换后, 撤销基准一并重置
    }
  }
}

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
  body = addSection("misayos 布局微调（画布上可直接拖拽）", "layout-misayos", {
    tool: { label: "全部复位", onClick: () => resetAll() },
    badge: "↑↓←→ 微调 · Shift×10 · 双击滑杆标签复位 · Ctrl+Z 撤销",
  });
  const lg = document.createElement("div");
  lg.className = "grid";
  body.appendChild(lg);
  addSlider(lg, "立绘 X 偏移", "tachieX", -200, 200, 0);
  addSlider(lg, "立绘 Y 偏移", "tachieY", -200, 200, 0);
  addSlider(lg, "立绘高度", "tachieH", 300, 800, 684);
  addSlider(lg, "立绘旋转", "tachieRot", -10, 10, 0, 0.1);
  addSlider(lg, "立绘透明度", "tachieOp", 0, 100, 100);
  addSlider(lg, "唱片大小", "recordSize", 200, 600, 468);
  addSlider(lg, "唱片 X 偏移", "recordX", -200, 200, 0);
  addSlider(lg, "唱片 Y 偏移", "recordY", -200, 200, 0);
  addSlider(lg, "标题字号", "titleSize", 30, 90, 53);
  addSlider(lg, "标题 X 偏移", "titleX", -300, 300, 0);
  addSlider(lg, "标题 Y 位置", "titleY", -100, 100, 0);
  addSlider(lg, "面板宽度", "panelW", 60, 140, 95);
  addSlider(lg, "面板 X 偏移", "panelX", -100, 100, 0);
  addSlider(lg, "block1 大小", "block1", 150, 400, 290);
  addSlider(lg, "block1 X 偏移", "blockX", -200, 200, 0);
  addSlider(lg, "block1 Y 偏移", "blockY", -200, 200, 0);
  const hint = document.createElement("div");
  hint.className = "hint";
  hint.innerHTML = "画布上: 点击选中 → 拖拽移动 / 拖角缩放; 双击文字定位到编辑框; Esc 取消选中; 方向键微调。布局/文本/配色/资源替换均可 Ctrl+Z 撤销、Ctrl+Y 重做。";
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
  const themeRow = document.createElement("div");
  themeRow.className = "slider-row";
  const themeLab = document.createElement("label");
  themeLab.textContent = "theme";
  const themeSel = document.createElement("select");
  themeSel.id = "themeSel";
  for (const [v, l] of [["misayos", "misayos（默认）"], ["poulsen", "poulsen"]]) {
    const o = document.createElement("option");
    o.value = v; o.textContent = l;
    themeSel.appendChild(o);
  }
  themeSel.value = S.menu.theme;
  /** 主题落地函数 (入栈与回放共用) */
  const applyTheme = (v) => {
    S.menu.theme = v;
    themeSel.value = v;
    relayout();
    saveState();
    toast("主题已改为 " + v + "（design.json menu.theme）");
  };
  themeSel.addEventListener("change", () => {
    const from = S.menu.theme, to = themeSel.value;
    if (from === to) return;
    applyTheme(to);
    pushHistory({
      label: `主题 ${to}`,
      undo: () => applyTheme(from),
      redo: () => applyTheme(to),
    });
  });
  themeRow.append(themeLab, themeSel);
  body.appendChild(themeRow);
  const th = document.createElement("div");
  th.className = "hint";
  th.textContent = "游戏内优先级: ~/.bocchi/theme.json（游戏内切换）> 材质包 design.json > mod 内置默认。";
  body.appendChild(th);

  /* design.json 预览 */
  body = addSection("design.json 实时预览", "json");
  const jsonPre = document.createElement("pre");
  jsonPre.id = "jsonPreview";
  body.appendChild(jsonPre);

  /* 导出 */
  body = addSection("导出", "export", { badge: "pack.mcmeta + design.json + 全部资源" });
  const row = document.createElement("div");
  row.className = "export-row";
  const exportBtn = document.createElement("button");
  exportBtn.className = "export-btn"; exportBtn.id = "exportBtn";
  exportBtn.textContent = "导 出 材 质 包 (zip)";
  exportBtn.addEventListener("click", () => exportPack());
  const jsonBtn = document.createElement("button");
  jsonBtn.className = "export-btn sec"; jsonBtn.id = "exportJsonBtn";
  jsonBtn.textContent = "下载 design.json";
  jsonBtn.addEventListener("click", () => exportJson());
  row.append(exportBtn, jsonBtn);
  body.appendChild(row);
  const copyBtn = document.createElement("button");
  copyBtn.className = "export-btn sec"; copyBtn.id = "copyJsonBtn";
  copyBtn.textContent = "复制 design.json";
  copyBtn.addEventListener("click", () => copyJson());
  body.appendChild(copyBtn);
  const exportHint = document.createElement("div");
  exportHint.className = "hint";
  exportHint.textContent = "生成 pack.mcmeta + assets/minecraft/client/design.json + 全部引用资源（未上传的资源自动用内置默认）。支持 1.21.1~1.21.5+（pack_format 33-9999）。";
  body.appendChild(exportHint);
}

