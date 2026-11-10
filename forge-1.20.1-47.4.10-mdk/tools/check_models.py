"""Revisa los modelos JSON del mod antes de abrir el juego (el servidor de prueba NO carga modelos, así que
estos errores solo se verían en el cliente como el cubo negro y rosa):
- coordenadas de los elementos dentro de [-16, 32] (si no, Minecraft descarta el modelo entero);
- rotaciones de elemento válidas (un eje, ángulo de -45 a 45 en pasos de 22.5);
- que existan las texturas referenciadas (del mod) y los modelos de los overrides.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/check_models.py      → lista los problemas (sale con código 1)
"""
import json
import sys
from pathlib import Path

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"
import json as _json
_ATLAS_DIRS = ["block/", "item/"]
_atlas = ASSETS.parent / "minecraft" / "atlases" / "blocks.json"     # el atlas se amplía en el espacio minecraft
if _atlas.exists():
    for src in _json.loads(_atlas.read_text(encoding="utf-8")).get("sources", []):
        if src.get("type") == "directory":
            _ATLAS_DIRS.append(src["source"].rstrip("/") + "/")


def in_atlas(path):
    """Las texturas de modelos de bloque/objeto deben estar en el atlas: carpetas block/, item/ o las añadidas."""
    return any(path.startswith(d) for d in _ATLAS_DIRS)


problems = []
for path in sorted((ASSETS / "models").rglob("*.json")):
    rel = path.relative_to(ASSETS)
    try:
        m = json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:
        problems.append(f"{rel}: JSON inválido ({e})")
        continue
    for i, el in enumerate(m.get("elements", [])):
        for key in ("from", "to"):
            if any(v < -16 or v > 32 for v in el.get(key, [])):
                problems.append(f"{rel}: elemento {i} '{key}' {el[key]} fuera de [-16, 32]")
        rot = el.get("rotation")
        if rot and (rot.get("angle") not in (-45, -22.5, 0, 22.5, 45) or rot.get("axis") not in ("x", "y", "z")):
            problems.append(f"{rel}: elemento {i} rotación no válida {rot}")
    for name, tex in m.get("textures", {}).items():
        if isinstance(tex, str) and tex.startswith("tecnocinetico:"):
            if not (ASSETS / "textures" / (tex.split(":", 1)[1] + ".png")).exists():
                problems.append(f"{rel}: falta la textura {tex}")
            elif not in_atlas(tex.split(":", 1)[1]):
                problems.append(f"{rel}: la textura {tex} no está en el atlas de bloques/objetos (se vería morada y negra; "
                                f"añade su carpeta a atlases/blocks.json)")
    for ov in m.get("overrides", []):
        target = ov.get("model", "")
        if target.startswith("tecnocinetico:") and not (ASSETS / "models" / (target.split(":", 1)[1] + ".json")).exists():
            problems.append(f"{rel}: override a un modelo que no existe {target}")
    parent = m.get("parent", "")
    if parent.startswith("tecnocinetico:") and not (ASSETS / "models" / (parent.split(":", 1)[1] + ".json")).exists():
        problems.append(f"{rel}: padre inexistente {parent}")

print("\n".join(problems) if problems else "ok: todos los modelos son válidos")
sys.exit(1 if problems else 0)
