"""Arsenal 3D de tiers 1-3, variantes de módulos/recarga, munición y varios componentes.
Cada arma comparte su atlas textures/item/gun/<nombre>.png con todas sus variantes.
Las 19 geometrías completas se construyen desde weapon_blueprints.py, sin modificar la Pistola Cinética.

Para regenerar solo las armas, ajustar el inventario y generar vistas: python tools/remodel_weapons.py.
Con --weapons-only, este generador omite munición, módulos sueltos, gadgets, nave y componentes.

Uso (desde forge-1.20.1-47.4.10-mdk):
    python tools/weapons_gen.py
    python tools/gui_fit.py $A "y-90;z25" pipe_shotgun pipe_shotgun_open      (armas largas, como la pistola)
    python tools/gui_fit.py $A "y-90;z25" bolt_rifle bolt_rifle_bolt
    python tools/gui_fit.py $A "y-135;x30" shotgun_shell ;  ... rifle_round
    python tools/weapons_gen.py   (otra vez: conserva display.gui)
Las armas largas usan las coordenadas de empuñadura y miras de la pistola (cañón hacia -Z, empuñadura en
z≈9..12, miras a y≈14-15) para compartir la animación en primera persona.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tecnocinetico"

import gunkit  # noqa: E402
from gunkit import el  # noqa: E402 (cada arma se pinta en su propia textura item/gun/<grupo>.png)
from weapon_blueprints import build_weapon, EXTRA_WEAPONS

# grupos de textura: todas las variantes de un arma comparten su textura
GROUPS = ["pipe_shotgun", "bolt_rifle", "pump_shotgun", "semi_rifle", "rail_rifle", "assault_rifle", "grenade_launcher",
          "pulse_cannon", "revolver", "flamethrower", "light_machine_gun", "kinetic_sniper", "rocket_launcher", "arc_caster",
          "minigun", "kinetic_smg"]


# ---------------------------------------------------------------- escopeta de caño (dos cañones, basculante)
CHOKE, SAWED, LOADER = 1, 2, 4                      # máscara local (orden de WeaponModule: CHOKE, SAWED_OFF, SPEED_LOADER)


def shotgun(open_=False, mods=0):
    return build_weapon("pipe_shotgun", mods=mods, reload=open_)


# ---------------------------------------------------------------- rifle de cerrojo
SCOPE, BOLT, BARREL = 1, 2, 4                       # máscara local (orden: SCOPE, POLISHED_BOLT, RIFLE_BARREL)


def rifle(bolt_back=False, mods=0):
    return build_weapon("bolt_rifle", mods=mods, reload=bolt_back)


# ---------------------------------------------------------------- tier 2: escopeta de corredera (módulos de escopeta)
def pump_shotgun(pumped=False, mods=0):
    return build_weapon("pump_shotgun", mods=mods, reload=pumped)


# ---------------------------------------------------------------- tier 2: rifle semiautomático (módulos de rifle)
def semi_rifle(mag_out=False, mods=0):
    return build_weapon("semi_rifle", mods=mods, reload=mag_out)


# ---------------------------------------------------------------- tier 2: subfusil (sin módulos)
def smg(mag_out=False):
    return build_weapon("kinetic_smg", reload=mag_out)


# ---------------------------------------------------------------- tier 3: rifle de riel
def rail_rifle(mag_out=False):
    return build_weapon("rail_rifle", reload=mag_out)


# ---------------------------------------------------------------- tier 3: fusil de asalto
def assault_rifle(mag_out=False):
    return build_weapon("assault_rifle", reload=mag_out)


# ---------------------------------------------------------------- tier 3: lanzagranadas
def grenade_launcher(open_=False):
    return build_weapon("grenade_launcher", reload=open_)


# ---------------------------------------------------------------- tier 3: cañón de impulso
def pulse_cannon(cell_out=False):
    return build_weapon("pulse_cannon", reload=cell_out)


def rail_slug():
    return [el([7.5, 2.0, 7.5], [8.5, 11.0, 8.5], "inercita"), el([7.6, 11.0, 7.6], [8.4, 12.2, 8.4], "steel_hi"),
            el([7.45, 6.0, 7.45], [8.55, 6.6, 8.55], "cyan")]


def kinetic_grenade():
    return build_weapon("kinetic_grenade")


def pulse_cell(full=True):
    return [el([6.8, 2.0, 6.8], [9.2, 10.0, 9.2], "cyan" if full else "graphite"),
            el([6.6, 1.6, 6.6], [9.4, 2.4, 9.4], "steel"), el([6.6, 9.6, 6.6], [9.4, 10.4, 9.4], "steel"),
            el([7.6, 10.4, 7.6], [8.4, 11.0, 8.4], "copper"), el([6.75, 5.6, 6.75], [9.25, 6.2, 9.25], "steel_dark")]


# ---------------------------------------------------------------- Caza del Núcleo (miniatura del objeto) y misil
def core_fighter_mini():
    """Miniatura con las mismas piezas que CoreFighterModel (coordenadas del modelo de entidad, escaladas)."""
    k = 0.17
    boxes = [(-6, -7, -28, 12, 7, 50, "graphite", None), (-4, -5, -40, 8, 5, 12, "graphite", None),
             (-2, -3, -46, 4, 3, 6, "steel", None), (-3, -10, -8, 6, 3, 20, "graphite", None),
             (-4, -12, -24, 8, 5, 14, "cyan", None),
             (-11, -6, 8, 6, 6, 16, "steel", None), (5, -6, 8, 6, 6, 16, "steel", None),
             (-10, -5, 24, 4, 4, 1, "cyan", None), (6, -5, 24, 4, 4, 1, "cyan", None),
             (6, -3, 14, 10, 1, 6, "inercita", None), (-16, -3, 14, 10, 1, 6, "inercita", None),
             (3, -17, 12, 2, 10, 10, "graphite", None), (-5, -17, 12, 2, 10, 10, "graphite", None),
             (6, -4, -10, 26, 2, 16, "graphite", ("y", -22.5, (6, -3, -2))),
             (-32, -4, -10, 26, 2, 16, "graphite", ("y", 22.5, (-6, -3, -2)))]
    out = []
    for (x, y, z, w, h, d, mat, rot) in boxes:
        y += 12
        fr = [8 + x * k, 8 + (12 - (y + h)) * k, 8 + z * k]
        to = [8 + (x + w) * k, 8 + (12 - y) * k, 8 + (z + d) * k]
        r = None
        if rot:
            ax, ang, (ox, oy, oz) = rot
            r = {"angle": -ang if ax == "y" else ang, "axis": ax, "origin": [8 + ox * k, 8 + (12 - (oy + 12)) * k, 8 + oz * k]}
        out.append(el(fr, to, mat, rot=r))
    return out


def core_missile():
    return [el([7.4, 7.4, 2.0], [8.6, 8.6, 13.0], "steel_hi"), el([7.5, 7.5, 0.6], [8.5, 8.5, 2.0], "shell_red"),
            el([6.6, 7.9, 11.0], [9.4, 8.1, 13.0], "graphite"), el([7.9, 6.6, 11.0], [8.1, 9.4, 13.0], "graphite"),
            el([7.6, 7.6, 13.0], [8.4, 8.4, 13.6], "cyan")]


# ---------------------------------------------------------------- Impulsor de Esquiva (gadget de cinturón) y su célula
def dash_thruster():
    return [el([4.0, 6.0, 7.0], [12.0, 7.0, 9.0], "leather"),                               # correa
            el([5.5, 5.0, 6.0], [10.5, 9.5, 10.0], "graphite"),                             # cuerpo
            el([5.8, 9.5, 6.4], [10.2, 10.0, 9.6], "steel_hi"),
            el([6.0, 3.6, 6.6], [7.6, 5.0, 8.4], "steel_dark"), el([8.4, 3.6, 6.6], [10.0, 5.0, 8.4], "steel_dark"),  # toberas
            el([6.3, 3.4, 6.9], [7.3, 3.6, 8.1], "cyan"), el([8.7, 3.4, 6.9], [9.7, 3.6, 8.1], "cyan"),
            el([7.4, 7.0, 5.8], [8.6, 8.6, 6.0], "cyan"),                                   # indicador de carga
            el([5.3, 6.4, 7.4], [5.5, 8.6, 8.6], "copper"), el([10.5, 6.4, 7.4], [10.7, 8.6, 8.6], "copper")]


def dash_cell():
    return [el([7.0, 3.0, 7.0], [9.0, 9.0, 9.0], "cyan"), el([6.8, 2.6, 6.8], [9.2, 3.4, 9.2], "copper"),
            el([6.8, 8.6, 6.8], [9.2, 9.4, 9.2], "copper"), el([7.6, 9.4, 7.6], [8.4, 10.0, 8.4], "steel_hi"),
            el([6.9, 5.6, 6.9], [9.1, 6.2, 9.1], "graphite")]


# ---------------------------------------------------------------- Motor Dimensional (botín de la Araña de Asedio)
def dimensional_drive():
    els = [el([5.0, 4.0, 5.0], [11.0, 6.0, 11.0], "graphite"), el([5.0, 10.0, 5.0], [11.0, 12.0, 11.0], "graphite"),  # bridas
           el([6.5, 6.0, 6.5], [9.5, 10.0, 9.5], "cyan"),                                  # núcleo de energía
           el([7.6, 12.0, 7.6], [8.4, 13.4, 8.4], "steel_hi")]
    for (x, z) in ((5.2, 5.2), (10.0, 5.2), (5.2, 10.0), (10.0, 10.0)):                      # columnas de inercita
        els.append(el([x, 6.0, z], [x + 0.8, 10.0, z + 0.8], "inercita"))
    els += [el([4.6, 7.4, 7.0], [5.0, 8.6, 9.0], "copper"), el([11.0, 7.4, 7.0], [11.4, 8.6, 9.0], "copper")]
    return els


# ---------------------------------------------------------------- más armas (tiers 1-3)
def revolver(open_=False):
    return build_weapon("revolver", reload=open_)


def flamethrower(tank_out=False):
    return build_weapon("flamethrower", reload=tank_out)


def light_machine_gun(mag_out=False):
    return build_weapon("light_machine_gun", reload=mag_out)


def kinetic_sniper(bolt_back=False):
    return build_weapon("kinetic_sniper", reload=bolt_back)


def kinetic_blade():
    return build_weapon("kinetic_blade")


def rocket_launcher(empty=False):
    return build_weapon("rocket_launcher", reload=empty)


def arc_caster(cell_out=False):
    return build_weapon("arc_caster", reload=cell_out)


def minigun(box_out=False, frame=0):
    return build_weapon("minigun", reload=box_out, spin=frame)




def fuel_canister():
    return [el([6.0, 2.0, 6.5], [10.0, 10.0, 9.5], "shell_red"), el([5.9, 5.0, 6.4], [10.1, 5.8, 9.6], "hazard"),
            el([7.4, 10.0, 7.4], [8.6, 11.4, 8.6], "steel"), el([6.6, 10.0, 7.7], [7.2, 11.0, 8.3], "steel_dark")]


def guided_rocket():
    return [el([7.4, 2.0, 7.4], [8.6, 11.0, 8.6], "olive"), el([7.5, 11.0, 7.5], [8.5, 12.6, 8.5], "shell_red"),
            el([6.6, 2.0, 7.9], [9.4, 4.0, 8.1], "steel"), el([7.9, 2.0, 6.6], [8.1, 4.0, 9.4], "steel"),
            el([7.35, 6.0, 7.35], [8.65, 6.4, 8.65], "hazard")]


# ---------------------------------------------------------------- machete (como una espada: diagonal en el plano XY)
def machete():
    return build_weapon("scrap_machete")


# ---------------------------------------------------------------- componentes en 3D
def cinetrite_shard():
    """Fragmento de cinetrita: racimo de cristales cian (uno alto, dos laterales) saliendo de un trozo de roca."""
    zr = lambda a, ox: {"angle": a, "axis": "z", "origin": [ox, 4, 8]}
    xr = lambda a: {"angle": a, "axis": "x", "origin": [8, 4, 8]}
    return [el([5.5, 2.0, 5.5], [10.5, 4.5, 10.5], "steel_dark"),                     # roca madre
            el([6.5, 1.5, 6.0], [9.5, 3.0, 11.0], "steel"),
            el([7.0, 4.0, 7.0], [9.0, 13.0, 9.0], "cyan", rot=zr(-22.5, 8)),          # cristal principal
            el([7.4, 13.0, 7.4], [8.6, 14.6, 8.6], "cyan", rot=zr(-22.5, 8)),         # punta
            el([7.6, 4.0, 7.6], [8.8, 9.5, 8.8], "cyan", rot=zr(22.5, 8.5)),          # lateral
            el([7.2, 4.0, 7.8], [8.4, 8.0, 9.0], "cyan", rot=xr(22.5)),               # trasero
            el([7.2, 4.0, 6.8], [8.2, 7.0, 7.8], "cyan", rot=xr(-22.5))]


def resistor():
    """Resistencia de 330 ohmios: cuerpo de cerámica con los extremos abombados, bandas naranja-naranja-marrón-dorado
    y patas de alambre."""
    return [el([5.0, 7.1, 7.1], [11.0, 8.9, 8.9], "ceramic"),
            el([4.2, 6.8, 6.8], [5.6, 9.2, 9.2], "ceramic"), el([10.4, 6.8, 6.8], [11.8, 9.2, 9.2], "ceramic"),
            el([6.0, 7.0, 7.0], [6.6, 9.0, 9.0], "copper"), el([7.2, 7.0, 7.0], [7.8, 9.0, 9.0], "copper"),   # 3 3
            el([8.4, 7.0, 7.0], [9.0, 9.0, 9.0], "wood_dark"),                                               # x10
            el([9.6, 7.0, 7.0], [10.0, 9.0, 9.0], "brass"),                                                  # 5 %
            el([0.5, 7.8, 7.8], [4.2, 8.2, 8.2], "steel_hi"), el([11.8, 7.8, 7.8], [15.5, 8.2, 8.2], "steel_hi")]


def piezo_transducer():
    """Transductor piezo-cúprico: disco de cobre, cerámica piezoeléctrica blanca, electrodo plateado con un punto de
    cinetrita y dos cables (rojo y negro)."""
    oct45 = {"angle": 45, "axis": "y", "origin": [8, 8, 8]}
    return [el([3.0, 7.0, 3.0], [13.0, 7.6, 13.0], "copper"),                        # disco (octógono: dos cuadrados)
            el([3.0, 7.0, 3.0], [13.0, 7.6, 13.0], "copper", rot=oct45),
            el([4.6, 7.6, 4.6], [11.4, 8.1, 11.4], "ceramic"),                        # cerámica
            el([4.6, 7.6, 4.6], [11.4, 8.1, 11.4], "ceramic", rot=oct45),
            el([6.0, 8.1, 6.0], [10.0, 8.4, 10.0], "steel_hi"),                       # electrodo
            el([7.6, 8.4, 7.6], [8.4, 8.7, 8.4], "cyan"),                             # cinetrita
            el([8.6, 8.4, 9.0], [9.2, 8.8, 15.5], "shell_red"),                       # cable rojo
            el([4.0, 7.6, 10.0], [4.6, 8.0, 15.5], "rubber")]                         # cable negro (al cobre)


def shell():
    return [el([7.2, 2.0, 7.2], [8.8, 3.4, 8.8], "brass"), el([7.3, 3.4, 7.3], [8.7, 10.0, 8.7], "shell_red"),
            el([7.4, 10.0, 7.4], [8.6, 10.3, 8.6], "wood_dark")]


def rifle_round():
    return [el([7.4, 2.0, 7.4], [8.6, 2.5, 8.6], "brass"), el([7.5, 2.5, 7.5], [8.5, 9.0, 8.5], "brass"),
            el([7.6, 9.0, 7.6], [8.4, 10.6, 8.4], "copper"), el([7.75, 10.6, 7.75], [8.25, 11.6, 8.25], "copper"),
            el([7.45, 8.6, 7.45], [8.55, 9.0, 8.55], "cyan")]


GUN_DISPLAY = {"thirdperson_righthand": {"translation": [0, -1.5, 0]}, "thirdperson_lefthand": {"translation": [0, -1.5, 0]},
               "firstperson_righthand": {"translation": [-1, 0, -3.25]}, "firstperson_lefthand": {"translation": [-0.5, 0, -3]},
               "ground": {"translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]}, "fixed": {"rotation": [0, 90, 0], "scale": [0.6, 0.6, 0.6]}}
HANDHELD = {"thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}, "fixed": {"rotation": [0, 180, 0]}}
SMALL = {"thirdperson_righthand": {"translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
         "firstperson_righthand": {"rotation": [0, -90, 15], "translation": [1, 3, 0], "scale": [0.6, 0.6, 0.6]},
         "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}}


# colgando del asa: en primera persona se sube para que el arma siga a la vista
MINIGUN_DISPLAY = dict(GUN_DISPLAY)
MINIGUN_DISPLAY["firstperson_righthand"] = {"translation": [-1, 5.5, -3.25]}
MINIGUN_DISPLAY["firstperson_lefthand"] = {"translation": [-0.5, 5.5, -3]}


def model(name, elements, display, overrides=None):
    group = max((g for g in GROUPS if name == g or name.startswith(g + "_")), key=len, default=name)
    weapon = group in GROUPS or group in EXTRA_WEAPONS
    if "--weapons-only" in sys.argv and not weapon:
        return
    gunkit.model(name, elements, display, overrides, group)


if __name__ == "__main__":
    # 8 combinaciones de módulos x (normal, recargando). Minecraft usa el ÚLTIMO override que encaja (>=):
    # se ordenan por máscara y luego por recarga.
    for base, fn, alt in (("pipe_shotgun", shotgun, "open"), ("bolt_rifle", rifle, "bolt"),
                          ("pump_shotgun", pump_shotgun, "pumped"), ("semi_rifle", semi_rifle, "mag_out")):
        overrides = []
        for m in range(8):
            name = base if m == 0 else f"{base}_m{m}"
            model(f"{name}_{alt}", fn(True, m), GUN_DISPLAY)
            if m:
                model(name, fn(False, m), GUN_DISPLAY)
                overrides.append({"predicate": {"tecnocinetico:modules": m, "tecnocinetico:reload": 0},
                                  "model": f"tecnocinetico:item/{name}"})
            overrides.append({"predicate": {"tecnocinetico:modules": m, "tecnocinetico:reload": 1},
                              "model": f"tecnocinetico:item/{name}_{alt}"})
        model(base, fn(False, 0), GUN_DISPLAY, overrides)
    for base, fn, alt in (("rail_rifle", rail_rifle, "mag_out"), ("assault_rifle", assault_rifle, "mag_out"),
                          ("grenade_launcher", grenade_launcher, "open"), ("pulse_cannon", pulse_cannon, "cell_out")):
        model(f"{base}_{alt}", fn(True), GUN_DISPLAY)
        model(base, fn(False), GUN_DISPLAY,
              [{"predicate": {"tecnocinetico:reload": 1}, "model": f"tecnocinetico:item/{base}_{alt}"}])
    model("core_fighter", core_fighter_mini(), SMALL)
    model("minigun_box_out", minigun(True), MINIGUN_DISPLAY)
    model("minigun_s1", minigun(False, 1), MINIGUN_DISPLAY)
    model("minigun_s2", minigun(False, 2), MINIGUN_DISPLAY)
    model("minigun", minigun(False), MINIGUN_DISPLAY,
          [{"predicate": {"tecnocinetico:spin": 0.5}, "model": "tecnocinetico:item/minigun_s1"},
           {"predicate": {"tecnocinetico:spin": 1.0}, "model": "tecnocinetico:item/minigun_s2"},
           {"predicate": {"tecnocinetico:reload": 1}, "model": "tecnocinetico:item/minigun_box_out"}])
    for base, fn, alt in (("revolver", revolver, "open"), ("flamethrower", flamethrower, "tank_out"),
                          ("light_machine_gun", light_machine_gun, "mag_out"), ("kinetic_sniper", kinetic_sniper, "bolt"),
                          ("rocket_launcher", rocket_launcher, "empty"), ("arc_caster", arc_caster, "cell_out")):
        model(f"{base}_{alt}", fn(True), GUN_DISPLAY)
        model(base, fn(False), GUN_DISPLAY,
              [{"predicate": {"tecnocinetico:reload": 1}, "model": f"tecnocinetico:item/{base}_{alt}"}])
    model("kinetic_blade", kinetic_blade(), HANDHELD)
    model("fuel_canister", fuel_canister(), SMALL)
    model("guided_rocket", guided_rocket(), SMALL)
    model("dash_thruster", dash_thruster(), SMALL)
    model("dimensional_drive", dimensional_drive(), SMALL)
    model("dash_cell", dash_cell(), SMALL)
    model("core_missile", core_missile(), SMALL)
    model("rail_slug", rail_slug(), SMALL)
    model("kinetic_grenade", kinetic_grenade(), SMALL)
    model("pulse_cell", pulse_cell(True), SMALL)
    model("empty_pulse_cell", pulse_cell(False), SMALL)
    model("kinetic_smg_mag_out", smg(True), GUN_DISPLAY)
    model("kinetic_smg", smg(False), GUN_DISPLAY,
          [{"predicate": {"tecnocinetico:reload": 1}, "model": "tecnocinetico:item/kinetic_smg_mag_out"}])
    # objetos de los módulos: solo sus piezas, centradas
    module_parts = {
        "choke": [el([6.9, 7.4, 6.4], [8.0, 8.6, 7.6], "copper", front="black"), el([8.0, 7.4, 6.4], [9.1, 8.6, 7.6], "copper", front="black")],
        "sawed_off": [el([6.5, 7.5, 4.0], [7.45, 8.5, 12.0], "steel", front="black"), el([7.55, 7.5, 4.0], [8.5, 8.5, 12.0], "steel", front="black"),
                      el([6.45, 7.45, 4.0], [8.55, 8.55, 4.4], "rust")],
        "speed_loader": [el([7.7, 6.0, 5.0], [8.0, 10.0, 11.0], "leather")] + [el([8.0, 6.6, z], [8.6, 9.4, z + 1.1], "shell_red") for z in (5.6, 7.6, 9.6)],
        "scope": [el([7.3, 7.3, 3.0], [8.7, 8.7, 13.0], "steel_dark"), el([7.1, 7.1, 1.6], [8.9, 8.9, 3.0], "steel", front="cyan"),
                  el([7.2, 7.2, 13.0], [8.8, 8.8, 14.0], "steel")],
        "polished_bolt": [el([7.8, 7.7, 5.0], [8.2, 8.3, 11.0], "brass"), el([8.2, 7.7, 10.0], [9.6, 8.1, 10.6], "brass"),
                          el([9.4, 7.3, 9.9], [10.0, 8.0, 10.7], "cyan")],
        "rifle_barrel": [el([7.6, 7.6, 1.0], [8.4, 8.4, 14.0], "inercita", front="black"), el([7.3, 7.3, 0.0], [8.7, 8.7, 1.4], "steel_hi", front="black")],
    }
    for mid, parts in module_parts.items():
        model(f"module_{mid}", parts, SMALL)
    model("scrap_machete", machete(), HANDHELD)
    model("shotgun_shell", shell(), SMALL)
    # fragmento, resistencia y transductor: arte plano (tools/parts_art.py; el usuario lo prefiere en estos objetos)
    model("rifle_round", rifle_round(), SMALL)
    flat = []                                                    # armaduras: modelo 3D de inventario en tools/armor3d_gen.py
    flat += ["inercita_plate", "precision_mechanism", "kinetic_servo", "ballistic_fabric"]          # tools/armor_t2_gen.py
    flat += ["core_processor"]                                                                       # tools/tier3_art.py
    for name in flat:
        if "--weapons-only" in sys.argv:
            continue
        (ROOT / "models/item" / f"{name}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"tecnocinetico:item/{name}"}}, indent=2), encoding="utf-8")
    gunkit.flush()
    print("ok: armas (texturas propias por arma)")
