#!/usr/bin/env python3
"""Builds the word-of-the-day list (app/src/main/assets/vocab.tsv) from open sources.

    python3 -m venv venv && venv/bin/pip install opencc-python-reimplemented
    venv/bin/python -I tools/make_vocab.py <cache dir>

The cache dir holds the downloads, made once and reused:
  n5.csv n4.csv n3.csv   JLPT word lists from github.com/jamsinclair/open-anki-jlpt-decks
                         (MIT, based on Jonathan Waller's tanos.co.uk lists, CC BY)
  jmdict-eng-*.json      JMdict from github.com/scriptin/jmdict-simplified (EDRDG, CC BY-SA 4.0)
  wikt/                  en.wiktionary pages, fetched as needed (CC BY-SA 4.0)

For each JLPT word, its English meaning leads to the English Wiktionary entry,
whose translation tables give the other languages. A table is used only when it
lists the Japanese word itself, which proves it is the right sense; words without
such a table are left out. Of what remains, each level keeps PER_LEVEL words:
common ones first, then those translated into the most languages. A language a
table lacks stays empty, and the app shows English there. Hand-checked words
from the old built-in list (OLD) keep their own translations.
"""
import csv, glob, json, os, re, sys, time, unicodedata, urllib.parse, urllib.request

from opencc import OpenCC  # pip install opencc-python-reimplemented

T2S = OpenCC("t2s")

CACHE = sys.argv[1]
ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "app/src/main/assets/vocab.tsv")
PER_LEVEL = 365
LEVELS = ["N5", "N4", "N3"]
# Translation columns, in the file's order; Wiktionary writes Mandarin as cmn.
LANGS = ["lv", "lt", "et", "ru", "uk", "de", "fr", "es", "pl", "fi", "sv", "zh", "ko", "el"]
WIKT = {l: ("cmn" if l == "zh" else l) for l in LANGS}
POS_HEAD = {"n": "Noun", "v": "Verb", "adj": "Adjective", "adv": "Adverb"}
# The 60 words of the old built-in table (Vocabulary.kt before this file), checked by hand.
OLD = os.path.join(CACHE, "old_vocab.tsv")


def fetch(title):
    os.makedirs(os.path.join(CACHE, "wikt"), exist_ok=True)
    path = os.path.join(CACHE, "wikt", urllib.parse.quote(title, safe="") + ".txt")
    if os.path.exists(path):
        return open(path, encoding="utf-8").read()
    url = "https://en.wiktionary.org/w/index.php?action=raw&title=" + urllib.parse.quote(title)
    req = urllib.request.Request(url, headers={"User-Agent": "Kamei-vocab-builder/0.2 (github.com/AndSni/Kamei-Launcher)"})
    for attempt in range(3):
        try:
            text = urllib.request.urlopen(req, timeout=30).read().decode("utf-8")
            break
        except urllib.error.HTTPError as e:
            text = ""
            if e.code == 404:
                break
            time.sleep(5 * (attempt + 1))
        except Exception:
            text = ""
            time.sleep(5 * (attempt + 1))
    open(path, "w", encoding="utf-8").write(text)
    time.sleep(0.25)
    return text


def english_section(text):
    m = re.search(r"^==English==\s*$(.*?)(?=^==[^=])", text + "\n==End==", re.S | re.M)
    return m.group(1) if m else ""


def tables(head):
    """Translation tables in the English entry for [head]: (part of speech, gloss, body)."""
    page = fetch(head)
    eng = english_section(page)
    if "{{see translation subpage" in eng or "/translations" in eng:
        eng += "\n" + english_section(fetch(head + "/translations")) + fetch(head + "/translations")
    out, pos = [], ""
    for m in re.finditer(r"^=+\s*([A-Za-z ]+?)\s*=+\s*$|\{\{trans-top(?:-also|-see)?\|([^}]*)\}\}(.*?)\{\{trans-bottom\}\}", eng, re.S | re.M):
        if m.group(1):
            if m.group(1) in ("Noun", "Verb", "Adjective", "Adverb", "Proper noun", "Interjection", "Pronoun", "Numeral", "Phrase"):
                pos = m.group(1)
        else:
            out.append((pos, m.group(2), m.group(3)))
    return out


def clean(term, lang):
    term = re.sub(r"\{\{[^{}]*\}\}", "", term)  # nested templates: {{+obj|...}}, {{q|...}}
    term = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", term)
    term = term.replace("\u200b", "")
    while re.search(r"\([^()]*\)", term):  # notes and hanja: (estar) cierto, 작년(昨年), contactar((se) con)
        term = re.sub(r"\([^()]*\)", "", term)
    term = re.split(r"\s*/\s*|,\s+", term)[0]  # the first of several: el año pasado / precedente
    term = re.sub(r"\s+", " ", term).strip(" \"“”„«»")
    s = unicodedata.normalize("NFD", term)
    # Stress marks dictionaries add but nobody writes: Russian, Ukrainian, Lithuanian.
    strip = {"ru": "\u0301\u0300", "uk": "\u0301\u0300", "lt": "\u0300\u0301\u0303"}.get(lang, "")
    s = "".join(c for c in s if c not in strip)
    if lang == "ko":
        s = s.replace("-", "")  # 물-을 주다: Wiktionary marks the particle off
    if lang == "zh":
        s = T2S.convert(s)  # Wiktionary writes Mandarin in traditional; Kamei's Chinese is simplified
    return unicodedata.normalize("NFC", s)


def param(text, start):
    """The template parameter starting at [start]: up to the next | or }} that is not
    inside a [[link]] or a nested {{template}}."""
    depth, i = 0, start
    while i < len(text):
        two = text[i:i + 2]
        if two in ("[[", "{{"):
            depth += 1
            i += 2
            continue
        if two in ("]]", "}}"):
            if depth == 0 and two == "}}":
                break
            depth -= 1
            i += 2
            continue
        if text[i] == "|" and depth == 0:
            break
        i += 1
    return text[start:i]


def terms(body, code):
    """The first translation a table gives in [code]."""
    m = re.search(r"\{\{tt?\+?\|" + re.escape(code) + r"\|", body)
    return param(body, m.end()) if m else None


def heads(meaning, glosses):
    """English headwords to look up: the list's meanings, then JMdict's."""
    out = []
    for g in re.split(r"[,;/]", meaning) + glosses:
        g = re.sub(r"\(.*?\)", "", g).strip().lower()
        g = re.sub(r"^(to|a|an|the) ", "", g).strip(" .!?")
        if g and len(g.split()) <= 3 and g not in out:
            out.append(g)
    return out[:4]


def pos_kind(tags):
    if any(t.startswith("v") and t not in ("vs-c",) for t in tags if t != "vs"):
        return "v"
    if any(t.startswith("adj") for t in tags):
        return "adj"
    if "adv" in tags:
        return "adv"
    if any(t in ("n", "vs", "n-adv", "n-t") for t in tags):
        return "n"
    return None


def main():
    jm = json.load(open(glob.glob(os.path.join(CACHE, "jmdict-eng-*.json"))[0], encoding="utf-8"))["words"]
    by_text = {}
    for e in jm:
        for k in e["kanji"] + e["kana"]:
            by_text.setdefault(k["text"], []).append(e)

    old = {}
    if os.path.exists(OLD):
        for line in open(OLD, encoding="utf-8"):
            f = line.rstrip("\n").split("\t")
            old[f[0]] = f

    seen, rows, report = set(), [], {}
    for level in LEVELS:
        cands = []
        for r in csv.DictReader(open(os.path.join(CACHE, level.lower() + ".csv"), encoding="utf-8")):
            ja, kana = r["expression"].strip(), r["reading"].strip()
            if ja in seen or not ja:
                continue
            # Loanwords teach little kanji or Japanese; grammar words make poor words of the day.
            if all("゠" <= c <= "ヿ" for c in ja):
                continue
            entries = [e for e in by_text.get(ja, []) if any(k["text"] == kana for k in e["kana"])] or by_text.get(ja, [])
            if not entries:
                continue
            e = entries[0]
            sense = e["sense"][0]
            kind = pos_kind(sense["partOfSpeech"])
            if kind is None:
                continue
            # Words usually written in kana, whose kanji are rare (為る, 只): shown in kana.
            if "uk" in e["sense"][0]["misc"] and not any(k.get("common") for k in e["kanji"] if k["text"] == ja):
                ja = kana
            # One entry per word: the lists give some twice (金 as かね and きん).
            if ja in seen or any(c[0] == ja for c in cands):
                continue
            readings = [k["text"] for k in e["kana"]]
            if kana not in readings:
                kana = kana[:-2] if kana.endswith("する") and kana[:-2] in readings else readings[0]
            common = any(k.get("common") for k in e["kanji"] + e["kana"])
            glosses = [g["text"] for g in sense["gloss"]]
            cands.append((ja, kana, r["meaning"], glosses, kind, common))
        picked = []
        for ja, kana, meaning, glosses, kind, common in cands:
            best = None
            for head in heads(meaning, glosses):
                for pos, gloss, body in tables(head):
                    ja_terms = [clean(t, "ja") for t in re.findall(r"\{\{tt?\+?\|ja\|([^|}]+)", body)]
                    if not any(t in (ja, kana) for t in ja_terms):
                        continue
                    found = {l: terms(body, WIKT[l]) for l in LANGS}
                    found = {l: clean(t, l) for l, t in found.items() if t}
                    score = len(found) + (2 if pos == POS_HEAD[kind] else 0)
                    if best is None or score > best[0]:
                        best = (score, head, found)
                if best:
                    break
            if not best:
                continue
            score, head, found = best
            row = {"level": level, "ja": ja, "kana": kana, "en": ("to " + head) if kind == "v" else head, **found}
            if ja in old:  # hand-checked translations win
                f = old[ja]
                row.update({"en": f[1], **{l: f[2 + i] for i, l in enumerate(LANGS)}})
                score = 99
            picked.append((common, score, row))
        picked.sort(key=lambda p: (not p[0], -p[1]))
        # Same sound, same meaning (在る and 有る, both "to be"): the first is enough.
        chosen, pairs = [], set()
        for _, _, row in picked:
            if (row["kana"], row["en"]) in pairs:
                continue
            pairs.add((row["kana"], row["en"]))
            chosen.append(row)
        chosen = chosen[:PER_LEVEL]
        seen.update(r["ja"] for r in chosen)
        rows += chosen
        full = sum(1 for r in chosen if all(r.get(l) for l in LANGS))
        report[level] = (len(cands), len(picked), len(chosen), full)

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("# level\tja\tkana\ten\t" + "\t".join(LANGS) + "\n")
        for r in rows:
            f.write("\t".join([r["level"], r["ja"], r["kana"], r["en"]] + [r.get(l, "") for l in LANGS]) + "\n")
    for level, (c, p, n, full) in report.items():
        print(f"{level}: {c} candidates, {p} with a confirmed sense, {n} kept, {full} in every language")
    gaps = {l: sum(1 for r in rows if not r.get(l)) for l in LANGS}
    print("empty (English shown):", ", ".join(f"{l} {n}" for l, n in gaps.items()))


if __name__ == "__main__":
    main()
