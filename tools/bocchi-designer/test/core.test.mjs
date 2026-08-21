/* core.js 纯函数单测: crc32 / zip 往返 / 转义 / debounce */
import test from "node:test";
import assert from "node:assert/strict";
import { crc32, zipWrite, zipRead, debounce, escapeHtml } from "../js/core.js";

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

test("rafThrottle: 同帧多次调用合并为一帧一次", async () => {
  let n = 0;
  const r = rafThrottle(() => n++);
  r(); r(); r();
  assert.equal(n, 0);
  await new Promise(r => setTimeout(r, 50));
  assert.equal(n, 1);
});


