#!/usr/bin/env python3
"""版本号一致性校验: 两棵树的 mod_version 必须相同, 且文档里不能残留旧版本字面量。

解决两个真实发生过的问题:

1. **割裂发版**(静默) —— 只把一棵树升版时, build-release.yml 的两个 Publish step
   各自用自己那棵树的版本号拼 jar 路径, 都能命中、都会绿, 于是发出
   「1.21.5=1.0.2 / 1.21.1=1.0.1」而两个 Release 都叫 Bocchi Client 的割裂组合。
   fail_on_unmatched_files 抓不到, 因为路径对每一步都是自洽的。

2. **README 漂移**(已发生两次) —— 升版提交 305b658 只改了两棵 gradle.properties,
   README 里的产物示例没跟上, 直到 33 天后由 ba93c8a 作为无关文档提交补上;
   再往前 b46ca80 也补过一次。文档没有任何 workflow 覆盖, 所以两次都是人发现的。

用法:
    python tools/check-version.py

退出码:
    0  一致
    1  发现版本割裂或文档残留旧版本
    2  用法/读取错误

跨平台: Windows / Linux / macOS, 仅需 Python 3(无第三方依赖)。
"""

import os
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="replace")
    sys.stderr.reconfigure(errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TREES = ("bocchi-1.21.5", "bocchi-1.21.1")

# jar 名形如 bocchi-fabric-1.21.5-1.0.1.jar / bocchi-neoforge-1.21.5-1.0.1-all.jar
# 第 1 组是 MC 版本(允许各树不同), 第 2 组是 mod_version(必须与 gradle.properties 一致)
JAR_RE = re.compile(
    r"bocchi-(?:fabric|neoforge)-\d+\.\d+(?:\.\d+)?-(\d+\.\d+(?:\.\d+)?)(-all)?\.jar"
)

DOC_GLOBS = (
    ("README.md",),
    ("docs", "*.md"),
    ("tools", "bocchi-designer", "README.md"),
)


def read_mod_version(tree: str) -> str:
    path = os.path.join(ROOT, "src", tree, "gradle.properties")
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("mod_version="):
                return line.split("=", 1)[1].strip()
    raise SystemExit(f"错误: {path} 中未找到 mod_version")


def iter_docs():
    """产出要扫描的 md 文件。DOC_GLOBS 的最后一段允许是 glob(如 *.md)。"""
    import glob as _glob
    for parts in DOC_GLOBS:
        target = os.path.join(ROOT, *parts)
        if "*" in parts[-1]:
            for hit in sorted(_glob.glob(target)):
                if os.path.isfile(hit):
                    yield hit
        elif os.path.isfile(target):
            yield target
        elif os.path.isdir(target):
            for name in sorted(os.listdir(target)):
                if name.endswith(".md"):
                    yield os.path.join(target, name)


def main() -> None:
    versions = {tree: read_mod_version(tree) for tree in TREES}
    unique = set(versions.values())
    failed = False

    # --- 1. 两棵树必须同版本 ---
    detail = "  ".join(f"{t}={v}" for t, v in versions.items())
    if len(unique) > 1:
        failed = True
        print("版本割裂: 两棵树的 mod_version 不一致 —— 直接发版会让用户拿到"
              "两个 MC 版本号不同的组合, 而两个 Release 都叫同一个名字。", file=sys.stderr)
        print(f"  {detail}", file=sys.stderr)
        print("  修法: 把两棵树一起升到同一版本后重跑。", file=sys.stderr)
        # 已割裂时无法确定"正确版本", 下面仍扫文档, 但把两侧版本都视为合法,
        # 只报出与两者都不符的残留, 避免输出"当前应为 None"这种误导。
        valid = unique
        current_label = "/".join(sorted(unique))
    else:
        current = unique.pop()
        valid = {current}
        current_label = current
        print(f"两棵树 mod_version 一致: {current}  ({detail})")

    # --- 2. 文档里不能残留别的 mod_version ---
    stale = []
    for path in iter_docs():
        rel = os.path.relpath(path, ROOT)
        with open(path, encoding="utf-8") as f:
            for lineno, line in enumerate(f, 1):
                for m in JAR_RE.finditer(line):
                    found = m.group(1)
                    if found not in valid:
                        stale.append((rel, lineno, found, m.group(0)))

    if stale:
        failed = True
        print(f"\n文档中残留与 gradle.properties 不符的产物版本 (应为 {current_label}):",
              file=sys.stderr)
        for rel, lineno, found, text in stale:
            print(f"  {rel}:{lineno}  写着 {found}  ->  {text}", file=sys.stderr)
        print("\n  修法: 把这些位置的版本号改成当前 mod_version, "
              "或改用 <mod_version> 占位符避免下次再漂。", file=sys.stderr)
    else:
        print(f"文档产物示例与 mod_version 一致(已扫描 {sum(1 for _ in iter_docs())} 个 md)。")

    if failed:
        print("\n===== 版本一致性校验未通过 =====", file=sys.stderr)
        raise SystemExit(1)
    print("版本一致性 OK")


if __name__ == "__main__":
    main()
