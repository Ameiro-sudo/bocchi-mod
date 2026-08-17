/* ============================================================================
 * io.js — 材质包 导入/导出 (zip), design.json 单文件下载/复制
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});
  const { $, toast, download, zipWrite, zipRead } = BD.core;
  const { S, buildDesignJSON, usedPath, localAsset } = BD.design;

  const README_TEXT = [
    "bocchi design pack - exported by Bocchi Designer",
    "",
    "用法:",
    "1. 把本 zip 放进 .minecraft/resourcepacks/ 目录 (可改名)",
    "2. 游戏内 选项 -> 资源包 启用",
    "3. 想微调: 直接编辑 assets/minecraft/client/design.json, 改哪项覆盖哪项",
    "   所有以 _ 开头的键是注释, 会被游戏忽略",
    "",
    "主题切换: design.json 的 menu.theme = \"misayos\" (喜多郁代) 或 \"poulsen\" (后藤独), 保存后重载资源即可",
    "换字体: 替换 client/fonts/ 下的 ttf 并重进游戏 (字体不支持热重载)",
  ].join("\r\n");

  async function exportPack() {
    const btn = $("exportBtn");
    btn.disabled = true;
    btn.textContent = "打包中...";
    try {
      const enc = new TextEncoder();
      const files = [
        { name: "pack.mcmeta", data: enc.encode(JSON.stringify({ pack: { pack_format: 46, supported_formats: [33, 9999], description: "bocchi design - exported by Bocchi Designer" } }, null, 2)) },
        { name: "README.txt", data: enc.encode(README_TEXT) },
        { name: "assets/minecraft/client/design.json", data: enc.encode(JSON.stringify(buildDesignJSON(), null, 2)) },
      ];
      const missing = [];
      for (const sec of ["textures", "svgs", "fonts"]) {
        for (const [k, v] of Object.entries(S[sec])) {
          const path = v.path.replace(/^[a-z0-9_.-]+:/, "");
          const name = "assets/minecraft/" + path;
          if (v.blob) {
            files.push({ name, data: new Uint8Array(await v.blob.arrayBuffer()) });
          } else {
            try {
              const r = await fetch(localAsset(path));
              if (!r.ok) throw new Error("fetch " + r.status);
              files.push({ name, data: new Uint8Array(await r.arrayBuffer()) });
            } catch (e) { missing.push(v.path); }
          }
        }
      }
      download(new Blob([zipWrite(files)]), "bocchi-design-pack.zip");
      const warn = missing.length ? `；缺失 ${missing.length} 个内置资源: ${missing.join(", ")}` : "";
      toast(`导出成功！${files.length - 3} 个资源已打包${warn}`);
    } catch (err) {
      toast("导出失败: " + err.message, true);
    }
    btn.disabled = false;
    btn.textContent = "导 出 材 质 包 (zip)";
  }

  function exportJson() {
    const dj = JSON.stringify(buildDesignJSON(), null, 2);
    download(new Blob([dj], { type: "application/json" }), "design.json");
    toast("design.json 已下载 (复制到材质包 assets/minecraft/client/design.json)");
  }

  function copyJson() {
    navigator.clipboard.writeText(JSON.stringify(buildDesignJSON(), null, 2)).then(
      () => toast("design.json 已复制到剪贴板"),
      () => toast("复制失败: 剪贴板不可用", true));
  }

  /** 从已解析的 design.json 对象载入编辑态 (导入 zip 与单文件共用) */
  function applyDesignJSON(root, entries) {
    let count = 0;
    for (const sec of ["textures", "svgs", "fonts", "colors"]) {
      const obj = root[sec];
      if (!obj || typeof obj !== "object") continue;
      for (const [k, val] of Object.entries(obj)) {
        if (k.startsWith("_") || !(k in S[sec]) || typeof val !== "string") continue;
        if (sec === "colors") {
          S.colors[k] = val;
          continue;
        }
        S[sec][k].path = val;
        S[sec][k].blob = null;
        if (entries) {
          const path = val.replace(/^[a-z0-9_.-]+:/, "");
          const entry = entries["assets/minecraft/" + path];
          if (entry) { S[sec][k].blob = new Blob([entry]); count++; }
        }
      }
    }
    if (root.menu && typeof root.menu.theme === "string" && ["misayos", "poulsen"].includes(root.menu.theme)) {
      S.menu.theme = root.menu.theme;
      const sel = BD.panels.getThemeSel();
      if (sel) sel.value = root.menu.theme;
    }
    BD.panels.updateResNames();
    BD.preview.refreshPreviews();
    BD.panels.relayout();
    return count;
  }

  async function importPack(file) {
    try {
      const buf = new Uint8Array(await file.arrayBuffer());
      const entries = await zipRead(buf);
      const dj = entries["assets/minecraft/client/design.json"];
      if (!dj) throw new Error("包内未找到 assets/minecraft/client/design.json");
      const root = JSON.parse(new TextDecoder().decode(dj));
      const count = applyDesignJSON(root, entries);
      BD.interactions.showStage("misayos");
      toast(`导入成功: ${file.name}（design.json + ${count} 个资源已载入预览）`);
    } catch (err) {
      toast("导入失败: " + err.message, true);
    }
  }

  async function importJsonFile(file) {
    try {
      const root = JSON.parse(await file.text());
      const count = applyDesignJSON(root, null);
      BD.interactions.showStage("misayos");
      toast(`已载入 design.json: ${file.name}（${count} 个资源路径, 上传文件需重新选择）`);
    } catch (err) {
      toast("导入失败: " + err.message, true);
    }
  }

  /* ---------- 事件绑定 ---------- */
  function bind() {
    $("btnImport").addEventListener("click", () => $("importInput").click());
    $("importInput").addEventListener("change", e => {
      const f = e.target.files[0];
      if (f) importFileByExt(f);
      e.target.value = "";
    });
    window.addEventListener("dragover", e => { e.preventDefault(); $("dropOverlay").style.display = "flex"; });
    window.addEventListener("dragleave", () => { $("dropOverlay").style.display = "none"; });
    window.addEventListener("drop", e => {
      e.preventDefault();
      $("dropOverlay").style.display = "none";
      const f = e.dataTransfer.files && e.dataTransfer.files[0];
      if (f) importFileByExt(f);
    });
  }
  function importFileByExt(f) {
    if (/\.json$/i.test(f.name)) importJsonFile(f);
    else importPack(f);
  }

  BD.io = { exportPack, exportJson, copyJson, applyDesignJSON, importPack, importJsonFile, importFileByExt, bind };
})();
