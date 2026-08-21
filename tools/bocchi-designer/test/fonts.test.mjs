/* fonts.js 元数据一致性单测 (不触发 canvas 度量) */
import test from "node:test";
import assert from "node:assert/strict";
import { FONT_META, FONT_SET_NAME } from "../js/fonts.js";
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
