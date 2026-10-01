#!/usr/bin/env python3
"""双树源码同步校验: src/bocchi-1.21.5 与 src/bocchi-1.21.1 的共享区域必须保持一致。

设计取向是 **fail-closed**: 任何"本该相同却不相同"或"单侧多出文件"都会让检查失败,
除非该路径被显式登记进 tools/sync-allowlist.txt。这样新增例外必须留下文字理由,
而不是像以前那样靠提交者自觉 + 一次性的人工哈希比对。

为什么需要它: 历史上双树同步一直靠人工纪律维持(提交信息里写「— 双树」),
纪律执行得很好, 但漏改没有任何一道门禁能发现。common 层 113 个共有 Java 文件里
有 102 个字节完全相同, 这意味着"应该同步"的集合是可机器判定的, 却没有人在判定。

用法:
    python tools/check-sync.py                 # 校验(默认)
    python tools/check-sync.py --init          # 用当前实际差异生成 allowlist 初稿
    python tools/check-sync.py --quiet         # 只在失败时输出
    python tools/check-sync.py --allow-stale   # allowlist 里的失效条目降级为警告

退出码:
    0  通过
    1  存在未登记的差异, 或 allowlist 条目已失效
    2  用法错误 / allowlist 文件缺失

跨平台: Windows / Linux / macOS, 仅需 Python 3(无第三方依赖)。
"""

import argparse
import hashlib
import os
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="replace")
    sys.stderr.reconfigure(errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TREE_A = "bocchi-1.21.5"
TREE_B = "bocchi-1.21.1"
ALLOWLIST = os.path.join(ROOT, "tools", "sync-allowlist.txt")

# 需要保持同步的区域(相对各棵树根目录)。目录递归, 文件只比自身。
# 选择依据: 这些位置承载了两棵树共享的全部逻辑与资源; 加载器专属代码
# (mixin 注册、入口类) 本就按版本分化, 不在范围内。
SYNC_ROOTS = (
    "common/src/main/java",
    "common/src/main/resources",
    "common/src/test",
    "buildSrc/src",
    "build.gradle.kts",
    "settings.gradle.kts",
    "common/build.gradle.kts",
    "fabric/build.gradle.kts",
    "neoforge/build.gradle.kts",
)


def collect(tree: str, sub: str) -> dict:
    """收集某棵树下 sub 区域的相对路径 -> 内容摘要。相对路径统一用 posix 分隔符。"""
    base = os.path.join(ROOT, "src", tree, *sub.split("/"))
    out = {}
    if os.path.isfile(base):
        out[sub] = digest(base)
    elif os.path.isdir(base):
        for dirpath, _dirnames, filenames in os.walk(base):
            for name in filenames:
                full = os.path.join(dirpath, name)
                rel = os.path.relpath(full, os.path.join(ROOT, "src", tree))
                out[rel.replace(os.sep, "/")] = digest(full)
    return out


def digest(path: str) -> str:
    """内容摘要。比较前归一化行尾, 避免 core.autocrlf 造成整片假阳性 —
    行尾差异不是我们要抓的漂移, 换台机器 checkout 就不该报警。"""
    with open(path, "rb") as f:
        data = f.read()
    return hashlib.sha256(data.replace(b"\r\n", b"\n")).hexdigest()


def load_allowlist(path: str) -> dict:
    """读 allowlist, 返回 {相对路径: 理由}。# 开头或行尾 # 之后是注释。"""
    entries = {}
    if not os.path.isfile(path):
        return entries
    # 本仓库吃过三次编码亏(gradle.properties 描述乱码 / .gitattributes 注释退化成
    # '?' / build-all.py 打印 UnicodeEncodeError), 这里不静默也不抛裸 traceback。
    try:
        with open(path, encoding="utf-8") as f:
            lines = f.readlines()
    except UnicodeDecodeError as e:
        print(f"错误: {os.path.relpath(path, ROOT)} 不是合法 UTF-8 ({e})。\n"
              f"  请用 UTF-8 无 BOM 重新保存该文件。", file=sys.stderr)
        raise SystemExit(2)
    for raw in lines:
        line = raw.rstrip("\n")
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        body, sep, reason = line.partition("#")
        key = body.strip().replace("\\", "/")
        if key:
            entries[key] = reason.strip() if sep else ""
    return entries


def classify() -> dict:
    """对比两棵树, 返回 {分类: [相对路径...]}。"""
    a, b = {}, {}
    for sub in SYNC_ROOTS:
        a.update(collect(TREE_A, sub))
        b.update(collect(TREE_B, sub))

    same, diff, only_a, only_b = [], [], [], []
    for path in sorted(set(a) | set(b)):
        if path in a and path in b:
            (same if a[path] == b[path] else diff).append(path)
        elif path in a:
            only_a.append(path)
        else:
            only_b.append(path)
    return {"SAME": same, "DIFF": diff, "ONLY_" + TREE_A: only_a, "ONLY_" + TREE_B: only_b}


def write_allowlist(path: str, buckets: dict) -> None:
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# 双树同步例外清单 (由 tools/check-sync.py 消费)\n")
        f.write("#\n")
        f.write("# 每行一个相对 src/<tree>/ 的路径, 行尾 # 写登记理由。\n")
        f.write("# 新增例外前先问: 这个差异是 MC 版本适配的必要产物, 还是漏改?\n")
        f.write("# 维护: python tools/check-sync.py --init 重新生成(会覆盖本文件)\n")
        for label, title in (
            ("DIFF", "两棵树同名但内容不同 —— 跨 MC API 的适配"),
            ("ONLY_" + TREE_A, "仅 1.21.5 存在 —— 新特性未回移"),
            ("ONLY_" + TREE_B, "仅 1.21.1 存在"),
        ):
            paths = buckets[label]
            f.write(f"\n# --- {title} ({len(paths)}) ---\n")
            for p in paths:
                f.write(f"{p}  # TODO 补登记理由\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="双树源码同步校验 (fail-closed)")
    parser.add_argument("--init", action="store_true",
                        help="用当前实际差异生成 allowlist 初稿并退出")
    parser.add_argument("--quiet", action="store_true", help="只在失败时输出明细")
    parser.add_argument("--allow-stale", action="store_true",
                        help="allowlist 中已不再适用的条目降级为警告而非失败")
    args = parser.parse_args()

    buckets = classify()

    if args.init:
        write_allowlist(ALLOWLIST, buckets)
        print(f"已生成 {os.path.relpath(ALLOWLIST, ROOT)} 初稿:")
        for label in ("DIFF", "ONLY_" + TREE_A, "ONLY_" + TREE_B):
            print(f"  {label}: {len(buckets[label])} 条")
        print("\n请逐条补上 # 理由后再提交 —— 空理由等于没登记。")
        return

    allow = load_allowlist(ALLOWLIST)

    # 未登记的差异 = 需要解释, 但还没解释
    unlisted = {label: [p for p in buckets[label] if p not in allow]
                for label in ("DIFF", "ONLY_" + TREE_A, "ONLY_" + TREE_B)}

    # 已登记但不再需要 = 例外已过期(该同步回去了/特性回移了/文件删了)
    still_applicable = set(buckets["DIFF"]) | set(buckets["ONLY_" + TREE_A]) | set(buckets["ONLY_" + TREE_B])
    stale = sorted(p for p in allow if p not in still_applicable)

    if not any(unlisted.values()) and not stale:
        if not args.quiet:
            n = len(buckets["SAME"])
            print(f"双树同步 OK: {n} 个文件一致, "
                  f"{len(allow)} 条已登记例外, 无新增未登记差异。")
        return

    failed = False
    for label, paths in unlisted.items():
        if not paths:
            continue
        failed = True
        print(f"\n未登记的 {label} ({len(paths)} 条) —— 这些差异没有在 "
              f"{os.path.relpath(ALLOWLIST, ROOT)} 里说明理由:", file=sys.stderr)
        for p in paths:
            print(f"  + {p}", file=sys.stderr)
        print("\n  若为跨 MC 版本的必要适配, 请登记进 allowlist 并写明理由;",
              file=sys.stderr)
        print("  若只是漏改, 请把另一棵树也同步过来。", file=sys.stderr)

    if stale:
        msg = (f"\nallowlist 中有 {len(stale)} 条已不再适用(文件已同步/回移/删除):",
               *stale)
        if args.allow_stale:
            print(msg[0], file=sys.stderr)
            for p in msg[1:]:
                print(f"  - {p}", file=sys.stderr)
            print("  (--allow-stale: 本次不判失败)", file=sys.stderr)
        else:
            failed = True
            print(msg[0], file=sys.stderr)
            for p in msg[1:]:
                print(f"  - {p}", file=sys.stderr)
            print("\n  请删除这些条目, 或确认后加 --allow-stale。", file=sys.stderr)

    if failed:
        print("\n===== 双树同步校验未通过 =====", file=sys.stderr)
        raise SystemExit(1)

    if not args.quiet:
        print("双树同步 OK (含过期条目警告)")


if __name__ == "__main__":
    main()
