#!/usr/bin/env python3
"""一键构建全部变体: 版本 (1.21.1 / 1.21.5) x 加载器 (Fabric / NeoForge)。

跨平台: Windows / Linux / macOS，仅需 Python 3（无第三方依赖）。
构建产物收集到 release/<mc>/<loader>/vanilla/ 目录。

用法:
    python tools/build-all.py             构建全部
    python tools/build-all.py --only-1215 仅构建 1.21.5
    python tools/build-all.py --only-1211 仅构建 1.21.1
"""

import argparse
import os
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_ROOT = os.path.join(ROOT, "release")
TREES = (("bocchi-1.21.5", "1.21.5"), ("bocchi-1.21.1", "1.21.1"))
GRADLEW = "gradlew.bat" if os.name == "nt" else "gradlew"


def build_tree(mc: str, label: str) -> None:
    print(f"===== 构建 {label} ({mc}) =====")
    tree = os.path.join(ROOT, "src", mc)
    gradlew = os.path.join(tree, GRADLEW)
    if not os.path.isfile(gradlew):
        sys.exit(f"错误: 未找到 {gradlew}")
    if os.name != "nt":
        try:
            os.chmod(gradlew, os.stat(gradlew).st_mode | 0o111)
        except OSError:
            pass

    subprocess.run(
        [gradlew, ":fabric:build", ":neoforge:build", "--no-daemon"],
        cwd=tree, check=True,
    )

    mod_version = read_mod_version(tree)
    copy_jar(tree, label, "fabric", f"bocchi-fabric-{label}-{mod_version}.jar")
    copy_jar(tree, label, "neoforge", f"bocchi-neoforge-{label}-{mod_version}-all.jar")


def read_mod_version(tree: str) -> str:
    path = os.path.join(tree, "gradle.properties")
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("mod_version="):
                return line.strip().split("=", 1)[1].strip()
    sys.exit(f"错误: {path} 中未找到 mod_version")


def copy_jar(tree: str, label: str, loader: str, pattern: str) -> None:
    libs = os.path.join(tree, loader, "build", "libs")
    matches = [p for p in os.listdir(libs) if p == pattern] if os.path.isdir(libs) else []
    if not matches:
        print(f"WARN: 未找到产物 {pattern}", file=sys.stderr)
        return
    dst = os.path.join(OUT_ROOT, label, loader, "vanilla")
    os.makedirs(dst, exist_ok=True)
    shutil.copy2(os.path.join(libs, matches[0]), dst)
    print(f"  -> {matches[0]} 已复制到 release/{label}/{loader}/vanilla/")


def main() -> None:
    parser = argparse.ArgumentParser(description="一键构建全部变体 (Fabric / NeoForge)")
    parser.add_argument("--only-1215", action="store_true", help="仅构建 1.21.5")
    parser.add_argument("--only-1211", action="store_true", help="仅构建 1.21.1")
    args = parser.parse_args()

    if args.only_1211:
        trees = [t for t in TREES if t[0].endswith("1.21.1")]
    elif args.only_1215:
        trees = [t for t in TREES if t[0].endswith("1.21.5")]
    else:
        trees = list(TREES)

    for mc, label in trees:
        build_tree(mc, label)
    print("===== 全部完成 =====")


if __name__ == "__main__":
    main()