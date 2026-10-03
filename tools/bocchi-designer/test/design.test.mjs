/* design.js 单测: 防护工具 / 颜色转换 / 文案导出 / design.json 组装 */
import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  S, DEFAULT_DESIGN, DEFAULT_TEXTS, UNSAFE_KEYS, cleanCopy, hexToCss,
  buildDesignJSON, textsForExport, uploadedCount,
} from "../js/design.js";

/* ---------- 契约测试: 工具的建模面必须覆盖 mod 内置 design.json ----------
 *
 * sTitle/sDone 被 SettingsPanel.java 真实消费, 却在工具里缺席了很久: 面板上没有这两行,
 * 用户改完文案导出的 design.json 也不含这两个键, 游戏端只能回退硬编码默认值。
 * 单测逐个 case 去看根本防不住 —— 新加一个键时不会有人记得补断言。
 * 这里改成拿 mod 的内置模板当事实来源反向校验: 模板里有的键, 工具必须能表示。
 */
const MOD_DESIGN = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..", "..", "..",
  "src", "bocchi-1.21.5", "common", "src", "main", "resources",
  "assets", "minecraft", "client", "design.json",
);

test("契约: 工具建模面覆盖 mod 内置 design.json 的每个键", (t) => {
  if (!fs.existsSync(MOD_DESIGN)) {
    t.skip("mod 内置 design.json 不在 (工具被单独分发时属预期)");
    return;
  }
  const mod = JSON.parse(fs.readFileSync(MOD_DESIGN, "utf8"));
  const known = (o) => Object.keys(o).filter((k) => !k.startsWith("_"));

  // texts: 工具的编辑面是 DEFAULT_TEXTS, 导出时才把 mInfo 一行摊成
  // mInfoLine1~3 (io.js 导入时再合回去)。所以拿 DEFAULT_TEXTS 比, 别拿导出结果比 ——
  // 导出结果里根本没有 mInfo 这个键, 拿它比会把三个 mInfoLine 全判成缺失。
  const toToolKey = (k) => k.replace(/^mInfoLine[123]$/, "mInfo");
  const toolTextKeys = new Set(Object.keys(DEFAULT_TEXTS));
  const missingTexts = known(mod.texts).filter((k) => !toolTextKeys.has(toToolKey(k)));
  assert.deepEqual(missingTexts, [], "mod 消费但工具无法编辑的文案键");

  for (const sec of ["textures", "svgs", "fonts", "colors"]) {
    const missing = known(mod[sec]).filter((k) => !Object.hasOwn(DEFAULT_DESIGN[sec], k));
    assert.deepEqual(missing, [], `mod 声明但工具缺失的 ${sec} 键`);
  }

  assert.equal(buildDesignJSON().menu.theme, mod.menu.theme, "默认主题与 mod 内置不一致");

  // 模板里有而工具没有的整个 section: 不失败, 但必须显式列出来让人看见。
  // shaders 段是 PassTest 这个开发期测试入口用的, 刻意不做 UI。
  const toolSections = new Set(["_readme", "textures", "svgs", "fonts", "colors", "texts", "menu"]);
  const extraSections = Object.keys(mod).filter((k) => !k.startsWith("_") && !toolSections.has(k));
  assert.deepEqual(extraSections, [], "mod 有工具完全未建模的 section (如需建模请一并补 UI)");
});

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
