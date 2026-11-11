import re

with open(r'C:\Users\mokas\.gemini\antigravity\brain\7ea36482-098c-4540-9af8-f543a908dc22\.system_generated\steps\3683\output.txt', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

apps = []
blocks = text.split('---------------------------------------------------------------------------------------------')
for b in blocks:
    title_m = re.search(r'app_title\s*=(.*?)---', b)
    id_m = re.search(r'app_id\s*=(.*?)---', b)
    tizen_id_m = re.search(r'app_tizen_id\s*=(.*?)---', b)
    pkg_m = re.search(r'app_package_name\s*=(.*?)---', b)
    icon_m = re.search(r'app_panel_icon_path\s*=(.*?)---', b)
    type_m = re.search(r'type\s*=(.*?)---', b)
    if title_m and id_m:
        title = title_m.group(1).strip()
        aid = id_m.group(1).strip()
        tid = tizen_id_m.group(1).strip() if tizen_id_m else ''
        pkg = pkg_m.group(1).strip() if pkg_m else ''
        icon = icon_m.group(1).strip() if icon_m else ''
        atype = type_m.group(1).strip() if type_m else ''
        apps.append({'title': title, 'app_id': aid, 'tizen_id': tid, 'pkg': pkg, 'icon': icon, 'type': atype})

print(f"Total apps extracted: {len(apps)}")
for i, a in enumerate(apps):
    print(f"{i+1:2d}. Title: {a['title']:<25} | AppID: {a['app_id']:<24} | TizenID: {a['tizen_id']:<26} | Type: {a['type']}")
