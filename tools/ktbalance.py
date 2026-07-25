#!/usr/bin/env python3
"""
Lightweight Kotlin bracket-balance checker.

This is NOT a compiler. It exists because this sandbox has no JDK, so the only
available structural check on Kotlin sources is lexical. Naive regex stripping
gets this wrong on Kotlin because of:

  - string templates: "${ expr }" contains real code, including braces
  - nested templates: "${ if (x) "${y}" else "" }"
  - raw strings: \"\"\" ... \"\"\" which may contain quotes and braces
  - char literals: '{' '}' '(' ')' '\\''
  - backtick identifiers: fun `a name with an apostrophe's quote`()
  - escapes inside strings
  - comments, including nested /* /* */ */ which Kotlin permits

It tokenizes properly, tracking template depth, and reports the first
unbalanced construct with a line number.

Usage:  python3 tools/ktbalance.py <file.kt> [more.kt ...]
Exit code 0 when every file balances, 1 otherwise.
"""
import sys


def check(path):
    src = open(path, encoding="utf-8").read()
    i, n = 0, len(src)
    line = 1
    stack = []           # (char, line) for code brackets
    # Each entry: depth of '{' seen inside the current string template
    template_stack = []

    def err(msg):
        return f"{path}: {msg}"

    while i < n:
        c = src[i]

        if c == "\n":
            line += 1
            i += 1
            continue

        # --- comments -------------------------------------------------------
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            while i < n and src[i] != "\n":
                i += 1
            continue

        if c == "/" and i + 1 < n and src[i + 1] == "*":
            depth = 1
            i += 2
            while i < n and depth:
                if src[i] == "\n":
                    line += 1
                elif src[i] == "/" and i + 1 < n and src[i + 1] == "*":
                    depth += 1
                    i += 1
                elif src[i] == "*" and i + 1 < n and src[i + 1] == "/":
                    depth -= 1
                    i += 1
                i += 1
            continue

        # --- backtick identifier ---------------------------------------------
        # Kotlin allows `names with spaces`, widely used for test method names.
        # Apostrophes inside them are NOT char literals.
        if c == "`":
            i += 1
            while i < n and src[i] != "`":
                if src[i] == "\n":
                    line += 1
                i += 1
            i += 1
            continue

        # --- char literal ---------------------------------------------------
        if c == "'":
            i += 1
            while i < n and src[i] != "'":
                if src[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue

        # --- raw string -----------------------------------------------------
        if src.startswith('"""', i):
            i += 3
            while i < n and not src.startswith('"""', i):
                if src[i] == "\n":
                    line += 1
                # Raw strings still support ${...} templates.
                if src[i] == "$" and i + 1 < n and src[i + 1] == "{":
                    template_stack.append(0)
                    i += 2
                    depth = 1
                    while i < n and depth:
                        if src[i] == "{":
                            depth += 1
                        elif src[i] == "}":
                            depth -= 1
                        elif src[i] == "\n":
                            line += 1
                        i += 1
                    template_stack.pop()
                    continue
                i += 1
            i += 3
            continue

        # --- regular string -------------------------------------------------
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    i += 2
                    continue
                if src[i] == "$" and i + 1 < n and src[i + 1] == "{":
                    i += 2
                    depth = 1
                    while i < n and depth:
                        if src[i] == "{":
                            depth += 1
                        elif src[i] == "}":
                            depth -= 1
                        elif src[i] == "\n":
                            line += 1
                        elif src[i] == '"':
                            # nested string inside template
                            i += 1
                            while i < n and src[i] != '"':
                                if src[i] == "\\":
                                    i += 1
                                i += 1
                        i += 1
                    continue
                if src[i] == "\n":
                    line += 1
                i += 1
            i += 1
            continue

        # --- brackets -------------------------------------------------------
        if c in "{([":
            stack.append((c, line))
        elif c in "})]":
            pair = {"}": "{", ")": "(", "]": "["}[c]
            if not stack:
                return err(f"line {line}: unexpected '{c}' with nothing open")
            opened, oline = stack.pop()
            if opened != pair:
                return err(
                    f"line {line}: '{c}' closes '{opened}' opened at line {oline}"
                )
        i += 1

    if stack:
        opened, oline = stack[-1]
        return err(f"unclosed '{opened}' opened at line {oline}")
    return None


def main(argv):
    failures = []
    for path in argv:
        problem = check(path)
        if problem:
            failures.append(problem)
            print("FAIL " + problem)
        else:
            print("OK   " + path)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
