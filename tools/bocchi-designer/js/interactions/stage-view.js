/* ============================================================================
 * stage-view.js - 舞台切换 (加载页 / misayos / poulsen)
 *
 * 只负责「当前是哪个舞台」这一个决定, 以及它带来的四处联动: 三个舞台容器的显隐、
 * 切换器的高亮、状态栏与手势提示条的更新、以及对应动画定时器的起停。
 * ==========================================================================*/
import { $, state } from "../core.js";
import { setStatus } from "../status.js";
import { replay, startSplashDemo, stopSplashDemo, startAmbient, stopAmbient } from "./animation.js";
import { fitStage } from "./zoom.js";
import { resetSelection } from "./select-drag.js";
import { setCurrent, stageStatus } from "./stage-state.js";

export function showStage(name) {
  setCurrent(name);
  resetSelection();
  $("splashStage").style.display = name === "splash" ? "block" : "none";
  $("misayosStage").style.display = name === "misayos" ? "block" : "none";
  $("poulsenStage").style.display = name === "poulsen" ? "block" : "none";
  $("swSplash").classList.toggle("active", name === "splash");
  $("swMisayos").classList.toggle("active", name === "misayos");
  $("swPoulsen").classList.toggle("active", name === "poulsen");
  setStatus(stageStatus());
  // 舞台操作提示只在 misayos 出现 —— 方向键微调/双击复位这些手势只在选中可调元素
  // 后才有意义, 在 splash/poulsen 上摆着一行做不到的说明比不摆更糟。
  $("stageKeys").hidden = name !== "misayos";
  // 提示条显隐会改变画框以外可用的高度, 「适应」模式必须重算 —— 否则从加载页切回
  // misayos 后, 舞台仍按「没有提示条」的那个高度摆, 底下会被切掉一截, 而用户看到的
  // 是「适应」按钮亮着, 却什么都不适应。
  if (state.zoom === 0) fitStage();
  if (name === "splash") startSplashDemo();
  else { stopSplashDemo(); replay(name); }
  if (name === "misayos") startAmbient();
  else stopAmbient(); // M4: 离开 misayos 停止常驻动画, 不再空转
}