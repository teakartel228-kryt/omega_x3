#!/usr/bin/env python3
"""Генерирует 3D-модели (JSON), текстуры (PNG), рецепты, зачарования и lang для [REMAKE] Arsenal.
Запуск из корня проекта:  python3 tools/generate_assets.py   (нужен Pillow)
"""
import json, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "remake_arsenal")
DATA = os.path.join(ROOT, "data", "remake_arsenal")
NS = "remake_arsenal"

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

# ---------------------------------------------------------------- 3D item models
# Каждый предмет = набор цветных боксов. Цвет берётся из палитры 16x16 (1 пиксель = 1 цвет).
DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, -90, 10], "translation": [0, 3, 1], "scale": [0.75, 0.75, 0.75]},
    "thirdperson_lefthand": {"rotation": [0, 90, -10], "translation": [0, 3, 1], "scale": [0.75, 0.75, 0.75]},
    "firstperson_righthand": {"rotation": [0, -90, -20], "translation": [1.1, 3, 1.1], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, 20], "translation": [1.1, 3, 1.1], "scale": [0.68, 0.68, 0.68]},
}

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)

class Model:
    def __init__(self):
        self.colors = []   # палитра
        self.elements = []

    def col(self, rgb):
        rgb = tuple(rgb)
        if rgb not in self.colors:
            self.colors.append(rgb)
        return self.colors.index(rgb)

    def box(self, a, b, rgb, rot=None):
        """a, b — углы бокса в 16-сетке. rot=(axis, angle, origin)."""
        i = self.col(rgb)
        u, v = i % 16, i // 16
        face = {"uv": [u, v, u + 1, v + 1], "texture": "#0"}
        el = {"from": list(a), "to": list(b), "faces": {f: dict(face) for f in ("north", "east", "south", "west", "up", "down")}}
        if rot:
            axis, angle, origin = rot
            el["rotation"] = {"angle": angle, "axis": axis, "origin": list(origin)}
        self.elements.append(el)

    def save(self, name):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for i, c in enumerate(self.colors):
            img.putpixel((i % 16, i // 16), c + (255,))
        os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
        img.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        w(os.path.join(ASSETS, "models", "item", name + ".json"), {
            "credit": "[REMAKE] Arsenal - generated model",
            "textures": {"0": f"{NS}:item/{name}", "particle": f"{NS}:item/{name}"},
            "elements": self.elements,
            "display": DISPLAY,
        })

LEATHER = (110, 70, 40); LEATHER_D = (74, 46, 26)
IRON = (170, 172, 180); IRON_D = (95, 98, 108); IRON_L = (215, 218, 226)
DARK = (40, 42, 50); GOLD = (240, 200, 60)

# --- Когти
def claws():
    m = Model()
    m.box((6, 0, 5), (10, 5, 11), LEATHER)                 # рукоять
    m.box((6, 1, 5), (10, 2, 11), LEATHER_D)               # обмотка
    m.box((6, 3, 5), (10, 4, 11), LEATHER_D)
    m.box((4, 5, 4), (12, 7, 12), IRON_D)                  # гарда
    for x in (4.75, 7.5, 10.25):                           # три когтя
        m.box((x, 7, 6.5), (x + 1.25, 12, 8.0), IRON, rot=("x", -22.5, (x, 7, 7)))
        m.box((x + 0.1, 11, 4.6), (x + 1.15, 15, 6.1), IRON_L, rot=("x", -22.5, (x, 7, 7)))
    m.save("climbing_claws")

# --- Ручной поршень
def piston():
    m = Model()
    m.box((4, 0, 4), (12, 9, 12), (130, 130, 135))         # каменный корпус
    m.box((4, 0, 4), (12, 1, 12), (90, 90, 95))
    m.box((3, 9, 3), (13, 12, 13), (190, 150, 90))         # деревянная голова
    m.box((3, 9, 3), (13, 10, 13), (150, 112, 62))
    m.box((7, 12, 7), (9, 16, 9), (190, 150, 90))          # шток
    m.box((12, 3, 7), (16, 5, 9), LEATHER)                 # боковая рукоять
    m.box((15, 2, 6.5), (16, 6, 9.5), LEATHER_D)
    m.box((5, 5, 3.5), (11, 6, 4), (200, 60, 50))          # красная метка
    m.save("hand_piston")

# --- Нотный шлем
def helmet():
    m = Model()
    m.box((3, 2, 3), (13, 10, 13), IRON_D)
    m.box((3, 2, 3), (13, 3, 13), DARK)
    m.box((4, 10, 4), (12, 12, 12), IRON)
    m.box((4, 4, 12.5), (12, 8, 13.2), (30, 30, 40))      # визор
    for sx in (0.5, 13):                                   # динамики
        m.box((sx, 4, 5), (sx + 2.5, 9, 11), DARK)
        m.box((sx - 0.3 if sx < 8 else sx + 2.5, 5.5, 6.5), (sx + 0.0 if sx < 8 else sx + 2.8, 7.5, 9.5), (60, 220, 240))
    m.box((7.5, 12, 7.5), (8.5, 16, 8.5), GOLD)           # нота
    m.box((5.5, 12, 7), (7.5, 13.5, 9), GOLD)
    m.box((8.5, 15, 7.5), (10.5, 16, 8.5), GOLD)
    m.save("note_helmet")

# --- Ненавистный мешок
def bag():
    m = Model()
    S = (170, 140, 95); SD = (130, 104, 66)
    m.box((3, 0, 3), (13, 9, 13), S)
    m.box((3, 0, 3), (13, 1, 13), SD)
    m.box((4, 9, 4), (12, 11, 12), S)
    m.box((5.5, 11, 5.5), (10.5, 13, 10.5), SD)            # горловина
    m.box((5, 13, 5), (11, 15, 11), S)                     # складки сверху
    m.box((4.5, 11, 4.5), (11.5, 12, 11.5), (120, 20, 30)) # красная верёвка
    m.box((4.5, 5.5, 13), (7, 7.5, 13.4), (230, 30, 40))   # злые глаза
    m.box((9, 5.5, 13), (11.5, 7.5, 13.4), (230, 30, 40))
    m.box((4.5, 7.5, 13), (6.5, 8.2, 13.4), DARK)          # брови
    m.box((9.5, 7.5, 13), (11.5, 8.2, 13.4), DARK)
    m.box((5.5, 2.5, 13), (10.5, 4, 13.4), DARK)           # рот
    for x in (6, 7.5, 9):
        m.box((x, 2.5, 13.3), (x + 0.5, 4, 13.6), (240, 240, 240))
    m.save("hate_bag")

# --- Электро-крылья
def wings():
    m = Model()
    CY = (60, 220, 250); CYD = (30, 130, 170); Y = (255, 230, 80)
    m.box((6, 4, 6), (10, 12, 10), DARK)                   # крепление
    m.box((7, 6, 9.8), (9, 10, 10.6), CY)                  # ядро
    for side in (1, -1):
        for i, (length, y) in enumerate([(8, 9), (6.5, 6.5), (5, 4), (3.5, 1.5)]):
            x0 = 10 if side == 1 else 6 - length
            x1 = x0 + length
            m.box((x0, y, 6.5), (x1, y + 1.5, 9.5), CY if i % 2 == 0 else CYD, rot=("z", 22.5 * side, (8, 8, 8)))
            tip = (x1 - 0.8, y, 6.5) if side == 1 else (x0, y, 6.5)
            m.box(tip, (tip[0] + 0.8, y + 1.5, 9.5), Y, rot=("z", 22.5 * side, (8, 8, 8)))
    m.save("electro_wings")

# --- Крюк (6 уровней)
TIER_COL = [(150, 150, 155), (200, 120, 70), (200, 200, 205), (245, 200, 60), (80, 230, 235), (70, 50, 90)]
def hook(tier):
    m = Model()
    acc = TIER_COL[tier]
    m.box((7, 0, 7), (9, 6, 9), LEATHER)
    m.box((6.8, 1, 6.8), (9.2, 2, 9.2), LEATHER_D)
    m.box((6.8, 3, 6.8), (9.2, 4, 9.2), LEATHER_D)
    m.box((5.5, 6, 5.5), (10.5, 7.5, 10.5), acc)           # гарда (цвет = уровень)
    for i in range(3):                                     # цепь
        m.box((7.4, 7.5 + i * 1.6, 7.4), (8.6, 8.8 + i * 1.6, 8.6), IRON if i % 2 == 0 else IRON_D)
    m.box((6.5, 12.3, 6.5), (9.5, 13.8, 9.5), IRON_D)      # крепление головы
    m.box((7, 13.8, 7), (9, 16, 9), IRON_L)                # центральный клык
    for s in (1, -1):                                      # боковые лапы
        x0, x1 = (9.5, 12.5) if s == 1 else (3.5, 6.5)
        m.box((x0, 12.3, 7), (x1, 13.3, 9), acc if tier else IRON)
        tx = 12 if s == 1 else 3.5
        m.box((tx, 10.3, 7), (tx + 0.5, 13.3, 9), IRON_L)
    if tier == 5:
        m.box((7.4, 6.8, 5.2), (8.6, 8, 5.5), (255, 70, 60)) # светящийся элемент незерита
    m.save(HOOK_NAMES[tier])

HOOK_NAMES = ["grappling_hook"] + [f"grappling_hook_{i}" for i in range(1, 6)]

def template_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(2, 14):
        for y in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            img.putpixel((x, y), (90, 60, 140, 255) if edge else (150, 110, 210, 255))
    for x, y in [(8, 4), (8, 5), (8, 6), (7, 7), (9, 7), (6, 8), (10, 8), (8, 9), (8, 10), (7, 11), (9, 11)]:
        img.putpixel((x, y), (255, 230, 80, 255))
    img.save(os.path.join(ASSETS, "textures", "item", "hook_upgrade_template.png"))
    w(os.path.join(ASSETS, "models", "item", "hook_upgrade_template.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/hook_upgrade_template"}})

# ---------------------------------------------------------------- textures: armor layers & entities
def paint_box(img, u, v, bw, bh, bd, base):
    """Закрашивает UV-развёртку бокса (стандартная раскладка Minecraft) с лёгким затенением граней."""
    def fill(x, y, ww, hh, k):
        for i in range(ww):
            for j in range(hh):
                if 0 <= u + x + i < img.width and 0 <= v + y + j < img.height:
                    edge = i in (0, ww - 1) or j in (0, hh - 1)
                    c = shade(base, k * (0.82 if edge else 1.0))
                    img.putpixel((u + x + i, v + y + j), c + (255,))
    fill(bd, 0, bw, bd, 1.15)            # верх
    fill(bd + bw, 0, bw, bd, 0.7)        # низ
    fill(0, bd, bd, bh, 0.85)            # право
    fill(bd, bd, bw, bh, 1.0)            # перед
    fill(bd + bw, bd, bd, bh, 0.85)      # лево
    fill(2 * bd + bw, bd, bw, bh, 0.9)   # зад

def textures():
    # Шлем: слой брони (64x32) — только область головы
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint_box(img, 0, 0, 8, 8, 8, (70, 74, 90))
    paint_box(img, 32, 0, 8, 8, 8, (70, 74, 90))
    for x in range(8, 16):
        for y in (14, 15):
            img.putpixel((x, y), (60, 220, 240, 255))      # голубая полоска на лбу
    os.makedirs(os.path.join(ASSETS, "textures", "models", "armor"), exist_ok=True)
    img.save(os.path.join(ASSETS, "textures", "models", "armor", "note_helmet_layer_1.png"))
    Image.new("RGBA", (64, 32), (0, 0, 0, 0)).save(os.path.join(ASSETS, "textures", "models", "armor", "note_helmet_layer_2.png"))
    # Крылья как нагрудник: слой полностью прозрачный (крылья рисует 3D-слой)
    for n in (1, 2):
        Image.new("RGBA", (64, 32), (0, 0, 0, 0)).save(os.path.join(ASSETS, "textures", "models", "armor", f"electro_wings_layer_{n}.png"))

    # 3D-слой шлема (динамики и нота) 32x16
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    paint_box(img, 0, 0, 2, 6, 6, (35, 36, 44))
    for x in range(8, 10):
        for y in range(8, 12):
            img.putpixel((x, y), (60, 220, 240, 255))
    paint_box(img, 16, 0, 1, 7, 1, (240, 200, 60))
    paint_box(img, 16, 8, 3, 2, 2, (240, 200, 60))
    paint_box(img, 16, 12, 2, 1, 1, (240, 200, 60))
    img.save(os.path.join(ASSETS, "textures", "entity", "note_helmet.png"))

    # Крылья 64x32
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for (u, v, bw, bd, c) in [(0, 0, 14, 3, (60, 220, 250)), (0, 4, 11, 3, (30, 130, 170)), (0, 8, 8, 3, (60, 220, 250)), (0, 12, 5, 3, (30, 130, 170))]:
        paint_box(img, u, v, bw, 1, bd, c)
    for x in range(0, 40):                                  # жёлтые искры по краям
        for y in range(0, 16):
            if img.getpixel((x, y))[3] and (x * 7 + y * 13) % 11 == 0:
                img.putpixel((x, y), (255, 235, 90, 255))
    img.save(os.path.join(ASSETS, "textures", "entity", "electro_wings.png"))

    # Помощник 64x32
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint_box(img, 0, 0, 10, 10, 8, (170, 140, 95))
    paint_box(img, 0, 18, 7, 7, 7, (150, 122, 82))
    paint_box(img, 40, 0, 3, 2, 3, (90, 60, 35))
    # лицо: перед головы = (u+d, v+d) = (7, 25), размер 7x7
    for (x, y) in [(8, 27), (9, 27), (8, 28), (9, 28), (12, 27), (13, 27), (12, 28), (13, 28)]:
        img.putpixel((x, y), (230, 30, 40, 255))
    for x in range(9, 13):
        img.putpixel((x, 31 - 1), (30, 30, 30, 255))
    for x in (8, 9, 12, 13):
        img.putpixel((x, 26), (30, 30, 30, 255))
    img.save(os.path.join(ASSETS, "textures", "entity", "hate_helper.png"))

    # Иконка мода 128x128
    ic = Image.new("RGBA", (128, 128), (22, 24, 34, 255))
    for i in range(30, 100):
        for t in range(-3, 4):
            ic.putpixel((i, 128 - i + t), (190, 195, 205, 255))
    for x in range(26, 52):
        for y in range(20, 30):
            ic.putpixel((x + 40, y + 10), (245, 200, 60, 255))
    ic.save(os.path.join(ASSETS, "icon.png"))

# ---------------------------------------------------------------- lang
RU = {
    "itemGroup.remake_arsenal": "[REMAKE] Арсенал",
    "item.remake_arsenal.climbing_claws": "Когти для боя",
    "item.remake_arsenal.hand_piston": "Ручной поршень",
    "item.remake_arsenal.note_helmet": "Нотный-шлем",
    "item.remake_arsenal.hate_bag": "Ненавистный мешок",
    "item.remake_arsenal.electro_wings": "Электро-крылья",
    "item.remake_arsenal.hook_upgrade_template": "Шаблон улучшения крюка",
    "item.remake_arsenal.grappling_hook": "Крюк",
    "item.remake_arsenal.grappling_hook_1": "Крюк I",
    "item.remake_arsenal.grappling_hook_2": "Крюк II",
    "item.remake_arsenal.grappling_hook_3": "Крюк III",
    "item.remake_arsenal.grappling_hook_4": "Крюк IV",
    "item.remake_arsenal.grappling_hook_5": "Крюк V",
    "entity.remake_arsenal.hate_helper": "Помощник мешка",
    "enchantment.remake_arsenal.hook_multishot": "Мультивыстрел",
    "enchantment.remake_arsenal.hook_quick_recharge": "Быстрая перезарядка",
    "enchantment.remake_arsenal.hook_power": "Сила",
    "enchantment.remake_arsenal.hook_arms": "Руки-крюки",
    "enchantment.remake_arsenal.redstone_hook": "Редстоуновый-крюк",
    "enchantment.remake_arsenal.defense_mechanism": "Защитный механизм",
    "key.categories.remake_arsenal": "[REMAKE] Арсенал",
    "key.remake_arsenal.dash": "Рывок (Электро-крылья)",
    "key.remake_arsenal.sonic": "Звуковая волна (Нотный-шлем)",
    "key.remake_arsenal.lightning": "Ненавистная молния (мешок)",
    "msg.remake_arsenal.no_energy": "Нет энергии!",
    "msg.remake_arsenal.sonic_cooldown": "Шлем перезаряжается: %s с",
    "msg.remake_arsenal.hook_locked": "Крюк в экстренном режиме: цели не притягиваются",
    "msg.remake_arsenal.hook_emergency": "Экстренное притягивание назад! Крюк заблокирован на %s с",
    "msg.remake_arsenal.hook_too_hard": "Слишком прочный блок: крюк не цепляется",
    "msg.remake_arsenal.hook_no_space": "Нет места рядом с тобой",
    "msg.remake_arsenal.helper_dismissed": "Помощник отозван",
    "msg.remake_arsenal.helper_cooldown": "Помощника можно призвать снова через %s с",
}
EN = {
    "itemGroup.remake_arsenal": "[REMAKE] Arsenal",
    "item.remake_arsenal.climbing_claws": "Combat Claws",
    "item.remake_arsenal.hand_piston": "Hand Piston",
    "item.remake_arsenal.note_helmet": "Note Helmet",
    "item.remake_arsenal.hate_bag": "Hate Bag",
    "item.remake_arsenal.electro_wings": "Electro Wings",
    "item.remake_arsenal.hook_upgrade_template": "Hook Upgrade Template",
    "item.remake_arsenal.grappling_hook": "Grappling Hook",
    "item.remake_arsenal.grappling_hook_1": "Grappling Hook I",
    "item.remake_arsenal.grappling_hook_2": "Grappling Hook II",
    "item.remake_arsenal.grappling_hook_3": "Grappling Hook III",
    "item.remake_arsenal.grappling_hook_4": "Grappling Hook IV",
    "item.remake_arsenal.grappling_hook_5": "Grappling Hook V",
    "entity.remake_arsenal.hate_helper": "Bag Helper",
    "enchantment.remake_arsenal.hook_multishot": "Multishot",
    "enchantment.remake_arsenal.hook_quick_recharge": "Quick Recharge",
    "enchantment.remake_arsenal.hook_power": "Power",
    "enchantment.remake_arsenal.hook_arms": "Hook Arms",
    "enchantment.remake_arsenal.redstone_hook": "Redstone Hook",
    "enchantment.remake_arsenal.defense_mechanism": "Defense Mechanism",
    "key.categories.remake_arsenal": "[REMAKE] Arsenal",
    "key.remake_arsenal.dash": "Dash (Electro Wings)",
    "key.remake_arsenal.sonic": "Sound Wave (Note Helmet)",
    "key.remake_arsenal.lightning": "Hate Lightning (Bag)",
    "msg.remake_arsenal.no_energy": "No energy!",
    "msg.remake_arsenal.sonic_cooldown": "Helmet recharging: %ss",
    "msg.remake_arsenal.hook_locked": "Hook in emergency mode: cannot pull targets",
    "msg.remake_arsenal.hook_emergency": "Emergency pullback! Hook locked for %ss",
    "msg.remake_arsenal.hook_too_hard": "Block too hard: the hook can't grab it",
    "msg.remake_arsenal.hook_no_space": "No room next to you",
    "msg.remake_arsenal.helper_dismissed": "Helper dismissed",
    "msg.remake_arsenal.helper_cooldown": "You can summon the helper again in %ss",
}

# ---------------------------------------------------------------- data: recipes / enchantments / tags
def item(i): return {"item": i}

def shaped(name, pattern, key, result, count=1):
    w(os.path.join(DATA, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
        "key": {k: item(v) for k, v in key.items()}, "result": {"id": result, "count": count}})

def smithing(name, base, addition, result):
    w(os.path.join(DATA, "recipe", name + ".json"), {
        "type": "minecraft:smithing_transform",
        "template": item(f"{NS}:hook_upgrade_template"), "base": item(base), "addition": item(addition),
        "result": {"id": result, "count": 1}})

def data():
    shaped("grappling_hook", [" T ", " C ", " S "], {"T": "minecraft:tripwire_hook", "C": "minecraft:chain", "S": "minecraft:stick"}, f"{NS}:grappling_hook")
    shaped("hook_upgrade_template", ["IDI", "ICI", "III"], {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "C": "minecraft:chain"}, f"{NS}:hook_upgrade_template", 2)
    mats = ["minecraft:copper_ingot", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:netherite_ingot"]
    for i, mat in enumerate(mats, start=1):
        smithing(f"grappling_hook_{i}", f"{NS}:{HOOK_NAMES[i-1]}", mat, f"{NS}:{HOOK_NAMES[i]}")
    shaped("climbing_claws", ["I I", "III", " S "], {"I": "minecraft:iron_ingot", "S": "minecraft:stick"}, f"{NS}:climbing_claws")
    shaped("hand_piston", ["P", "S", "L"], {"P": "minecraft:piston", "S": "minecraft:stick", "L": "minecraft:leather"}, f"{NS}:hand_piston")
    shaped("note_helmet", ["INI", "I I"], {"I": "minecraft:iron_ingot", "N": "minecraft:note_block"}, f"{NS}:note_helmet")
    shaped("hate_bag", ["LSL", "LEL", "LLL"], {"L": "minecraft:leather", "S": "minecraft:string", "E": "minecraft:fermented_spider_eye"}, f"{NS}:hate_bag")
    shaped("electro_wings", ["MCM", "MLM", "M M"], {"M": "minecraft:phantom_membrane", "C": "minecraft:copper_ingot", "L": "minecraft:lightning_rod"}, f"{NS}:electro_wings")

    # теги предметов для зачарований
    w(os.path.join(DATA, "tags", "item", "enchantable", "hook.json"), {"values": [f"{NS}:{n}" for n in HOOK_NAMES]})

    # зачарования (1.21.1 data-driven, логика — в коде мода)
    def ench(name, max_level, weight, base, per, anvil):
        w(os.path.join(DATA, "enchantment", name + ".json"), {
            "description": {"translate": f"enchantment.{NS}.{name}"},
            "supported_items": f"#{NS}:enchantable/hook",
            "weight": weight, "max_level": max_level,
            "min_cost": {"base": base, "per_level_above_first": per},
            "max_cost": {"base": base + 20, "per_level_above_first": per},
            "anvil_cost": anvil,
            "slots": ["hand"],
        })
    ench("hook_multishot", 1, 5, 20, 0, 4)
    ench("hook_quick_recharge", 3, 5, 12, 20, 2)
    ench("hook_power", 5, 10, 1, 10, 1)
    ench("hook_arms", 1, 2, 25, 0, 6)       # уникальные: сокровища
    ench("redstone_hook", 5, 2, 15, 10, 4)
    ench("defense_mechanism", 1, 2, 30, 0, 8)
    w(os.path.join(DATA, "..", "minecraft", "tags", "enchantment", "in_enchanting_table.json"),
      {"values": [f"{NS}:hook_multishot", f"{NS}:hook_quick_recharge", f"{NS}:hook_power"]})
    w(os.path.join(DATA, "..", "minecraft", "tags", "enchantment", "treasure.json"),
      {"values": [f"{NS}:hook_arms", f"{NS}:redstone_hook", f"{NS}:defense_mechanism"]})
    w(os.path.join(DATA, "..", "minecraft", "tags", "enchantment", "tradeable.json"),
      {"values": [f"{NS}:hook_multishot", f"{NS}:hook_quick_recharge", f"{NS}:hook_power"]})

if __name__ == "__main__":
    claws(); piston(); helmet(); bag(); wings()
    for t in range(6): hook(t)
    template_item(); textures()
    w(os.path.join(ASSETS, "lang", "ru_ru.json"), RU)
    w(os.path.join(ASSETS, "lang", "en_us.json"), EN)
    data()
    print("assets generated")
