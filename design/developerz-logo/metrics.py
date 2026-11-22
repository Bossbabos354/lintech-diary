#!/usr/bin/env python3
"""DeveloperZ logo tracing the Metallica-style letter anatomy.

The letterforms are reconstructed from public logos, not copied from the
reference image. Glyphs follow the originals: heavy slab-square capitals,
compact width, steep top terminals, sharp wedge serifs, and long blade
spikes on D and Z that run past the baseline/cap line.
"""

import math
import os

from PIL import ImageFont

FONT_BOLD = "/usr/share/fonts/truetype/croscore/Arimo-Bold.ttf"
SIZE = 1000

_cap = ImageFont.truetype(FONT_BOLD, SIZE)
CAP = _cap.getbbox("H")[3] - _cap.getbbox("H")[1]
THICK = (_cap.getbbox("I")[2] - _cap.getbbox("I")[0]) * SIZE / SIZE


def adv(ch):
    return _cap.getlength(ch) * 1000.0 / SIZE


def thick(ch):
    b = _cap.getbbox(ch)
    return (b[2] - b[0]) * 1000.0 / SIZE


print("cap", round(CAP), "O", round(adv("O"), 1), "H", round(adv("H"), 1),
      "E", round(adv("E"), 1), "V", round(adv("V"), 1), "L", round(adv("L"), 1),
      "P", round(adv("P"), 1), "R", round(adv("R"), 1), "Z", round(adv("Z"), 1),
      "D", round(adv("D"), 1))
print("thick O", round(thick("O")), "H", round(thick("H")), "E", round(thick("E")))
