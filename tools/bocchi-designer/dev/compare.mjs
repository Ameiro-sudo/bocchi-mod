/* ============================================================================
 * compare.mjs - 对比两份 verify.mjs 快照, 报告差异
 *
 * 数值容差默认 0.05 (px 级浮点噪声); 字符串必须完全一致。
 * 退出码: 0 = 等价; 1 = 存在差异; 2 = 用法错误或基线不兼容。
 *
 * 用法: node dev/compare.mjs <base.json> <head.json> [--tol 0.05] [--max 40] [--struct]
 *   --struct 只比结构与字符串, 不比数值 (跨平台 CI 用; 见 STRUCT 注释)
 * ==========================================================================*/
import fs from "fs";

/* 参数解析: 先把 argv 拆成「位置参数」与「--flag [value]」两类, 再各取所需。
   早先的实现用 process.argv.slice(2) 直接解构位置参数, 于是
   `compare.mjs --tol 0.5 a.json b.json` 会把 "--tol" 当文件名, 抛一个裸 ENOENT
   栈 —— 标志与位置参数换个顺序就崩的解析器不算解析器。 */
const argv = process.argv.slice(2);
const flags = new Map();
const files = [];
for (let i = 0; i < argv.length; i++) {
  const cur = argv[i];
  if (cur.startsWith("--")) {
    const next = argv[i + 1];
    /* 下一个不是标志时当作本标志的值; 否则视为无值布尔标志 */
    if (next !== undefined && !next.startsWith("--")) { flags.set(cur.slice(2), next); i++; }
    else flags.set(cur.slice(2), true);
  } else files.push(cur);
}
const [fileA, fileB] = files;
if (!fileA || !fileB) {
  console.error("用法: node dev/compare.mjs <base.json> <head.json> [--tol 0.05] [--max 40] [--struct]");
  process.exit(2);
}
const TOL = parseFloat(flags.get("tol") ?? "0.05");
const MAX = parseInt(flags.get("max") ?? "40", 10);
/* 结构模式: 只比键集合与字符串, 完全不比数值。给 CI 用 —— 快照里的 rect / naturalW
   来自真实布局, 同一份代码在 Linux runner 与开发者 Windows 上会有零点几个像素的
   亚像素差, 拿全量数值比对当门禁只会得到一堆假红, 而假红会训练人忽略红色。 */
const STRUCT = flags.has("struct");
if (!Number.isFinite(TOL) || TOL < 0) {
  console.error(`用法错误: --tol 需要一个 >= 0 的数, 实得 ${flags.get("tol")}`);
  process.exit(2);
}

const readJson = (p) => JSON.parse(fs.readFileSync(p, "utf8"));
/* head 支持 "-" 读 stdin, 便于把快照管道进来比 */
const a = readJson(fileA);
const b = fileB === "-" ? readJson(0) : readJson(fileB);

/* 版本闸门: verify.mjs 往快照头部写 schemaVersion / excludeVersion。
   两者任一不同, 两份快照的「可比集」本身就不一样 (采集字段变了或排除表变了):
   逐键比对只会报出一堆「仅存在于新版/基线」噪声, 或者更糟 —— 排除表变松时安静地
   判出「等价」。所以版本不同直接判不兼容, 退出码 2, 要求重采基线。
   缺失字段按 1 处理: 于是 gitignore 里年代更老的基线会明确报错, 而不是静默混入。 */
for (const key of ["schemaVersion", "excludeVersion"]) {
  const va = a[key] ?? 1;
  const vb = b[key] ?? 1;
  if (va !== vb) {
    console.error(
      `基线不兼容: ${key} 基线=${va} 新版=${vb}\n` +
      `  快照格式或排除表在两次采集之间变过 —— 这两份不是同一把尺子量的, 不能比。\n` +
      `  做法: 用当前 verify.mjs 重新采集基线 (node dev/verify.mjs --out dev/dumps/baseline.json)。`
    );
    process.exit(2);
  }
}

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
    if (STRUCT) return;
    if (Math.abs(x - y) > TOL) diffs.push(`${p}: ${x} != ${y} (差 ${+(x - y).toFixed(4)})`);
    return;
  }
  if (x !== y) diffs.push(`${p}: ${JSON.stringify(x)} != ${JSON.stringify(y)}`);
}
walk(a, b, "$");

const MODE = STRUCT ? "struct" : `tol=${TOL}`;
if (diffs.length === 0) {
  console.log(`等价: ${fileA} == ${fileB} (${MODE})`);
  process.exit(0);
}
console.log(`发现 ${Math.min(diffs.length, MAX)}${diffs.length > MAX ? "+" : ""} 处差异 (${MODE}):`);
for (const d of diffs.slice(0, MAX)) console.log("  " + d);
process.exit(1);