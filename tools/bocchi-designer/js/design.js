/* ============================================================================
 * design.js — design.json 数据模型 + 状态持久化
 *
 * S = 当前编辑态: { textures/svgs/fonts: {key: {path, blob}}, colors: {key: path}, menu: {theme}, texts: {key: 文案} }
 * 上传的资源 (blob) 仅存在内存; 布局/文本/配色等偏好存 localStorage。
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});

  const DEFAULT_DESIGN = {
    textures: {
      bocchi: "client/textures/bocchi.png", gotoh: "client/textures/gotoh.png",
      gotoh_image_1: "client/textures/gotoh_image_1.png", gotoh_image_2: "client/textures/gotoh_image_2.png",
      bocchi_loading: "client/textures/bocchi_loading.png", logo: "client/textures/logo.png",
    },
    svgs: {
      single: "client/svgs/single.svg", multi: "client/svgs/multi.svg", option: "client/svgs/option.svg",
      lang: "client/svgs/lang.svg", quit: "client/svgs/quit.svg", theme: "client/svgs/theme.svg",
    },
    fonts: {
      "Radikal-Black": "client/fonts/radikal-black.ttf", "Radikal-Regular": "client/fonts/radikal-regular.ttf",
      "meiryo-bold": "client/fonts/meiryo-bold.ttf",
      "SourceHanSansSC-Light": "client/fonts/sourcehansanssc-light.ttf",
      "SourceHanSansSC-Regular": "client/fonts/sourcehansanssc-regular.ttf",
      "SourceHanSansSC-Heavy": "client/fonts/sourcehansanssc-heavy.ttf",
      "SourceHanSansSC-Normal": "client/fonts/sourcehansanssc-normal.ttf",
      "SourceHanSansSC-Bold": "client/fonts/sourcehansanssc-bold.ttf",
    },
    colors: {
      vinyl_edge: "#050505", vinyl_base: "#1A1A1A", vinyl_shine_1: "#33FFFFFF", vinyl_shine_2: "#1AFFFFFF",
      vinyl_shine_3: "#001A1A1A", vinyl_groove: "#1FFFFFFF", vinyl_label: "#981A1A1A",
    },
    menu: { theme: "misayos" },
  };

  const S = { textures: {}, svgs: {}, fonts: {}, colors: {}, menu: { theme: "misayos" } };
  for (const sec of ["textures", "svgs", "fonts"]) {
    S[sec] = {};
    for (const [k, v] of Object.entries(DEFAULT_DESIGN[sec])) S[sec][k] = { path: v, blob: null };
  }
  S.colors = { ...DEFAULT_DESIGN.colors };
  // H2: 导入时未知键/未知 section 原样保留, 导出时合并回去 (与 Java 端纯累加覆盖语义一致)
  // H3: extra 用 null 原型容器, 杜绝 __proto__/constructor 键原型污染
  S.extra = Object.create(null);

  /* ---------- H3: 原型污染防护 ----------
   * design.json / zip / localStorage 都是外部输入, 统一经 cleanCopy 过滤危险键后再入模 */
  const UNSAFE_KEYS = new Set(["__proto__", "constructor", "prototype"]);
  function cleanCopy(src) {
    const out = Object.create(null);
    if (!src || typeof src !== "object") return out;
    for (const [k, v] of Object.entries(src)) {
      if (!UNSAFE_KEYS.has(k)) out[k] = v;
    }
    return out;
  }

  const localAsset = (p) => "assets/" + p.replace(/^[a-z0-9_.-]+:/, "").replace(/^client\//, "");
  const usedPath = (sec, k) => (S[sec][k].blob ? blobUrl(S[sec][k]) : localAsset(S[sec][k].path));

  /* ---------- blob ObjectURL 缓存 (M1: 不再每次调用新建, 替换时 revoke) ---------- */
  const blobUrl = (entry) => {
    if (entry._url) return entry._url;
    return (entry._url = URL.createObjectURL(entry.blob));
  };
  function revokeBlobUrl(entry) {
    if (entry._url) { URL.revokeObjectURL(entry._url); entry._url = null; }
  }
  /** 替换/清空资源 blob: 先 revoke 旧 URL, 再写新值 */
  function setBlob(sec, k, blob) {
    const entry = S[sec][k];
    if (entry && entry._url) revokeBlobUrl(entry);
    if (entry) entry.blob = blob || null;
  }

  /* ---------- design.json 路径解析 (H1: 保留命名空间) ---------- */
  const NS_RE = /^([a-z0-9_.-]+):(.+)$/;
  function splitPath(p) {
    const m = NS_RE.exec(p || "");
    return m ? { ns: m[1], rest: m[2] } : { ns: "minecraft", rest: p };
  }
  const zipEntry = (p) => "assets/" + splitPath(p).ns + "/" + splitPath(p).rest;

  /* ---------- 颜色 ---------- */
  function hexToCss(hex) {
    const m = /^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.exec((hex || "").trim());
    if (!m) return null;
    let h = m[1];
    if (h.length === 6) return "#" + h;
    const a = parseInt(h.slice(0, 2), 16) / 255, rgb = h.slice(2);
    return `rgba(${parseInt(rgb.slice(0, 2), 16)},${parseInt(rgb.slice(2, 4), 16)},${parseInt(rgb.slice(4, 6), 16)},${a.toFixed(3)})`;
  }

  /* ---------- 可编辑文本 (导出 design.json 的 texts 段) ----------
   * 注: 不换行空格用 \u00A0 字符而非 &nbsp; 实体 — applyText 会对文本做 HTML 转义,
   * 实体写法会被原样显示成 "&nbsp;" 字面量。导出时经 textsForExport 转纯文本 */
  const DEFAULT_TEXTS = {
    mBocchi: "BOCCHI", mRock: "THE ROCK!",
    mBoxGotoh: "Gotoh Hitori", mBoxGirl: "A reclusive girl",
    mPhobia: "SOCIAL\u00A0\u00A0PHOBIA",
    mInfo: 'Goto, nicknamed "Little Solitude",<br>is a girl who always starts her speech with "Ah..."<br>and is extremely accepting and introverted',
    pTitle: "BOCCHI", pVer: "1.0", pBranch: '"ALPHA"',
    pCopy1: "Bocchi Client\u00A0\u00A0\u00A0\u00A0Version - 1.0", pCopy2: "@COPYRIGHT MISAYO",
    pHitoriTop: "HITORI", pHitoriBottom: "HITORI", pGoto1: "GOTO", pGoto2: "GOTO",
    pJName: "後藤 ひとり", pJKana: "ご\u00A0\u00A0\u00A0とう",
    pAliasText: "ギターヒーロー",
    pAdd1: "FEBRUARY 21", pAdd2: "50 kg & 156 cm", pAdd3: "Aqua eye",
  };
  // 单一数据源: state.TEXTS 与导出用的 S.texts 指向同一对象
  S.texts = { ...DEFAULT_TEXTS };
  BD.state.TEXTS = S.texts;

  const htmlToPlain = (s) => String(s).replace(/\u00A0/g, " ");
  /** 预览文案 → design.json texts 段纯文本 (\u00A0 转空格, mInfo 按 <br> 拆三行) */
  function textsForExport() {
    const o = {};
    for (const [k, v] of Object.entries(S.texts)) {
      if (UNSAFE_KEYS.has(k)) continue;
      if (k === "mInfo") {
        const lines = String(v).split(/\s*<br\s*\/?>\s*/i);
        for (let i = 0; i < 3; i++) o["mInfoLine" + (i + 1)] = htmlToPlain(lines[i] != null ? lines[i] : "");
      } else o[k] = htmlToPlain(v);
    }
    return o;
  }

  /* ---------- design.json 生成 (导出/预览) ---------- */
  function objOf(sec) {
    const o = {};
    // textures/svgs/fonts 值为 {path, blob}, colors 值为字符串 — 兼容两种形态
    for (const [k, v] of Object.entries(S[sec])) o[k] = v && typeof v === "object" ? v.path : v;
    return o;
  }
  function mergeExtras(sec, base) {
    const ex = Object.hasOwn(S.extra, sec) ? S.extra[sec] : null;
    if (!ex || typeof ex !== "object") return base;
    const out = { ...base };
    for (const [k, v] of Object.entries(ex)) {
      if (!UNSAFE_KEYS.has(k) && !Object.hasOwn(out, k)) out[k] = v;
    }
    return out;
  }
  function buildDesignJSON() {
    const o = {
      _readme: "bocchi 设计模板 (Design Template). 复制本文件到材质包 assets/minecraft/client/design.json 即可替换整个设计. 想换哪项就改哪项, 其余自动回退到 mod 内置默认. 所有以 _ 开头的键是注释/说明, 加载时会忽略. 路径格式: namespace:path, 省略命名空间则默认 minecraft. 颜色格式: #AARRGGBB 或 #RRGGBB.",
      textures: mergeExtras("textures", { _comment: "位图资源: 立绘/Logo/加载图", ...objOf("textures") }),
      svgs: mergeExtras("svgs", { _comment: "主菜单按钮图标 (SVG), 与按钮 icon 参数对应: lang/multi/option/quit/single/theme", ...objOf("svgs") }),
      fonts: mergeExtras("fonts", { _comment: "Skia 字体. 键 = FontSet 中的字体名 (区分大小写), 值 = ttf/otf 文件路径. 换同名字体直接换文件, 换路径改这里.", ...objOf("fonts") }),
      colors: mergeExtras("colors", { _comment: "设计色板, 代码内硬编码颜色已接入此表. 格式 #RRGGBB 或 #AARRGGBB", ...objOf("colors") }),
      texts: mergeExtras("texts", { _comment: "界面文案. mInfoLine1~3 为 misayos 介绍三行; 其余键与 Bocchi Designer 文本面板一致", ...textsForExport() }),
      menu: { _comment: "主菜单主题: misayos (默认) / poulsen. 材质包覆盖此项即可切换主题, 资源重载后生效", theme: S.menu.theme },
    };
    for (const [sec, val] of Object.entries(S.extra)) {
      if (sec === "textures" || sec === "svgs" || sec === "fonts" || sec === "colors" || sec === "texts") continue;
      if (sec === "menu") { for (const [k, v] of Object.entries(val)) if (!UNSAFE_KEYS.has(k) && !Object.hasOwn(o.menu, k)) o.menu[k] = v; continue; }
      o[sec] = typeof val === "object" && val !== null ? { ...val } : val;
    }
    return o;
  }

  /* ---------- 状态持久化 (localStorage) ----------
   * 持久化: OV 布局微调 / 文本内容 / colors 段 / menu.theme / 预览配色 / 面板开合 / 缩放
   * 不持久化: 上传的 blob (体积大, 刷新即还原, 用 toast 提示)
   */
  const LS_KEY = "bocchi-designer:v1";
  function saveState() {
    try {
      const st = {
        ov: BD.state.OV,
        texts: BD.state.TEXTS,
        colors: S.colors,
        theme: S.menu.theme,
        previewColors: BD.state.PREVIEW_COLORS,
        openSections: BD.state.openSections,
        zoom: BD.state.zoom,
      };
      localStorage.setItem(LS_KEY, JSON.stringify(st));
    } catch (e) { /* 存储不可用时静默 */ }
  }
  function loadState() {
    try {
      const st = JSON.parse(localStorage.getItem(LS_KEY) || "null");
      if (!st) return;
      // H3: localStorage 同样按外部输入处理, 过滤危险键
      if (st.ov && typeof st.ov === "object") Object.assign(BD.state.OV, cleanCopy(st.ov));
      if (st.texts && typeof st.texts === "object") Object.assign(BD.state.TEXTS, cleanCopy(st.texts));
      if (st.colors && typeof st.colors === "object") Object.assign(S.colors, cleanCopy(st.colors));
      if (st.theme && ["misayos", "poulsen"].includes(st.theme)) S.menu.theme = st.theme;
      if (st.previewColors && typeof st.previewColors === "object") Object.assign(BD.state.PREVIEW_COLORS, cleanCopy(st.previewColors));
      if (Array.isArray(st.openSections)) BD.state.openSections = st.openSections;
      if (typeof st.zoom === "number") BD.state.zoom = st.zoom;
    } catch (e) { /* 损坏状态忽略 */ }
  }

  BD.design = { DEFAULT_DESIGN, S, DEFAULT_TEXTS, UNSAFE_KEYS, cleanCopy, localAsset, usedPath, blobUrl, revokeBlobUrl, setBlob, splitPath, zipEntry, hexToCss, buildDesignJSON, saveState, loadState };
})();
