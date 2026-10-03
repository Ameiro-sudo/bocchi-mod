/* design.js 单测: 防护工具 / 颜色转换 / 文案导出 / design.json 组装 */
import test from "node:test";
import assert from "node:assert/strict";
import {
  S, DEFAULT_DESIGN, DEFAULT_TEXTS, UNSAFE_KEYS, cleanCopy, hexToCss,
  buildDesignJSON, textsForExport, uploadedCount,
} from "../js/design.js";

test("cleanCopy 过滤危险键并返回 null 原型对象", () => {
  const evil = JSON.parse('{"__proto__": {"x": 1}, "constructor": 1, "ok": 2}');
  const out = cleanCopy(evil);
  assert.equal(Object.getPrototypeOf(out), null);
  assert.ok(!UNSAFE_KEYS.has("ok"));
  assert.deepEqual(Object.keys(out).sort(), ["ok"]);
  assert.equal(cleanCopy(null).constructor, undefined);   // 非对象输入 -> 空容器
  assert.equal(cleanCopy("str").constructor, undefined);
  assert.equal(({}).x, undefined);                        // 原型未被污染
});

test("hexToCss: 6/8 位十六进制与非法值", () => {
  assert.equal(hexToCss("#FF0000"), "#FF0000");
  assert.equal(hexToCss("00FF00"), "#00FF00");            // # 可省略
  assert.equal(hexToCss("#33FFFFFF"), "rgba(255,255,255,0.200)");
  assert.equal(hexToCss("#001A1A1A"), "rgba(26,26,26,0.000)");
  assert.equal(hexToCss("#XYZ"), null);
  assert.equal(hexToCss(""), null);
  assert.equal(hexToCss(undefined), null);
});

test("textsForExport: \\u00A0 转空格, mInfo 按 <br> 拆三行", () => {
  const t = textsForExport();
  assert.equal(t.mPhobia, "SOCIAL  PHOBIA");
  assert.equal(t.pCopy1, "Bocchi Client    Version - 1.0");
  const infoLines = ["L one", "L two", "L three"];
  S.texts.mInfo = infoLines.join("<br>");
  const t2 = textsForExport();
  assert.deepEqual([t2.mInfoLine1, t2.mInfoLine2, t2.mInfoLine3], infoLines);
  S.texts.mInfo = DEFAULT_TEXTS.mInfo;                    // 还原
});

test("buildDesignJSON: 结构完整且默认值就位", () => {
  const o = buildDesignJSON();
  assert.ok(o._readme.startsWith("bocchi 设计模板"));
  for (const sec of ["textures", "svgs", "fonts", "colors", "texts", "menu"]) {
    assert.ok(o[sec] && typeof o[sec] === "object", sec);
    assert.ok(Object.keys(o[sec]).some(k => k.startsWith("_")), sec + " 注释键");
  }
  assert.equal(o.textures.bocchi, DEFAULT_DESIGN.textures.bocchi);
  assert.equal(o.menu.theme, "misayos");
  assert.equal(o.colors.vinyl_edge, DEFAULT_DESIGN.colors.vinyl_edge);
});

test("未知键保留区 extra 合并进导出且不覆盖已知键", () => {
  S.extra.custom_section = Object.assign(Object.create(null), { foo: "bar" });
  S.extra.texts = Object.assign(Object.create(null), { customText: "hi", mBocchi: "HIJACK" });
  const o = buildDesignJSON();
  assert.deepEqual(o.custom_section, { foo: "bar" });
  assert.equal(o.texts.customText, "hi");
  assert.equal(o.texts.mBocchi, DEFAULT_TEXTS.mBocchi);   // 已知键不被 extras 覆盖
  delete S.extra.custom_section;
  delete S.extra.texts;
});

/* uploadedCount 是 beforeunload 拦截的唯一依据: 数少了 = 上传的资源被静默丢弃,
 * 数多了 = 每次关页面都弹提示, 用户学会一律点掉, 提示照样失效。
 * 所以两个方向都得钉住 —— 尤其是 colors (值是字符串, 不是 {blob} 资源项) 不能被算进去。 */
test("uploadedCount: 只数 textures/svgs/fonts 的 blob, colors/menu/texts 不算", () => {
  assert.equal(uploadedCount(), 0, "初始状态没有上传资源");
  S.textures.bocchi.blob = { fake: true };
  assert.equal(uploadedCount(), 1);
  S.fonts["meiryo-bold"].blob = { fake: true };
  assert.equal(uploadedCount(), 2, "跨 section 累加");
  S.colors.vinyl_edge = "not-a-resource-entry";
  S.texts.mBocchi = "changed";
  S.menu.theme = "poulsen";
  assert.equal(uploadedCount(), 2, "非资源段的变化不计入");
  S.textures.bocchi.blob = null;
  S.fonts["meiryo-bold"].blob = null;
  assert.equal(uploadedCount(), 0);
  S.colors.vinyl_edge = DEFAULT_DESIGN.colors.vinyl_edge;
  S.texts.mBocchi = DEFAULT_TEXTS.mBocchi;
  S.menu.theme = "misayos";
});
