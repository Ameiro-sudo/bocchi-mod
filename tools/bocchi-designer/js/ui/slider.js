/* ============================================================================
 * ui/slider.js - 布局微调滑杆行 (双击标签复位)
 *
 * 值域逻辑在 ov.js (注册表 / 设值 / 复位), 行 DOM 的创建与文案在这里。
 * 16 组值域定义在 sliders.js —— 纯数据, 所以 test/sliders.test.mjs 能直接断言
 * 「默认值等于 mod 内置常量」这个不变量, 而不必去解析一段 HTML 字符串。
 * ==========================================================================*/
import { registerSlider, clampSaved, onSliderInput, setFill, setOV } from "../ov.js";

export function addSlider(body, label, key, min, max, def, step) {
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
  // 先 clampSaved 再 registerSlider: OV_FIELDS[key] 的撤销基准在注册那一刻读 OV[key],
  // 反过来 (先注册后夹) 会让基准停在那个越界值上, 首次拖动的 undo 就回不到夹过的值。
  registerSlider(key, def, input, val);
}