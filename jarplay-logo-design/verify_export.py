# -*- coding: utf-8 -*-
"""生成验收拼版：把导出的各图层放在同一张图上肉眼核对。"""
import os

from PIL import Image, ImageDraw

OUT = r"D:\JarPlay\JarPlay\JarPlay\jarplay-logo-design\android-export"
TILE = (14, 20, 48)

PANEL = 320
GAP = 24
LABELS = ["monochrome @indigo", "foreground @indigo", "legacy 192", "playstore 512"]


def load(path, px):
    im = Image.open(os.path.join(OUT, path)).convert("RGBA")
    im = im.resize((px, px), Image.LANCZOS)
    return im


def on_tile(im):
    plate = Image.new("RGBA", im.size, TILE + (255,))
    plate.alpha_composite(im)
    return plate.convert("RGBA")


sheet = Image.new("RGB", (PANEL * 4 + GAP * 5, PANEL + GAP * 2 + 40), (245, 245, 247))
d = ImageDraw.Draw(sheet)

items = [
    on_tile(load("ic_launcher_monochrome-xxxhdpi.png", PANEL)),
    on_tile(load("ic_launcher_foreground-xxxhdpi.png", PANEL)),
    load("ic_launcher-xxxhdpi.png", PANEL),
    load("ic_launcher-playstore.png", PANEL),
]

for i, im in enumerate(items):
    x = GAP + i * (PANEL + GAP)
    sheet.paste(im, (x, GAP), im)
    d.text((x + 2, GAP + PANEL + 10), LABELS[i], fill=(30, 30, 30))

sheet.save(os.path.join(OUT, "_verify-sheet.png"))

for p in ("ic_launcher_monochrome-xxxhdpi.png", "ic_launcher_foreground-xxxhdpi.png",
          "ic_launcher-xxxhdpi.png", "ic_launcher-playstore.png",
          "ic_launcher-master-1024.png", "ic_launcher-mdpi.png"):
    im = Image.open(os.path.join(OUT, p))
    info = "%s  %sx%s  mode=%s" % (p, im.size[0], im.size[1], im.mode)
    if im.mode == "RGBA":
        info += "  alpha_bbox=%s" % (im.split()[3].getbbox(),)
    print(info)
