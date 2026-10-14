"""Translation check (run with python3 -I, caches downloads in the given dir).
Find Android's own translations of a few phrases and set them beside Kamei's.
usage: python3 -I aosp.py <aosp dir> <kamei lang dir>"""
import os, re, sys, xml.etree.ElementTree as ET
D, LANG_DIR = sys.argv[1], sys.argv[2]
LANGS = {"lv": "lv", "lt": "lt", "et": "et", "ru": "ru", "uk": "uk", "de": "de", "fr": "fr", "es": "es", "pl": "pl", "fi": "fi", "sv": "sv", "ja": "ja", "zh": "zh-rCN", "ko": "ko", "el": "el"}
WANT = {"Notifications": "NOTIFICATIONS", "No notifications": "NO_NOTIFICATIONS", "No service": "NO_SERVICE", "Battery": "BATTERY", "Weather": "WEATHER", "Just now": "JUST_NOW", "Calendar": "KIND_EVENT", "Alarm": "KIND_ALARM", "System": "KIND_SYSTEM"}

def load(path):
    out = {}
    try:
        root = ET.parse(path).getroot()
    except Exception:
        return out
    for s in root.iter("string"):
        out[s.get("name")] = "".join(s.itertext()).strip().strip('"')
    return out

ph = open(os.path.join(LANG_DIR, "Phrases.kt"), encoding="utf-8").read()
names = [n for n in re.split(r"[,\s]+", re.search(r"enum class Phrase \{(.*?)\}", ph, re.S).group(1)) if n]
lists = {m.group(1): re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(2)) for m in re.finditer(r'"(\w\w)" to listOf\((.*?)\n        \)', ph, re.S)}

for mod in ["SystemUI", "core"]:
    en = load(os.path.join(D, f"{mod}-values.xml"))
    for phrase_en, pname in WANT.items():
        keys = [k for k, v in en.items() if v == phrase_en][:3]
        for k in keys:
            print(f"\n== {mod}:{k} = {phrase_en!r}  (ours: {pname})")
            for code, folder in LANGS.items():
                t = load(os.path.join(D, f"{mod}-values-{folder}.xml")).get(k, "—")
                ours = lists[code][names.index(pname)] if pname in names else ""
                print(f"  {code}: android={t!r:34} ours={ours!r}")
