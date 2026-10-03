/* ============================================================================
 * store.js - 可撤销字段 (field) 抽象
 *
 * 面板里每一行 (配色/文本/资源/主题/预览色) 的编辑都是同一个形状:
 *   1. 读当前值            (read)
 *   2. 调落地函数写界面+模型 (apply)
 *   3. 压一条撤销条目        (undo 回 from, redo 回 to)
 *   4. 维护一个「最近入栈值」基准 (committed), 供同键连续拖动合并成一步
 *   5. 导入模型后把基准重对齐(resync)
 *
 * 这五件事此前在 color/text/res/theme/preview-color 五个文件里各手写一遍, 于是:
 *   - 回放 (undo/redo) 时忘记搬动基准 → 下一次编辑生成「从被撤销掉的值出发」的
 *     伪造条目, 撤销次数凭空多一次;
 *   - 合并键各不相同 ("color:"+key / "pcolor:"+key / 无), 基准丢失的时机也随之不同。
 *
 * createField 把这套机制收在一处, 每个字段只声明它自己的 read/apply/label。
 * ==========================================================================*/
import { push as pushHistory } from "./history.js";

/**
 * 建一个可撤销字段。
 * @param {object}   o
 * @param {string|Function} o.label  撤销栈里显示的名字 (如 "配色 primary")。
 *                                 传函数则在每次入栈时才求值 —— 同一行有多种编辑
 *                                 动作 (资源的「改路径 / 换文件 / 还原内置」) 共用
 *                                 一个 field 时, 用它区分各自的措辞。
 * @param {Function} o.read       读当前值 () => any
 * @param {Function} o.apply      落地函数 (value) => void, 写界面 + 模型 + 落盘
 * @param {string}  [o.coalesce]  同键合并的合并键; 省略则该字段每次编辑独立成一条
 *                                 (例如: 取色器连拖要合并, 资源上传不合并)
 * @param {Function}[o.isSame]    值相等判定, 默认 ===。资源行的值是 [blob, path]
 *                                 二元组, 必须显式给判定, 否则引用不等会被当成
 *                                 「改过了」而白白入栈。
 * @returns {{
 *   set: (value:any) => boolean,        // 读→落地→入栈, 值未变返回 false
 *   flush: () => boolean,               // 不落地, 仅在 read()!==committed 时补一条 (拖动松手兜底)
 *   resync: () => void,                 // 导入模型后把基准对齐到当前值
 *   isModified: () => boolean           // 当前值是否与最近入栈值不同 (供 UI 标记)
 * }}
 */
export function createField({ label, read, apply, coalesce, isSame }) {
  let committed = read();
  const same = isSame || ((a, b) => a === b);
  // 刻意每次入栈才求值: label 传函数时, 同一 field 的多次编辑可以各自带自己的措辞,
  // 而 undo/redo 闭包里用的仍是入栈那一刻的 label。
  const nameOf = () => (typeof label === "function" ? label() : label);

  function set(value) {
    const from = committed;
    if (same(from, value)) return false;
    const name = nameOf();
    apply(value);
    pushHistory({
      label: name,
      undo: () => { apply(from); committed = from; },
      redo: () => { apply(value); committed = value; },
    }, coalesce);
    committed = value;
    return true;
  }

  // 取色器 / 滑杆拖动结束时的兜底: 若界面值已偏离基准却还没入栈 (例如拖动中途
  // 合并窗吃掉了最后一步), 补一条把当前值封口。
  function flush() {
    const to = read();
    const from = committed;
    if (same(from, to)) return false;
    const name = nameOf();
    pushHistory({
      label: name,
      undo: () => { apply(from); committed = from; },
      redo: () => { apply(to); committed = to; },
    }, coalesce);
    committed = to;
    return true;
  }

  return {
    set,
    flush,
    resync() { committed = read(); },
    isModified() { return !same(read(), committed); },
  };
}