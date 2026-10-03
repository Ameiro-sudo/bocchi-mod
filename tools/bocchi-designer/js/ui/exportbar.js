/* ============================================================================
 * ui/exportbar.js - design.json 实时预览 + 导出区
 * ==========================================================================*/
import { exportPack, exportJson, copyJson } from "../io.js";

/** 预览块; 内容由 render.relayout() 刷新, 这里只负责给出这个元素 */
export function addJsonPreview(body) {
  const pre = document.createElement("pre");
  pre.id = "jsonPreview";
  body.appendChild(pre);
}

export function addExportBar(body) {
  const row = document.createElement("div");
  row.className = "export-row";
  const exportBtn = document.createElement("button");
  exportBtn.className = "export-btn"; exportBtn.id = "exportBtn";
  exportBtn.textContent = "导 出 材 质 包 (zip)";
  exportBtn.addEventListener("click", () => exportPack());
  const jsonBtn = document.createElement("button");
  jsonBtn.className = "export-btn sec"; jsonBtn.id = "exportJsonBtn";
  jsonBtn.textContent = "下载 design.json";
  jsonBtn.addEventListener("click", () => exportJson());
  row.append(exportBtn, jsonBtn);
  body.appendChild(row);
  const copyBtn = document.createElement("button");
  copyBtn.className = "export-btn sec"; copyBtn.id = "copyJsonBtn";
  copyBtn.textContent = "复制 design.json";
  copyBtn.addEventListener("click", () => copyJson());
  body.appendChild(copyBtn);
  const hint = document.createElement("div");
  hint.className = "hint";
  // 这段清单是「产物里到底有什么」的唯一说明, 所以逐段写实。写不写全, 用户就只能
  // 靠导出后再打开 zip 去核对; 而漏写的那一段恰好是他刚才花了十分钟调的东西。
  hint.textContent =
    "实际写入 design.json 的有七段: textures / svgs / fonts / colors / menu / texts / layout。"
    + "layout 是 misayos 菜单的布局系数 (归一化比例, 覆盖游戏端硬编码); 布局微调段里标「仅预览」的 7 个滑杆不在其中 —— 游戏端不读, 写进去也只是让人误以为生效了。"
    + "预览配色同样只存在于本页面, 不进产物 —— 它调的是设计器自己的界面主题, 不是 mod 的。"
    + "包内另有 pack.mcmeta 与全部被引用的资源 (未上传的自动用内置默认)。支持 1.21.1~1.21.5+ (pack_format 33-9999)。";
  body.appendChild(hint);
}