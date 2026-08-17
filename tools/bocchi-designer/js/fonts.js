/* ============================================================================
 * fonts.js — 字体度量与加载
 *
 * Skia 约定: 屏上字号 = Java 字号/2, baseline = y + getHeight。
 * gh(px)  = 行高 (Java getHeight 的屏幕像素)
 * cssTop  = 把"基线 y"转成 CSS top (减掉升部偏移)
 * tw      = 文本宽度 (带可选字距)
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});

  const FONT_META = {
    "SH Heavy":        { gh: 0.47, topK: 0.08 },
    "SH Regular":      { gh: 0.47, topK: 0.08 },
    "SH Light":        { gh: 0.47, topK: 0.08 },
    "SH Normal":       { gh: 0.47, topK: 0.08 },
    "SH Bold":         { gh: 0.47, topK: 0.08 },
    "Radikal Black":   { gh: 0.375, topK: -0.035 },
    "Radikal Regular": { gh: 0.375, topK: -0.035 },
    "Meiryo":          { gh: 0.41, topK: -0.02 },
  };

  // design.json fonts 段的键 → 预览用 CSS 字体族
  const FONT_SET_NAME = {
    "Radikal-Black": "Radikal Black", "Radikal-Regular": "Radikal Regular", "meiryo-bold": "Meiryo",
    "SourceHanSansSC-Light": "SH Light", "SourceHanSansSC-Regular": "SH Regular",
    "SourceHanSansSC-Heavy": "SH Heavy", "SourceHanSansSC-Normal": "SH Normal", "SourceHanSansSC-Bold": "SH Bold",
  };

  const mctx = document.createElement("canvas").getContext("2d");
  const gh = (px, fam) => px * FONT_META[fam].gh;
  const cssTop = (y, px, fam) => y + px * FONT_META[fam].topK;
  function tw(text, px, fam, spacing) {
    mctx.font = `${px}px "${fam}"`;
    return mctx.measureText(text).width + (spacing || 0) * text.length;
  }

  /** 加载上传的字体到页面 (异步, 完成后回调) */
  async function loadUploadedFonts(done) {
    const jobs = [];
    for (const [name, cssFam] of Object.entries(FONT_SET_NAME)) {
      const f = BD.design.S.fonts[name];
      if (f && f.blob) {
        jobs.push(fetch(URL.createObjectURL(f.blob)).then(r => r.arrayBuffer()).then(buf => {
          return new FontFace(cssFam, buf).load().then(ff => document.fonts.add(ff));
        }).catch(() => {}));
      }
    }
    if (done) await Promise.all(jobs).then(done);
  }

  BD.fonts = { FONT_META, FONT_SET_NAME, gh, cssTop, tw, loadUploadedFonts };
})();
