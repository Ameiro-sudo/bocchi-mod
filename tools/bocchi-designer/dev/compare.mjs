/* ============================================================================
 * compare.mjs - 对比两份 verify.mjs 快照, 报告差异
 *
 * 数值容差默认 0.05 (px 级浮点噪声); 字符串必须完全一致。
 * 退出码: 0 = 等价; 1 = 存在差异; 2 = 用法错误。
 *
 * 用法: node dev/compare.mjs <base.json> <head.json> [--tol 0.05] [--max 40]
 * ==========================================================================*/
import fs from "fs";

function arg(name, def) {
  const i = process.argv.indexOf("--" + name);
  return i >= 0 ? process.argv[i + 1] : def;
}
const [fileA, fileB] = process.argv.slice(2);
if (!fileA || !fileB) { console.error("用法: node dev/compare.mjs <base.json> <head.json>"); process.exit(2); }
const TOL = parseFloat(arg("tol", "0.05"));
const MAX = parseInt(arg("max", "40"), 10);

const a = JSON.parse(fs.readFileSync(fileA, "utf8"));
const b = JSON.parse(fileB === "-" ? fs.readFileSync(0, "utf8") : fs.readFileSync(fileB, "utf8"));

const diffs = [];
function walk(x, y, p) {
  if (diffs.length >= MAX + 1) return;
  /* 注意: 数组也是 "object" —— 统一走键遍历分支, 否则会退化成引用比较误报 */
  const tx = x === null ? "null" : typeof x;
  const ty = y === null ? "null" : typeof y;
  if (tx !== ty) { diffs.push(`${p}: 类型 ${tx} != ${ty}`); return; }
  if (tx === "object") {
    const keys = new Set([...Object.keys(x), ...Object.keys(y)]);
    for (const k of [...keys].sort()) {
      if (!(k in x)) { diffs.push(`${p}.${k}: 仅存在于新版 (${JSON.stringify(y[k]).slice(0, 80)})`); continue; }
      if (!(k in y)) { diffs.push(`${p}.${k}: 仅存在于基线 (${JSON.stringify(x[k]).slice(0, 80)})`); continue; }
      walk(x[k], y[k], `${p}.${k}`);
      if (diffs.length >= MAX + 1) return;
    }
    return;
  }
  if (typeof x === "number" && typeof y === "number") {
    if (Math.abs(x - y) > TOL) diffs.push(`${p}: ${x} != ${y} (差 ${+(x - y).toFixed(4)})`);
    return;
  }
  if (x !== y) diffs.push(`${p}: ${JSON.stringify(x)} != ${JSON.stringify(y)}`);
}
walk(a, b, "$");

if (diffs.length === 0) {
  console.log(`等价: ${fileA} == ${fileB} (tol=${TOL})`);
  process.exit(0);
}
console.log(`发现 ${Math.min(diffs.length, MAX)}${diffs.length > MAX ? "+" : ""} 处差异 (tol=${TOL}):`);
for (const d of diffs.slice(0, MAX)) console.log("  " + d);
process.exit(1);

