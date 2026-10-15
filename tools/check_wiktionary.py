"""Translation check (run with python3 -I, caches downloads in the given dir).
Compare Kamei's word list (assets/vocab.tsv) and weather words against en.wiktionary translation tables.
usage: python3 -I wikt.py <kamei lang dir> <cache dir>"""
import json, os, re, sys, time, unicodedata, urllib.parse, urllib.request

LANG_DIR, CACHE = sys.argv[1], sys.argv[2]
CODES = ["lv", "lt", "et", "ru", "uk", "de", "fr", "es", "pl", "fi", "sv", "ja", "zh", "ko", "el"]
WIKT = {c: c for c in CODES}
WIKT["zh"] = "cmn"

def fetch(title):
    path = os.path.join(CACHE, urllib.parse.quote(title, safe="") + ".txt")
    if os.path.exists(path):
        return open(path, encoding="utf-8").read()
    url = "https://en.wiktionary.org/w/index.php?action=raw&title=" + urllib.parse.quote(title)
    req = urllib.request.Request(url, headers={"User-Agent": "Kamei-translation-check/0.1 (github.com/AndSni)"})
    try:
        text = urllib.request.urlopen(req, timeout=20).read().decode("utf-8")
    except Exception:
        text = ""
    open(path, "w", encoding="utf-8").write(text)
    time.sleep(0.3)
    return text

def norm(s):
    s = unicodedata.normalize("NFD", s.lower().strip())
    s = "".join(ch for ch in s if unicodedata.category(ch) != "Mn")  # drop accents/stress marks
    return s.replace("ё", "е")

def translations(word):
    text = fetch(word) + "\n" + fetch(word + "/translations")
    if word == "colour":
        text += fetch("color") + fetch("color/translations")
    found = {c: set() for c in CODES}
    for code, w in WIKT.items():
        for m in re.finditer(r"\{\{tt?\+?\|" + re.escape(w) + r"\|([^|}]+)", text):
            found[code].add(m.group(1).strip())
    return found

# Our word list (assets/vocab.tsv): level, ja, kana, en, then the other languages.
VOCAB = os.path.join(LANG_DIR, "../../../../../assets/vocab.tsv")
cols = ["level", "ja", "kana", "en", "lv", "lt", "et", "ru", "uk", "de", "fr", "es", "pl", "fi", "sv", "zh", "ko", "el"]
rows = []
for line in open(VOCAB, encoding="utf-8"):
    if line.startswith("#"):
        continue
    r = dict(zip(cols, line.rstrip("\n").split("\t")))
    r["key"] = r["ja"]
    r["en"] = r["en"].removeprefix("to ")
    rows.append(r)

# Weather nouns from the phrase table.
ph = open(os.path.join(LANG_DIR, "Phrases.kt"), encoding="utf-8").read()
enum = re.search(r"enum class Phrase \{(.*?)\}", ph, re.S).group(1)
names = [n.strip() for n in re.split(r"[,\s]+", enum) if n.strip()]
lists = {}
for m in re.finditer(r'"(\w\w)" to listOf\((.*?)\n        \)', ph, re.S):
    lists[m.group(1)] = re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(2))
weather = {"fog": "FOG", "drizzle": "DRIZZLE", "rain": "RAIN", "sleet": "SLEET", "snow": "SNOW", "thunderstorm": "THUNDER", "notification": "KIND_OTHER", "battery": None}
for en, p in weather.items():
    if p is None:
        continue
    i = names.index(p)
    rows.append({"key": "phrase:" + p, "en": en, **{c: lists[c][i] for c in CODES}})

report = []
for r in rows:
    tr = translations(r["en"])
    for c in CODES:
        ours = r.get(c, "")
        if not ours:
            continue
        theirs = tr[c]
        if not theirs:
            report.append((r["key"], c, ours, "NO WIKTIONARY DATA"))
            continue
        if norm(ours) in {norm(t) for t in theirs} or any(norm(ours) in norm(t) or norm(t) in norm(ours) for t in theirs if len(t) > 1):
            continue
        report.append((r["key"], c, ours, ", ".join(sorted(theirs))[:160]))

checked = len(rows) * len(CODES)
print(f"checked {checked} word/language pairs, {len(report)} not found as-is in Wiktionary")
for k, c, ours, theirs in report:
    print(f"{k:22} {c}: ours={ours!r:28} wiktionary: {theirs}")
