#!/usr/bin/env python3
import os

T = 25.0
GAP = 0.5
TAPER = 0.20

def area(pts):
    s = 0.0
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        s += x1 * y2 - x2 * y1
    return s / 2.0

def orient(pts, positive=True):
    a = area(pts)
    if (a > 0) != positive:
        return list(reversed(pts))
    return list(pts)

def sub(pts):
    d = "M %.1f %.1f " % (pts[0][0], pts[0][1])
    for p in pts[1:]:
        d += "L %.1f %.1f " % (p[0], p[1])
    return d + "Z "

# ---------- glyphs: (width, [outer, holes...]) ----------

def D():
    outer = [(25, 0), (50, 0), (72, 22), (72, 78), (50, 100), (30, 100),
             (-66, 280), (7, 100), (0, 100), (0, 0), (3, -112)]
    hole = [(25, 25), (40, 25), (48, 34), (48, 66), (40, 75), (25, 75)]
    return 72, [outer, hole]

def E():
    outer = [(0, 0), (58, 0), (40, 25), (25, 25), (25, 38), (46, 38),
             (33, 62), (25, 62), (25, 75), (58, 75), (40, 100), (0, 100)]
    return 58, [outer]

def V():
    outer = [(0, 0), (26, 0), (36, 82), (46, 0), (72, 0), (36, 100)]
    return 72, [outer]

def L():
    outer = [(0, 0), (25, 0), (25, 75), (56, 75), (38, 100), (0, 100)]
    return 56, [outer]

def O():
    outer = [(0, 20), (20, 0), (54, 0), (74, 20), (74, 80), (54, 100), (20, 100), (0, 80)]
    hole = [(25, 28), (32, 22), (42, 22), (49, 28), (49, 72), (42, 78), (32, 78), (25, 72)]
    return 74, [outer, hole]

def P():
    outer = [(0, 0), (44, 0), (68, 25), (68, 52), (44, 75), (25, 75), (25, 100), (0, 100)]
    hole = [(25, 25), (37, 25), (43, 31), (43, 46), (37, 52), (25, 52)]
    return 68, [outer, hole]

def R():
    outer = [(0, 0), (44, 0), (68, 25), (68, 48), (52, 65), (68, 100),
             (41, 100), (30, 74), (25, 74), (25, 100), (0, 100)]
    hole = [(25, 25), (37, 25), (43, 31), (43, 42), (37, 48), (25, 48)]
    return 68, [outer, hole]

def Z():
    outer = [(0, 0), (64, 0), (161, -122), (83, 0), (66, 25), (46, 75),
             (66, 75), (66, 100), (93, 300), (28, 100), (0, 100), (0, 75),
             (20, 75), (41, 25), (0, 25)]
    return 66, [outer]

WORD = [D(), E(), V(), E(), L(), O(), P(), E(), R(), Z()]

def taper(contours, w):
    cx = w / 2.0
    out = []
    for c in contours:
        nc = []
        for x, y in c:
            t = min(max(y, 0.0), 100.0) / 100.0
            nc.append((cx + (x - cx) * (1.0 - TAPER * t), y))
        out.append(nc)
    return out

WORD = [(w, taper(cs, w)) for w, cs in WORD]

x = 0.0
placed = []
for w, contours in WORD:
    placed.append((x, contours))
    x += w + GAP
total_w = x - GAP

xs, ys = [], []
for ox, contours in placed:
    for c in contours:
        for px, py in c:
            xs.append(px + ox)
            ys.append(py)
xmin, xmax = min(xs), max(xs)
ymin, ymax = min(ys), max(ys)
bw, bh = xmax - xmin, ymax - ymin
print("units bbox", round(xmin, 1), round(xmax, 1), round(ymin, 1), round(ymax, 1),
      "size", round(bw, 1), round(bh, 1), "word", round(total_w, 1))

SCALE = 1.45
PAD = 44
W = bw * SCALE + PAD * 2
H = bh * SCALE + PAD * 2
print("canvas", round(W), round(H))

def build_path():
    out = ""
    for ox, contours in placed:
        for i, c in enumerate(contours):
            pts = [(px + ox, py) for px, py in c]
            pts = orient(pts, positive=(i == 0))
            out += sub(pts)
    return out.strip()

PATH = build_path()

tx = PAD - xmin * SCALE
ty = PAD - ymin * SCALE

def svg(bg, fg, name):
    body = ('<svg xmlns="http://www.w3.org/2000/svg" width="%.0f" height="%.0f" '
            'viewBox="0 0 %.0f %.0f">\n'
            '<rect x="0" y="0" width="%.0f" height="%.0f" fill="%s"/>\n'
            '<g transform="translate(%.2f,%.2f) scale(%s)">\n'
            '<path fill="%s" d="%s"/>\n</g>\n</svg>\n'
            % (W, H, W, H, W, H, bg, tx, ty, SCALE, fg, PATH))
    with open(name, "w") as f:
        f.write(body)

here = os.path.dirname(os.path.abspath(__file__))
svg("#0B0B0E", "#FFFFFF", os.path.join(here, "developerz-dark.svg"))
svg("#FFFFFF", "#0B0B0E", os.path.join(here, "developerz-light.svg"))


def svg_mono(fg, name):
    body = ('<svg xmlns="http://www.w3.org/2000/svg" width="%.0f" height="%.0f" '
            'viewBox="0 0 %.0f %.0f">\n'
            '<g transform="translate(%.2f,%.2f) scale(%s)">\n'
            '<path fill="%s" d="%s"/>\n</g>\n</svg>\n'
            % (W, H, W, H, tx, ty, SCALE, fg, PATH))
    with open(name, "w") as f:
        f.write(body)


svg_mono("#FFFFFF", os.path.join(here, "developerz-mono.svg"))
print("ok")
