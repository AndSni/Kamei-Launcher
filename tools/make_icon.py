#!/usr/bin/env python3
"""Draws the launcher icon: the kanji 仮 (from 仮名, "pseudonym" and "kana") as a white outline, like the clock.

    venv/bin/python tools/make_icon.py <path to MPLUS1Code[wght].ttf> [preview.svg]

Takes the glyph from M PLUS 1 Code at medium weight (so each stroke is wide enough
to show as a double line), merges its overlapping parts, and writes it as the
adaptive icon's foreground (also used as the monochrome layer) on the 108-unit
canvas, inside the 66-unit safe circle.
"""
import os
import sys

from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

CHAR = "仮"
CANVAS = 108
BOX = 52          # the glyph's larger side, in canvas units
STROKE = 0.9      # outline width, in canvas units
ROOT = os.path.join(os.path.dirname(__file__), "..")


def glyph_path(font_path):
    font = TTFont(font_path)
    font = instancer.instantiateVariableFont(font, {"wght": 500}, overlap=instancer.OverlapMode.REMOVE)
    name = font.getBestCmap()[ord(CHAR)]
    glyphs = font.getGlyphSet()
    bp = BoundsPen(glyphs)
    glyphs[name].draw(bp)
    x0, y0, x1, y1 = bp.bounds
    s = BOX / max(x1 - x0, y1 - y0)
    # Centre on the canvas and flip y (fonts grow upwards, the canvas downwards).
    dx = CANVAS / 2 - (x0 + x1) / 2 * s
    dy = CANVAS / 2 + (y0 + y1) / 2 * s
    pen = SVGPathPen(glyphs, ntos=lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
    glyphs[name].draw(TransformPen(pen, (s, 0, 0, -s, dx, dy)))
    return pen.getCommands()


def main(font_path, preview=None):
    d = glyph_path(font_path)
    xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Made by tools/make_icon.py: {CHAR} from M PLUS 1 Code (OFL), outlined. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="{CANVAS}"
    android:viewportHeight="{CANVAS}">
    <path
        android:pathData="{d}"
        android:strokeWidth="{STROKE}"
        android:strokeColor="#FFFFFF"
        android:strokeLineJoin="round" />
</vector>
"""
    with open(os.path.join(ROOT, "app/src/main/res/drawable/ic_launcher_foreground.xml"), "w") as f:
        f.write(xml)
    if preview:
        with open(preview, "w") as f:
            f.write(f'<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 {CANVAS} {CANVAS}">'
                    f'<circle cx="54" cy="54" r="54" fill="#000"/>'
                    f'<path d="{d}" fill="none" stroke="#fff" stroke-width="{STROKE}" stroke-linejoin="round"/></svg>')
    print("ok")


if __name__ == "__main__":
    main(*sys.argv[1:])
