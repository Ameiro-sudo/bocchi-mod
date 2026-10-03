/* sliders.js 单测: 值域完整性 + 「默认值等于 mod 内置常量」这条不变量
 *
 * 动机见 js/sliders.js 头注释: 定义埋在 panels.js 的 build() 里时, 改错一个默认值
 * 只能靠肉眼比对发现, 而 test/facts.test.mjs 里那句「滑杆默认值同源」的注释断言的
 * 其实是 panels.js 自己的字面量 —— 改字面量、测字面量, 重复常量被测试掩盖。
 * 这里直接从 facts.js 取值比对, 让「同源」成为可执行断言而不是注释。
 */
import test from "node:test";
import assert from "node:assert/strict";
import { SLIDER_SPEC, WEB_ONLY_KEYS } from "../js/sliders.js";
import { value } from "../js/facts.js";
import { LAYOUT_SPEC } from "../js/design.js";

const byKey = new Map(SLIDER_SPEC.map(s => [s.key, s]));

test("滑杆键唯一, 且数量与面板约定一致", () => {
  assert.equal(SLIDER_SPEC.length, 16);
  assert.equal(byKey.size, 16, "存在重复 key");
});

test("每项的 label / 值域 / 默认值都在可用范围内", () => {
  for (const s of SLIDER_SPEC) {
    assert.ok(typeof s.label === "string" && s.label.length > 0, s.key + " 缺 label");
    assert.ok(Number.isFinite(s.min) && Number.isFinite(s.max) && s.min < s.max, s.key + " 值域非法");
    assert.ok(s.def >= s.min && s.def <= s.max, `${s.key} 默认值 ${s.def} 越界 [${s.min}, ${s.max}]`);
    if (s.step !== undefined) assert.ok(s.step > 0 && (s.max - s.min) / s.step > 1, s.key + " step 非法");
    // label 必须自报口径: 游戏端不读的必须挂「仅预览」, 否则用户以为改了生效
    if (s.webOnly) assert.ok(s.label.includes("仅预览"), s.key + " 标了 webOnly 却没在 label 上说「仅预览」");
    else assert.ok(!s.label.includes("仅预览"), s.key + " 不是 webOnly 却标了「仅预览」");
  }
});

test("默认值等于 mod 内置常量 (与 facts.js 逐项比对)", () => {
  const withFact = SLIDER_SPEC.filter(s => s.fact);
  assert.equal(withFact.length, 3, "有内置值对应的滑杆应当是 tachieH / recordSize / block1");
  for (const s of withFact) {
    const fv = value(s.fact);
    assert.ok(Math.abs(s.def - fv) < 1e-9,
      `${s.key} 默认值 ${s.def} != ${s.fact} 的 ${fv} —— 面板给的起点已经不是游戏端的值了`);
  }
});

test("偏移类滑杆默认 0 (缺省即内置位置, 不该预置偏移)", () => {
  for (const s of SLIDER_SPEC) {
    if (/Offset$|X$|Y$/.test(s.key) || s.key === "recordX" || s.key === "recordY") {
      assert.equal(s.def, 0, s.key + " 是偏移类却不以 0 为默认值");
    }
  }
});

test("可导出的滑杆与 webOnly 滑杆恰好划分全部 16 个 (与 LAYOUT_SPEC 对齐)", () => {
  // 三处口径必须一致: SLIDER_SPEC / design.js 的 LAYOUT_SPEC / WEB_ONLY_KEYS。
  // 少一处就会有滑杆要么导出了游戏端不读的键, 要么改了却不进产物 —— 后者正是这个
  // 工具最初最严重的问题 (16 个滑杆全部只写 localStorage)。
  const exported = LAYOUT_SPEC.map(s => s.ov);
  assert.equal(exported.length + WEB_ONLY_KEYS.length, SLIDER_SPEC.length);
  const all = [...exported, ...WEB_ONLY_KEYS].sort();
  assert.deepEqual(all, SLIDER_SPEC.map(s => s.key).sort(), "两个集合不是 SLIDER_SPEC 的一个划分");
  assert.equal(new Set(all).size, all.length, "划分有重叠");
});

test("被导出的滑杆默认值可往返 (与 design.test 的往返用例互补)", () => {
  // 这里只钉住「起点就是内置值」, 往返精度由 design.test.mjs 覆盖
  for (const s of LAYOUT_SPEC) {
    const spec = byKey.get(s.ov);
    assert.ok(spec, s.ov + " 不在 SLIDER_SPEC 里");
    if (s.offset) assert.equal(spec.def, 0, s.ov + " 声明为偏移导出却以非 0 为默认");
  }
});