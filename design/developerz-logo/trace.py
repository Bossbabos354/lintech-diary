#!/usr/bin/env python3
"""DeveloperZ — wordmark in the Metallica letter spirit (custom drawing)."""

import os

# All coordinates are in a grid where cap height = 100, baseline = 100.
# Glyph = (right edge of body, [contour, ...]); contour 0 = outer, others = counters.

def D():
    # stem, flat shoulder, straight right side, blade that runs away up-right
    # and a long lower bound that keeps the counter / blade relationship.
    outer = [(-6, -26), (8, -8), (30, -8), (46, 16), (50, 26), (50, 74),
             (46, 84), (30, 100), (8, 100), (8, 108), (-14, 148),
             (-8, 108), (-8, 0), (-30, -66)]
    hole = [(13, 20), (40, 20), (45, 28), (45, 72), (40, 80), (13, 80)]
    return 50, [outer, hole]


def E():
    """Stepped E: the upper arm juts up-left past the stem, lower arm to the right."""
    outer = [(-10, 0), (2, -14), (58, 0), (58, 22), (10, 22), (10, 40),
             (50, 40), (50, 62), (10, 62), (10, 78), (58, 78), (58, 100),
             (-4, 100), (-16, 114)]
    return 58, [outer]


print("lib")
