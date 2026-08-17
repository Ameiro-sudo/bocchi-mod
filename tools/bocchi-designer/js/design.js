/* ============================================================================
 * design.js — design.json 数据模型 + 状态持久化
 *
 * S = 当前编辑态: { textures/svgs/fonts: {key: {path, blob}}, colors: {key: path}, menu: {theme} }
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

  const localAsset = (p) => "assets/" + p.replace(/^[a-z0-9_.-]+:/, "").replace(/^client\//, "");
  const usedPath = (sec, k) => (S[sec][k].blob ? URL.createObjectURL(S[sec][k].blob) : localAsset(S[sec][k].path));

  /* ---------- 颜色 ---------- */
  function hexToCss(hex) {
    const m = /^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.exec((hex || "").trim());
    if (!m) return null;
    let h = m[1];
    if (h.length === 6) return "#" + h;
    const a = parseInt(h.slice(0, 2), 16) / 255, rgb = h.slice(2);
    return `rgba(${parseInt(rgb.slice(0, 2), 16)},${parseInt(rgb.slice(2, 4), 16)},${parseInt(rgb.slice(4, 6), 16)},${a.toFixed(3)})`;
  }

  /* ---------- 可编辑文本 (仅预览, 不导出) ---------- */
  const DEFAULT_TEXTS = {
    mBocchi: "BOCCHI", mRock: "THE ROCK!",
    mBoxGotoh: "Gotoh Hitori", mBoxGirl: "A reclusive girl",
    mPhobia: "SOCIAL&nbsp;&nbsp;PHOBIA",
    mInfo: 'Goto, nicknamed "Little Solitude",<br>is a girl who always starts her speech with "Ah..."<br>and is extremely accepting and introverted',
    pTitle: "BOCCHI", pVer: "1.0", pBranch: '"ALPHA"',
    pCopy1: "Bocchi Client&nbsp;&nbsp;&nbsp;Version - 1.0", pCopy2: "@COPYRIGHT MISAYO",
    pHitoriTop: "HITORI", pHitoriBottom: "HITORI", pGoto1: "GOTO", pGoto2: "GOTO",
    pJName: "後藤 ひとり", pJKana: "ご&nbsp;&nbsp;&nbsp;&nbsp;とう",
    pAliasText: "ギターヒーロー",
    pAdd1: "FEBRUARY 21", pAdd2: "50 kg & 156 cm", pAdd3: "Aqua eye",
  };

  /* ---------- design.json 生成 (导出/预览) ---------- */
  function objOf(sec) {
    const o = {};
    for (const [k, v] of Object.entries(S[sec])) o[k] = v.path;
    return o;
  }
  function buildDesignJSON() {
    return {
      _readme: "bocchi 设计模板 (Design Template). 复制本文件到材质包 assets/minecraft/client/design.json 即可替换整个设计. 想换哪项就改哪项, 其余自动回退到 mod 内置默认. 所有以 _ 开头的键是注释/说明, 加载时会忽略. 路径格式: namespace:path, 省略命名空间则默认 minecraft. 颜色格式: #AARRGGBB 或 #RRGGBB.",
      textures: { _comment: "位图资源: 立绘/Logo/加载图", ...objOf("textures") },
      svgs: { _comment: "主菜单按钮图标 (SVG), 与按钮 icon 参数对应: lang/multi/option/quit/single/theme", ...objOf("svgs") },
      fonts: { _comment: "Skia 字体. 键 = FontSet 中的字体名 (区分大小写), 值 = ttf/otf 文件路径. 换同名字体直接换文件, 换路径改这里.", ...objOf("fonts") },
      colors: { _comment: "设计色板, 代码内硬编码颜色已接入此表. 格式 #RRGGBB 或 #AARRGGBB", ...objOf("colors") },
      menu: { _comment: "主菜单主题: misayos (喜多郁代, 默认) / poulsen (后藤独). 材质包覆盖此项即可切换主题, 资源重载后生效", theme: S.menu.theme },
    };
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
      if (st.ov && typeof st.ov === "object") Object.assign(BD.state.OV, st.ov);
      if (st.texts && typeof st.texts === "object") Object.assign(BD.state.TEXTS, st.texts);
      if (st.colors && typeof st.colors === "object") Object.assign(S.colors, st.colors);
      if (st.theme && ["misayos", "poulsen"].includes(st.theme)) S.menu.theme = st.theme;
      if (st.previewColors && typeof st.previewColors === "object") Object.assign(BD.state.PREVIEW_COLORS, st.previewColors);
      if (Array.isArray(st.openSections)) BD.state.openSections = st.openSections;
      if (typeof st.zoom === "number") BD.state.zoom = st.zoom;
    } catch (e) { /* 损坏状态忽略 */ }
  }

  BD.design = { DEFAULT_DESIGN, S, DEFAULT_TEXTS, localAsset, usedPath, hexToCss, buildDesignJSON, saveState, loadState };
})();
