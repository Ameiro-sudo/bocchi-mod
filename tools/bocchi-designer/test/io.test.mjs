/* io.applyDesignJSON 单测: 合并语义 / 命名空间 / 原型污染防护 */
import test from "node:test";
import assert from "node:assert/strict";
import { S, DEFAULT_TEXTS } from "../js/design.js";
import { applyDesignJSON } from "../js/io.js";
import { zipEntry, splitPath } from "../js/design.js";

test("splitPath/zipEntry: 命名空间保留 (H1)", () => {
  assert.deepEqual(splitPath("minecraft:textures/a.png"), { ns: "minecraft", rest: "textures/a.png" });
  assert.deepEqual(splitPath("my_ns:path/x.ttf"), { ns: "my_ns", rest: "path/x.ttf" });
  assert.deepEqual(splitPath("client/fonts/a.ttf"), { ns: "minecraft", rest: "client/fonts/a.ttf" });
  assert.equal(zipEntry("my_pack:x.svg"), "assets/my_pack/x.svg");
  assert.equal(zipEntry("textures/b.png"), "assets/minecraft/textures/b.png");
});

test("applyDesignJSON: 已知段覆盖 + 未知键入 extras", () => {
  const root = {
    _comment_key: "ignored",
    colors: { vinyl_base: "#ABCDEF" },
    textures: { bocchi: "client/textures/new-bocchi.png", brand_new_thing: "keep-me" },
    totally_new_section: { nested: true },
    menu: { theme: "poulsen", future_opt: 1 },
  };
  applyDesignJSON(root, null);
  assert.equal(S.colors.vinyl_base, "#ABCDEF");
  assert.equal(S.textures.bocchi.path, "client/textures/new-bocchi.png");
  assert.equal(S.extra.textures.brand_new_thing, "keep-me");
  assert.deepEqual({ ...S.extra.totally_new_section }, { nested: true });
  assert.equal(S.menu.theme, "poulsen");
  assert.equal(S.extra.menu.future_opt, 1);
});

test("texts 段: mInfoLine1~3 无 mInfo 时组合回预览字段", () => {
  const before = S.menu.theme;
  applyDesignJSON({ texts: { mInfoLine1: "甲", mInfoLine2: "乙", mInfoLine3: "丙", other_line: "x" } }, null);
  assert.equal(S.texts.mInfo, "甲<br>乙<br>丙");
  assert.equal(S.extra.texts.other_line, "x");
  S.texts.mInfo = DEFAULT_TEXTS.mInfo;
  S.menu.theme = before;
});

test("原型污染防护: 危险键被丢弃, Object.prototype 不受影响", () => {
  const root = JSON.parse('{"__proto__": {"polluted": 1}, "menu": {"__proto__": 2, "theme": "misayos"}, "colors": {"constructor": 3}}');
  applyDesignJSON(root, null);
  assert.equal(({}).polluted, undefined);
  assert.equal(S.colors.constructor === 3, false);
  assert.equal(S.menu.theme, "misayos");
});

test("路径字段非字符串时原样保留入 extras", () => {
  applyDesignJSON({ svgs: { single: 42 } }, null);
  assert.equal(S.extra.svgs.single, 42);
  assert.notEqual(S.svgs.single.path, 42);
});
