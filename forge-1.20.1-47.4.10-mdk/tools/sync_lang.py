"""Copia es_es.json a las demás variantes de español de Minecraft.

Minecraft no usa es_es como respaldo para es_mx, es_ec, etc.: si falta el archivo de la variante, cae al
inglés. Ejecutar tras editar es_es.json:  python tools/sync_lang.py
"""
import shutil
from pathlib import Path

LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico/lang"
VARIANTS = ["es_ar", "es_cl", "es_ec", "es_mx", "es_uy", "es_ve"]

if __name__ == "__main__":
    for v in VARIANTS:
        shutil.copyfile(LANG / "es_es.json", LANG / f"{v}.json")
    print("ok:", ", ".join(VARIANTS))
