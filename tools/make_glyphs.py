#!/usr/bin/env python3
"""Builds app/src/main/assets/glyphs/tabler.tsv from the Tabler Icons npm package.

    npm pack @tabler/icons@3.31.0 && tar xzf tabler-icons-3.31.0.tgz
    python3 -I tools/make_glyphs.py package/

One line per outline icon: name, category, search tags, then its paths, all
tab-separated. Each path is SVG path data on Tabler's 24x24 grid; a leading
"F" marks a filled shape (dots), the rest are drawn as strokes. Circles and
rects are turned into paths so the app only has to parse one kind.
"""
import json
import os
import sys


def circle(a):
    cx, cy, r = float(a["cx"]), float(a["cy"]), float(a["r"])
    return f"M{cx - r} {cy}a{r} {r} 0 1 0 {2 * r} 0a{r} {r} 0 1 0 {-2 * r} 0z"


def rect(a):
    x, y = float(a.get("x", 0)), float(a.get("y", 0))
    w, h = float(a["width"]), float(a["height"])
    return f"M{x} {y}h{w}v{h}h{-w}z"


def main(pkg):
    nodes = json.load(open(os.path.join(pkg, "tabler-nodes-outline.json")))
    meta = json.load(open(os.path.join(pkg, "icons.json")))
    root = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
    out = []
    for name in sorted(nodes):
        paths = []
        for tag, a in nodes[name]:
            if a.get("stroke") == "none" and a.get("fill", "none") == "none":
                continue  # Tabler's invisible 24x24 frame
            if tag == "path":
                d = a["d"]
            elif tag == "circle":
                d = circle(a)
            elif tag == "rect":
                d = rect(a)
            else:
                continue
            filled = a.get("fill") not in (None, "none")
            paths.append(("F" if filled else "") + d)
        if not paths:
            continue
        m = meta.get(name, {})
        tags = " ".join(str(t) for t in m.get("tags", []))
        out.append("\t".join([name, m.get("category", ""), tags.replace("\t", " ")] + paths))
    os.makedirs(os.path.join(root, "glyphs"), exist_ok=True)
    with open(os.path.join(root, "glyphs", "tabler.tsv"), "w") as f:
        f.write("\n".join(out) + "\n")
    with open(os.path.join(root, "licenses", "TablerIcons-MIT.txt"), "w") as f:
        f.write(open(os.path.join(pkg, "LICENSE")).read())
    print(len(out), "icons")


if __name__ == "__main__":
    main(sys.argv[1])
