/* fonts.js 元数据一致性单测 (不触发 canvas 度量) */
import test from "node:test";
import assert from "node:assert/strict";
import { FONT_META, FONT_SET_NAME, gh, cssTop, tw } from "../js/fonts.js";
import { DEFAULT_DESIGN } from "../js/design.js";

test("design.json fonts 键全部有 CSS 字体族映射", () => {
  for (const key of Object.keys(DEFAULT_DESIGN.fonts)) {
    assert.ok(FONT_SET_NAME[key], "缺少映射: " + key);
  }
});

test("每个映射到的字体族都有度量元数据 (gh/topK)", () => {
  for (const fam of Object.values(FONT_SET_NAME)) {
    const m = FONT_META[fam];
    assert.ok(m && typeof m.gh === "number" && typeof m.topK === "number", "缺少度量: " + fam);
  }
});

test("gh/cssTop 是 FONT_META 的纯线性映射", () => {
  // gh(px) = 屏幕行高; cssTop(y,px) = 基线 y + 上移量。两者共同决定文字在舞台上
  // 落在哪 —— 它们此前零覆盖, 改错一个系数只表现为「字整体偏上一点」, 没人会去找因。
  for (const [fam, m] of Object.entries(FONT_META)) {
    assert.equal(gh(100, fam), 100 * m.gh);
    assert.equal(cssTop(200, 100, fam), 200 + 100 * m.topK);
  }
  assert.equal(gh(0, "Meiryo"), 0, "0px 不应有行高");
  // cssTop 对负 topK (下沉的字体) 必须真的把基线上移
  assert.ok(cssTop(100, 100, "Radikal Black") < 100);
  // 未知字体族应直接炸, 而不是悄悄算成 NaN 拖垮整张舞台的排版
  assert.throws(() => gh(100, "不存在的字体"), TypeError);
});

test("tw: 文本宽度 = 度量宽度 + 字距×字符数", async () => {
  // tw 里的度量 canvas 是惰性建的 (首次调用才 document.createElement), 所以这里
  // 可以替一个最小 document。字距按字符数线性叠加, 空串与缺省字距都要安全。
  const prevDoc = globalThis.document;
  const seen = [];
  globalThis.document = {
    createElement: () => ({
      getContext: () => ({
        set font(v) { seen.push(v); },
        measureText: (t) => ({ width: t.length * 7 }),
      }),
    }),
  };
  try {
    const { tw: measure } = await import("../js/fonts.js");
    assert.equal(measure("abcd", 20, "Meiryo"), 28);
    assert.equal(measure("abcd", 20, "Meiryo", 2), 28 + 8);
    assert.equal(measure("", 20, "Meiryo"), 0, "空文本不得产生 NaN (会污染居中计算)");
    assert.equal(measure("abcd", 20, "Meiryo", undefined), 28, "缺省字距按 0 处理");
    assert.ok(seen.length > 0 && seen[0].includes("Meiryo"), "必须把字体族写进度量 canvas");
  } finally {
    if (prevDoc === undefined) delete globalThis.document; else globalThis.document = prevDoc;
  }
});
