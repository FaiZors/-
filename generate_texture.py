"""
Генератор текстуры Песчаного Эндермена.
Требует: Pillow (pip install Pillow)

Запуск: python generate_texture.py

Создает файл sand_enderman.png — это стандартная текстура эндермена,
но перекрашенная в песчано-коричневые тона с жёлтыми глазами.
Поместите итоговый файл в:
  src/main/resources/assets/sandenderman/textures/entity/sand_enderman.png
"""

from PIL import Image, ImageDraw
import os

WIDTH, HEIGHT = 64, 32

img = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
pixels = img.load()

BODY_COLOR = (50, 35, 15, 255)
EYE_COLOR = (255, 210, 0, 255)

DARK_BODY = (35, 22, 8, 255)
MID_BODY  = (50, 35, 15, 255)

def fill_rect(px, x, y, w, h, color):
    for cy in range(y, y + h):
        for cx in range(x, x + w):
            if 0 <= cx < WIDTH and 0 <= cy < HEIGHT:
                px[cx, cy] = color

def fill_rect_outline(px, x, y, w, h, fill, outline):
    fill_rect(px, x, y, w, h, fill)
    for cx in range(x, x + w):
        px[cx, y]         = outline
        px[cx, y + h - 1] = outline
    for cy in range(y, y + h):
        px[x, cy]         = outline
        px[x + w - 1, cy] = outline

HEAD_OUTER = (40, 28, 10, 255)
HEAD_INNER = (55, 40, 18, 255)
fill_rect_outline(pixels, 0, 0, 8, 8, HEAD_INNER, HEAD_OUTER)

fill_rect(pixels, 1, 4, 2, 2, EYE_COLOR)
fill_rect(pixels, 5, 4, 2, 2, EYE_COLOR)

fill_rect_outline(pixels, 8, 0, 8, 8, HEAD_INNER, HEAD_OUTER)

BODY_OUTER = (30, 20, 8, 255)
BODY_INNER = (45, 32, 12, 255)
fill_rect_outline(pixels, 16, 16, 8, 12, BODY_INNER, BODY_OUTER)
fill_rect_outline(pixels, 24, 16, 4,  4, BODY_INNER, BODY_OUTER)
fill_rect_outline(pixels, 28, 16, 4,  4, BODY_INNER, BODY_OUTER)
fill_rect_outline(pixels, 32, 16, 4, 12, BODY_INNER, BODY_OUTER)
fill_rect_outline(pixels, 36, 16, 4, 12, BODY_INNER, BODY_OUTER)

ARM_OUTER = (28, 18, 7, 255)
ARM_INNER = (43, 30, 11, 255)
for ax in [0, 11, 22, 33]:
    fill_rect_outline(pixels, 40 + (ax // 11) * 14, 16, 4, 12, ARM_INNER, ARM_OUTER)

LEG_OUTER = (28, 18, 7, 255)
LEG_INNER = (43, 30, 11, 255)
fill_rect_outline(pixels, 0,  16, 4, 12, LEG_INNER, LEG_OUTER)
fill_rect_outline(pixels, 16, 0,  4, 12, LEG_INNER, LEG_OUTER)

out_dir = os.path.join(
    os.path.dirname(__file__),
    "src", "main", "resources", "assets", "sandenderman", "textures", "entity"
)
os.makedirs(out_dir, exist_ok=True)
out_path = os.path.join(out_dir, "sand_enderman.png")
img.save(out_path)
print(f"Текстура сохранена: {out_path}")
print("Размер: 64x32 RGBA")
print("Теперь откройте файл в Blockbench/GIMP и подправьте детали по вкусу.")
