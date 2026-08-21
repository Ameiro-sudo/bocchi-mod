/* history.js 纯逻辑单测: 入栈/合并/静音手势/守卫/上限 */
import test from "node:test";
import assert from "node:assert/strict";
import {
  push, undo, redo, clearHistory, beginGesture, endGesture,
  stats, canUndo, canRedo, MAX_HISTORY, COALESCE_MS, _setNow,
} from "../js/history.js";

function makeClock(start = 0) {
  let now = start;
  _setNow(() => now);
  return { advance: (ms) => { now += ms; } };
}

test("push/undo/redo 基本 LIFO 与 label 回传", () => {
  makeClock();
  clearHistory();
  const log = [];
  push({ label: "a", undo: () => log.push("ua"), redo: () => log.push("ra") });
  push({ label: "b", undo: () => log.push("ub"), redo: () => log.push("rb") });
  assert.deepEqual(stats(), { undo: 2, redo: 0 });
  assert.equal(undo(), "b");
  assert.equal(undo(), "a");
  assert.equal(undo(), null);
  assert.deepEqual(log, ["ub", "ua"]);
  assert.equal(redo(), "a");
  assert.equal(redo(), "b");
  assert.equal(redo(), null);
  assert.deepEqual(log, ["ub", "ua", "ra", "rb"]);
});

test("undo 后新 push 分叉历史 (清空 redo)", () => {
  makeClock();
  clearHistory();
  push({ label: "a", undo: () => {}, redo: () => {} });
  undo();
  assert.ok(canRedo());
  push({ label: "b", undo: () => {}, redo: () => {} });
  assert.ok(!canRedo());
  assert.equal(redo(), null);
});

test("同键时间窗合并: 保留首条 undo + 最新 redo", () => {
  const c = makeClock();
  clearHistory();
  const seq = [];
  push({ label: "ov", undo: () => seq.push("u1"), redo: () => seq.push("r1") }, "ov:x");
  c.advance(Math.floor(COALESCE_MS / 2));
  push({ label: "ov", undo: () => seq.push("u2"), redo: () => seq.push("r2") }, "ov:x");
  c.advance(Math.floor(COALESCE_MS / 2));
  push({ label: "ov", undo: () => seq.push("u3"), redo: () => seq.push("r3") }, "ov:x");
  assert.deepEqual(stats(), { undo: 1, redo: 0 });
  undo();
  redo();
  assert.deepEqual(seq, ["u1", "r3"]);   // 首条 undo + 最新 redo
});

test("不同键不合并; 同键超窗不合并; 无键 push 永不合并", () => {
  const c = makeClock(1000);
  clearHistory();
  push({ label: "x", undo: () => {}, redo: () => {} }, "ov:x");
  c.advance(100);
  push({ label: "y", undo: () => {}, redo: () => {} }, "ov:y");   // 异键同窗 -> 不合并
  push({ label: "p", undo: () => {}, redo: () => {} });           // 无键 -> 不合并
  c.advance(COALESCE_MS + 1);
  push({ label: "z", undo: () => {}, redo: () => {} }, "ov:y");   // 同键超窗 -> 不合并
  assert.equal(stats().undo, 4);
});

test("手势静音区: 区间内 push 被吞掉, 结束后所有者合成的命令正常入栈", () => {
  makeClock();
  clearHistory();
  const seq = [];
  beginGesture();
  push({ label: "x", undo: () => seq.push("ux"), redo: () => {} });
  push({ label: "y", undo: () => seq.push("uy"), redo: () => {} });
  assert.deepEqual(stats(), { undo: 0, redo: 0 });   // 静音区吞掉
  endGesture();
  // 所有者用起止快照合成完整命令 (模拟 ov.pushOVGesture)
  const before = { x: 0, y: 0 }, after = { x: 30, y: -12 };
  push({
    label: "拖拽 立绘",
    undo: () => seq.push("restore:" + JSON.stringify(before)),
    redo: () => seq.push("restore:" + JSON.stringify(after)),
  });
  assert.equal(undo(), "拖拽 立绘");
  assert.equal(redo(), "拖拽 立绘");
  assert.deepEqual(seq, [
    "restore:{\"x\":0,\"y\":0}",
    "restore:{\"x\":30,\"y\":-12}",
  ]);
});

test("未闭合的静音区会吞掉后续普通 push, clearHistory 解除", () => {
  makeClock();
  clearHistory();
  beginGesture();
  push({ label: "swallowed", undo: () => {}, redo: () => {} });
  clearHistory();                        // 清空同时解除静音 (防状态泄漏)
  push({ label: "ok", undo: () => {}, redo: () => {} });
  assert.equal(stats().undo, 1);
});

test("applying 守卫: undo 回放期间的 push 被丢弃", () => {
  makeClock();
  clearHistory();
  let once = false;
  push({
    label: "a",
    undo: () => {
      if (!once) {
        once = true;
        push({ label: "evil", undo: () => {}, redo: () => {} });
      }
    },
    redo: () => {},
  });
  assert.equal(stats().undo, 1);
  undo();
  assert.deepEqual(stats(), { undo: 0, redo: 1 });
});

test("上限 MAX_HISTORY: 溢出丢最旧", () => {
  makeClock();
  clearHistory();
  for (let i = 0; i < MAX_HISTORY + 5; i++) {
    push({ label: "c" + i, undo: () => {}, redo: () => {} });
  }
  assert.equal(stats().undo, MAX_HISTORY);
  assert.ok(canUndo() && stats().redo === 0);
  assert.equal(undo(), "c" + (MAX_HISTORY + 4));   // 最新仍在
});