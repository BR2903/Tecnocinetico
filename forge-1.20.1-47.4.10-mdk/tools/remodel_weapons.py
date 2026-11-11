"""Regenera solo el arsenal (sin pistola), ajusta inventario por variante y compone las vistas."""
import subprocess
import sys
import re
from pathlib import Path
from weapons_gen import GROUPS
from weapon_blueprints import EXTRA_WEAPONS

ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/"src/main/resources/assets/tecnocinetico"


def run(script,*args,quiet=False):
    subprocess.run([sys.executable,str(ROOT/"tools"/script),*map(str,args)],cwd=ROOT,check=True,
                   stdout=subprocess.DEVNULL if quiet else None)


def main():
    run("weapons_gen.py","--weapons-only")
    fitted=0
    for group in GROUPS+sorted(EXTRA_WEAPONS):
        models={p.stem for p in (ASSETS/"models/item").glob(group+"*.json")}
        if group in {"pipe_shotgun","bolt_rifle","pump_shotgun","semi_rifle"}:
            bases=[group]+[f"{group}_m{i}" for i in range(1,8)]
        else: bases=[group]
        for base in bases:
            names=sorted(n for n in models if n==base or n.startswith(base+"_") and not re.match(r"_m\d",n[len(base):]))
            # La base debe ser la referencia; todos sus fotogramas comparten encuadre.
            names=[base]+[n for n in names if n!=base]
            rotation="0,0,0" if group in {"scrap_machete","kinetic_blade"} else "y-135;x30" if group=="kinetic_grenade" else "y-90;z25"
            run("gui_fit.py",ASSETS,rotation,*names,quiet=True)
            fitted+=len(names)
    print(f"Inventario ajustado: {fitted} modelos y fotogramas.")
    run("preview_weapon_remodel.py")


if __name__=="__main__": main()
