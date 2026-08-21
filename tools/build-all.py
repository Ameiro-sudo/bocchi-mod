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

# 非 UTF-8 控制台 (如 LANG=C 的 Linux) 下打印中文不致 UnicodeEncodeError
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="replace")
    sys.stderr.reconfigure(errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_ROOT = os.path.join(ROOT, "release")
TREES = (("bocchi-1.21.5", "1.21.5"), ("bocchi-1.21.1", "1.21.1"))
GRADLEW = "gradlew.bat" if os.name == "nt" else "gradlew"


def build_tree(mc: str, label: str) -> bool:
    print(f"===== 构建 {label} ({mc}) =====")
    tree = os.path.join(ROOT, "src", mc)
    gradlew = os.path.join(tree, GRADLEW)
    if not os.path.isfile(gradlew):
        print(f"错误: 未找到 {gradlew}", file=sys.stderr)
        return False
    if os.name != "nt":
        try:
            os.chmod(gradlew, os.stat(gradlew).st_mode | 0o111)
        except OSError:
            pass

    try:
        subprocess.run(
            [gradlew, ":fabric:build", ":neoforge:build", "--no-daemon"],
            cwd=tree, check=True,
        )
    except subprocess.CalledProcessError as e:
        print(f"错误: {label} 构建失败 (退出码 {e.returncode})", file=sys.stderr)
        return False

    mod_version = read_mod_version(tree)
    ok = True
    # 清理该版本旧产物, 避免升版后残留旧 jar 被误分发
    shutil.rmtree(os.path.join(OUT_ROOT, label), ignore_errors=True)
    ok &= copy_jar(tree, label, "fabric", f"bocchi-fabric-{label}-{mod_version}.jar")
    ok &= copy_jar(tree, label, "neoforge", f"bocchi-neoforge-{label}-{mod_version}-all.jar")
    return ok


def read_mod_version(tree: str) -> str:
    path = os.path.join(tree, "gradle.properties")
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("mod_version="):
                return line.strip().split("=", 1)[1].strip()
    print(f"错误: {path} 中未找到 mod_version", file=sys.stderr)
    raise SystemExit(1)


def copy_jar(tree: str, label: str, loader: str, pattern: str) -> bool:
    libs = os.path.join(tree, loader, "build", "libs")
    matches = [p for p in os.listdir(libs) if p == pattern] if os.path.isdir(libs) else []
    if not matches:
        print(f"WARN: 未找到产物 {pattern}", file=sys.stderr)
        return False
    dst = os.path.join(OUT_ROOT, label, loader, "vanilla")
    os.makedirs(dst, exist_ok=True)
    shutil.copy2(os.path.join(libs, matches[0]), dst)
    print(f"  -> {matches[0]} 已复制到 release/{label}/{loader}/vanilla/")
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description="一键构建全部变体 (Fabric / NeoForge)")
    parser.add_argument("--only-1215", action="store_true", help="仅构建 1.21.5")
    parser.add_argument("--only-1211", action="store_true", help="仅构建 1.21.1")
    args = parser.parse_args()

    if args.only_1215 and args.only_1211:
        parser.error("--only-1215 与 --only-1211 不可同时指定")

    if args.only_1211:
        trees = [t for t in TREES if t[0].endswith("1.21.1")]
    elif args.only_1215:
        trees = [t for t in TREES if t[0].endswith("1.21.5")]
    else:
        trees = list(TREES)

    all_ok = True
    for mc, label in trees:
        all_ok &= build_tree(mc, label)

    if all_ok:
        print("===== 全部完成 =====")
    else:
        print("===== 完成但存在错误, 请检查上方输出 =====", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
