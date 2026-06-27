#!/usr/bin/env bash
#
# add-screensaver.sh — управлять списком заставки на Android TV по adb.
#
# Что делает:
#   1. достаёт id видео из ссылки (или принимает голый id);
#   2. тянет название через публичный oEmbed (без ключа);
#   3. подключается к ТВ по сети (adb tcp 5555);
#   4. читает текущие настройки приложения и ДОПИСЫВАЕТ/удаляет видео в списке
#      (остальные не теряются), при необходимости делает его выбранным;
#   5. один раз выводит приложение на передний план — иначе TCL-прошивка
#      (TclAppBoot) не даст заставке стартовать после изменения данных.
#
# Требования: установлен adb, на ТВ включена отладка, приложение — debug-сборка
# (нужен `adb run-as`). ТВ и комп в одной сети.
#
# Использование:
#   ./scripts/add-screensaver.sh <ссылка|id>                  # добавить в список
#   ./scripts/add-screensaver.sh --select <ссылка|id>         # добавить и сделать активной
#   ./scripts/add-screensaver.sh --select --play <ссылка|id>  # ещё и запустить сейчас
#   ./scripts/add-screensaver.sh --remove <ссылка|id>         # убрать из списка
#   ./scripts/add-screensaver.sh --list                       # показать текущий список
#   ./scripts/add-screensaver.sh --ip 192.168.1.77 <ссылка>
#   TV_IP=192.168.1.77 ./scripts/add-screensaver.sh <ссылка>
#
set -euo pipefail

PKG="org.denistouch.youtubescreensaver"
PREF="/data/data/$PKG/shared_prefs/${PKG}_preferences.xml"
TV_IP="${TV_IP:-192.168.1.56}"
ACTION="add"        # add | remove | list
SELECT=0
PLAY=0
URL=""

# --- найти adb ---
ADB="$(command -v adb || true)"
if [[ -z "$ADB" ]]; then
  for c in \
    /opt/homebrew/share/android-commandlinetools/platform-tools/adb \
    "$HOME/Library/Android/sdk/platform-tools/adb"; do
    [[ -x "$c" ]] && { ADB="$c"; break; }
  done
fi
[[ -z "$ADB" ]] && { echo "✖ adb не найден в PATH"; exit 1; }

# --- разобрать аргументы ---
while [[ $# -gt 0 ]]; do
  case "$1" in
    --select) SELECT=1; shift ;;
    --play)   PLAY=1; shift ;;
    --remove) ACTION="remove"; shift ;;
    --list)   ACTION="list"; shift ;;
    --ip)     TV_IP="$2"; shift 2 ;;
    -h|--help) sed -n '2,33p' "$0"; exit 0 ;;
    -*) echo "✖ неизвестный флаг: $1"; exit 1 ;;
    *) URL="$1"; shift ;;
  esac
done

# --- подключиться к ТВ ---
"$ADB" connect "$TV_IP:5555" >/dev/null 2>&1 || true
if ! "$ADB" -s "$TV_IP:5555" get-state >/dev/null 2>&1; then
  echo "✖ ТВ недоступен по adb ($TV_IP:5555). Проверь: одна сеть, включена отладка, подтверждён RSA-запрос."
  exit 1
fi
ADBS=("$ADB" -s "$TV_IP:5555")

read_prefs() { "${ADBS[@]}" shell run-as "$PKG" cat "$PREF" 2>/dev/null || true; }

# --- режим просмотра ---
if [[ "$ACTION" == "list" ]]; then
  CUR_XML="$(read_prefs)" python3 <<'PY'
import os, json, xml.etree.ElementTree as ET
cur = os.environ.get("CUR_XML", "").strip()
if not cur:
    print("(список пуст)"); raise SystemExit
root = ET.fromstring(cur)
vals = {s.get("name"): (s.text or "") for s in root.findall("string")}
sel = vals.get("videos.selected", "")
for v in json.loads(vals.get("videos.list", "[]") or "[]"):
    mark = "▶" if v.get("id") == sel else " "
    print(f'{mark} {v.get("id")}  {v.get("title","")[:60]}')
PY
  exit 0
fi

[[ -z "$URL" ]] && { echo "✖ укажи ссылку/id видео. См. --help"; exit 1; }

# --- id видео из ссылки (или голый id) ---
VID="$(python3 - "$URL" <<'PY'
import sys, re
u = sys.argv[1]
m = (re.search(r'(?:v=|/embed/|/shorts/|youtu\.be/|/v/)([A-Za-z0-9_-]{11})', u)
     or re.search(r'(?<![A-Za-z0-9_-])([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])', u))
print(m.group(1) if m else "")
PY
)"
[[ -z "$VID" ]] && { echo "✖ не удалось распознать id видео из: $URL"; exit 1; }

# --- название через oEmbed (только для добавления; фолбэк на id) ---
TITLE="$VID"
if [[ "$ACTION" == "add" ]]; then
  T="$(curl -s "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$VID&format=json" \
    | python3 -c 'import sys,json
try: print(json.load(sys.stdin)["title"])
except Exception: print("")' 2>/dev/null || true)"
  [[ -n "$T" ]] && TITLE="$T"
fi

# --- убить процесс (сбросить кэш SharedPreferences) и забрать текущие настройки ---
"${ADBS[@]}" shell am force-stop "$PKG"
CUR="$(read_prefs)"

# --- merge через python: текущие настройки приходят в CUR_XML (env, НЕ stdin —
#     stdin занят heredoc-программой) ---
NEWXML="$(CUR_XML="$CUR" python3 - "$VID" "$TITLE" "$ACTION" "$SELECT" <<'PY'
import os, sys, json, xml.etree.ElementTree as ET
vid, title, action, do_select = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4] == "1"
cur = os.environ.get("CUR_XML", "").strip()
videos, selected = [], ""
if cur:
    try:
        root = ET.fromstring(cur)
        vals = {s.get("name"): (s.text or "") for s in root.findall("string")}
        videos = json.loads(vals.get("videos.list", "[]") or "[]")
        selected = vals.get("videos.selected", "")
    except Exception:
        videos, selected = [], ""

if action == "remove":
    videos = [v for v in videos if v.get("id") != vid]
    if selected == vid:
        selected = videos[0]["id"] if videos else ""
else:  # add
    for v in videos:
        if v.get("id") == vid:
            v["title"] = title
            break
    else:
        videos.append({"id": vid, "title": title})
    if do_select or not selected:
        selected = vid

list_json = json.dumps(videos, ensure_ascii=False)
def esc(s):
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace('"',"&quot;")
print("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>")
print("<map>")
print(f'    <string name="videos.selected">{esc(selected)}</string>')
print(f'    <string name="videos.list">{esc(list_json)}</string>')
print("</map>")
PY
)"

# --- залить обратно ---
TMP="$(mktemp)"; printf '%s\n' "$NEWXML" > "$TMP"
"${ADBS[@]}" push "$TMP" /data/local/tmp/yt_pref.xml >/dev/null
"${ADBS[@]}" shell run-as "$PKG" cp /data/local/tmp/yt_pref.xml "$PREF"
"${ADBS[@]}" shell run-as "$PKG" chmod 660 "$PREF"
rm -f "$TMP"

# --- TclAppBoot: вывести приложение на передний план один раз ---
"${ADBS[@]}" shell am start -n "$PKG/.ScreensaverSettingsActivity" >/dev/null 2>&1 || true
sleep 2
"${ADBS[@]}" shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true

if [[ "$ACTION" == "remove" ]]; then
  echo "✓ удалено из списка: $VID"
else
  echo "✓ добавлено: $TITLE"
  echo "  id: $VID"
  [[ "$SELECT" == "1" ]] && echo "  ✓ назначено активной заставкой"
fi

# --- по желанию: запустить заставку сейчас ---
if [[ "$PLAY" == "1" ]]; then
  echo "  ▶ запускаю заставку…"
  "${ADBS[@]}" shell am start -n com.android.systemui/.Somnambulator >/dev/null 2>&1 || true
fi
