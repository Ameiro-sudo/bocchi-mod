/* ============================================================================
 * ui/res.js - 资源行 (纹理 / SVG / 字体): 路径可编辑 + 上传 + 还原内置
 *
 * applyRes 是「资源态落地」的唯一入口 (上传/撤销/改路径/恢复内置共用):
 * blob + path 一起写回, 字体段同步重注册 FontFace, 再刷新预览。
 * ==========================================================================*/
import { $, toast } from "../core.js";
import { S, DEFAULT_DESIGN, setBlob, localAsset } from "../design.js";
import { refreshPreviews } from "../preview.js";
import { FONT_SET_NAME, replaceFace } from "../fonts.js";
import { relayout } from "../render.js";
import { createField } from "../store.js";
import { markDirty, scheduleDirty } from "./dirty.js";

function applyRes(sec, key, blob, path) {
  const entry = S[sec][key];
  setBlob(sec, key, blob);
  if (entry) entry.path = path;
  if (sec === "fonts") {
    const cssFam = FONT_SET_NAME[key];
    // blob 与内置文件二选一作为字体源; 注册完成后再重排 (度量准确)
    if (cssFam && (blob || path)) replaceFace(cssFam, blob || localAsset(path)).then(() => relayout()).catch(() => {});
  }
  updateResName(sec, key);
  refreshPreviews();
  relayout();
  scheduleDirty();
}

/** 资源值 = [blob, path] 二元组; 二元组每次都是新引用, 必须按内容判等 */
const sameRes = (a, b) => a[0] === b[0] && a[1] === b[1];

export function addResRow(body, label, sec, key) {
  const row = document.createElement("div");
  row.className = "res-row";
  const lab = document.createElement("div");
  lab.className = "r-label"; lab.textContent = label;
  // 路径此前是一个只读 span —— 于是「换个命名空间 / 换个文件名」这种最常见的诉求
  // 在界面上完全没有入口, 而 Design.java 和 design.js 早就都支持 namespace:path。
  // 能力齐备只差一个输入框: 改成 input 后 path 一栏才真正可写。
  const name = document.createElement("input");
  name.type = "text"; name.spellcheck = false;
  name.className = "r-name"; name.id = `rn_${sec}_${key}`;
  name.title = "资源路径 (namespace:path, 省略命名空间则默认 minecraft)。改完回车或失焦生效, 可撤销。";
  name.addEventListener("keydown", e => { if (e.key === "Enter") { e.preventDefault(); name.blur(); } });
  // 三处编辑 (改路径 / 换文件 / 还原内置) 全部经它, 于是「撤销回放时基准同步搬动」
  // 这件事只有一份实现。label 是函数: 同一个 field 三种编辑动作措辞不同, 每次入栈
  // 时才求值, 而 undo/redo 闭包里保留的是入栈那一刻的措辞。
  let verb = "资源";
  const field = createField({
    label: () => `${verb} ${label}`,
    read: () => { const e = S[sec][key]; return [e.blob, e.path]; },
    apply: ([blob, p]) => applyRes(sec, key, blob, p),
    isSame: sameRes,
  });
  name.addEventListener("change", () => {
    const entry = S[sec][key];
    const to = name.value.trim();
    if (!to) { toast(`${label} 的路径不能为空`); updateResName(sec, key); return; }
    if (to === entry.path) return;
    // 只换路径不动 blob: 内容还在内存里, 只是打包时落到另一个条目名。
    verb = "改资源路径";
    field.set([entry.blob, to]);
  });
  const rst = document.createElement("button");
  rst.className = "row-reset"; rst.textContent = "内置";
  rst.title = "恢复 mod 内置默认 (上传/导入路径一并还原, Ctrl+Z 可撤销)";
  const btn = document.createElement("button");
  btn.className = "r-btn"; btn.textContent = "选择文件...";
  const input = document.createElement("input");
  input.type = "file"; input.style.display = "none";
  input.addEventListener("change", () => {
    const f = input.files[0];
    if (!f) return;
    // 记录可撤销的资源替换 (blob 引用互换, 无拷贝开销)
    verb = "替换资源";
    if (field.set([f, S[sec][key].path])) {
      toast(`已上传 ${f.name} (仅本次会话生效, 刷新后还原; Ctrl+Z 可撤销)`);
    }
    input.value = "";
  });
  btn.addEventListener("click", () => input.click());
  // 「内置」: 还原 mod 内置默认路径 + 清除已上传 blob (可撤销)
  rst.addEventListener("click", () => {
    const entry = S[sec][key];
    const defPath = DEFAULT_DESIGN[sec][key];
    if (!entry.blob && entry.path === defPath) { toast(`${label} 已是内置默认`); return; }
    verb = "还原资源";
    if (field.set([null, defPath])) toast(`已还原内置默认: ${label}`);
  });
  row.append(lab, name, rst, btn, input);
  body.appendChild(row);
  markDirty(row, () => {
    const e = S[sec][key];
    return !!e.blob || e.path !== DEFAULT_DESIGN[sec][key];
  });
}

/* ---------- 内置资源缺失标记 ----------
 * DEFAULT_DESIGN 里写着 client/fonts/meiryo-bold.ttf, 但那个 9.3MB 的字体没随工具
 * 分发。面板上却显示成一条和别的资源一模一样的正常路径 —— 用户以为它会进包, 实际
 * 导出时被 io.exportPack 静默跳过 (只留一条 toast), 游戏端 SkiaFont 回退到系统默认
 * 字体, 日文块字形突变。路径写在表里 ≠ 文件真的在, 所以挨个探一次。 */
const assetProbe = new Map(); // "sec/key" -> 内置文件是否可达

export async function probeBundledAssets() {
  const jobs = [];
  // 记相对路径而不是绝对 URL: 门禁跑在随机端口上, 绝对 URL 每次都不一样,
  // 快照基线就永远对不上 —— 门禁自己变成永久红灯。
  const probed = [];   // 探过的相对路径: 门禁据此剔除自己造成的 404 噪声
  for (const sec of ["textures", "svgs", "fonts"]) {
    for (const key of Object.keys(S[sec])) {
      if (S[sec][key].blob) continue;   // 用的是上传件, 内置在不在都无所谓
      const id = sec + "/" + key;
      const path = localAsset(S[sec][key].path);
      probed.push(path);
      jobs.push(
        fetch(new URL(path, location.href).href, { method: "HEAD" })
          .then(r => { assetProbe.set(id, r.ok); })
          .catch(() => { assetProbe.set(id, false); })
      );
    }
  }
  await Promise.all(jobs);
  updateResNames();
  // 返回可序列化的普通对象 (Map 传不进 page.evaluate 的返回值)
  return { checked: Object.fromEntries(assetProbe), probed };
}

function updateResName(sec, key) {
  const el = $(`rn_${sec}_${key}`);
  if (!el) return;
  const f = S[sec][key];
  const has = !!f.blob;
  // 正在编辑时不要回写 value —— 那会把用户敲到一半的字吞掉
  if (document.activeElement !== el) el.value = f.path;
  el.classList.toggle("uploaded", has);
  const missing = !has && assetProbe.get(sec + "/" + key) === false;
  el.classList.toggle("missing", missing);
  el.title = missing
    ? `内置文件未随工具分发 (${localAsset(f.path)} 不可达): 导出时该条目会被跳过, 游戏端回退内置字体。改路径或上传自己的文件即可。`
    : (has ? "已上传文件 (仅本次会话有效, 刷新后还原; 可撤销)" : "资源路径 (namespace:path, 省略命名空间则默认 minecraft)");
}

export function updateResNames() {
  for (const sec of ["textures", "svgs", "fonts"])
    for (const key of Object.keys(S[sec])) updateResName(sec, key);
}