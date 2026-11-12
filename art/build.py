"""Draws the story covers as SVG and renders them to WebP for the app.
Run: python3 art/build.py   (needs cairosvg and pillow)"""
import io, os, math
import cairosvg
from PIL import Image

N, V, G = "#0C1024", "#F0E8D6", "#E0B252"
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "app", "src", "main", "res", "drawable-nodpi")
C = {}

def svg(bg, body):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 500" width="800" height="1000"><rect width="400" height="500" fill="{bg}"/>{body}</svg>'

# Aksum: stelae against a highland sun
win = "".join(f'<rect x="{x}" y="{y}" width="14" height="20" rx="2" fill="{N}"/>' for y in range(150, 370, 44) for x in (181, 205))
C["aksum"] = svg("#D9733C", f'''
<circle cx="292" cy="150" r="78" fill="#F2C766"/>
<path d="M0 380 Q110 300 220 372 T400 340 V500 H0Z" fill="#A84E26"/>
<path d="M84 440 L96 230 Q110 208 124 230 L136 440Z" fill="{V}" opacity=".78" transform="rotate(-7 110 440)"/>
<path d="M166 446 L176 112 Q200 72 224 112 L234 446Z" fill="{V}"/>
{win}
<path d="M188 446 V396 Q200 382 212 396 V446Z" fill="{N}"/>
<path d="M0 436 Q140 404 260 438 T400 428 V500 H0Z" fill="{N}"/>
<circle cx="318" cy="418" r="5" fill="{G}"/><circle cx="60" cy="424" r="4" fill="{G}"/>''')

# Rome: columns, one of them broken
def column(x, top, h, fill=V):
    flutes = "".join(f'<rect x="{x+8+i*11}" y="{top+22}" width="3" height="{h-34}" fill="#7A4FB5" opacity=".35"/>' for i in range(4))
    return f'<rect x="{x-8}" y="{top}" width="66" height="14" rx="3" fill="{fill}"/><rect x="{x-3}" y="{top+14}" width="56" height="8" fill="{fill}"/><rect x="{x}" y="{top+22}" width="50" height="{h-34}" fill="{fill}"/>{flutes}<rect x="{x-8}" y="{top+h-12}" width="66" height="12" rx="2" fill="{fill}"/>'
C["rome"] = svg("#6F47AE", f'''
<path d="M60 400 A140 140 0 0 1 340 400Z" fill="#F2C766"/>
<path d="M40 150 L200 78 L262 106 L250 128 L200 108 L66 168Z" fill="{V}"/>
{column(62, 168, 240)}
{column(150, 168, 240)}
<rect x="238" y="322" width="50" height="74" fill="{V}"/><path d="M238 322 l12 -14 l10 12 l12 -18 l16 20Z" fill="{V}"/><rect x="230" y="396" width="66" height="12" rx="2" fill="{V}"/>
<g transform="translate(-6 34) rotate(78 322 352)"><rect x="296" y="318" width="50" height="78" fill="{V}"/><rect x="288" y="304" width="66" height="14" rx="3" fill="{V}"/></g>
<rect x="0" y="406" width="400" height="94" fill="{N}"/>
<rect x="30" y="428" width="120" height="5" rx="2" fill="{V}" opacity=".3"/><rect x="210" y="450" width="150" height="5" rx="2" fill="{V}" opacity=".3"/>''')

# Nicaea: a council gathered under an arch
fig = ""
cols = [V, G, "#9DB4F5", V, G, "#9DB4F5", V]
for i in range(7):
    a = math.radians(200 + i * 140 / 6)
    cx, cy = 200 + 128 * math.cos(a), 470 + 150 * math.sin(a) * -1 * -1
    cx, cy = 200 + (i - 3) * 44, 392 + abs(i - 3) * 9
    fig += f'<path d="M{cx-17} {cy+70} V{cy+14} Q{cx} {cy-6} {cx+17} {cy+14} V{cy+70}Z" fill="{cols[i]}"/><circle cx="{cx}" cy="{cy-8}" r="12" fill="{cols[i]}"/><path d="M{cx-10} {cy-17} L{cx} {cy-40} L{cx+10} {cy-17}Z" fill="{cols[i]}"/>'
C["nicaea"] = svg("#2B4BA6", f'''
<path d="M70 500 V230 A130 130 0 0 1 330 230 V500Z" fill="{N}"/>
<path d="M70 230 A130 130 0 0 1 330 230" fill="none" stroke="{V}" stroke-width="12"/>
<path d="M44 500 V230 A156 156 0 0 1 356 230 V500" fill="none" stroke="{V}" stroke-width="4" opacity=".45"/>
<circle cx="200" cy="222" r="64" fill="#F2C766" opacity=".22"/>
<rect x="191" y="150" width="18" height="150" rx="3" fill="{G}"/><rect x="152" y="192" width="96" height="18" rx="3" fill="{G}"/>
{fig}
<rect x="0" y="462" width="400" height="38" fill="#1C3276"/>''')

# The press
lines = "".join(f'<rect x="150" y="{y}" width="{w}" height="5" rx="2" fill="{N}" opacity=".7"/>' for y, w in ((316, 100), (330, 84), (344, 100), (358, 62)))
screw = "".join(f'<rect x="{186 - (i % 2) * 4}" y="{120 + i * 14}" width="{28 + (i % 2) * 8}" height="9" rx="4" fill="{G}"/>' for i in range(8))
typ = "".join(f'<rect x="{84 + i * 62}" y="424" width="50" height="50" rx="6" fill="{N}"/><text x="{109 + i * 62}" y="462" text-anchor="middle" font-family="DejaVu Serif, serif" font-size="34" font-weight="bold" fill="{V}">{ch}</text>' for i, ch in enumerate("ABCD"))
C["press"] = svg("#1E8573", f'''
<rect x="62" y="70" width="28" height="330" rx="5" fill="{N}"/><rect x="310" y="70" width="28" height="330" rx="5" fill="{N}"/>
<rect x="48" y="62" width="304" height="40" rx="8" fill="{N}"/>
{screw}
<rect x="118" y="96" width="164" height="12" rx="6" fill="{V}"/><circle cx="118" cy="102" r="12" fill="{V}"/><circle cx="282" cy="102" r="12" fill="{V}"/>
<rect x="112" y="232" width="176" height="34" rx="6" fill="{N}"/>
<rect x="132" y="296" width="136" height="84" rx="4" fill="{V}"/>{lines}
<rect x="90" y="380" width="220" height="20" rx="4" fill="#12584C"/>
{typ}''')

# Compound interest: stacks that grow, and something living on top
stacks = ""
heights = [1, 2, 3, 5, 9]
for i, h in enumerate(heights):
    x = 78 + i * 62
    for k in range(h):
        y = 430 - k * 26
        stacks += f'<rect x="{x-24}" y="{y-10}" width="48" height="20" fill="{N}"/><ellipse cx="{x}" cy="{y+10}" rx="24" ry="8" fill="{N}"/><ellipse cx="{x}" cy="{y-10}" rx="24" ry="8" fill="#2A3470"/><ellipse cx="{x}" cy="{y-10}" rx="15" ry="4.5" fill="none" stroke="{G}" stroke-width="2"/>'
tx, ty = 78 + 4 * 62, 430 - 8 * 26 - 10
C["compound"] = svg("#E3B24C", f'''
<circle cx="96" cy="112" r="56" fill="{V}" opacity=".55"/>
<path d="M54 418 Q250 400 330 196" fill="none" stroke="{N}" stroke-width="4" stroke-dasharray="2 12" stroke-linecap="round"/>
{stacks}
<path d="M{tx} {ty} V{ty-58}" stroke="#1E8573" stroke-width="6" stroke-linecap="round"/>
<path d="M{tx} {ty-40} q-34 -6 -40 -40 q36 0 40 40Z" fill="#1E8573"/><path d="M{tx} {ty-56} q34 -8 42 -44 q-38 2 -42 44Z" fill="#27A58E"/>
<rect x="0" y="446" width="400" height="54" fill="{N}"/>''')

# Tenochtitlan: a temple on an island in the lake
waves = "".join(f'<path d="M{x} {y} q14 -10 28 0 t28 0" fill="none" stroke="{V}" stroke-width="4" stroke-linecap="round" opacity=".55"/>' for x, y in ((24, 300), (300, 318), (40, 392), (286, 404), (150, 462), (20, 468), (300, 474)))
tiers = "".join(f'<rect x="{104 + i * 24}" y="{340 - i * 42}" width="{192 - i * 48}" height="42" fill="{V if i % 2 == 0 else "#DDD2B8"}"/>' for i in range(3))
C["tenochtitlan"] = svg("#1F93A8", f'''
<circle cx="306" cy="108" r="54" fill="#F2C766"/>
<path d="M0 250 L70 170 L130 232 L196 150 L270 236 L330 190 L400 250 V300 H0Z" fill="#17748A"/>
{waves}
<ellipse cx="200" cy="392" rx="150" ry="30" fill="#12584C"/>
{tiers}
<rect x="180" y="256" width="40" height="126" fill="{G}"/>
<rect x="156" y="210" width="34" height="46" fill="#C8443A"/><rect x="210" y="210" width="34" height="46" fill="#2B4BA6"/>
<rect x="150" y="200" width="46" height="12" rx="2" fill="{N}"/><rect x="204" y="200" width="46" height="12" rx="2" fill="{N}"/>
<rect x="336" y="386" width="64" height="10" fill="{V}" opacity=".9"/>
<path d="M56 428 q30 16 60 0 l-8 12 h-44Z" fill="{N}"/><rect x="84" y="400" width="4" height="30" fill="{N}"/>''')

# The English Bible: an open book, light behind it
rays = "".join(f'<path d="M200 300 L{200 + 420 * math.cos(math.radians(a - 5))} {300 + 420 * math.sin(math.radians(a - 5))} L{200 + 420 * math.cos(math.radians(a + 5))} {300 + 420 * math.sin(math.radians(a + 5))}Z" fill="#F2C766" opacity=".34"/>' for a in range(-170, -5, 22))
tl = "".join(f'<rect x="{x}" y="{y}" width="{w}" height="5" rx="2" fill="{N}" opacity=".55"/>' for x in (78, 222) for y, w in ((296, 100), (312, 88), (328, 100), (344, 70), (360, 96)))
C["english_bible"] = svg("#B33B59", f'''
{rays}
<path d="M40 416 V282 Q124 250 200 282 Q276 250 360 282 V416 Q276 388 200 416 Q124 388 40 416Z" fill="{N}"/>
<path d="M56 398 V268 Q130 240 200 270 V402 Q130 374 56 398Z" fill="{V}"/>
<path d="M344 398 V268 Q270 240 200 270 V402 Q270 374 344 398Z" fill="#E4DAC2"/>
{tl}
<path d="M190 270 h20 v170 l-10 -14 l-10 14Z" fill="{G}"/>
<path d="M186 150 h28 v30 h30 v26 h-30 v52 h-28 v-52 h-30 v-26 h30Z" fill="{V}" opacity=".0"/>''')

# 80/20: a few filled, most not
dots = ""
filled = {(0, 0), (1, 0), (0, 1), (1, 1)}
for r in range(4):
    for c in range(5):
        x, y = 84 + c * 58, 96 + r * 58
        if (c, r) in filled:
            dots += f'<circle cx="{x}" cy="{y}" r="22" fill="{G}"/>'
        else:
            dots += f'<circle cx="{x}" cy="{y}" r="19" fill="none" stroke="{V}" stroke-width="4" opacity=".7"/>'
C["pareto"] = svg("#353A8C", f'''
{dots}
<rect x="62" y="356" width="276" height="56" rx="28" fill="none" stroke="{V}" stroke-width="4" opacity=".7"/>
<path d="M90 356 H282 V412 H90 A28 28 0 0 1 90 356Z" fill="{G}"/>
<text x="176" y="396" text-anchor="middle" font-family="DejaVu Serif, serif" font-size="32" font-weight="bold" fill="{N}">80</text>
<text x="308" y="395" text-anchor="middle" font-family="DejaVu Serif, serif" font-size="24" font-weight="bold" fill="{V}">20</text>
<path d="M114 300 Q114 330 140 340" fill="none" stroke="{G}" stroke-width="4" stroke-linecap="round"/><path d="M132 330 l12 11 l-16 4Z" fill="{G}"/>''')

os.makedirs(OUT, exist_ok=True)
for name, src in C.items():
    open(os.path.join(HERE, name + ".svg"), "w").write(src)
    png = cairosvg.svg2png(bytestring=src.encode(), output_width=800, output_height=1000)
    Image.open(io.BytesIO(png)).convert("RGB").save(os.path.join(OUT, f"cover_{name}.webp"), "WEBP", quality=90)
# contact sheet for review
sheet = Image.new("RGB", (4 * 400, 2 * 500))
for i, name in enumerate(C):
    im = Image.open(os.path.join(OUT, f"cover_{name}.webp")).resize((400, 500))
    sheet.paste(im, ((i % 4) * 400, (i // 4) * 500))
sheet.save("/tmp/covers.png")
print("ok", list(C))
