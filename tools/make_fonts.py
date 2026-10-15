#!/usr/bin/env python3
"""Cuts the bundled fonts down to the characters the home screen can show.

    python3 -m venv venv && venv/bin/pip install fonttools skia-pathops
    venv/bin/python tools/make_fonts.py <dir with the downloaded Google Fonts .ttf files> [name ...]

A full Japanese font is 4-9 MB; the home screen only ever writes Japanese: the
phrases and readings in lang/, the N5 and N4 words in assets/vocab.tsv and the
kanji in ui/, plus dates. So
each text font keeps those characters, ASCII and Latin-1 (rōmaji); clock faces
keep only digits and the colon. Translations, and the drawer's katakana names,
are drawn in the phone's own font, so their characters aren't kept.
Variable fonts are pinned first (regular weight unless FONTS says otherwise).
Run again whenever lang/ gains words. Names after the folder limit the run to
those fonts.
"""
import glob
import os
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.ttLib.removeOverlaps import removeOverlaps
from fontTools.varLib import instancer

ROOT = os.path.join(os.path.dirname(__file__), "..")
SRC_DIR = os.path.join(ROOT, "app/src/main/java/com/asnidev/kamei")
LANG_DIR = os.path.join(SRC_DIR, "lang")
# Output name: which characters it keeps ("text" = everything the home screen writes,
# "digits" = a clock face only), and any variable-font axes to pin (weight defaults to 400).
FONTS = {
    # Japanese text fonts.
    "mplus1code": ("text", {}),
    "zenmarugothic": ("text", {}),
    "dotgothic16": ("text", {}),
    "shipporimincho": ("text", {}),
    "kleeone": ("text", {}),
    "delagothicone": ("text", {}),
    "rampartone": ("text", {}),
    "trainone": ("text", {}),
    "stick": ("text", {}),
    "rocknrollone": ("text", {}),
    "reggaeone": ("text", {}),
    "yuseimagic": ("text", {}),
    "hachimarupop": ("text", {}),
    "pottaone": ("text", {}),
    "kaiseidecol": ("text", {}),
    "murecho": ("text", {}),
    "bizudgothic": ("text", {}),
    "yujisyuku": ("text", {}),
    "kiwimaru": ("text", {}),
    # Latin faces, for the clock.
    "inter": ("digits", {}),
    "spacegrotesk": ("digits", {"wght": 300}),
    "unbounded": ("digits", {"wght": 300}),
    "bebasneue": ("digits", {}),
    "majormonodisplay": ("digits", {}),
    "monoton": ("digits", {}),
    "bungeehairline": ("digits", {}),
    "orbitron": ("digits", {}),
    "syne": ("digits", {}),
    "bigshouldersdisplay": ("digits", {"wght": 300}),
    "instrumentserif": ("digits", {}),
    "fraunces": ("digits", {"wght": 300, "opsz": 144, "SOFT": 100}),
    "doto": ("digits", {"wght": 500}),
    "sixtyfour": ("digits", {}),
    "tiltneon": ("digits", {}),
    "codystar": ("digits", {}),
    "rubikmonoone": ("digits", {}),
}
DIGITS = "0123456789: "
FONT_LEVELS = ("N5", "N4")
# Weekdays and months as java.time writes them in Japanese, and the weather kanji.
EXTRA = "月火水木金土日曜一二三四五六七八九十天年時分秒午前後晴曇雨雪霧雷霙風"


def japanese(c):
    """CJK punctuation, kana, kanji and full-width forms: what Japanese text is written in."""
    o = ord(c)
    return 0x3000 <= o <= 0x30FF or 0x3400 <= o <= 0x9FFF or 0xFF00 <= o <= 0xFFEF


def read(name):
    return open(os.path.join(LANG_DIR, name), encoding="utf-8").read()


def characters():
    text = ""
    # The Japanese row of the phrase table (the other rows are translations).
    phrases = read("Phrases.kt")
    start = phrases.index('"ja" to listOf(')
    text += phrases[start:phrases.index("),", start)]
    # The word list's Japanese and kana, for the levels the fonts carry (Vocabulary.FONT_LEVELS);
    # words from higher levels with other kanji are drawn in the phone's font.
    for line in open(os.path.join(ROOT, "app/src/main/assets/vocab.tsv"), encoding="utf-8"):
        cols = line.split("\t")
        if cols[0] in FONT_LEVELS:
            text += cols[1] + cols[2]
    # Readings, and the kanji the UI writes itself.
    text += read("JaReading.kt")
    text += "".join(open(p, encoding="utf-8").read() for p in sorted(glob.glob(os.path.join(SRC_DIR, "ui", "*.kt"))))
    chars = {c for c in text if japanese(c)}
    chars |= set(EXTRA)
    chars |= {chr(c) for c in range(0x20, 0x7F)}
    chars |= {chr(c) for c in range(0xA0, 0x180)}  # Latin-1 and Latin Extended-A (°, ā, ē…)
    chars |= set("…·–—‘’“”°\u2009")
    return "".join(sorted(chars))


def main(src, only):
    text = characters()
    out_dir = os.path.join(ROOT, "app/src/main/res/font")
    lic_dir = os.path.join(ROOT, "app/src/main/assets/licenses")
    for name, (keep, pins) in FONTS.items():
        if only and name not in only:
            continue
        # Downloads are named "<google fonts folder>-<file>.ttf"; this name is the folder.
        path = sorted(p for p in glob.glob(os.path.join(src, name + "-*.ttf")) if "Italic" not in p)[0]
        font = TTFont(path)
        if "fvar" in font:
            axes = {a.axisTag: a for a in font["fvar"].axes}
            fixed = {tag: pins.get(tag, 400 if tag == "wght" else a.defaultValue) for tag, a in axes.items()}
            fixed = {tag: min(max(v, axes[tag].minValue), axes[tag].maxValue) for tag, v in fixed.items()}
            # Variable glyphs are built from overlapping shapes, which show as stray
            # lines once the clock draws them as outlines: merge them.
            font = instancer.instantiateVariableFont(font, fixed, overlap=instancer.OverlapMode.REMOVE)
        chars = text if keep == "text" else DIGITS
        missing = [c for c in chars if ord(c) > 0x2FFF and ord(c) not in font.getBestCmap()]
        options = subset.Options()
        options.layout_features = ["*"]
        options.name_IDs = ["*"]
        options.notdef_outline = True
        sub = subset.Subsetter(options)
        sub.populate(text=chars)
        sub.subset(font)
        if "glyf" in font:
            removeOverlaps(font)  # static fonts can have them too
        out = os.path.join(out_dir, name + ".ttf")
        font.save(out)
        with open(os.path.join(lic_dir, name + "-OFL.txt"), "w") as f:
            f.write(open(os.path.join(src, name + "-OFL.txt"), encoding="utf-8").read())
        note = f"  (falls back for {''.join(missing)})" if missing else ""
        print(f"{name}: {os.path.getsize(path) // 1024} KB -> {os.path.getsize(out) // 1024} KB{note}")


if __name__ == "__main__":
    main(sys.argv[1], set(sys.argv[2:]))
