# -*- coding: utf-8 -*-
"""JarPlay 图标资源导出：从定稿位图派生安卓启动图标全套资源。

产出：
  - 圆角方块版（透明四角）：legacy ic_launcher，mdpi..xxxhdpi
  - 满幅版：Play 商店 512
  - 自适应图标前景层（透明底、符号居中、占 108dp 画布的 ~63%）：ic_launcher_foreground
  - 单色主题图标层（安卓 13+）：ic_launcher_monochrome
"""
import os
import shutil

from PIL import Image, ImageChops, ImageDraw

SRC = (r"D:\JarPlay\JarPlay\JarPlay\jarplay-logo-design\jarplay-logo_assets"
       r"\ddc59950-miora_edit_image-1790065163798-0-519e42d34c03.png")
DESIGN = r"D:\JarPlay\JarPlay\JarPlay\jarplay-logo-design"
OUT = os.path.join(DESIGN, "android-export")
RES = r"D:\JarPlay\JarPlay\JarPlay\app\src\main\res"

TILE = (14, 20, 48)      # #0E1430
TILE_HEX = "#0E1430"
BG_CUT = 238             # 亮于此值视为方块外的空白背景
SS = 4                   # 圆角遮罩超采样倍数

DENSITIES = [("mdpi", 1.0), ("hdpi", 1.5), ("xhdpi", 2.0),
             ("xxhdpi", 3.0), ("xxxhdpi", 4.0)]


def ensure(path):
    os.makedirs(path, exist_ok=True)


def rounded_mask(size, radius):
    """抗锯齿的圆角矩形 alpha 遮罩。"""
    w, h = size
    big = Image.new("L", (w * SS, h * SS), 0)
    ImageDraw.Draw(big).rounded_rectangle(
        [0, 0, w * SS - 1, h * SS - 1], radius=max(0, int(round(radius * SS))), fill=255)
    return big.resize((w, h), Image.LANCZOS)


def ramp(v):
    """把与底色的差值映射成柔和的 alpha，边界不留硬齿。"""
    if v <= 24:
        return 0
    if v >= 80:
        return 255
    return int(round((v - 24) * 255.0 / 56.0))


def main():
    ensure(OUT)

    img = Image.open(SRC).convert("RGB")
    print("[1] 定稿原图尺寸:", img.size)

    # --- 裁出方块本体 ---------------------------------------------------
    outside = img.convert("L").point(lambda v: 255 if v > BG_CUT else 0)
    bb = ImageChops.invert(outside).getbbox()
    tile = img.crop(bb)
    tw, th = tile.size
    print("[2] 方块裁剪框:", bb, "->", tile.size)

    # --- 量圆角半径 -----------------------------------------------------
    radius = 0
    for x in range(tw):
        if sum(tile.getpixel((x, 0))) / 3.0 <= (BG_CUT - 20):
            radius = x
            break
    print("[3] 圆角半径: %d px  (%.1f%% 边长)" % (radius, radius * 100.0 / tw))

    # --- 底色校准到品牌色 -----------------------------------------------
    sample = tile.getpixel((tw // 2, max(radius + 2, int(th * 0.07))))
    delta = max(abs(sample[i] - TILE[i]) for i in range(3))
    print("[4] 采样底色 #%02X%02X%02X，与品牌色 %s 最大通道差 %d"
          % (sample[0], sample[1], sample[2], TILE_HEX, delta))

    diff = ImageChops.difference(tile, Image.new("RGB", (tw, th), sample)).convert("L")
    if delta > 4:
        flat = Image.new("RGB", (tw, th), TILE)
        tile = Image.composite(flat, tile, diff.point(lambda v: 255 if v <= 26 else 0))
        diff = ImageChops.difference(tile, Image.new("RGB", (tw, th), TILE)).convert("L")
        print("    底色已校准为 " + TILE_HEX)

    mark_alpha = diff.point(ramp)
    # 方块外缘的抗锯齿像素与底色的差值很大，会被误判成符号；先按内缩的圆角内区裁剪掉。
    pad = max(4, radius // 8)
    inner = Image.new("L", (tw, th), 0)
    ImageDraw.Draw(inner).rounded_rectangle(
        [pad, pad, tw - 1 - pad, th - 1 - pad],
        radius=max(0, radius - pad), fill=255)
    mark_alpha = ImageChops.multiply(mark_alpha, inner)
    mb = mark_alpha.getbbox()
    print("[5] 符号组包围盒:", mb)

    # --- 圆角方块版（透明四角） -----------------------------------------
    alpha = rounded_mask((tw, th), radius)
    squircle = tile.convert("RGBA")
    squircle.putalpha(alpha)

    # --- 满幅版（四角填品牌色） -----------------------------------------
    full = Image.new("RGB", (tw, th), TILE)
    full = Image.composite(tile, full, alpha)

    # --- 符号层 ----------------------------------------------------------
    mark = tile.convert("RGBA")
    mark.putalpha(mark_alpha)
    mark = mark.crop(mb)
    mw, mh = mark.size
    ratio_w, ratio_h = mw / float(tw), mh / float(th)
    print("[6] 符号组 %s  占方块宽 %.1f%% / 高 %.1f%%"
          % (mark.size, ratio_w * 100, ratio_h * 100))

    # --- 单色层：琥珀色形体（播放三角自然成为镂空） ----------------------
    r_ch, g_ch, b_ch = tile.split()
    amber = ImageChops.multiply(
        ImageChops.multiply(r_ch.point(lambda v: 255 if v >= 165 else 0),
                            g_ch.point(lambda v: 255 if 95 <= v <= 215 else 0)),
        b_ch.point(lambda v: 255 if v <= 110 else 0))
    mono = Image.new("RGBA", (tw, th), (255, 255, 255, 0))
    mono.putalpha(ImageChops.multiply(amber, inner))
    mono = mono.crop(mb)
    print("[6a] 单色层包围盒:", mono.getbbox())

    def plate(px):
        """把符号按比例放进 px 见方的透明画布（108dp 自适应画布）。"""
        canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        w = int(round(px * ratio_w))
        h = int(round(w * mh / float(mw)))
        return canvas, (w, h)

    def place(layer, px):
        canvas, (w, h) = plate(px)
        canvas.alpha_composite(layer.resize((w, h), Image.LANCZOS),
                               ((px - w) // 2, (px - h) // 2))
        return canvas

    # --- 备份旧资源 ------------------------------------------------------
    backup = os.path.join(OUT, "_backup_old_icons")
    for name, _ in DENSITIES:
        for fn in ("ic_launcher.png", "ic_launcher_foreground.png"):
            src = os.path.join(RES, "mipmap-" + name, fn)
            dst = os.path.join(backup, "mipmap-" + name, fn)
            if os.path.exists(src) and not os.path.exists(dst):
                ensure(os.path.join(backup, "mipmap-" + name))
                shutil.copy2(src, dst)
    print("[7] 旧图标已备份到", backup)

    # --- 输出 ------------------------------------------------------------
    for name, scale in DENSITIES:
        legacy = int(round(48 * scale))
        fg = int(round(108 * scale))
        ddir = os.path.join(RES, "mipmap-" + name)
        ensure(ddir)

        squircle.resize((legacy, legacy), Image.LANCZOS).save(
            os.path.join(ddir, "ic_launcher.png"))
        place(mark, fg).save(os.path.join(ddir, "ic_launcher_foreground.png"))
        place(mono, fg).save(os.path.join(ddir, "ic_launcher_monochrome.png"))

        squircle.resize((legacy, legacy), Image.LANCZOS).save(
            os.path.join(OUT, "ic_launcher-%s.png" % name))
        place(mark, fg).save(os.path.join(OUT, "ic_launcher_foreground-%s.png" % name))
        place(mono, fg).save(os.path.join(OUT, "ic_launcher_monochrome-%s.png" % name))
        print("[8] %-8s legacy %-4d 前景 %-4d 单色 %-4d" % (name, legacy, fg, fg))

    squircle.resize((1024, 1024), Image.LANCZOS).save(
        os.path.join(OUT, "ic_launcher-master-1024.png"))
    full.resize((512, 512), Image.LANCZOS).save(
        os.path.join(OUT, "ic_launcher-playstore.png"))
    print("[9] master 1024 + playstore 512 已输出")


if __name__ == "__main__":
    main()
