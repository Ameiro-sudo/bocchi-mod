/* ============================================================================
 * animation.js - 舞台上的三组定时动画: 入场重播 / 加载页进度演示 / 常驻呼吸
 *
 * 三者原先散在 interactions.js 里的三处, 共同点是「都是自己起自己停的定时器」, 与
 * 选中拖拽、舞台切换的逻辑都无关。单独成模块后, 「切舞台该停哪个定时器」这件事在
 * stage-view 里是一组显式的 start/stop 调用, 而不是散落的 clearInterval。
 * ==========================================================================*/
import { $ } from "../core.js";
import { value as factValue } from "../facts.js";

/* ---------- 入场动画重播 ---------- */
const REPLAY_IDS = {
  misayos: ["mBlock1", "mStroke1", "mBlock3", "mTachie", "mRecord", "mInfo", "mPhobia", "mBocchi", "mRock", "mBoxGotoh", "mBoxGirl", "mLogo", "mPanel"],
  poulsen: ["pGoto1", "pGoto2", "pHitoriTop", "pHitoriBottom", "pSq1", "pSq2", "pSq3", "pBar", "pBoxA", "pBoxB", "pJName", "pJKana", "pAddInfo", "pAlias", "pTachie", "pDots", "pDot1", "pDot2", "pDot3", "pLogo", "pBtnSingle", "pBtnMulti", "pBtnOptions", "pBtnQuitP", "pBtnMisayos"],
};
export function replay(name) {
  const ids = REPLAY_IDS[name] || [];
  ids.forEach(id => {
    const el = $(id);
    if (el) { el.style.transition = "none"; el.style.opacity = "0"; }
  });
  // 两帧之后再落 transition: 同帧里先设 transition:none 再设 opacity:1, 浏览器会把
  // 两次改动合并成一次, 动画就不播了。
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
export function startSplashDemo() {
  clearInterval(splashTimer);
  $("progressFill").style.width = "0%";
  let p = 0;
  splashTimer = setInterval(() => {
    p = Math.min(1, p + 0.012);
    $("progressFill").style.width = (p * 100) + "%";
    const frame = Math.floor(p * 20) % 20;
    $("loadingIcon").style.backgroundPosition = `-${frame * factValue("splash.loadingFrameW")}px 0`;
    if (p >= 1) clearInterval(splashTimer);
  }, 50);
}
/** 离开加载页必须显式停掉: 否则那个 50ms 的定时器会一直跑到 100% 才停, 在别的舞台上
 *  继续改一个已经看不见的元素 —— CPU 白烧, 而且从别的页面切回来时进度条是满的。 */
export function stopSplashDemo() {
  clearInterval(splashTimer);
  splashTimer = null;
}

/* ---------- 常驻动画: 立绘呼吸 + 唱片旋转 (M4: 仅在 misayos 舞台运行) ---------- */
let ambientTimer = null;
export function startAmbient() {
  stopAmbient();
  ambientTimer = setInterval(() => {
    const t = Date.now() / 1000;
    const breath = Math.sin(t * 2 * Math.PI / 3) * 3;
    const base = +($("mTachie").dataset.baseRot || 0);
    $("mTachie").style.transform = `rotate(${base - (breath + 1.5) / 3}deg)`;
    $("mRecord").style.transform = `rotate(${(t * 20) % 360}deg)`;
  }, 33);
}
export function stopAmbient() {
  if (ambientTimer) { clearInterval(ambientTimer); ambientTimer = null; }
}