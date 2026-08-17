/* ============================================================================
 * facts.js — 布局常量单一数据源
 *
 * 这里的每一项都是游戏端 Java 布局代码中"魔法数字"的唯一记录点:
 *   - expr  布局公式 (Java 语法), 运行时用 W=1280 H=720 求值
 *   - java  对应的 Java 源文件与行号 (相对 1.21.5 common/src/main/java)
 *   - note  可选的说明 (webOnly 表示仅预览端推导, Java 无直接对应)
 *
 * 保持同步: 修改 Java 布局后运行  tools/bocchi-designer/sync/check-layout.py
 * 即可检测哪些常量已经漂移 (用法: python3 sync/check-layout.py)。
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});

  const W = 1280, H = 720;

  /* ---------- 表达式求值 (安全子集: 数字 + - * / ( ) min() + 绑定变量) ---------- */
  const TOKEN_RE = /\s*(?:(\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?|([A-Za-z_][\w.]*)\(\)?|([+\-*/(),]))/y;
  function parseExpr(src) {
    const toks = [];
    let pos = 0;
    while (pos < src.length) {
      TOKEN_RE.lastIndex = pos;
      const m = TOKEN_RE.exec(src);
      if (!m) throw new Error("无法解析表达式: " + src + " (位置 " + pos + ")");
      if (m[1]) toks.push({ t: "num", v: parseFloat(m[1]) });
      else if (m[2]) toks.push({ t: m[3] ? "id" : "fn", v: m[2] });
      else toks.push({ t: m[3], v: m[3] });
      pos = TOKEN_RE.lastIndex;
    }
    let i = 0;
    function peek() { return toks[i]; }
    function next() { return toks[i++]; }
    function expect(t) { const x = next(); if (!x || x.t !== t) throw new Error("期望 " + t + ", 得到 " + (x && x.v) + " in: " + src); return x; }
    function parseExpr2() {
      let v = parseTerm();
      for (;;) {
        const op = peek();
        if (op && (op.t === "+" || op.t === "-")) { next(); const r = parseTerm(); v = op.t === "+" ? v + r : v - r; }
        else break;
      }
      return v;
    }
    function parseTerm() {
      let v = parseFactor();
      for (;;) {
        const op = peek();
        if (op && (op.t === "*" || op.t === "/")) { next(); const r = parseFactor(); v = op.t === "*" ? v * r : v / r; }
        else break;
      }
      return v;
    }
    function parseFactor() {
      const x = next();
      if (!x) throw new Error("表达式意外结束: " + src);
      if (x.t === "-" || x.t === "+") { const v = parseFactor(); return x.t === "-" ? -v : v; }
      if (x.t === "num") return x.v;
      if (x.t === "(") { const v = parseExpr2(); expect(")"); return v; }
      if (x.t === "fn") {
        // fn 令牌已含 "(", 直接解析参数列表
        if (x.v === "min" || x.v === "max") {
          const vals = [parseExpr2()];
          while (peek() && peek().t === ",") { next(); vals.push(parseExpr2()); }
          expect(")");
          return x.v === "min" ? Math.min(...vals) : Math.max(...vals);
        }
        throw new Error("未知函数 " + x.v + " in: " + src);
      }
      if (x.t === "id") {
        // 标识符函数调用, 如 block3Pos.getX() — 展开为已绑定变量
        const v = parseExpr2();
        expect(")");
        return v;
      }
      throw new Error("意外的符号 " + x.v + " in: " + src);
    }
    const v = parseExpr2();
    if (i !== toks.length) throw new Error("多余内容: " + src);
    return v;
  }

  /* ---------- 事实表 ----------
   * 变量引用: 同组内可用别的 fact 名 (如 "misayos.block1Size") 作为绑定变量;
   * 组内简写可用短名 (block1Size)。
   */
  const G = {
    misayos: {
      block1X:      { expr: "scaledWidth * 0.095",                          java: "ui/model/MainMenuMisayosFrameContext.java:15" },
      block1Y:      { expr: "scaledHeight * 0.4",                           java: "ui/model/MainMenuMisayosFrameContext.java:15" },
      block1Size:   { expr: "min(scaledWidth * 0.2265625, scaledHeight * 0.4027777777777778)", java: "ui/model/MainMenuMisayosFrameContext.java:16" },
      block3X:      { expr: "scaledWidth * 0.263125",                       java: "ui/model/MainMenuMisayosFrameContext.java:17" },
      block3Y:      { expr: "scaledHeight * 0.0972222222222222",            java: "ui/model/MainMenuMisayosFrameContext.java:17" },
      block3Size:   { expr: "min(scaledWidth * 0.4166666666666667, scaledHeight * 0.7407407407407407)", java: "ui/model/MainMenuMisayosFrameContext.java:18" },
      // 立绘 (MainTachieComponent)
      tachieH:      { expr: "scaledHeight * 0.95",                          java: "ui/mainmenu/misayos/MainTachieComponent.java:68" },
      tachieW:      { expr: "scaledHeight * 0.95 * 1.035483870967742",      java: "ui/mainmenu/misayos/MainTachieComponent.java:69" },
      tachieX:      { expr: "block3X + block3Size * 0.1",                   java: "ui/mainmenu/misayos/MainTachieComponent.java:70" },
      tachieY:      { expr: "scaledHeight * 0.05",                          java: "ui/mainmenu/misayos/MainTachieComponent.java:71" },
      // 唱片 (MainTachieComponent / AlbumRenderer)
      recordX:      { expr: "scaledWidth * 0.6",                            java: "ui/mainmenu/misayos/MainTachieComponent.java:94" },
      recordY:      { expr: "scaledHeight * 0.18",                          java: "ui/mainmenu/misayos/MainTachieComponent.java:94" },
      recordSize:   { expr: "scaledHeight * 0.65",                          java: "ui/mainmenu/misayos/MainTachieComponent.java:94" },
      // 简介文字 (TextElementsComponent)
      infoX:        { expr: "block1X + block1Size * 0.15",                  java: "ui/mainmenu/misayos/TextElementsComponent.java:61" },
      infoY:        { expr: "block1Size * 0.05",                            java: "ui/mainmenu/misayos/TextElementsComponent.java:62" },
      phobiaX:      { expr: "block1X + block1Size * 0.04",                  java: "ui/mainmenu/misayos/TextElementsComponent.java:105" },
      phobiaY:      { expr: "block1Y + 0.5",                                java: "ui/mainmenu/misayos/TextElementsComponent.java:106" },
      decorateSpacing: { expr: "2.4",                                       java: "ui/mainmenu/misayos/TextElementsComponent.java:99", note: "SOCIAL PHOBIA 字距" },
      // 虚线 (StrokeElementsComponent / TextElementsComponent)
      dash1X:       { expr: "block1X + block1Size * 0.1",                   java: "ui/mainmenu/misayos/StrokeElementsComponent.java:46" },
      dash1EndX:    { expr: "block1X + block1Size * 0.72",                  java: "ui/mainmenu/misayos/StrokeElementsComponent.java:47" },
      dash1Y:       { expr: "block1Y * 0.1",                                java: "ui/mainmenu/misayos/StrokeElementsComponent.java:46" },
      dash2EndX:    { expr: "block1X + block1Size * 0.735 + 4.5",           java: "ui/mainmenu/misayos/TextElementsComponent.java:80" },
      dash2Y:       { expr: "block1Y + 5 - 0.2",                            java: "ui/mainmenu/misayos/TextElementsComponent.java:86" },
      // 白色小方块 (TextElementsComponent)
      rectWX:       { expr: "block1X - block1Size * 0.15",                  java: "ui/mainmenu/misayos/TextElementsComponent.java:93" },
      rectWW:       { expr: "block1Size * 0.6",                             java: "ui/mainmenu/misayos/TextElementsComponent.java:89" },
      // 侧栏面板 (GuiComponent)
      titleFontSize:{ expr: "21",                                           java: "ui/mainmenu/misayos/GuiComponent.java:241", note: "BOCCHI 面板标题字号 (Java px)" },
      panelFooterY: { expr: "0.85",                                         java: "ui/mainmenu/misayos/GuiComponent.java:272", note: "footer 线/主题按钮按高度比例" },
      btnGap:       { expr: "20.5",                                         java: "ui/mainmenu/misayos/GuiComponent.java:126", note: "按钮间距 = 20.5 * scale" },
      btnStartY:    { expr: "30",                                           java: "ui/mainmenu/misayos/GuiComponent.java:123", note: "首按钮 Y = 30 * scale" },
      btnYOffset:   { expr: "10",                                           java: "ui/mainmenu/misayos/GuiComponent.java:120", note: "按钮 Y 偏移 = 10 * scale" },
    },
    poulsen: {
      rect1W:       { expr: "scaledWidth * 0.412",                          java: "ui/model/MainMenuPoulsenFrameContext.java:12" },
      rect1X:       { expr: "(scaledWidth - rect1W) / 2",                   java: "ui/model/MainMenuPoulsenFrameContext.java:13" },
      bgFontSize:   { expr: "scaledHeight * 0.205 / 0.305",                 java: "ui/model/MainMenuPoulsenFrameContext.java:15" },
      // 底图 / 条纹 (BgImageComponent)
      stripY:       { expr: "scaledHeight * 0.586",                         java: "ui/mainmenu/poulsen/BgImageComponent.java:37" },
      stripH:       { expr: "scaledHeight * 0.086",                         java: "ui/mainmenu/poulsen/BgImageComponent.java:38" },
      bgImgW:       { expr: "rect1W * 0.644 * 2.74",                        java: "ui/mainmenu/poulsen/BgImageComponent.java:40" },
      bgImgX:       { expr: "-(scaledWidth * 0.188)",                       java: "ui/mainmenu/poulsen/BgImageComponent.java:42" },
      bgImgAlpha:   { expr: "0.18",                                         java: "ui/mainmenu/poulsen/BgImageComponent.java:62" },
      // 大字 HITORI (BigFirstNameComponent)
      hitoriFont:   { expr: "bgFontSize * 0.552",                           java: "ui/mainmenu/poulsen/BigFirstNameComponent.java:32" },
      hitoriX:      { expr: "rect1X * 0.5",                                 java: "ui/mainmenu/poulsen/BigFirstNameComponent.java:36" },
      hitoriY:      { expr: "scaledHeight * 0.2",                           java: "ui/mainmenu/poulsen/BigFirstNameComponent.java:37" },
      // GOTO 大字 / 方块图 (BigLastNameComponent / ImagesBlockComponent)
      gotoSpacing:  { expr: "scaledHeight * 0.045",                         java: "ui/mainmenu/poulsen/ImagesBlockComponent.java:41" },
      square:       { expr: "scaledHeight * 0.117",                         java: "ui/mainmenu/poulsen/ImagesBlockComponent.java:50" },
      squareImg:    { expr: "scaledHeight * 0.117 * 0.85",                  java: "ui/mainmenu/poulsen/ImagesBlockComponent.java:101" },
      // 日文名 (JapaneseNamesComponent)
      nameFont:     { expr: "bgFontSize * 0.221",                           java: "ui/mainmenu/poulsen/JapaneseNamesComponent.java:35" },
      kanaFont:     { expr: "bgFontSize * 0.221 * 0.375",                   java: "ui/mainmenu/poulsen/JapaneseNamesComponent.java:36" },
      nameY:        { expr: "scaledHeight * 0.8",                           java: "ui/mainmenu/poulsen/JapaneseNamesComponent.java:42" },
      // 别名 (AliasTextComponent)
      aliasFont:    { expr: "bgFontSize * 0.221",                           java: "ui/mainmenu/poulsen/AliasTextComponent.java:35" },
      quoteFont:    { expr: "bgFontSize * 0.221 * 1.25",                    java: "ui/mainmenu/poulsen/AliasTextComponent.java:36" },
      aliasY:       { expr: "scaledHeight * 0.45",                          java: "ui/mainmenu/poulsen/AliasTextComponent.java:40" },
      aliasScaleX:  { expr: "0.85",                                         java: "ui/mainmenu/poulsen/AliasTextComponent.java:78", note: "canvas.scale(0.85, 1)" },
      // 信息条 (AdditionInfoComponent)
      addW:         { expr: "scaledWidth * 0.1076",                         java: "ui/mainmenu/poulsen/AdditionInfoComponent.java:36" },
      addH:         { expr: "scaledHeight * 0.0322",                        java: "ui/mainmenu/poulsen/AdditionInfoComponent.java:37" },
      addFont:      { expr: "scaledHeight * 0.0322 / 0.8",                  java: "ui/mainmenu/poulsen/AdditionInfoComponent.java:38" },
      addY:         { expr: "scaledHeight * 0.734",                         java: "ui/mainmenu/poulsen/AdditionInfoComponent.java:42" },
      // 彩点 (CirclesComponent)
      circleX:      { expr: "scaledWidth * 0.03",                           java: "ui/mainmenu/poulsen/CirclesComponent.java:40" },
      circleY:      { expr: "scaledHeight * 0.05",                          java: "ui/mainmenu/poulsen/CirclesComponent.java:41" },
      // 按钮 (MainMenuScreen)
      btnFont:      { expr: "bgFontSize * 0.221 * 0.3",                     java: "ui/mainmenu/poulsen/MainMenuScreen.java:84" },
      btnX:         { expr: "scaledWidth * 0.03",                           java: "ui/mainmenu/poulsen/MainMenuScreen.java:86" },
      btnY0:        { expr: "scaledHeight * 0.1",                           java: "ui/mainmenu/poulsen/MainMenuScreen.java:87" },
      btnGap:       { expr: "bgFontSize * 0.221 * 0.3 * 2.6",               java: "ui/mainmenu/poulsen/MainMenuScreen.java:87", note: "index * fontSize * 2.6" },
    },
    splash: {
      logoW:        { expr: "60",                                           java: "ui/splash/SplashUI.java:102" },
      logoH:        { expr: "60 * 0.317",                                   java: "ui/splash/SplashUI.java:66" },
      logoX:        { expr: "rect1X * 0.3",                                 java: "ui/splash/SplashUI.java:100", note: "rect1X = (W - W*0.412)/2 (poulsen rect1)" },
      logoY:        { expr: "scaledHeight * 0.1",                           java: "ui/splash/SplashUI.java:101" },
      barX:         { expr: "scaledWidth * 0.2",                            java: "ui/splash/SplashUI.java:144" },
      barW:         { expr: "scaledWidth * 0.6",                            java: "ui/splash/SplashUI.java:146" },
      loadingFrameW:{ expr: "23.5", note: "webOnly: 预览帧宽 = 图标宽度/20 (雪碧图 1800×90, 20 帧), Java 端见 LoadingAnimateRenderer" },
    },
  };

  /* ---------- 求值: 支持引用同组 fact 名做绑定变量 (拓扑两遍法) ---------- */
  const cache = {};
  const IDENT_RE = /\b([A-Za-z_][A-Za-z0-9_.]*)\b/g;
  function refsOf(expr) {
    const out = new Set();
    let m;
    IDENT_RE.lastIndex = 0;
    while ((m = IDENT_RE.exec(expr))) {
      const id = m[1];
      if (!/^(min|max|scaledWidth|scaledHeight|width|height|W|H)$/.test(id) && !/^\d/.test(id)) out.add(id);
    }
    return out;
  }
  function evalWithBindings(expr, bind) {
    let s = expr;
    for (const k of Object.keys(bind)) s = s.replace(new RegExp("\\b" + k + "\\b", "g"), "(" + bind[k] + ")");
    return parseExpr(s);
  }
  function value(name) {
    if (cache[name] !== undefined) return cache[name];
    const [group, key] = name.split(".");
    const fact = G[group] && G[group][key];
    if (!fact) throw new Error("未知布局常量: " + name);
    const bind = { scaledWidth: W, scaledHeight: H, W: W, H: H, width: W, height: H };
    for (const id of refsOf(fact.expr)) {
      // 引用另一组的 fact (如 splash.logoX -> poulsen.rect1X)
      const other = G[group][id];
      if (other) bind[id] = value(group + "." + id);
      else if (G.poulsen[id]) bind[id] = value("poulsen." + id);
      else throw new Error("布局常量引用未定义: " + name + " 引用了 " + id);
    }
    const result = evalWithBindings(fact.expr, bind);
    cache[name] = result;
    return result;
  }

  BD.facts = {
    W, H,
    groups: G,
    value,
    all: () => {
      const out = {};
      for (const g of Object.keys(G)) for (const k of Object.keys(G[g])) out[g + "." + k] = value(g + "." + k);
      return out;
    },
  };
})();
