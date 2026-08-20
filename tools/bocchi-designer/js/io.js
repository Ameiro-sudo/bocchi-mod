/* ============================================================================
 * io.js — 材质包 导入/导出 (zip), design.json 单文件下载/复制
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});
  const { $, toast, download, zipWrite, zipRead } = BD.core;
  const { S, buildDesignJSON, usedPath, localAsset, zipEntry } = BD.design;

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
      const skipped = [];
      for (const sec of ["textures", "svgs", "fonts"]) {
        for (const [k, v] of Object.entries(S[sec])) {
          // H1: 保留命名空间 — 打包到 assets/<namespace>/<rest>, 默认 minecraft
          const name = zipEntry(v.path);
          if (v.blob) {
            files.push({ name, data: new Uint8Array(await v.blob.arrayBuffer()) });
          } else {
            try {
              const r = await fetch(localAsset(v.path));
              if (!r.ok) throw new Error("fetch " + r.status);
              files.push({ name, data: new Uint8Array(await r.arrayBuffer()) });
            } catch (e) {
              // 内置资源缺失 (如 meiryo-bold.ttf 未随工具分发): 不打包, 游戏端回退内置默认
              skipped.push(v.path);
            }
          }
        }
      }
      download(new Blob([zipWrite(files)]), "bocchi-design-pack.zip");
      const warn = missing.length ? `；缺失 ${missing.length} 个资源: ${missing.join(", ")}` : "";
      const skip = skipped.length ? `；${skipped.length} 个内置资源未分发, 游戏端回退默认: ${skipped.join(", ")}` : "";
      toast(`导出成功！${files.length - 3} 个资源已打包${warn}${skip}`);
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
    const KNOWN = new Set(["textures", "svgs", "fonts", "colors", "menu"]);
    for (const [sec, obj] of Object.entries(root)) {
      if (sec.startsWith("_")) continue;
      if (!obj || typeof obj !== "object" || Array.isArray(obj)) {
        // 非对象 section 值: 原样保留
        S.extra[sec] = obj;
        continue;
      }
      if (!KNOWN.has(sec)) {
        // H2: 未知 section 原样保留 (Java 端纯累加覆盖, 不丢弃)
        S.extra[sec] = { ...obj };
        continue;
      }
      if (sec === "menu") {
        for (const [k, v] of Object.entries(obj)) {
          if (k.startsWith("_")) continue;
          if (k === "theme" && typeof v === "string" && ["misayos", "poulsen"].includes(v)) {
            S.menu.theme = v;
            const sel = BD.panels.getThemeSel();
            if (sel) sel.value = v;
          } else {
            if (!S.extra.menu) S.extra.menu = {};
            S.extra.menu[k] = v;
          }
        }
        continue;
      }
      for (const [k, val] of Object.entries(obj)) {
        if (k.startsWith("_")) continue;
        if (!(k in S[sec])) {
          // H2: 已知 section 中的未知键原样保留
          if (!S.extra[sec]) S.extra[sec] = {};
          S.extra[sec][k] = val;
          continue;
        }
        if (sec === "colors") {
          S.colors[k] = val;
          continue;
        }
        if (typeof val !== "string") {
          // 路径字段必须为字符串, 否则保留原值并入 extras
          if (!S.extra[sec]) S.extra[sec] = {};
          S.extra[sec][k] = val;
          continue;
        }
        // H1: 保留命名空间前缀, 仅在按命名空间查 zip 条目时拆分
        S[sec][k].path = val;
        BD.design.setBlob(sec, k, null);
        if (entries) {
          const { ns, rest } = BD.design.splitPath(val);
          const entry = entries["assets/" + ns + "/" + rest];
          if (entry) { S[sec][k].blob = new Blob([entry]); count++; }
        }
      }
    }
    if (root.menu && typeof root.menu.theme === "string" && ["misayos", "poulsen"].includes(root.menu.theme)) {
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
      toast(`导入成功: ${file.name}（design.json + ${count} 个资源已载入预览）`);
    } catch (err) {
      toast("导入失败: " + err.message, true);
    }
  }

  async function importJsonFile(file) {
    try {
      const root = JSON.parse(await file.text());
      const count = applyDesignJSON(root, null);
      toast(`已载入 design.json: ${file.name}（${count} 个资源路径, 上传文件需重新选择）`);
    } catch (err) {
      toast("导入失败: " + err.message, true);
    }
  }

  /* ---------- 事件绑定 ---------- */
  let dragDepth = 0; // L4: 进入/离开计数, 避免跨子元素闪烁
  function bind() {
    $("btnImport").addEventListener("click", () => $("importInput").click());
    $("importInput").addEventListener("change", e => {
      const f = e.target.files[0];
      if (f) importFileByExt(f);
      e.target.value = "";
    });
    window.addEventListener("dragenter", e => {
      if (!hasFiles(e)) return;
      e.preventDefault();
      if (dragDepth === 0) $("dropOverlay").style.display = "flex";
      dragDepth++;
    });
    window.addEventListener("dragover", e => {
      if (!hasFiles(e)) return;
      e.preventDefault();
    });
    window.addEventListener("dragleave", e => {
      if (!hasFiles(e)) return;
      dragDepth = Math.max(0, dragDepth - 1);
      if (dragDepth === 0) $("dropOverlay").style.display = "none";
    });
    window.addEventListener("drop", e => {
      e.preventDefault();
      dragDepth = 0;
      $("dropOverlay").style.display = "none";
      const f = e.dataTransfer.files && e.dataTransfer.files[0];
      if (f) importFileByExt(f);
    });
  }
  function hasFiles(e) {
    return e.dataTransfer && e.dataTransfer.types && Array.prototype.indexOf.call(e.dataTransfer.types, "Files") >= 0;
  }
  function importFileByExt(f) {
    if (/\.json$/i.test(f.name)) importJsonFile(f);
    else importPack(f);
  }

  BD.io = { exportPack, exportJson, copyJson, applyDesignJSON, importPack, importJsonFile, importFileByExt, bind };
})();
