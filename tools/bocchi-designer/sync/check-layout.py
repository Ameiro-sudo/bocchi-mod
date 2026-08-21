#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""check-layout.py — 布局常量漂移检测器

对比 web 工具 (tools/bocchi-designer/js/facts.js) 与游戏端 Java 布局代码:
每个 fact 的 expr 用 W=1280/H=720 求值, 再在 Java 源文件中寻找同值的表达式;
Java 改动了数值而 web 未同步 → 报 DRIFT。

用法:
  python3 check-layout.py [--json] [--tree <1.21.5|1.21.1>]

退出码: 0 = 全部通过; 1 = 存在 DRIFT。
"""
import os
import re
import sys
import argparse

ROOT = os.path.dirname(os.path.abspath(__file__))
FACTS_JS = os.path.join(ROOT, "..", "js", "facts.js")
SRC_DIR = os.path.join(ROOT, "..", "..", "..", "src")
JAVA_BASE = ("bocchi-1.21.5", "bocchi-1.21.1")
JAVA_SUB = os.path.join("common", "src", "main", "java", "me", "baier", "client")

# ---------------------------------------------------------------- 表达式求值
TOKEN_RE = re.compile(r"""
    \s*(?:
        (\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?   # 数字
      | ([A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)*)(\()  # 函数调用 foo.bar(
      | ([A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)*)(?!\() # 裸标识符
      | ([+\-*/(),])
    )""", re.VERBOSE)


class ExprError(Exception):
    pass


class Parser:
    def __init__(self, src, elide=False):
        self.elide = elide
        self.toks = []
        pos = 0
        while pos < len(src):
            m = TOKEN_RE.match(src, pos)
            if not m:
                raise ExprError("无法解析: %s (位置 %d)" % (src, pos))
            num, call_name, bare_name, op = m.group(1), m.group(2), m.group(4), m.group(5)
            if num is not None:
                self.toks.append(("num", float(num)))
            elif call_name is not None:
                self.toks.append(("call", call_name))
            elif bare_name is not None:
                self.toks.append(("id", bare_name))
            else:
                self.toks.append((op, op))
            pos = m.end()
        self.i = 0

    def peek(self):
        return self.toks[self.i] if self.i < len(self.toks) else None

    def next(self):
        t = self.peek()
        self.i += 1
        return t

    def expect(self, t):
        x = self.next()
        if not x or x[0] != t:
            raise ExprError("期望 %s 得到 %s" % (t, x))
        return x

    def expr(self, bind):
        v = self.term(bind)
        while True:
            t = self.peek()
            if t and t[0] in ("+", "-"):
                self.next()
                r = self.term(bind)
                v = v + r if t[0] == "+" else v - r
            else:
                return v

    def term(self, bind):
        v = self.factor(bind)
        while True:
            t = self.peek()
            if t and t[0] in ("*", "/"):
                self.next()
                r = self.factor(bind)
                v = v * r if t[0] == "*" else v / r
            else:
                return v

    def factor(self, bind):
        t = self.next()
        if t is None:
            raise ExprError("表达式意外结束")
        if t[0] == "num":
            return t[1]
        if t[0] == "(":
            v = self.expr(bind)
            self.expect(")")
            return v
        if t[0] in ("+", "-"):
            v = self.factor(bind)
            return v if t[0] == "+" else -v
        if t[0] == "call":
            name = t[1]
            if name in ("min", "max"):
                vals = [self.expr(bind)]
                while self.peek() and self.peek()[0] == ",":
                    self.next()
                    vals.append(self.expr(bind))
                self.expect(")")
                return min(vals) if name == "min" else max(vals)
            if name in bind:
                # 空调用 blockPos1.getX(): 吞掉紧随的右括号
                if self.peek() and self.peek()[0] == ")":
                    self.next()
                return bind[name]
            if self.elide:  # 未知调用按 1 处理 (跳过参数)
                d = 1
                while self.peek():
                    t2 = self.peek()
                    self.next()
                    if t2[0] == "(":
                        d += 1
                    elif t2[0] == ")":
                        d -= 1
                        if d == 0:
                            break
                return 1
            raise ExprError("未解析调用: %s" % name)
        if t[0] == "id":
            if t[1] in bind:
                return bind[t[1]]
            if self.elide:
                return 1
            # 变量调用: name(...) — 解析但结果必须可绑定
            if self.peek() and self.peek()[0] == "(":
                self.next()
                v = self.expr(bind)
                self.expect(")")
                return v
            raise ExprError("未绑定变量: %s" % t[1])
        raise ExprError("意外的符号 %s" % t[1])


def eval_expr(src, bind, elide=False):
    return Parser(src, elide=elide).expr(bind)


# ---------------------------------------------------------------- Java 文本清洗
# 关键: Java 端逻辑画布是 FrameContext.java 里的 scaledWidth=480 / scaledHeight=270,
# 检查器用同一组 (480, 270) 作为 JS 与 Java 两边的统一求值参考 (比例一致即可比)。
W, H = 480.0, 270.0


def strip_java_comments(text):
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    lines = []
    for ln in text.splitlines():
        ln = re.sub(r"//.*$", "", ln)
        lines.append(ln)
    return "\n".join(lines)


def java_substitutions(text):
    text = re.sub(r"\((?:float|double|int|long)\)", " ", text)   # 类型强转
    text = re.sub(r"\b0[xX][0-9a-fA-F]+\b", "0", text)            # 十六进制颜色 → 0 (与布局无关)
    text = text.replace("Math.min", "min")
    text = text.replace("Math.max", "max")
    text = text.replace("Mth.lerp", "lerp")      # lerp 终态常量在 args 里; 剥成不可解析调用, 参数单独切分
    text = text.replace("Mth.clamp", "clamp")
    text = text.replace("getScaledWidth()", "SCALED_WIDTH")
    text = text.replace("getScaledHeight()", "SCALED_HEIGHT")
    # 帧上下文 getter → 加 frame 前缀, 避免与组件内同名局部变量冲突
    text = text.replace("getBlock1Pos()", "frameBlock1Pos")
    text = text.replace("getBlock3Pos()", "frameBlock3Pos")
    text = text.replace("getBlock1Size()", "frameBlock1Size")
    text = text.replace("getBlock3Size()", "frameBlock3Size")
    text = text.replace("getRect1Width()", "frameRect1Width")
    text = text.replace("getRect1PosX()", "frameRect1PosX")
    text = text.replace("getBgFontSize()", "frameBgFontSize")
    text = text.replace("frame.", "")
    text = re.sub(r"\bscaledWidth\b", "SCALED_WIDTH", text)
    text = re.sub(r"\bscaledHeight\b", "SCALED_HEIGHT", text)
    text = re.sub(r"\bscreenWidth\b", "SCALED_WIDTH", text)
    text = re.sub(r"\bscreenHeight\b", "SCALED_HEIGHT", text)
    text = re.sub(r"(\d+)\.(?:f|F|d|D)\b", r"\1", text)   # "5.f" → "5"
    text = re.sub(r"(\d+)\.(\d+)[fFdD]?\b", r"\1.\2", text)  # "0.2f" → "0.2"
    text = re.sub(r"(\d+)[fFdD]\b", r"\1", text)             # "60f" → "60"
    return text


KEYWORDS = ("var ", "final ", "static ", "private ", "public ", "protected ",
            "float ", "int ", "double ", "long ", "boolean ", "byte ", "short ", "char ", "void ")


def strip_keywords(s):
    """剥掉行首的类型/修饰符关键字, 让 name = expr 绑定得以匹配"""
    for _ in range(8):
        changed = False
        for kw in KEYWORDS:
            if s.startswith(kw):
                s = s[len(kw):].lstrip()
                changed = True
        if not changed:
            return s
    return s


def split_chunks(line):
    """把一行切成可求值的表达式片段 (按 ; {} [] <> ! & | ? : 和字符串边界)"""
    line = line.strip()
    line = re.sub(r'"[^"]*"', '""', line)
    parts = re.split(r"[;{}\[\]]|&&|\|\||[<>!?:]", line)
    return [p.strip() for p in parts if p.strip() and re.search(r"\d", p)]


def strip_wrappers(chunk):
    """反复剥掉 Ident(...) / new Ident(...) 包裹与最外层括号, 直到出现顶层逗号可切分"""
    for _ in range(8):
        s = chunk.strip()
        if not s:
            return s
        s2 = s[4:].lstrip() if s.startswith("new ") else s
        m = re.match(r"^([\w$.]+)\s*\(", s2)
        if m and _balanced(s):
            end = _matching_paren(s2, m.end() - 1)
            if end == len(s2) - 1:
                chunk = s2[m.end():end]
                continue
        if s.startswith("(") and _balanced(s):
            end = _matching_paren(s, 0)
            if end == len(s) - 1:
                chunk = s[1:end]
                continue
        break
    return chunk.strip()


def _balanced(s):
    d = 0
    for c in s:
        if c == "(":
            d += 1
        elif c == ")":
            d -= 1
            if d < 0:
                return False
    return d == 0


def _matching_paren(s, i):
    d = 0
    for j in range(i, len(s)):
        if s[j] == "(":
            d += 1
        elif s[j] == ")":
            d -= 1
            if d == 0:
                return j
    return -1


def top_level_splits(chunk):
    chunk = strip_wrappers(chunk)
    depth = 0
    segs, cur = [], []
    for ch in chunk:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            segs.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
    segs.append("".join(cur))
    return [s.strip() for s in segs if s.strip()]


class LazyBind(dict):
    """dict, 但字符串值按需惰性求值 (支持 变量=变量 链), 深度限制防环"""

    def __init__(self, depth=0, **kw):
        super().__init__(**kw)
        self._depth = depth

    def __getitem__(self, k):
        v = dict.__getitem__(self, k)
        if isinstance(v, str):
            if self._depth > 6:
                raise ExprError("绑定链过深: " + k)
            return eval_expr(v, LazyBind(self._depth + 1, **self))
        return v


def additive_terms(seg):
    """按顶层 +/- 切成加数 (去掉一元符号), 供"部分求值"兜底"""
    terms, cur, depth, last = [], [], 0, 0
    for i, ch in enumerate(seg):
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        elif depth == 0 and ch in "+-":
            terms.append(seg[last:i])
            last = i + 1
    terms.append(seg[last:])
    return [t.strip() for t in terms if t.strip()]


def evaluate_file(text, prelude=None):
    """扫描 Java 文本, 返回 (数值集合, 字面量集合, 未解析片段列表, 绑定表)"""
    text = strip_java_comments(text)
    text = java_substitutions(text)
    bind = {"SCALED_WIDTH": W, "SCALED_HEIGHT": H}
    if prelude:
        bind.update(prelude)

    found = {}
    literals = {}
    found_loose = {}
    unverified = []

    lines = text.splitlines()
    for _ in range(3):
        for ln, line in enumerate(lines, 1):
            for chunk in split_chunks(line):
                chunk = strip_keywords(chunk)
                m = re.match(r"^([\w$]+)\s*=\s*(.+)$", chunk)
                if m:
                    name, rhs = m.group(1), m.group(2)
                    # 自引用 (progress = clamp(progress, ...)) 不建立绑定:
                    # 惰性求值会把自身拉成无限链, 保持未绑定走 elide 兜底即可
                    self_ref = re.search(r"\b" + re.escape(name) + r"\b", rhs)
                    if name not in bind and not self_ref:
                        mp = re.match(r"^(?:new\s+)?Point\s*\(\s*(.+?)\s*,\s*(.+?)\s*\)\s*$", rhs)
                        if mp:
                            bind[name + ".getX"] = mp.group(1).strip()
                            bind[name + ".getY"] = mp.group(2).strip()
                        else:
                            try:
                                bind[name] = eval_expr(rhs, LazyBind(0, **bind))
                            except ExprError:
                                if _balanced(rhs):
                                    bind[name] = rhs  # 惰性: 变量 = 变量
                                    if re.fullmatch(r"[\w$.]+", rhs):
                                        # 别名: 同步 .getX/.getY 链条 (blockPos1 -> block1Pos)
                                        bind.setdefault(name + ".getX", rhs + ".getX")
                                        bind.setdefault(name + ".getY", rhs + ".getY")
                    chunk = rhs  # rhs 同时继续参与数值扫描
                for seg in top_level_splits(chunk):
                    try:
                        v = eval_expr(seg, LazyBind(0, **bind))
                        found.setdefault(round(v, 6), []).append(ln)
                        continue
                    except ExprError:
                        pass
                    # 兜底 1: 加数分别求值 (含未知标识符按 1 估)
                    partial = False
                    for term in additive_terms(seg):
                        try:
                            v = eval_expr(term, LazyBind(0, **bind))
                            found.setdefault(round(v, 6), []).append(ln)
                            partial = True
                        except ExprError:
                            pass
                        try:
                            v = eval_expr(term, LazyBind(0, **bind), elide=True)
                            found_loose.setdefault(round(v, 6), []).append(ln)
                        except ExprError:
                            pass
                    # 兜底 2: 整段按未知标识符 1 估
                    if not partial:
                        try:
                            v = eval_expr(seg, LazyBind(0, **bind), elide=True)
                            found_loose.setdefault(round(v, 6), []).append(ln)
                        except ExprError:
                            pass
                    for n in re.findall(r"\d+\.?\d*", seg):
                        literals.setdefault(float(n), []).append(ln)
                    unverified.append((ln, seg[:60]))
    return found, literals, found_loose, unverified, bind


# ---------------------------------------------------------------- facts 解析
GROUP_RE = re.compile(r"^    (\w+): \{", re.M)
FACT_RE = re.compile(r'^      (\w+):\s*\{\s*expr:\s*"([^"]+)"\s*,\s*java:\s*"([^"]+)"', re.M)


def load_facts():
    src = open(FACTS_JS, encoding="utf-8").read()
    groups = {}
    # 按位置归组: fact 属于它前面的最后一个 group 头
    events = []
    for m in re.finditer(r"^    (\w+): \{", src, re.M):
        events.append((m.start(), "group", m.group(1)))
    for m in re.finditer(r'^      (\w+):\s*\{\s*expr:\s*"([^"]+)"\s*,\s*java:\s*"([^"]+)"', src, re.M):
        events.append((m.start(), "fact", m.group(1), m.group(2), m.group(3)))
    events.sort(key=lambda e: e[0])
    cur = None
    for e in events:
        if e[1] == "group":
            cur = e[2]
            groups.setdefault(cur, {})
        elif cur:
            groups[cur][e[2]] = {"expr": e[3], "java": e[4]}
    return groups


def resolve_all(groups):
    """计算所有 fact 的值 (引用同组 fact 名做绑定)"""
    vals = {}
    for group in groups:
        for name in groups[group]:
            resolve_one(groups, vals, group, name, set())
    return vals


def resolve_one(groups, vals, group, name, seen):
    key = group + "." + name
    if key in vals:
        return vals[key]
    if key in seen:
        raise ExprError("循环引用: " + key)
    seen = seen | {key}
    expr = groups[group][name]["expr"]
    bind = {"scaledWidth": W, "scaledHeight": H, "W": W, "H": H, "width": W, "height": H}
    for ref in re.findall(r"\b([A-Za-z_][A-Za-z0-9_]*)\b", expr):
        if ref in ("min", "max", "scaledWidth", "scaledHeight", "width", "height", "W", "H"):
            continue
        if ref in groups[group]:
            bind[ref] = resolve_one(groups, vals, group, ref, seen)
        elif ref in groups["poulsen"]:
            bind[ref] = resolve_one(groups, vals, "poulsen", ref, seen)
        else:
            raise ExprError("未知引用 %s (fact %s)" % (ref, key))
    v = eval_expr(expr, bind)
    vals[key] = v
    return v


# ---------------------------------------------------------------- 主流程
def main():
    # Windows GBK 控制台无法打印 ✓/中文: 统一按 UTF-8 输出, 异常字符以 ? 代替而非崩溃
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", action="store_true", help="JSON 输出 (便于 CI)")
    ap.add_argument("--tree", default="bocchi-1.21.5", choices=JAVA_BASE, help="检查哪个版本树 (默认 1.21.5)")
    args = ap.parse_args()

    groups = load_facts()
    vals = resolve_all(groups)
    base = os.path.join(SRC_DIR, args.tree, JAVA_SUB)

    # 预读: 帧上下文 (FrameContext/主菜单 frame) 里的布局绑定, 供各组件文件复用
    prelude = {}
    for pf in ("ui/model/FrameContext.java", "ui/model/MainMenuMisayosFrameContext.java", "ui/model/MainMenuPoulsenFrameContext.java"):
        p = os.path.join(base, pf)
        if os.path.exists(p):
            _, _, _, _, binds = evaluate_file(open(p, encoding="utf-8").read())
            prelude.update(binds)
    prelude.pop("SCALED_WIDTH", None)
    prelude.pop("SCALED_HEIGHT", None)
    # 组件内 getter 名带 frame 前缀 (见 java_substitutions), 为它补别名
    for base_name in ("block1Pos", "block3Pos", "block1Size", "block3Size", "rect1Width", "rect1PosX", "bgFontSize"):
        alias = "frame" + base_name[0].upper() + base_name[1:]
        if base_name in prelude:
            prelude.setdefault(alias, prelude[base_name])
        for suffix in (".getX", ".getY"):
            if base_name + suffix in prelude:
                prelude.setdefault(alias + suffix, prelude[base_name + suffix])

    results = []  # (group, name, status, detail)
    drift = 0
    missing = 0
    for group, facts in groups.items():
        for name, f in facts.items():
            java = f["java"]
            if not java:
                results.append((group, name, "SKIP", "webOnly (无 Java 对应)"))
                continue
            path, _, line = java.partition(":")
            jfile = os.path.join(base, path)
            line = int(line) if line.isdigit() else None
            expected = round(vals[group + "." + name], 6)
            if not os.path.exists(jfile):
                results.append((group, name, "MISSING", "Java 文件不存在: " + path))
                missing += 1
                continue
            text = open(jfile, encoding="utf-8").read()
            found, literals, found_loose, unverified, _ = evaluate_file(text, prelude)
            if expected in found:
                lines = ",".join(str(x) for x in found[expected])
                results.append((group, name, "OK", "%s:%s → %s" % (path, lines, fmt(expected))))
            elif expected in literals:
                results.append((group, name, "OK~", "%s:%s → %s (字面量存在, 上下文含未解析变量)" % (path, ",".join(str(x) for x in literals[expected]), fmt(expected))))
            elif expected in found_loose:
                results.append((group, name, "OK?", "%s:%s → %s (未知标识符按 1 估得, 建议人工复核)" % (path, ",".join(str(x) for x in found_loose[expected]), fmt(expected))))
            else:
                ctx = "; ".join(u[1] for u in unverified[:3])
                results.append((group, name, "DRIFT", "%s:%s 期望 %s, 文件中未找到。疑似 Java 已改动或表达式已重构。未解析片段: %s" % (path, line, fmt(expected), ctx or "无")))
                drift += 1

    # 输出
    if args.json:
        import json as j
        print(j.dumps([{"group": g, "name": n, "status": s, "detail": d} for g, n, s, d in results], ensure_ascii=False, indent=2))
    else:
        cur_group = None
        for g, n, s, d in results:
            if g != cur_group:
                cur_group = g
                print("\n[%s]" % g)
            mark = {"OK": "  ✓", "OK~": "  ◐", "OK?": "  ◕", "SKIP": "  -", "MISSING": "  ✗", "DRIFT": "  ✗"}[s]
            print("%s %-16s %s" % (mark, n, d))
        ok = sum(1 for r in results if r[2] in ("OK", "OK~", "OK?"))
        print("\n%s / %s 通过; DRIFT %s; MISSING %s; SKIP %s" % (ok, len(results), drift, missing, sum(1 for r in results if r[2] == "SKIP")))
    # Java 文件缺失与布局漂移同等视为失败, 避免重命名后检查器静默通过
    sys.exit(1 if (drift or missing) else 0)


def fmt(v):
    if abs(v - round(v)) < 1e-6:
        return str(int(round(v)))
    return "%.6g" % v


if __name__ == "__main__":
    main()
