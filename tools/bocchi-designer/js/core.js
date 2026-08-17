/* ============================================================================
 * core.js — 基础工具: DOM 助手 / 舞台坐标 / ZIP 读写 (零依赖) / 下载 / toast
 * ==========================================================================*/
(function () {
  "use strict";
  const BD = (window.BD = window.BD || {});

  const $ = (id) => document.getElementById(id);
  const set = (el, css) => { if (el) el.style.cssText = css; };

  /* 应用状态 (布局微调/文本/预览配色/面板开合/缩放) — 由 design.js 持久化 */
  BD.state = {
    OV: {},                 // misayos 布局微调 (预览用)
    TEXTS: {},              // 可编辑文本 (仅预览)
    PREVIEW_COLORS: {       // 预览配色 (仅预览, 不导出)
      "--accent": "#FBA0BE", "--accent-deep": "#E88BA6",
      "--bg-top": "#07021C", "--bg-bottom": "#492F49",
      "--splash-bg": "#1F1F1F", "--btn-bg": "#353535",
    },
    openSections: [],       // 展开的面板 section id
    zoom: 0,                // 0=适应窗口 1=100% 2=200%
  };

  /* ---------- toast ---------- */
  let toastTimer = null;
  function toast(msg, warn) {
    let t = $("toast");
    if (!t) {
      t = document.createElement("div");
      t.id = "toast";
      document.body.appendChild(t);
    }
    t.textContent = msg;
    t.classList.toggle("warn", !!warn);
    t.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove("show"), 3200);
  }

  /* ---------- 下载 ---------- */
  function download(blob, name) {
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 30000);
  }

  /* ---------- ZIP 写入 (store 压缩) ---------- */
  const CRC_TABLE = (() => {
    const t = new Uint32Array(256);
    for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1; t[n] = c; }
    return t;
  })();
  function crc32(u8) {
    let c = 0xFFFFFFFF;
    for (let i = 0; i < u8.length; i++) c = CRC_TABLE[(c ^ u8[i]) & 0xFF] ^ (c >>> 8);
    return (c ^ 0xFFFFFFFF) >>> 0;
  }
  function zipWrite(files) {
    const enc = new TextEncoder();
    const parts = [], central = [];
    let offset = 0;
    for (const f of files) {
      const name = enc.encode(f.name);
      const crc = crc32(f.data);
      const lh = new DataView(new ArrayBuffer(30));
      lh.setUint32(0, 0x04034b50, true); lh.setUint16(4, 20, true); lh.setUint16(6, 0x0800, true);
      lh.setUint32(14, crc, true); lh.setUint32(18, f.data.length, true); lh.setUint32(22, f.data.length, true);
      lh.setUint16(26, name.length, true);
      parts.push(new Uint8Array(lh.buffer), name, f.data);
      central.push({ name, crc, size: f.data.length, offset });
      offset += 30 + name.length + f.data.length;
    }
    const cdStart = offset, cdParts = [];
    for (const c of central) {
      const ch = new DataView(new ArrayBuffer(46));
      ch.setUint32(0, 0x02014b50, true); ch.setUint16(4, 20, true); ch.setUint16(6, 20, true); ch.setUint16(8, 0x0800, true);
      ch.setUint32(16, c.crc, true); ch.setUint32(20, c.size, true); ch.setUint32(24, c.size, true);
      ch.setUint16(28, c.name.length, true); ch.setUint32(42, c.offset, true);
      cdParts.push(new Uint8Array(ch.buffer), c.name);
    }
    const cdLen = cdParts.reduce((a, p) => a + p.length, 0);
    const eocd = new DataView(new ArrayBuffer(22));
    eocd.setUint32(0, 0x06054b50, true);
    eocd.setUint16(8, central.length, true); eocd.setUint16(10, central.length, true);
    eocd.setUint32(12, cdLen, true); eocd.setUint32(16, cdStart, true);
    const all = parts.concat(cdParts, [new Uint8Array(eocd.buffer)]);
    const out = new Uint8Array(all.reduce((a, p) => a + p.length, 0));
    let o = 0; for (const p of all) { out.set(p, o); o += p.length; }
    return out;
  }
  async function zipRead(buf) {
    let eocd = -1;
    for (let i = buf.length - 22; i >= Math.max(0, buf.length - 66000); i--) {
      if (buf[i] === 0x50 && buf[i + 1] === 0x4b && buf[i + 2] === 0x05 && buf[i + 3] === 0x06) { eocd = i; break; }
    }
    if (eocd < 0) throw new Error("不是有效的 zip 文件");
    const dv = new DataView(buf.buffer, buf.byteOffset, buf.byteLength);
    const count = dv.getUint16(eocd + 10, true);
    let cdPos = dv.getUint32(eocd + 16, true);
    const out = {};
    for (let i = 0; i < count; i++) {
      if (dv.getUint32(cdPos, true) !== 0x02014b50) throw new Error("zip 中央目录损坏");
      const method = dv.getUint16(cdPos + 10, true);
      const compSize = dv.getUint32(cdPos + 20, true);
      const nameLen = dv.getUint16(cdPos + 28, true), extraLen = dv.getUint16(cdPos + 30, true), commentLen = dv.getUint16(cdPos + 32, true);
      const localOffset = dv.getUint32(cdPos + 42, true);
      const name = new TextDecoder().decode(buf.subarray(cdPos + 46, cdPos + 46 + nameLen));
      if (dv.getUint32(localOffset, true) !== 0x04034b50) throw new Error("zip 本地头损坏: " + name);
      const lnameLen = dv.getUint16(localOffset + 26, true), lextraLen = dv.getUint16(localOffset + 28, true);
      const dataStart = localOffset + 30 + lnameLen + lextraLen;
      let data = buf.subarray(dataStart, dataStart + compSize);
      if (method === 8) {
        data = new Uint8Array(await new Response(new Blob([data]).stream().pipeThrough(new DecompressionStream("deflate-raw"))).arrayBuffer());
      } else if (method !== 0) {
        throw new Error("不支持的 zip 压缩方式: " + method + " (" + name + ")");
      }
      out[name] = data;
      cdPos += 46 + nameLen + extraLen + commentLen;
    }
    return out;
  }

  BD.core = { $, set, toast, download, zipWrite, zipRead };
})();
