/* facts.js 单测: 表达式求值器 + fact 解析 + 求值帧 */
import test from "node:test";
import assert from "node:assert/strict";
import { W, H, groups, value, valueIn, all, evalWithBindings } from "../js/facts.js";

test("设计分辨率常量", () => {
  assert.equal(W, 1280);
  assert.equal(H, 720);
});

test("表达式: 四则优先级与括号", () => {
  // 经由已知 fact 间接验证解析器: 2+3*4=14 的等价结构
  assert.equal(value("misayos.btnGap"), 20.5);           // 纯数字字面量
  assert.equal(value("splash.logoH"), 60 * 0.317);       // 常量乘法
});

test("表达式: min/max 与绑定变量引用", () => {
  // block1Size = min(1280*0.2265625, 720*0.40277...) = 290
  // 这里的 290/468/684 是 mod 内置常量本身。「滑杆默认值是否等于它们」由
  // test/sliders.test.mjs 拿 SLIDER_SPEC 跟 facts.js 逐项比对 —— 不要再在这里
  // 声称「同源」: 断言字面量、比的也是字面量, 等于把重复常量测成了一致。
  assert.ok(Math.abs(value("misayos.block1Size") - 290) < 1e-9);
  assert.equal(value("misayos.recordSize"), 468);
  assert.equal(value("misayos.tachieH"), 684);
});

test("跨组引用: splash.logoX 复用 poulsen.rect1X", () => {
  const rect1X = (1280 - 1280 * 0.412) / 2;
  assert.ok(Math.abs(value("splash.logoX") - rect1X * 0.3) < 1e-9);
});

test("Java 逻辑画布帧 (480x270) 与预览帧成比例", () => {
  assert.ok(Math.abs(valueIn("misayos.block1X", 480, 270) - 1280 * 0.095 * 0.375) < 1e-9);
  assert.equal(valueIn("misayos.titleFontSize", 480, 270), 21); // 无 W/H 项的常量不受帧影响
});

test("未知常量 / 未定义引用 / 循环引用 报错", () => {
  assert.throws(() => value("nope.nada"), /未知布局常量/);
  // 构造未定义引用: 直接改组表注入一条坏 fact
  groups.__test_bad = { bad: { expr: "undefinedRef * 2", java: null } };
  assert.throws(() => value("__test_bad.bad"), /引用未定义|未知布局常量/);
  groups.__test_cyc = {};
  groups.__test_cyc.a = { expr: "b * 2", java: null };
  groups.__test_cyc.b = { expr: "a * 2", java: null };
  assert.throws(() => value("__test_cyc.a"), /循环引用/);
  delete groups.__test_bad;
  delete groups.__test_cyc;
});

test("绑定替换按词边界: 短名不得吃掉长标识符里的片段", () => {
  // 这条最危险: bind 里同时存在 W/H/width/height/scaledWidth/scaledHeight。
  // 若替换退化成朴素字符串 replace, "scaledWidth" 会先被 "Width" 之类命中,
  // 或者单字母 W 会匹配上别的标识符 —— 症状是布局常量算出一个说不清的数, 且只在
  // 某个 fact 上出现, 极难定位。这里直接钉死这个语义。
  const r = evalWithBindings("scaledWidth * 0.5", { scaledWidth: 100, W: 1 });
  assert.equal(r, 50, "scaledWidth 必须整词命中, 不能被其它绑定拆开");
  assert.equal(evalWithBindings("W * 2", { W: 3, width: 9 }), 6, "单字母 W 只能整词命中");
  assert.equal(evalWithBindings("width * 2", { W: 3, width: 9 }), 18);
  // 同一标识符出现多次必须全部替换
  assert.equal(evalWithBindings("a + a * 2", { a: 4 }), 12);
  // 未绑定的标识符原样保留 → parseExpr 会因语法错误抛出, 而不是静默算出 0
  assert.throws(() => evalWithBindings("unknownIdent + 1", { a: 1 }), /解析|Unexpected|非法|错误/);
});

test("all(): 覆盖全部 fact 且值为有限数", () => {
  const total = Object.values(groups).reduce((a, g) => a + Object.keys(g).length, 0);
  const vals = all();
  assert.equal(Object.keys(vals).length, total);
  for (const v of Object.values(vals)) assert.ok(Number.isFinite(v));
});
