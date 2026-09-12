"""Paint square center icons onto ME elevator and laser connector master textures."""
from pathlib import Path

from PIL import Image

TEX_DIR = Path(__file__).resolve().parents[2] / "common" / "src" / "main" / "resources" / "assets" / "appliedflooring" / "textures" / "block"

FRAME_E = (45, 52, 65, 255)
DARK_E = (35, 40, 50, 255)
CYAN_E = (80, 230, 255, 255)
WHITE_E = (220, 250, 255, 255)

FRAME_L = (37, 44, 57, 255)
BEVEL_L = (57, 64, 77, 255)
CYAN_L = (0, 160, 220, 255)
GLOW_L = (90, 230, 255, 255)
WHITE_L = (245, 255, 255, 255)

# 10x10 patterns at (3,3)..(12,12). Letters map to palette entries.
ELEVATOR_PATTERN = [
    "FFFFFFFFFF",
    "FDDCCCCDDF",
    "FDCDDDDCDF",
    "FDDDWWDDDF",
    "FCDWWWWCDF",
    "FCDWWWWCDF",
    "FDDDWWDDDF",
    "FDCDDDDCDF",
    "FDDCCCCDDF",
    "FFFFFFFFFF",
]

LASER_PATTERN = [
    "FFFFFFFFFF",
    "FBBBBBBBBF",
    "FBCCCCCCBF",
    "FBCGGGGCBF",
    "FBCGWWGCBF",
    "FBCGWWGCBF",
    "FBCGGGGCBF",
    "FBCCCCCCBF",
    "FBBBBBBBBF",
    "FFFFFFFFFF",
]


def normalize(pattern):
    rows = []
    for row in pattern:
        row = row.replace(" ", "")
        if len(row) != 10:
            raise ValueError(f"pattern row length {len(row)}: {row}")
        rows.append(row)
    return rows


def paint(base: Image.Image, pattern, palette):
    px = base.load()
    rows = normalize(pattern)
    for iy, row in enumerate(rows):
        for ix, ch in enumerate(row):
            color = palette.get(ch)
            if color is None:
                raise ValueError(f"unknown pattern char {ch}")
            px[3 + ix, 3 + iy] = color


def main():
    flooring = Image.open(TEX_DIR / "me_flooring.png").convert("RGBA")

    elevator = flooring.copy()
    paint(
        elevator,
        ELEVATOR_PATTERN,
        {"F": FRAME_E, "D": DARK_E, "C": CYAN_E, "W": WHITE_E},
    )
    elevator.save(TEX_DIR / "me_elevator.png")

    laser = flooring.copy()
    paint(
        laser,
        LASER_PATTERN,
        {"F": FRAME_L, "B": BEVEL_L, "C": CYAN_L, "G": GLOW_L, "W": WHITE_L},
    )
    laser.save(TEX_DIR / "me_laser_connector.png")
    print(f"Updated {TEX_DIR / 'me_elevator.png'}")
    print(f"Updated {TEX_DIR / 'me_laser_connector.png'}")


if __name__ == "__main__":
    main()
