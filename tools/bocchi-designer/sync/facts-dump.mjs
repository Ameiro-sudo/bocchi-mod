/* ============================================================================
 * facts-dump.mjs - 用 Node 直接导入 js/facts.js 求值全部布局常量
 *
 * 输出 (stdout): { "<group>.<name>": { "value": number, "java": string|null } }
 * 这是布局常量的唯一权威求值源 (浏览器预览与漂移检查共用同一份代码),
 * 替代 check-layout.py 内置的正则+解释器双实现。
 *
 * 用法: node sync/facts-dump.mjs
 * ==========================================================================*/
import { groups, valueIn } from "../js/facts.js";

/* 求值帧: 默认预览帧 1280x720; check-layout.py 用 --w 480 --h 270 对齐 Java 逻辑画布 */
function argOf(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 ? parseFloat(process.argv[i + 1]) : def;
}
const w = argOf("w", 1280), h = argOf("h", 720);

const out = {};
for (const g of Object.keys(groups)) {
  for (const k of Object.keys(groups[g])) {
    out[`${g}.${k}`] = { value: valueIn(`${g}.${k}`, w, h), java: groups[g][k].java ?? null };
  }
}
process.stdout.write(JSON.stringify(out));
