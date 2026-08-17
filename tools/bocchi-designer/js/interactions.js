/* ============================================================================
 * interactions.js — 舞台切换 / 入场动画 / 选中与拖拽 / 键盘微调 / 缩放
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});
  const { $, set } = BD.core;
  const state = BD.state;

  /* ---------- 舞台切换 ---------- */
  let current = "misayos";
  function showStage(name) {
    current = name;
    selKey = null;
    drag = null;
    $("selBox").style.display = "none";
    $("splashStage").style.display = name === "splash" ? "block" : "none";
    $("misayosStage").style.display = name === "misayos" ? "block" : "none";
    $("poulsenStage").style.display = name === "poulsen" ? "block" : "none";
    $("swSplash").classList.toggle("active", name === "splash");
    $("swMisayos").classList.toggle("active", name === "misayos");
    $("swPoulsen").classList.toggle("active", name === "poulsen");
    const names = {
      splash: "加载页（点击 TAP TO START）",
      misayos: "主菜单 misayos（喜多郁代）",
      poulsen: "主菜单 poulsen（后藤独）",
    };
    BD.panels.setStatus("<b>" + names[name] + "</b> · 1:1 预览 · 1280×720 设计分辨率" + (name === "misayos" ? " · 点击元素可选中拖拽" : ""));
    if (name === "splash") startSplashDemo();
    else replay(name);
  }

  /* ---------- 入场动画重播 ---------- */
  function replay(name) {
    const map = {
      misayos: ["mBlock1", "mStroke1", "mBlock3", "mTachie", "mRecord", "mInfo", "mPhobia", "mBocchi", "mRock", "mBoxGotoh", "mBoxGirl", "mLogo", "mPanel"],
      poulsen: ["pGoto1", "pGoto2", "pHitoriTop", "pHitoriBottom", "pSq1", "pSq2", "pSq3", "pBar", "pBoxA", "pBoxB", "pJName", "pJKana", "pAddInfo", "pAlias", "pTachie", "pDots", "pDot1", "pDot2", "pDot3", "pLogo", "pBtnSingle", "pBtnMulti", "pBtnOptions", "pBtnQuitP", "pBtnMisayos"],
    };
    const ids = map[name] || [];
    ids.forEach(id => {
      const el = $(id);
      if (el) { el.style.transition = "none"; el.style.opacity = "0"; }
    });
    requestAnimationFrame(() => requestAnimationFrame(() => {
      const ease = "cubic-bezier(.33,1,.68,1)";
      ids.forEach((id, i) => {
        const el = $(id);
        if (el) { el.style.transition = `opacity 700ms ${ease} ${i * 40}ms`; el.style.opacity = "1"; }
      });
    }));
  }

  /* ---------- 加载页演示 ---------- */
  let splashTimer = null;
  function startSplashDemo() {
    clearInterval(splashTimer);
    $("progressFill").style.width = "0%";
    let p = 0;
    splashTimer = setInterval(() => {
      p = Math.min(1, p + 0.012);
      $("progressFill").style.width = (p * 100) + "%";
      const frame = Math.floor(p * 20) % 20;
      $("loadingIcon").style.backgroundPosition = `-${frame * BD.facts.value("splash.loadingFrameW")}px 0`;
      if (p >= 1) clearInterval(splashTimer);
    }, 50);
  }

  /* ---------- 选中与拖拽 (misayos) ---------- */
  const SEL_MAP = {
    mTachie: "mTachie", mTachieImg: "mTachie", mRecord: "mRecord", mRecordCover: "mRecord",
    mBocchi: "title", mRock: "title", mBoxGotoh: "title", mBoxGirl: "title",
    mPanel: "mPanel", mBlock1: "mBlock1", mStroke1: "mBlock1", mDash1: "mBlock1",
    mDash2: "mBlock1", mRectW: "mBlock1",
  };
  // 文本元素 → 可编辑文本 key (dblclick 定位到输入框)
  const SEL_TEXT = {
    mBocchi: "mBocchi", mRock: "mRock", mBoxGotoh: "mBoxGotoh", mBoxGirl: "mBoxGirl",
    mPhobia: "mPhobia", mInfo: "mInfo", pTitle: "pTitle", pVer: "pVer", pBranch: "pBranch",
    pCopy1: "pCopy1", pCopy2: "pCopy2",
  };
  let selKey = null;
  let drag = null;

  function stageScale() {
    const m = /scale\(([\d.]+)\)/.exec($("stageScale").style.transform || "");
    return m ? parseFloat(m[1]) : 1;
  }
  function stagePos(e) {
    const r = $("stage").getBoundingClientRect();
    const s = stageScale();
    return { x: (e.clientX - r.left) / s, y: (e.clientY - r.top) / s };
  }
  function cssRect(el) {
    const r = el.getBoundingClientRect();
    const sr = $("stage").getBoundingClientRect();
    const s = stageScale();
    return { left: (r.left - sr.left) / s, top: (r.top - sr.top) / s, width: r.width / s, height: r.height / s };
  }
  function updateSelBox() {
    const box = $("selBox");
    if (!drag) box.style.display = current === "misayos" && selKey ? "block" : "none";
    if (current !== "misayos" || !selKey) return;
    let r;
    if (selKey === "title") {
      const rects = ["mBocchi", "mRock", "mBoxGotoh", "mBoxGirl"].map(id => cssRect($(id)));
      r = {
        left: Math.min(...rects.map(x => x.left)), top: Math.min(...rects.map(x => x.top)),
        right: Math.max(...rects.map(x => x.left + x.width)), bottom: Math.max(...rects.map(x => x.top + x.height)),
      };
      r = { left: r.left, top: r.top, width: r.right - r.left, height: r.bottom - r.top };
    } else {
      r = cssRect($(selKey));
    }
    box.style.cssText = `display:block;left:${r.left - 2}px;top:${r.top - 2}px;width:${r.width + 4}px;height:${r.height + 4}px;`;
    BD.panels.setStatus(
      `<span class="sel-chip">${selName(selKey)}</span> 已选中 · 拖动移动 · 拖角缩放 · 方向键微调 (Shift×10) · Esc 取消`);
  }
  function selName(key) {
    return {
      mTachie: "立绘", mRecord: "唱片", title: "标题组",
      mPanel: "侧栏面板", mBlock1: "背景方块",
    }[key] || key;
  }

  function nudge(dx, dy) {
    if (!selKey) return;
    const setOV = BD.panels.setOV;
    switch (selKey) {
      case "mTachie": setOV("tachieX", (state.OV.tachieX ?? 0) + dx); setOV("tachieY", (state.OV.tachieY ?? 0) + dy); break;
      case "mRecord": setOV("recordX", (state.OV.recordX ?? 0) + dx); setOV("recordY", (state.OV.recordY ?? 0) + dy); break;
      case "title": setOV("titleX", (state.OV.titleX ?? 0) + dx); setOV("titleY", (state.OV.titleY ?? 0) + dy); break;
      case "mPanel": setOV("panelX", (state.OV.panelX ?? 0) + dx); break;
      case "mBlock1": setOV("blockX", (state.OV.blockX ?? 0) + dx); setOV("blockY", (state.OV.blockY ?? 0) + dy); break;
    }
  }
  function clearSel() {
    if (!selKey) return;
    selKey = null;
    updateSelBox();
    BD.panels.setStatus("");
  }

  $("stageScale").addEventListener("mousedown", e => {
    if (current !== "misayos") return;
    const handle = e.target.closest("#selBox i");
    const mover = e.target.closest(".sel-move");
    if (handle || mover) {
      if (!selKey) return;
      e.preventDefault();
      const snap = {};
      for (const k of Object.keys(BD.panels.SLIDERS || {})) snap[k] = state.OV[k] != null ? state.OV[k] : +BD.panels.SLIDERS[k].input.value;
      drag = { mode: handle ? "resize" : "move", handle: handle && handle.dataset.h, start: stagePos(e), startOV: snap };
      return;
    }
    const t = e.target.closest("[id]");
    const key = t && SEL_MAP[t.id];
    if (key) {
      if (key === selKey) {
        // 已选中: 直接开始拖拽
        e.preventDefault();
        const snap = {};
        for (const k of Object.keys(BD.panels.SLIDERS)) snap[k] = state.OV[k] != null ? state.OV[k] : +BD.panels.SLIDERS[k].input.value;
        drag = { mode: "move", handle: null, start: stagePos(e), startOV: snap };
        return;
      }
      selKey = key;
    } else {
      selKey = null;
    }
    updateSelBox();
  });

  window.addEventListener("mousemove", e => {
    if (!drag || !selKey) return;
    const p = stagePos(e);
    const dx = p.x - drag.start.x, dy = p.y - drag.start.y;
    const s = drag.startOV;
    const setOV = BD.panels.setOV;
    if (drag.mode === "move") {
      switch (selKey) {
        case "mTachie": setOV("tachieX", s.tachieX + dx); setOV("tachieY", s.tachieY + dy); break;
        case "mRecord": setOV("recordX", s.recordX + dx); setOV("recordY", s.recordY + dy); break;
        case "title": setOV("titleX", s.titleX + dx); setOV("titleY", s.titleY + dy); break;
        case "mPanel": setOV("panelX", s.panelX + dx); break;
        case "mBlock1": setOV("blockX", s.blockX + dx); setOV("blockY", s.blockY + dy); break;
      }
    } else {
      const h = drag.handle;
      const xDir = h.includes("e") ? 1 : h.includes("w") ? -1 : 0;
      const yDir = h.includes("s") ? 1 : h.includes("n") ? -1 : 0;
      const g = Math.max(dx * xDir, dy * yDir);
      switch (selKey) {
        case "mTachie": if (yDir) setOV("tachieH", s.tachieH + dy * yDir); break;
        case "mRecord": setOV("recordSize", s.recordSize + g); break;
        case "title": setOV("titleSize", s.titleSize + g); break;
        case "mPanel": if (xDir < 0) setOV("panelW", s.panelW - dx); break;
        case "mBlock1": setOV("block1", s.block1 + g); break;
      }
    }
  });
  window.addEventListener("mouseup", () => { drag = null; updateSelBox(); });

  // 双击文本元素 → 定位到编辑框
  $("stage").addEventListener("dblclick", e => {
    const t = e.target.closest("[id]");
    if (!t || current !== "misayos") return;
    const textKey = t && SEL_TEXT[t.id];
    if (textKey && BD.panels.focusTextInput(textKey)) {
      e.stopPropagation();
    }
  });

  // 键盘微调 / Esc
  window.addEventListener("keydown", e => {
    if (!selKey || current !== "misayos") return;
    if (document.activeElement && /input|textarea|select/.test(document.activeElement.tagName.toLowerCase())) return;
    const step = e.shiftKey ? 10 : 1;
    const k = e.key;
    if (k === "ArrowLeft") { nudge(-step, 0); e.preventDefault(); }
    else if (k === "ArrowRight") { nudge(step, 0); e.preventDefault(); }
    else if (k === "ArrowUp") { nudge(0, -step); e.preventDefault(); }
    else if (k === "ArrowDown") { nudge(0, step); e.preventDefault(); }
    else if (k === "Escape") { clearSel(); }
  });

  // 常驻动画: 立绘呼吸 + 唱片旋转
  setInterval(() => {
    const t = Date.now() / 1000;
    if (current !== "misayos") return;
    const breath = Math.sin(t * 2 * Math.PI / 3) * 3;
    const base = +($("mTachie").dataset.baseRot || 0);
    $("mTachie").style.transform = `rotate(${base - (breath + 1.5) / 3}deg)`;
    $("mRecord").style.transform = `rotate(${(t * 20) % 360}deg)`;
  }, 33);

  /* ---------- 缩放控制 ---------- */
  function fitStage() {
    const wrap = $("stageScale").closest(".preview-wrap");
    const scale = Math.min((wrap.clientWidth - 44) / BD.facts.W, (wrap.clientHeight - 56) / BD.facts.H);
    applyScale(scale, true);
  }
  function applyScale(scale, fromFit) {
    $("stageScale").style.transform = `scale(${scale})`;
    const buttons = document.querySelectorAll(".seg button[data-zoom]");
    for (const b of buttons) b.classList.toggle("on", b.dataset.zoom == (fromFit ? "0" : state.zoom));
  }
  function setZoom(z) {
    state.zoom = z;
    BD.design.saveState();
    if (z === 0) fitStage();
    else applyScale(z);
  }
  document.querySelectorAll(".seg button[data-zoom]").forEach(b => {
    b.addEventListener("click", () => setZoom(+b.dataset.zoom));
  });
  window.addEventListener("resize", () => {
    if (state.zoom === 0) fitStage();
  });

  BD.interactions = {
    showStage, replay, startSplashDemo, updateSelBox, fitStage,
    current: () => current, selKey: () => selKey, setSel: (k) => { selKey = k; updateSelBox(); },
  };
})();
