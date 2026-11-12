"""Draws one illustration per story scene (400x300) and renders them to WebP.
Run: python3 art/scenes.py   (needs cairosvg and pillow)
Output name: scene_<story id>_<scene number>.webp, scene numbers counted from 0
(scene 0 is the title scene, which uses the cover)."""
import io, os, math
import cairosvg
from PIL import Image

N, V, G = "#0C1024", "#F0E8D6", "#E0B252"
SUN = "#F2C766"
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "app", "src", "main", "res", "drawable-nodpi")
S = {}

def T(x, y, body, s=1, rot=0):
    return f'<g transform="translate({x} {y}) rotate({rot}) scale({s})">{body}</g>'

def scene(key, bg, body):
    S[key] = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="800" height="600"><rect width="400" height="300" fill="{bg}"/>{body}</svg>'

def sun(x, y, r, c=SUN, o=1): return f'<circle cx="{x}" cy="{y}" r="{r}" fill="{c}" opacity="{o}"/>'
def ground(y, c=N): return f'<rect x="0" y="{y}" width="400" height="{300-y}" fill="{c}"/>'
def hills(y, c, amp=30): return f'<path d="M0 {y} Q100 {y-amp} 200 {y-6} T400 {y-amp//2} V300 H0Z" fill="{c}"/>'
def waves(pts, c=V, o=.55): return "".join(f'<path d="M{x} {y} q10 -8 20 0 t20 0" fill="none" stroke="{c}" stroke-width="3.5" stroke-linecap="round" opacity="{o}"/>' for x, y in pts)

def person(c=V, hat=None, hc=G):
    b = f'<path d="M-18 40 V8 Q0 -14 18 8 V40Z" fill="{c}"/><circle cx="0" cy="-16" r="12" fill="{c}"/>'
    if hat == "mitre": b += f'<path d="M-10 -25 L0 -48 L10 -25Z" fill="{c}"/>'
    if hat == "crown": b += f'<path d="M-13 -25 v-15 l7 8 l6 -11 l6 11 l7 -8 v15Z" fill="{hc}"/>'
    if hat == "helmet": b += f'<path d="M-13 -18 A13 13 0 0 1 13 -18Z" fill="{hc}"/><rect x="-2" y="-40" width="4" height="12" fill="{hc}"/><path d="M-10 -40 h20 l-4 6 h-12Z" fill="#C8443A"/>'
    return b

def ship(hull=N, sail=V):
    return f'<path d="M-54 0 H54 L40 22 H-40Z" fill="{hull}"/><rect x="-2" y="-74" width="4" height="74" fill="{hull}"/><path d="M5 -70 Q50 -40 5 -8Z" fill="{sail}"/><path d="M-5 -62 Q-38 -38 -5 -12Z" fill="{sail}" opacity=".85"/>'

def coin(r=20, c=G, mark=""):
    return f'<circle r="{r}" fill="{c}"/><circle r="{r*0.72}" fill="none" stroke="{N}" stroke-width="{max(1.5, r*0.08)}" opacity=".45"/>{mark}'

def cross(c=G, w=8): return f'<rect x="{-w/2}" y="-30" width="{w}" height="60" rx="2" fill="{c}"/><rect x="-18" y="{-14-w/2}" width="36" height="{w}" rx="2" fill="{c}"/>'

def book(w=60, h=80, c=N, band=G):
    return f'<rect x="{-w/2}" y="{-h/2}" width="{w}" height="{h}" rx="4" fill="{c}"/><rect x="{-w/2+8}" y="{-h/2}" width="5" height="{h}" fill="{band}"/><rect x="{-w/2+20}" y="{-h/2+14}" width="{w-30}" height="6" rx="2" fill="{band}"/>'

def openbook(page=V, cover=N, ink=N):
    ln = "".join(f'<rect x="{x}" y="{y}" width="{w}" height="4" rx="2" fill="{ink}" opacity=".5"/>' for x in (-62, 10) for y, w in ((-22, 50), (-10, 44), (2, 50), (14, 34)))
    return f'<path d="M-80 44 V-30 Q-40 -46 0 -30 Q40 -46 80 -30 V44 Q40 30 0 44 Q-40 30 -80 44Z" fill="{cover}"/><path d="M-72 34 V-38 Q-36 -52 0 -36 V38 Q-36 22 -72 34Z" fill="{page}"/><path d="M72 34 V-38 Q36 -52 0 -36 V38 Q36 22 72 34Z" fill="#E4DAC2"/>{ln}'

def scroll(c=V, ink=N):
    ln = "".join(f'<rect x="-34" y="{y}" width="{w}" height="4" rx="2" fill="{ink}" opacity=".5"/>' for y, w in ((-38, 68), (-26, 56), (-14, 68), (-2, 48), (10, 62), (22, 40)))
    return f'<rect x="-46" y="-56" width="92" height="112" fill="{c}"/><rect x="-54" y="-64" width="108" height="16" rx="8" fill="#DDD2B8"/><rect x="-54" y="48" width="108" height="16" rx="8" fill="#DDD2B8"/>{ln}'

def stele(h=200, c=V, win=N):
    w = "".join(f'<rect x="{x}" y="{-h+34+i*34}" width="9" height="14" rx="1.5" fill="{win}"/>' for i in range(int((h-70)/34)) for x in (-13, 4))
    return f'<path d="M-24 0 L-17 {-h+22} Q0 {-h-8} 17 {-h+22} L24 0Z" fill="{c}"/>{w}<path d="M-8 0 v-22 q8 -9 16 0 v22Z" fill="{win}"/>'

def column(h=150, c=V, sh="#00000022"):
    fl = "".join(f'<rect x="{-13+i*9}" y="{-h+20}" width="2.5" height="{h-30}" fill="{sh}"/>' for i in range(4))
    return f'<rect x="-26" y="{-h}" width="52" height="11" rx="2" fill="{c}"/><rect x="-19" y="{-h+11}" width="38" height="{h-21}" fill="{c}"/>{fl}<rect x="-26" y="-10" width="52" height="10" rx="2" fill="{c}"/>'

def stump(c=V): return f'<rect x="-19" y="-52" width="38" height="42" fill="{c}"/><path d="M-19 -52 l9 -11 l8 9 l9 -14 l12 16Z" fill="{c}"/><rect x="-26" y="-10" width="52" height="10" rx="2" fill="{c}"/>'

def arch(w=80, h=120, c=N): return f'<path d="M{-w/2} 0 V{-h+w/2} A{w/2} {w/2} 0 0 1 {w/2} {-h+w/2} V0Z" fill="{c}"/>'
def house(c=V, roof="#C8443A", w=30, h=22): return f'<rect x="{-w/2}" y="{-h}" width="{w}" height="{h}" fill="{c}"/><path d="M{-w/2-4} {-h} L0 {-h-14} L{w/2+4} {-h}Z" fill="{roof}"/>'
def pyramid(c=V, c2="#DDD2B8", stairs=G):
    t = "".join(f'<rect x="{-66+i*16}" y="{-24-i*24}" width="{132-i*32}" height="24" fill="{c if i%2==0 else c2}"/>' for i in range(3))
    return f'{t}<rect x="-12" y="-72" width="24" height="72" fill="{stairs}"/><rect x="-30" y="-100" width="22" height="28" fill="#C8443A"/><rect x="8" y="-100" width="22" height="28" fill="#2B4BA6"/><rect x="-34" y="-106" width="30" height="8" fill="{N}"/><rect x="4" y="-106" width="30" height="8" fill="{N}"/>'
def cactus(c="#12584C"): return f'<ellipse cx="0" cy="-22" rx="14" ry="22" fill="{c}"/><ellipse cx="-20" cy="-40" rx="10" ry="15" fill="{c}" transform="rotate(-24 -20 -40)"/><ellipse cx="20" cy="-44" rx="10" ry="16" fill="{c}" transform="rotate(22 20 -44)"/><circle cx="-24" cy="-56" r="4" fill="#C8443A"/><circle cx="26" cy="-61" r="4" fill="#C8443A"/>'
def canoe(c=N): return f'<path d="M-30 0 q30 14 60 0 l-8 10 h-44Z" fill="{c}"/><rect x="-2" y="-24" width="3.5" height="26" fill="{c}"/><circle cx="0" cy="-28" r="5" fill="{c}"/>'
def gear(r=30, n=8, c=G, hole=N):
    teeth = "".join(f'<rect x="{-r*0.16}" y="{-r*1.22}" width="{r*0.32}" height="{r*0.36}" rx="2" fill="{c}" transform="rotate({i*360/n})"/>' for i in range(n))
    return f'{teeth}<circle r="{r}" fill="{c}"/><circle r="{r*0.38}" fill="{hole}"/>'
def flame(c="#F2C766", c2="#EC8A62"): return f'<path d="M0 0 C-18 -10 -12 -34 0 -50 C12 -34 18 -10 0 0Z" fill="{c2}"/><path d="M0 -2 C-9 -8 -6 -22 0 -30 C6 -22 9 -8 0 -2Z" fill="{c}"/>'
def sword(c=V, hilt=G): return f'<path d="M-4 -60 L0 -72 L4 -60 V6 H-4Z" fill="{c}"/><rect x="-14" y="6" width="28" height="6" rx="2" fill="{hilt}"/><rect x="-3" y="12" width="6" height="18" fill="{hilt}"/>'
def sheet(c=V, ink=N, w=44, h=58):
    ln = "".join(f'<rect x="{-w/2+7}" y="{-h/2+10+i*10}" width="{(w-14)*(1 if i%2==0 else .75)}" height="3.5" rx="1.5" fill="{ink}" opacity=".5"/>' for i in range(4))
    return f'<rect x="{-w/2}" y="{-h/2}" width="{w}" height="{h}" rx="2" fill="{c}"/>{ln}'
def block(c=N, glyph="", w=56): return f'<rect x="{-w/2}" y="{-w/2}" width="{w}" height="{w}" rx="6" fill="{c}"/><rect x="{-w/2}" y="{w/2-8}" width="{w}" height="8" rx="3" fill="#00000033"/>{glyph}'
def plant(h=40, c="#1E8573", c2="#27A58E"): return f'<path d="M0 0 V{-h}" stroke="{c}" stroke-width="5" stroke-linecap="round"/><path d="M0 {-h*0.6} q-26 -4 -30 -30 q28 0 30 30Z" fill="{c}"/><path d="M0 {-h} q26 -6 32 -34 q-30 2 -32 34Z" fill="{c2}"/>'
def txt(x, y, s, size=30, c=V, anchor="middle"): return f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-family="DejaVu Serif, serif" font-size="{size}" font-weight="bold" fill="{c}">{s}</text>'

# ---------- Aksum ----------
A, A2 = "#D9733C", "#A84E26"
thr = "".join(T(62 + i * 92, 236, arch(70, 150, N if i < 3 else "#3A1D10") + T(0, -46, person(V if i < 3 else G, "crown", G if i < 3 else V), 1.25)) for i in range(4))
scene("aksum_1", A, sun(200, 70, 150, SUN, .25) + thr + ground(236, A2) + "".join(f'<rect x="{28+i*92}" y="244" width="68" height="6" rx="3" fill="{N}" opacity=".35"/>' for i in range(4)))
scene("aksum_2", A, sun(318, 78, 46) + f'<rect x="0" y="176" width="400" height="124" fill="#1F93A8"/>' + waves([(30, 206), (250, 196), (300, 250), (150, 270)]) + T(238, 196, ship(), 1.25)
      + f'<path d="M0 300 V210 H128 V300Z" fill="{N}"/><rect x="0" y="200" width="136" height="12" rx="3" fill="{A2}"/>'
      + f'<rect x="20" y="158" width="40" height="42" rx="3" fill="{V}"/><rect x="66" y="170" width="34" height="30" rx="3" fill="{G}"/><path d="M96 200 q26 -44 6 -78 q34 26 16 78Z" fill="{V}"/>')
scene("aksum_3", A, sun(200, 150, 190, SUN, .22) + T(170, 150, coin(84, G, T(0, 0, cross(N, 13), 1.5))) + T(292, 206, coin(40, SUN, T(0, 0, cross(N, 9), .7))) + T(318, 110, coin(26, V)) + T(66, 228, coin(30, V)))
scene("aksum_4", A, sun(92, 84, 52) + hills(262, A2, 26) + T(214, 268, stele(238)) + T(292, 268, person(N), .5)
      + f'<path d="M330 34 V268" stroke="{N}" stroke-width="3" stroke-dasharray="3 9" stroke-linecap="round"/><path d="M320 34 h20 M320 268 h20" stroke="{N}" stroke-width="3" stroke-linecap="round"/>' + ground(268))
scene("aksum_5", A2, f'<path d="M70 196 A130 130 0 0 1 330 196Z" fill="{SUN}"/><rect x="0" y="196" width="400" height="104" fill="#17748A"/>' + waves([(40, 222), (230, 236), (130, 270), (310, 268)], V, .4)
      + T(300, 190, ship(N, N), .42) + f'<path d="M0 300 V216 Q70 196 150 230 V300Z" fill="{N}"/>' + T(64, 224, stele(170, N, A2), .9) + T(112, 236, stele(120, N, A2), .7))

# ---------- Rome ----------
R, R2 = "#6F47AE", "#4E2F84"
scene("rome_1", R, f'<circle cx="200" cy="150" r="118" fill="{N}"/><path d="M200 32 A118 118 0 0 0 200 268 L188 232 L210 204 L186 168 L212 134 L190 98 L208 64Z" fill="{V}"/><path d="M200 32 A118 118 0 0 1 200 268 L188 232 L210 204 L186 168 L212 134 L190 98 L208 64Z" fill="{G}" transform="translate(16 0)"/>'
      + T(134, 150, column(84, R), .9) + T(282, 138, f'<path d="M-30 30 V-4 A30 30 0 0 1 30 -4 V30Z" fill="{R}"/><rect x="-3" y="-52" width="6" height="20" fill="{R}"/>'))
wall = f'<rect x="0" y="120" width="400" height="126" fill="{V}"/>' + "".join(f'<rect x="{i*44}" y="100" width="26" height="22" fill="{V}"/>' for i in range(10)) + T(200, 246, arch(84, 104, N))
scene("rome_2", R, sun(330, 60, 40, "#EC8A62") + wall + T(200, 232, flame(), 1.3) + T(60, 108, flame(), .9) + T(330, 108, flame(), .8)
      + "".join(T(x, 232, person(N, "helmet", N), 1.35) + f'<rect x="{x+28}" y="120" width="4.5" height="140" fill="{N}"/><path d="M{x+22} 122 l8 -22 l8 22Z" fill="{N}"/>' for x in (56, 118, 282, 344)) + ground(256, R2))
cells = [(110, 90), (290, 90), (110, 212), (290, 212)]
scene("rome_3", R, "".join(f'<rect x="{x-78}" y="{y-52}" width="156" height="104" rx="16" fill="{R2}"/>' for x, y in cells)
      + T(110, 100, T(-10, 16, sword(), .78, -32) + T(10, 16, sword(), .78, 32))
      + T(290, 90, coin(34, G) + f'<path d="M-6 -34 L6 -10 L-8 6 L8 34" stroke="{R2}" stroke-width="6" fill="none"/>')
      + T(110, 218, f'<path d="M-30 -28 H30 V4 Q30 30 0 40 Q-30 30 -30 4Z" fill="{V}"/><path d="M0 -28 V40" stroke="#C8443A" stroke-width="8"/><path d="M-30 -4 H30" stroke="#C8443A" stroke-width="8"/>', .9)
      + T(290, 240, "".join(f'<rect x="{-50+i*26}" y="{-22 - (16 if i in (1,2) else 0) - (16 if i==1 else 0)}" width="24" height="{22 + (16 if i in (1,2) else 0) + (16 if i==1 else 0)}" fill="{V}"/>' for i in range(4)) + f'<rect x="20" y="-8" width="20" height="8" fill="{V}" transform="rotate(24 30 -4)"/>'))
scene("rome_4", R, f'<rect x="200" y="0" width="200" height="300" fill="#2B4BA6"/>' + f'<path d="M30 236 A70 70 0 0 1 170 236Z" fill="#EC8A62"/>' + T(66, 236, stump()) + T(128, 236, column(120), .9)
      + sun(332, 62, 26, SUN) + f'<rect x="232" y="170" width="136" height="66" fill="{V}"/><path d="M252 170 A48 48 0 0 1 348 170Z" fill="{G}"/><rect x="297" y="104" width="6" height="22" fill="{G}"/><rect x="224" y="130" width="14" height="106" fill="{V}"/><rect x="362" y="130" width="14" height="106" fill="{V}"/><path d="M222 130 l9 -20 l9 20Z M360 130 l9 -20 l9 20Z" fill="{G}"/>'
      + T(300, 236, arch(30, 44, N)) + ground(236) + txt(100, 280, "476", 26) + txt(300, 280, "1453", 26))

# ---------- Nicaea ----------
Nb, Nb2 = "#2B4BA6", "#1C3276"
scene("nicaea_1", Nb, sun(200, 120, 150, SUN, .14) + T(104, 236, person(V, "mitre"), 2.1) + T(296, 236, person(G, "mitre"), 2.1)
      + f'<path d="M150 70 h44 a12 12 0 0 1 12 12 v26 a12 12 0 0 1 -12 12 h-22 l-14 16 v-16 h-8 a12 12 0 0 1 -12 -12 v-26 a12 12 0 0 1 12 -12Z" fill="{N}"/>' + txt(172, 108, "?", 34, V)
      + f'<path d="M206 150 h44 a12 12 0 0 1 12 12 v26 a12 12 0 0 1 -12 12 h-8 v16 l-14 -16 h-22 a12 12 0 0 1 -12 -12 v-26 a12 12 0 0 1 12 -12Z" fill="{V}"/>' + txt(228, 188, "!", 34, N) + ground(284, Nb2))
rows = ""
palette = [V, G, "#9DB4F5"]
for r in range(3):
    for c in range(9 - r):
        rows += T(40 + c * 40 + r * 20, 250 - r * 52, person(palette[(c + r) % 3], "mitre"), .72)
scene("nicaea_2", Nb, sun(200, 70, 120, SUN, .18) + T(200, 104, person(G, "crown", V), 1.2) + rows + ground(278, Nb2))
scene("nicaea_3", Nb, sun(200, 150, 170, SUN, .2) + T(200, 150, scroll(), 1.55) + T(262, 226, f'<circle r="26" fill="#C8443A"/>' + T(0, 0, cross(V, 6), .55)) + f'<path d="M250 244 l-8 40 l16 -10 l14 12 l-4 -40Z" fill="#C8443A"/>')
scene("nicaea_4", Nb, "".join(T(x, 250, arch(w, h, N) + f'<path d="M{-w/2} {-h+w/2} A{w/2} {w/2} 0 0 1 {w/2} {-h+w/2}" fill="none" stroke="{V}" stroke-width="7"/>' + T(0, -h * .55, cross(G, 8), s)) for x, w, h, s in ((76, 70, 120, .7), (190, 92, 168, .95), (318, 116, 214, 1.2))) + ground(250, Nb2)
      + T(318, 232, openbook(), .34))

# ---------- Printing press ----------
P, P2 = "#1E8573", "#12584C"
gl1 = f'<path d="M-14 -14 H14 M0 -14 V16 M-12 2 H12 M-14 16 H14" stroke="{V}" stroke-width="5" stroke-linecap="round" fill="none"/>'
gl2 = f'<circle cx="0" cy="-6" r="10" fill="none" stroke="{N}" stroke-width="5"/><path d="M-14 14 H14 M0 4 V14" stroke="{N}" stroke-width="5" stroke-linecap="round"/>'
scene("press_1", P, f'<rect x="0" y="206" width="400" height="94" fill="{P2}"/>' + T(84, 160, block("#C8643A", gl1, 84)) + T(200, 150, block(G, gl2, 96)) + T(322, 138, block(N, txt(0, 22, "A", 62, V), 108))
      + f'<path d="M132 160 h14 M254 150 h10" stroke="{V}" stroke-width="5" stroke-linecap="round" stroke-dasharray="1 9"/>')
scene("press_2", P, f'<rect x="0" y="216" width="400" height="84" fill="{P2}"/>'
      + T(74, 160, f'<rect x="-34" y="-50" width="68" height="100" rx="6" fill="{N}"/><rect x="-18" y="-50" width="36" height="62" fill="{G}"/><rect x="-10" y="-64" width="20" height="18" fill="{G}"/>')
      + T(200, 150, f'<rect x="-7" y="-70" width="14" height="60" rx="5" fill="{V}"/><circle cx="0" cy="22" r="40" fill="{N}"/><path d="M36 60 q10 16 0 26 q-10 -10 0 -26Z" fill="{N}"/>')
      + T(322, 214, f'<rect x="-52" y="-150" width="12" height="150" fill="{N}"/><rect x="40" y="-150" width="12" height="150" fill="{N}"/><rect x="-58" y="-156" width="116" height="18" rx="4" fill="{N}"/>' + "".join(f'<rect x="{-9-(i%2)*3}" y="{-134+i*10}" width="{18+(i%2)*6}" height="7" rx="3" fill="{G}"/>' for i in range(6)) + f'<rect x="-36" y="-72" width="72" height="16" rx="3" fill="{N}"/><rect x="-28" y="-44" width="56" height="34" fill="{V}"/>'))
stack = "".join(T(124 + (i % 2) * 8, 232 - i * 30, f'<rect x="-70" y="-13" width="140" height="26" rx="4" fill="{[N, "#C8443A", G, N, "#2B4BA6"][i]}"/><rect x="-62" y="-8" width="124" height="16" fill="{V}" opacity=".9"/><rect x="-70" y="-13" width="16" height="26" rx="4" fill="{[N, "#C8443A", G, N, "#2B4BA6"][i]}"/>') for i in range(5))
scene("press_3", P, f'<rect x="0" y="246" width="400" height="54" fill="{P2}"/>' + stack + T(292, 206, openbook(), .82) + sun(300, 80, 34, SUN, .9))
fly = "".join(T(x, y, sheet(), s, r) for x, y, s, r in ((196, 150, .9, -14), (246, 96, .75, 18), (300, 166, .7, -26), (340, 84, .55, 10), (360, 216, .5, 30), (268, 236, .6, -8)))
scene("press_4", P, T(92, 262, f'<rect x="-64" y="-210" width="128" height="210" rx="6" fill="{N}"/><path d="M-64 -150 H64 M-64 -70 H64" stroke="{P2}" stroke-width="6"/><circle cx="40" cy="-100" r="7" fill="{G}"/>') + T(84, 130, sheet(V, N, 60, 80)) + f'<circle cx="84" cy="96" r="5" fill="#C8443A"/>' + fly + ground(262, P2))

# ---------- Compound interest ----------
C1, C2 = "#E3B24C", "#B98626"
scene("compound_1", C1, sun(320, 70, 44, V, .5) + f'<path d="M200 260 V96 M200 190 Q150 180 126 136 M200 160 Q250 146 280 104 M200 128 Q170 112 160 76" stroke="{N}" stroke-width="7" fill="none" stroke-linecap="round"/>'
      + T(200, 84, coin(26, N, txt(0, 10, "$", 28, G))) + T(122, 126, coin(22, N, txt(0, 8, "$", 22, G))) + T(284, 96, coin(20, N, txt(0, 8, "$", 20, G))) + T(158, 66, coin(15, N)) + T(244, 150, coin(13, N)) + T(100, 180, coin(11, N))
      + f'<path d="M150 260 Q200 236 250 260Z" fill="{C2}"/>' + ground(260))
scene("compound_2", C1, sun(64, 64, 34, V, .55) + T(80, 250, plant(34), 1) + T(200, 250, plant(60), 1.5)
      + f'<rect x="310" y="130" width="14" height="120" fill="{N}"/><circle cx="317" cy="110" r="56" fill="#1E8573"/><circle cx="282" cy="140" r="34" fill="#27A58E"/><circle cx="350" cy="136" r="30" fill="#1E8573"/>'
      + T(300, 100, coin(11, G)) + T(336, 86, coin(11, G)) + T(322, 130, coin(11, G)) + T(280, 146, coin(9, G)) + T(352, 140, coin(9, G))
      + f'<path d="M110 232 h50 M236 232 h40" stroke="{N}" stroke-width="4" stroke-linecap="round" stroke-dasharray="1 10"/>' + ground(250))
def jar(n):
    coins = "".join(T(-24 + (i % 3) * 24 + (i // 3 % 2) * 6, 34 - (i // 3) * 20, coin(13, G)) for i in range(n))
    return f'<path d="M-50 -54 H50 V50 Q50 62 38 62 H-38 Q-50 62 -50 50Z" fill="{V}" opacity=".55"/><rect x="-56" y="-66" width="112" height="14" rx="6" fill="{N}"/>{coins}'
scene("compound_3", C1, T(110, 170, jar(4), 1.2) + T(290, 170, jar(15), 1.2) + T(110, 64, coin(13, N)) + f'<path d="M110 92 v-16 M102 82 l8 -10 l8 10" stroke="{N}" stroke-width="4" fill="none" stroke-linecap="round"/>' + T(272, 64, coin(12, G)) + T(300, 54, coin(12, G)) + T(318, 76, coin(12, G)) + ground(256))
scene("compound_4", C1, T(140, 150, f'<circle r="92" fill="{N}"/><circle r="78" fill="{V}"/>' + "".join(f'<rect x="-2.5" y="-74" width="5" height="12" fill="{N}" transform="rotate({i*30})"/>' for i in range(12)) + f'<path d="M0 0 V-56 M0 0 L38 22" stroke="{N}" stroke-width="7" stroke-linecap="round"/><circle r="8" fill="#C8443A"/>')
      + T(300, 96, coin(26, N, txt(0, 10, "$", 28, G))) + f'<path d="M300 132 v26 M290 150 l10 12 l10 -12" stroke="{N}" stroke-width="5" fill="none" stroke-linecap="round"/>' + T(274, 206, coin(26, N, txt(0, 10, "$", 28, G))) + T(326, 206, coin(26, N, txt(0, 10, "$", 28, G))))
scene("compound_5", C2, f'<path d="M0 90 L400 250 V300 H0Z" fill="{N}"/>' + "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{V}"/><circle cx="{x}" cy="{y}" r="{r*.6}" fill="none" stroke="#C8443A" stroke-width="{r*.14}" stroke-dasharray="{r*.3} {r*.3}"/>' for x, y, r in ((66, 96, 16), (160, 122, 30), (290, 148, 56)))
      + f'<path d="M96 76 q20 -14 36 4 M206 80 q34 -22 60 6" stroke="{N}" stroke-width="4" fill="none" stroke-linecap="round" stroke-dasharray="1 9"/>')

# ---------- Tenochtitlan ----------
Tn, Tn2 = "#1F93A8", "#17748A"
mount = f'<path d="M0 150 L60 96 L110 136 L170 82 L236 142 L300 104 L400 160 V190 H0Z" fill="{Tn2}"/>'
scene("tenochtitlan_1", Tn, sun(316, 64, 40) + mount + waves([(30, 214), (300, 208), (60, 268), (290, 264)]) + f'<ellipse cx="200" cy="236" rx="110" ry="26" fill="#12584C"/><path d="M168 232 q32 -34 64 0Z" fill="#6B5B45"/>' + T(200, 214, cactus(), 1.5)
      + f'<path d="M176 118 q24 -18 26 6 q20 -26 40 -8 q-22 0 -30 16 q-16 4 -36 -14Z" fill="{N}"/><path d="M200 130 l-4 14 l10 -8Z" fill="{G}"/>' + T(96, 236, house(V, "#B98626", 22, 14)) + T(304, 240, house(V, "#B98626", 22, 14)))
scene("tenochtitlan_2", Tn, f'<path d="M0 0 H400 V40 Q300 70 200 44 Q90 20 0 60Z" fill="#12584C"/><path d="M0 300 V250 Q110 226 200 262 Q300 290 400 244 V300Z" fill="#12584C"/>'
      + f'<path d="M200 150 V36 M200 150 V266 M200 150 H8" stroke="{V}" stroke-width="9"/><path d="M200 150 L392 96" stroke="{G}" stroke-width="5" stroke-dasharray="10 7"/>'
      + f'<circle cx="200" cy="150" r="62" fill="#DDD2B8"/><path d="M146 150 H254 M200 96 V204 M162 112 L238 188 M238 112 L162 188" stroke="{Tn}" stroke-width="5"/><rect x="182" y="132" width="36" height="36" fill="{G}"/><rect x="190" y="140" width="20" height="20" fill="#C8443A"/>'
      + T(90, 96, canoe(), .7, -10) + T(310, 210, canoe(), .7, 14) + waves([(40, 196), (300, 150)]))
beds = "".join(f'<rect x="{x}" y="{y}" width="92" height="50" rx="5" fill="#6B5B45"/><rect x="{x+5}" y="{y+5}" width="82" height="40" rx="3" fill="#12584C"/>' + "".join(f'<circle cx="{x+16+i*20}" cy="{y+25}" r="6" fill="{[G, "#27A58E", "#C8443A", "#27A58E"][i]}"/>' for i in range(4)) for x, y in ((24, 70), (154, 70), (284, 70), (24, 196), (284, 196)))
scene("tenochtitlan_3", Tn, beds + waves([(140, 152), (40, 152), (300, 160), (170, 270)]) + T(204, 226, canoe(), 1.5) + T(232, 214, coin(9, G)) + T(184, 216, coin(7, "#27A58E")))
city = "".join(T(24 + i * 32 + (j % 2) * 14, 214 + j * 30, house(V if (i + j) % 3 else "#DDD2B8", ["#C8443A", "#B98626", "#2B4BA6"][(i + j) % 3], 22, 15)) for j in range(3) for i in range(12) if not (140 < 24 + i * 32 + (j % 2) * 14 < 260 and j == 0))
scene("tenochtitlan_4", Tn, sun(70, 56, 30) + mount + f'<rect x="0" y="186" width="400" height="114" fill="#12584C"/>' + T(200, 214, pyramid(), 1.15) + city)
scene("tenochtitlan_5", "#17748A", f'<path d="M210 70 q30 -30 10 -64 M250 80 q40 -22 34 -70" stroke="{N}" stroke-width="16" fill="none" stroke-linecap="round" opacity=".5"/>' + f'<rect x="0" y="206" width="400" height="94" fill="{Tn}"/>' + waves([(20, 236), (150, 270), (330, 262)])
      + f'<ellipse cx="282" cy="210" rx="110" ry="18" fill="#12584C"/>' + T(282, 206, pyramid(), .95) + T(232, 100, flame(), 1.1) + T(104, 226, ship(N, V), 1.25) + T(112, 172, cross("#C8443A", 6), .5))

# ---------- English Bible ----------
B, B2 = "#B33B59", "#7E2540"
scene("english_bible_1", B, f'<rect x="0" y="226" width="400" height="74" fill="{B2}"/>' + T(76, 180, book(64, 92, N, V)) + f'<path d="M120 110 q-30 30 -44 90 l8 2 q10 -50 44 -88Z" fill="{V}"/>'
      + T(196, 190, book(52, 72, "#2B4BA6", G)) + T(316, 162, book(96, 128, N, G) + T(6, 20, cross(G, 7), .7)) + f'<path d="M293 70 v-22 l10 11 l13 -16 l13 16 l10 -11 v22Z" fill="{G}"/>')
scene("english_bible_2", "#17748A", sun(330, 60, 34) + f'<rect x="0" y="200" width="400" height="100" fill="#1F93A8"/>' + waves([(20, 226), (150, 262), (320, 250)]) + T(290, 216, ship(N, V), 1.1)
      + f'<path d="M0 300 V222 H170 V300Z" fill="{N}"/>' + f'<rect x="34" y="132" width="104" height="90" rx="14" fill="{V}"/><path d="M34 160 H138 M34 194 H138" stroke="#B98626" stroke-width="6"/><path d="M70 132 V222 M104 132 V222" stroke="#DDD2B8" stroke-width="3"/>'
      + T(96, 124, book(40, 54, B, G), 1, -16))
scene("english_bible_3", N, sun(200, 120, 150, B, .35) + f'<rect x="92" y="236" width="216" height="12" rx="4" fill="{V}"/><rect x="110" y="248" width="12" height="52" fill="{V}"/><rect x="278" y="248" width="12" height="52" fill="{V}"/>'
      + T(150, 222, openbook(), .62) + f'<rect x="246" y="170" width="16" height="66" rx="3" fill="{V}"/><rect x="240" y="232" width="28" height="6" rx="2" fill="{G}"/><path d="M254 168 q-16 -24 2 -44 q-4 22 10 30 q-14 6 -4 34" stroke="{V}" stroke-width="4" fill="none" stroke-linecap="round" opacity=".6"/>'
      + f'<path d="M206 150 q-22 34 -30 78 l7 2 q8 -40 30 -76Z" fill="{G}"/>')
scene("english_bible_4", B, "".join(f'<path d="M200 190 L{200+400*math.cos(math.radians(a-6))} {190+400*math.sin(math.radians(a-6))} L{200+400*math.cos(math.radians(a+6))} {190+400*math.sin(math.radians(a+6))}Z" fill="{SUN}" opacity=".3"/>' for a in range(-168, -6, 26))
      + T(200, 196, openbook(), 1.5) + f'<path d="M166 96 v-34 l15 16 l19 -24 l19 24 l15 -16 v34Z" fill="{G}"/><rect x="166" y="96" width="68" height="10" rx="3" fill="{G}"/>' + txt(200, 286, "1611", 26, V))

# ---------- Pareto ----------
X, X2 = "#353A8C", "#232762"
parc = "".join(f'<rect x="{150+c*46}" y="{38+r*46}" width="42" height="42" rx="5" fill="{G if (r*5+c) < 20 else "none"}" stroke="{V}" stroke-width="{0 if (r*5+c) < 20 else 3}" opacity="{1 if (r*5+c) < 20 else .7}"/>' for r in range(5) for c in range(5))
scene("pareto_1", X, parc + T(70, 96, person(G, "crown", V), 1.5) + "".join(T(30 + (i % 2) * 44 + 20, 196 + (i // 2) * 52, person(V), .7) for i in range(4)))
gears = T(132, 150, gear(72, 10, G, X)) + "".join(T(x, y, gear(r, 8, "none", X).replace('fill="none"', f'fill="{X}" stroke="{V}" stroke-width="3" opacity=".75"')) for x, y, r in ((262, 86, 26), (316, 150, 22), (268, 214, 28), (346, 226, 16), (340, 70, 15)))
scene("pareto_2", X, f'<rect x="0" y="262" width="400" height="38" fill="{X2}"/>' + gears)
dots = "".join(f'<circle cx="{40+c*40}" cy="{50+r*40}" r="{12 if (c, r) in ((3, 2), (4, 3), (4, 2)) else 9}" fill="{G if (c, r) in ((3, 2), (4, 3), (4, 2)) else "none"}" stroke="{V}" stroke-width="{0 if (c, r) in ((3, 2), (4, 3), (4, 2)) else 3}" opacity="{1 if (c, r) in ((3, 2), (4, 3), (4, 2)) else .6}"/>' for r in range(6) for c in range(9))
scene("pareto_3", X, dots + f'<circle cx="182" cy="146" r="74" fill="{V}" opacity=".16"/><circle cx="182" cy="146" r="74" fill="none" stroke="{V}" stroke-width="10"/><path d="M236 200 L300 264" stroke="{V}" stroke-width="18" stroke-linecap="round"/>')
scene("pareto_4", X, f'<rect x="194" y="70" width="12" height="176" fill="{V}"/><path d="M150 262 H250 L230 244 H170Z" fill="{V}"/><circle cx="200" cy="70" r="12" fill="{G}"/>'
      + T(200, 82, f'<rect x="-130" y="-5" width="260" height="10" rx="5" fill="{V}"/><path d="M-120 0 V70 M120 0 V40" stroke="{V}" stroke-width="3"/><path d="M-164 70 H-76 Q-120 104 -164 70Z" fill="{V}"/><path d="M76 40 H164 Q120 74 76 40Z" fill="{V}"/>' + T(-120, 46, coin(22, G)) + "".join(T(96 + i * 16, 30 - (i % 2) * 14, f'<circle r="8" fill="none" stroke="{V}" stroke-width="3"/>') for i in range(4)), 1, -9)
      + f'<rect x="0" y="262" width="400" height="38" fill="{X2}"/>')

os.makedirs(OUT, exist_ok=True)
names = list(S)
for name in names:
    src = S[name]
    open(os.path.join(HERE, name + ".svg"), "w").write(src)
    png = cairosvg.svg2png(bytestring=src.encode(), output_width=800, output_height=600)
    Image.open(io.BytesIO(png)).convert("RGB").save(os.path.join(OUT, f"scene_{name}.webp"), "WEBP", quality=88)
for part, chunk in enumerate([names[:18], names[18:]]):
    sheet_img = Image.new("RGB", (6 * 400, 3 * 300), "#000")
    for i, name in enumerate(chunk):
        im = Image.open(os.path.join(OUT, f"scene_{name}.webp")).resize((400, 300))
        sheet_img.paste(im, ((i % 6) * 400, (i // 6) * 300))
    sheet_img.save(f"/tmp/scenes{part}.png")
print(len(names))
