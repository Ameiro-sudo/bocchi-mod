/* core.js 纯函数单测: crc32 / zip 往返 / 转义 / debounce */
import test from "node:test";
import assert from "node:assert/strict";
import { crc32, zipWrite, zipRead, debounce, escapeHtml, download } from "../js/core.js";

// Node 下补 rAF (rafThrottle 用), 16ms 定时器近似
if (typeof globalThis.requestAnimationFrame === "undefined") {
  globalThis.requestAnimationFrame = (cb) => setTimeout(() => cb(performance.now()), 16);
  globalThis.cancelAnimationFrame = (id) => clearTimeout(id);
}
const { rafThrottle } = await import("../js/core.js");

test("crc32 标准校验向量", () => {
  const enc = new TextEncoder();
  assert.equal(crc32(enc.encode("123456789")), 0xCBF43926);  // CRC-32 标准检查值
  assert.equal(crc32(new Uint8Array(0)), 0);
});

test("zip 写入->读取 往返一致", async () => {
  const enc = new TextEncoder();
  const files = [
    { name: "assets/minecraft/client/design.json", data: enc.encode("{\"a\":1}") },
    { name: "中文名.txt", data: new Uint8Array([0, 1, 2, 250, 251, 255]) },
    { name: "empty.bin", data: new Uint8Array(0) },
  ];
  const buf = zipWrite(files);
  const out = await zipRead(buf);
  assert.equal(out["assets/minecraft/client/design.json"].length, 7);
  assert.deepEqual([...out["中文名.txt"]], [0, 1, 2, 250, 251, 255]);
  assert.equal(out["empty.bin"].length, 0);
});

test("zip 读取: 非 zip 输入报错", async () => {
  await assert.rejects(() => zipRead(new TextEncoder().encode("not a zip at all........")),
    /不是有效的 zip 文件/);
});

test("escapeHtml 转义四类字符", () => {
  assert.equal(escapeHtml('<img src=x onerror="a&b">'),
    "&lt;img src=x onerror=&quot;a&amp;b&quot;&gt;");
});

test("debounce: 高频调用聚合为一次, flush 立即执行", async () => {
  let n = 0;
  const d = debounce(() => n++, 30);
  d(); d(); d();
  assert.equal(n, 0);                 // 未到时延不执行
  await new Promise(r => setTimeout(r, 60));
  assert.equal(n, 1);                 // 只执行一次
  d.flush();                          // 无 pending 时 flush 安全
  assert.equal(n, 1);
  d(); d.flush();
  assert.equal(n, 2);
});

test("download: 用 Blob URL 触发点击, 并在 30s 后撤销该 URL", async (t) => {
  // 导出 zip/JSON 的唯一落盘入口。浏览器里唯一的可观测面就三个:
  // createObjectURL 拿到的 url、<a> 的 download 名、以及是否点了。
  // 漏掉 revoke 会让每次导出都把整包留在内存里 (zip 可到几十 MB), 且没有任何症状。
  // 用 mock 时钟而不是真等 30s —— 整套单测要保持在秒级。
  const created = [];
  const revoked = [];
  const clicks = [];
  const prevCreate = globalThis.URL.createObjectURL;
  const prevRevoke = globalThis.URL.revokeObjectURL;
  const prevDoc = globalThis.document;
  globalThis.URL.createObjectURL = (b) => { const u = "blob:stub/" + created.length; created.push({ blob: b, url: u }); return u; };
  globalThis.URL.revokeObjectURL = (u) => revoked.push(u);
  globalThis.document = { createElement: () => ({ click() { clicks.push(this.download); } }) };
  t.mock.timers.enable({ apis: ["setTimeout"] });
  try {
    const blob = { __tag: "BLOB" };
    download(blob, "pack.zip");
    assert.equal(created.length, 1);
    assert.equal(created[0].blob, blob, "必须传原始 blob, 而不是拷贝");
    assert.deepEqual(clicks, ["pack.zip"], "必须以 <a download> 触发, 否则文件名与落盘都无从谈起");
    assert.deepEqual(revoked, [], "立刻撤销会让浏览器还没开始读就失效");
    t.mock.timers.tick(29999);
    assert.deepEqual(revoked, [], "撤销时机未到");
    t.mock.timers.tick(2);
    assert.deepEqual(revoked, [created[0].url], "到期后必须撤销, 否则 blob 永远驻留");
  } finally {
    t.mock.timers.reset();
    globalThis.URL.createObjectURL = prevCreate;
    globalThis.URL.revokeObjectURL = prevRevoke;
    if (prevDoc === undefined) delete globalThis.document; else globalThis.document = prevDoc;
  }
});

test("rafThrottle: 同帧多次调用合并为一帧一次", async () => {
  let n = 0;
  const r = rafThrottle(() => n++);
  r(); r(); r();
  assert.equal(n, 0);
  await new Promise(r => setTimeout(r, 50));
  assert.equal(n, 1);
});


