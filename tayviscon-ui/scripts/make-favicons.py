from pathlib import Path

from PIL import Image

public = Path(__file__).resolve().parents[1] / "public"
brand = public / "brand"
# Prefer cleaned brand/icon.png; fall back to downloaded GitHub org avatar.
src = brand / "icon.png"
if not src.exists():
    src = brand / "github-org-avatar.png"

img = Image.open(src).convert("RGBA")
img.save(brand / "icon.png", optimize=True)

for size, name in (
    (32, "favicon-32x32.png"),
    (16, "favicon-16x16.png"),
    (48, "favicon-48x48.png"),
):
    img.resize((size, size), Image.Resampling.LANCZOS).save(public / name, optimize=True)

img.resize((180, 180), Image.Resampling.LANCZOS).save(
    public / "apple-touch-icon.png", optimize=True
)

img.save(
    public / "favicon.ico",
    format="ICO",
    sizes=[(16, 16), (32, 32), (48, 48)],
)

print("ok", (public / "favicon.ico").stat().st_size)
