"""Armas tier 1 en 3D: textura atlas textures/item/weapons_t1.png (64x64, zonas de 8x8) y modelos de
pipe_shotgun (+ _open), bolt_rifle (+ _bolt), scrap_machete, shotgun_shell y rifle_round; modelos planos de la
armadura de chatarra (iconos de tools/armor_gen.py).

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
from gunkit import el, scaled  # noqa: E402,F401  (cada arma se pinta en su propia textura item/gun/<grupo>.png)

# grupos de textura: todas las variantes de un arma comparten su textura
GROUPS = ["pipe_shotgun", "bolt_rifle", "pump_shotgun", "semi_rifle", "rail_rifle", "assault_rifle", "grenade_launcher",
          "pulse_cannon", "revolver", "flamethrower", "light_machine_gun", "kinetic_sniper", "rocket_launcher", "arc_caster",
          "minigun", "kinetic_smg"]


GRIP_ROT = {"angle": -22.5, "axis": "x", "origin": [8, 11, 11]}


def trigger_group():
    return [el([7.6, 9.4, 7.5], [8.4, 9.8, 10.6], "steel_dark"), el([7.6, 9.4, 7.3], [8.4, 11.0, 7.7], "steel_dark"),
            el([7.85, 9.9, 8.6], [8.15, 11.0, 9.0], "steel_hi")]


# ---------------------------------------------------------------- escopeta de caño (dos cañones, basculante)
CHOKE, SAWED, LOADER = 1, 2, 4                      # máscara local (orden de WeaponModule: CHOKE, SAWED_OFF, SPEED_LOADER)


def shotgun(open_=False, mods=0):
    hinge = {"angle": -22.5, "axis": "x", "origin": [8, 11.7, 6]} if open_ else None     # los cañones bajan
    front = -4.0 if mods & SAWED else -12.0                                               # recortados: cañones cortos
    # hecha a mano: dos tubos oxidados unidos con abrazaderas de manguera y cinta, guardamanos de tablón clavado
    barrels = [el([7.0, 12.4, front], [7.95, 13.4, 6], "rust", front="black", rot=hinge),
               el([8.05, 12.4, front], [9.0, 13.4, 6], "rust", front="black", rot=hinge),
               el([7.8, 13.4, front], [8.2, 13.6, 6], "steel", rot=hinge),                   # cordón de soldadura
               el([7.9, 13.6, front + 0.2], [8.1, 13.9, front + 0.6], "brass", rot=hinge),
               el([6.9, 12.3, 1.0], [9.1, 13.5, 2.0], "tape", rot=hinge),
               el([7.1, 11.3, max(front + 1, -4.0)], [8.9, 12.4, 4.0], "wood_dark", rot=hinge),
               el([8.9, 11.6, 0.0], [9.05, 11.8, 0.2], "steel_hi", rot=hinge),               # clavos
               el([8.9, 11.6, 3.0], [9.05, 11.8, 3.2], "steel_hi", rot=hinge)]
    for z in (-9.5, -2.6):                                                                 # abrazaderas de manguera
        if z > front + 0.5:
            barrels += [el([6.85, 12.25, z], [9.15, 13.55, z + 0.5], "steel_hi", rot=hinge),
                        el([9.15, 12.6, z - 0.1], [9.6, 13.1, z + 0.6], "steel", rot=hinge)]
    if not mods & SAWED:
        barrels.append(el([6.9, 12.3, -6.4], [9.1, 13.5, -5.4], "tape", rot=hinge))
    else:
        barrels.append(el([6.95, 12.35, front], [9.05, 13.45, front + 0.4], "rust", rot=hinge))  # corte tosco
    if mods & CHOKE:                                                                       # anillas del estrangulador
        barrels += [el([6.9, 12.3, front - 0.6], [8.0, 13.5, front], "copper", front="black", rot=hinge),
                    el([8.0, 12.3, front - 0.6], [9.1, 13.5, front], "copper", front="black", rot=hinge)]
    action = [el([6.9, 11.0, 6.0], [9.1, 13.6, 9.5], "steel_dark"),
              el([6.8, 11.4, 5.6], [9.2, 12.0, 6.4], "steel_hi"),
              el([7.6, 13.6, 8.6], [8.4, 14.2, 9.4], "steel_dark")]
    stock = [el([7.1, 7.6, 9.4], [8.9, 11.0, 12.6], "wood", rot=GRIP_ROT),
             el([7.05, 8.6, 9.6], [8.95, 9.4, 12.4], "tape", rot=GRIP_ROT),
             el([7.0, 8.6, 12.0], [9.0, 12.0, 21.0], "wood"),
             el([7.2, 12.0, 13.0], [8.8, 12.6, 20.0], "wood_dark"),
             el([6.95, 10.2, 12.4], [9.05, 11.4, 13.6], "tape"),                           # remiendo de cinta
             el([9.0, 9.2, 15.0], [9.1, 9.4, 15.2], "steel_hi"), el([9.0, 11.2, 15.0], [9.1, 11.4, 15.2], "steel_hi"),
             el([9.0, 9.2, 19.4], [9.1, 9.4, 19.6], "steel_hi"), el([9.0, 11.2, 19.4], [9.1, 11.4, 19.6], "steel_hi"),
             el([6.9, 8.0, 21.0], [9.1, 12.2, 22.0], "rubber")]
    if mods & LOADER:                                                                      # cartuchera en la culata
        stock.append(el([9.0, 9.2, 14.0], [9.3, 11.6, 19.5], "leather"))
        for z in (14.4, 16.2, 18.0):
            stock.append(el([9.3, 9.5, z], [9.9, 11.3, z + 1.1], "shell_red"))
    return barrels + action + stock + trigger_group()


# ---------------------------------------------------------------- rifle de cerrojo
SCOPE, BOLT, BARREL = 1, 2, 4                       # máscara local (orden: SCOPE, POLISHED_BOLT, RIFLE_BARREL)


def rifle(bolt_back=False, mods=0):
    barrel_mat = "inercita" if mods & BARREL else "steel_dark"
    bolt_mat = "brass" if mods & BOLT else "steel_hi"
    els = [el([7.6, 12.7, -15.4], [8.4, 13.5, 4.0], barrel_mat, front="black"),          # cañón (límite: z >= -16)
           el([7.9, 13.5, -15.0], [8.1, 14.4, -14.5], "steel_hi"),
           el([7.5, 13.8, 3.0], [7.8, 14.5, 3.6], "steel"), el([8.2, 13.8, 3.0], [8.5, 14.5, 3.6], "steel"),
           el([7.3, 12.2, 4.0], [8.7, 14.0, 11.0], "steel_dark"),
           el([7.4, 11.0, 6.0], [8.6, 12.2, 9.0], "steel"),
           el([7.1, 11.6, -9.0], [8.9, 13.0, 4.0], "wood"),
           el([7.0, 11.9, -2.0], [9.0, 12.5, -1.4], "copper"),
           el([7.2, 8.0, 10.5], [8.8, 12.2, 13.5], "wood", rot=GRIP_ROT),
           el([7.1, 9.0, 13.0], [8.9, 12.4, 23.0], "wood"),
           el([7.3, 12.4, 14.0], [8.7, 13.0, 21.0], "wood_dark"),
           el([7.0, 8.2, 23.0], [9.0, 12.6, 23.6], "steel_dark"),
           el([7.7, 11.35, -6.0], [8.3, 11.6, -2.0], "cyan"),
           # rifle de caza clásico: madera hasta casi la boca, abrazaderas, guardamanos superior, alza de hoja, anillas
           el([7.4, 13.5, -6.0], [8.6, 13.9, 2.0], "wood_dark"),                                # guardamanos superior
           el([7.0, 11.5, -8.0], [9.0, 14.0, -7.4], "steel"), el([7.0, 11.5, -3.6], [9.0, 14.0, -3.0], "steel"),  # abrazaderas
           el([7.05, 11.55, -9.4], [8.95, 13.1, -9.0], "steel_dark"),                          # nariz del guardamanos
           el([7.6, 13.5, -0.6], [8.4, 14.3, 0.0], "steel_dark"),                              # alza de hoja
           el([7.85, 10.9, -6.6], [8.15, 11.6, -5.8], "steel"), el([7.85, 8.4, 18.5], [8.15, 9.1, 19.3], "steel")]  # anillas
    if mods & BARREL:                                                                     # freno de boca
        els.append(el([7.3, 12.4, -16.0], [8.7, 13.8, -14.6], "steel_hi", front="black"))
    else:
        els.append(el([7.5, 12.6, -16.0], [8.5, 13.6, -15.3], "steel"))
    if mods & SCOPE:                                                                      # mira telescópica
        els += [el([7.6, 14.0, 5.0], [8.4, 14.6, 5.6], "steel_dark"), el([7.6, 14.0, 9.0], [8.4, 14.6, 9.6], "steel_dark"),
                el([7.3, 14.6, 2.0], [8.7, 16.0, 12.0], "steel_dark"),
                el([7.1, 14.4, 0.6], [8.9, 16.2, 2.0], "steel", front="cyan"),
                el([7.2, 14.5, 12.0], [8.8, 16.1, 13.0], "steel")]
    if bolt_back:
        els += [el([8.7, 13.0, 9.0], [9.1, 13.6, 13.0], bolt_mat),
                el([8.9, 13.0, 12.0], [9.4, 14.6, 12.6], bolt_mat), el([8.8, 14.4, 11.9], [9.6, 15.1, 12.7], "steel")]
    else:
        els += [el([8.7, 13.0, 7.0], [9.1, 13.6, 11.0], bolt_mat),
                el([9.1, 12.6, 10.0], [10.4, 13.0, 10.6], bolt_mat),
                el([10.1, 12.0, 9.8], [10.9, 12.9, 10.8], "cyan" if mods & BOLT else "steel_hi")]  # bola de la palanca
    return els + trigger_group()


# ---------------------------------------------------------------- tier 2: escopeta de corredera (módulos de escopeta)
def pump_shotgun(pumped=False, mods=0):
    """Escopeta táctica de corredera: banda ventilada sobre el cañón, guardamanos de polímero estriado, linterna
    bajo el cañón, alza de anillo fantasma y culata de polímero con cantonera de goma."""
    front = -5.0 if mods & SAWED else -14.0
    dz = 3.0 if pumped else 0.0                                                            # guardamanos atrás
    els = [el([7.45, 12.6, front], [8.55, 13.7, 4.0], "steel_dark", front="black"),          # cañón
           el([7.8, 14.0, front + 0.5], [8.2, 14.15, 4.0], "steel_hi"),                      # banda ventilada
           el([7.88, 14.15, front + 0.5], [8.12, 14.45, front + 0.9], "brass"),              # punto de mira
           el([7.55, 11.6, max(front + 2.0, -10.0)], [8.45, 12.6, 4.0], "steel"),             # depósito tubular
           el([7.5, 11.5, max(front + 1.6, -10.4)], [8.5, 12.7, max(front + 2.0, -10.0)], "steel_dark"),  # tapón
           el([7.15, 11.3, -8.0 + dz], [8.85, 12.9, -3.0 + dz], "graphite"),                  # guardamanos (corredera)
           el([7.2, 11.6, -3.0 + dz], [8.8, 12.6, -2.4 + dz], "steel_dark"),                 # barras de acción
           el([7.2, 11.0, 4.0], [8.8, 14.0, 10.0], "steel_dark"),                            # cajón de mecanismos
           el([8.8, 12.6, 5.5], [8.9, 13.4, 8.0], "black"),                                  # ventana de expulsión
           el([8.82, 11.5, 5.0], [8.92, 11.8, 9.0], "cyan"),                                 # línea de cinetrita
           el([7.3, 14.0, 8.4], [7.6, 15.1, 9.2], "steel_dark"), el([8.4, 14.0, 8.4], [8.7, 15.1, 9.2], "steel_dark"),  # alas del alza
           el([7.6, 14.8, 8.5], [8.4, 15.1, 9.1], "steel"),                                  # anillo fantasma
           el([7.2, 7.8, 9.6], [8.8, 11.2, 12.6], "rubber", rot=GRIP_ROT),                   # empuñadura
           el([7.1, 9.0, 12.0], [8.9, 12.2, 21.0], "graphite"),                              # culata de polímero
           el([7.3, 12.2, 12.0], [8.7, 12.6, 18.0], "steel_dark"),
           el([7.05, 9.6, 15.0], [7.15, 11.6, 19.0], "black"), el([8.85, 9.6, 15.0], [8.95, 11.6, 19.0], "black"),  # aligerado
           el([7.0, 8.4, 21.0], [9.0, 12.4, 22.0], "rubber")]                                # cantonera
    for z in range(int(front) + 1, 4, 2):                                                   # postes de la banda ventilada
        els.append(el([7.9, 13.7, z], [8.1, 14.0, z + 0.4], "steel_dark"))
    for k in range(4):                                                                     # estrías del guardamanos
        z = -7.6 + k * 1.3 + dz
        els.append(el([7.1, 11.25, z], [8.9, 12.95, z + 0.4], "rubber"))
    lz = max(front + 2.0, -10.0)                                                           # linterna táctica
    els += [el([7.55, 10.9, lz], [8.45, 11.6, lz + 2.4], "graphite", front="edge"),
            el([7.5, 10.85, lz + 2.0], [8.5, 11.65, lz + 2.4], "steel")]
    if mods & CHOKE:
        els.append(el([7.35, 12.5, front - 0.8], [8.65, 13.8, front], "copper", front="black"))
    if mods & SAWED:
        els.append(el([7.4, 12.55, front], [8.6, 13.75, front + 0.4], "rust"))               # corte
    if mods & LOADER:                                                                      # portacartuchos lateral
        els.append(el([6.9, 11.6, 4.5], [7.2, 13.4, 9.0], "steel"))
        for z in (4.8, 6.0, 7.2, 8.4):
            els.append(el([6.4, 11.8, z], [6.9, 13.2, z + 0.9], "shell_red"))
    return els + trigger_group()


# ---------------------------------------------------------------- tier 2: rifle semiautomático (módulos de rifle)
def semi_rifle(mag_out=False, mods=0):
    """Fusil semiautomático de posguerra: madera completa con tapa del tubo de gases, bayoneta plegable bajo
    el cañón, guía de peines en el cajón, alza de diópter y cargador de 10."""
    barrel_mat = "inercita" if mods & BARREL else "steel_dark"
    handle = "brass" if mods & BOLT else "steel_hi"
    els = [el([7.6, 12.8, -15.4], [8.4, 13.6, 2.0], barrel_mat, front="black"),            # cañón
           el([7.75, 13.6, -8.0], [8.25, 14.0, 2.0], "steel"),                               # tubo de gases
           el([7.6, 13.5, -8.8], [8.4, 14.1, -8.0], "steel_dark"),                           # bloque de gases
           el([7.5, 14.0, -7.4], [8.5, 14.45, 1.4], "wood_dark"),                            # tapa de madera
           el([7.85, 13.6, -14.8], [8.15, 14.6, -14.2], "steel_hi"),                         # punto de mira
           el([7.7, 13.55, -15.0], [8.3, 13.75, -14.0], "steel_dark"),
           el([7.1, 12.0, -9.0], [8.9, 14.0, 2.0], "wood"),                                  # guardamanos
           el([7.0, 11.95, -8.8], [9.0, 14.05, -8.3], "steel"),                              # abrazadera
           el([7.05, 12.6, -6.0], [7.15, 12.9, -1.0], "cyan"),
           el([7.25, 11.8, 2.0], [8.75, 14.0, 10.0], "steel_dark"),                          # cajón
           el([7.5, 14.0, 3.0], [8.5, 14.3, 4.4], "steel_hi"),                               # guía de peines
           el([8.75, 13.0, 4.5], [9.6, 13.5, 5.2], handle),                                  # palanca de carga
           el([9.4, 12.9, 4.4], [9.9, 13.6, 5.3], handle),
           el([7.2, 8.0, 10.5], [8.8, 12.0, 13.5], "wood", rot=GRIP_ROT),
           el([7.1, 9.0, 13.0], [8.9, 12.4, 23.0], "wood"),
           el([7.3, 12.4, 14.0], [8.7, 13.0, 20.0], "wood_dark"),
           el([7.85, 8.5, 18.6], [8.15, 9.0, 19.4], "steel"),                                # anilla del portafusil
           el([7.0, 8.2, 23.0], [9.0, 12.6, 23.6], "steel")]                                  # cantonera de chapa
    # bayoneta plegable (hoja bajo el cañón, bisagra delante del guardamanos)
    els += [el([7.6, 11.2, -10.2], [8.4, 12.2, -9.0], "steel_dark"),
            el([7.75, 11.45, -15.6], [8.25, 12.0, -10.2], "blade"),
            el([7.8, 11.25, -15.2], [8.2, 11.45, -10.2], "edge")]
    if not mag_out:
        els += [el([7.35, 9.0, 4.0], [8.65, 11.8, 6.6], "steel_dark"),                     # cargador de 10
                el([7.3, 8.8, 3.9], [8.7, 9.2, 6.7], "steel")]
    if mods & BARREL:
        els.append(el([7.3, 12.5, -16.0], [8.7, 13.9, -14.6], "steel_hi", front="black"))
    else:
        els.append(el([7.5, 12.7, -16.0], [8.5, 13.7, -15.3], "steel"))
    if mods & SCOPE:
        els += [el([7.6, 14.0, 5.0], [8.4, 14.6, 5.6], "steel_dark"), el([7.6, 14.0, 9.0], [8.4, 14.6, 9.6], "steel_dark"),
                el([7.3, 14.6, 2.0], [8.7, 16.0, 12.0], "steel_dark"),
                el([7.1, 14.4, 0.6], [8.9, 16.2, 2.0], "steel", front="cyan"),
                el([7.2, 14.5, 12.0], [8.8, 16.1, 13.0], "steel")]
    else:                                                                                  # alza de diópter
        els += [el([7.4, 14.0, 8.2], [7.7, 15.2, 9.4], "steel_dark"), el([8.3, 14.0, 8.2], [8.6, 15.2, 9.4], "steel_dark"),
                el([7.7, 14.9, 8.5], [8.3, 15.2, 9.1], "steel"), el([7.7, 14.0, 8.5], [8.3, 14.3, 9.1], "steel")]
    return els + trigger_group()


# ---------------------------------------------------------------- tier 2: subfusil (sin módulos)
def smg(mag_out=False):
    """Subfusil: cuerpo anguloso de polímero, cañón corto con bobina de cinetrita, cargador recto inclinado,
    empuñadura delantera y culata de alambre plegable."""
    els = [el([7.65, 12.65, -6.6], [8.35, 13.35, -1.0], "steel_dark", front="black"),          # cañón
           el([7.45, 12.45, -7.2], [8.55, 13.55, -6.6], "copper", front="black"),             # anillo de boca
           el([7.2, 11.4, -2.0], [8.8, 14.0, 4.0], "graphite"),                               # cuerpo delantero
           el([7.25, 11.0, 4.0], [8.75, 14.2, 10.0], "steel_dark"),                           # cajón
           el([7.3, 10.6, -1.6], [8.7, 11.4, 2.6], "graphite"),                               # quilla
           el([7.5, 14.0, -1.0], [8.5, 14.3, 9.0], "steel"),                                  # riel
           el([7.85, 14.3, -0.8], [8.15, 15.0, -0.3], "steel_hi"),                            # punto de mira
           el([7.55, 14.3, 8.3], [8.45, 15.0, 9.0], "steel"),                                 # alza
           el([8.8, 12.3, -1.6], [8.9, 13.2, 3.6], "cyan"),                                   # bobina
           el([8.82, 12.1, -1.8], [8.95, 12.3, 3.8], "copper"), el([8.82, 13.2, -1.8], [8.95, 13.4, 3.8], "copper"),
           el([8.75, 13.2, 5.0], [9.3, 13.6, 6.0], "steel_hi"),                               # palanca de carga
           el([7.4, 9.4, -1.4], [8.6, 11.4, 0.4], "rubber"),                                  # empuñadura delantera
           el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
           el([7.6, 11.0, 10.0], [7.9, 11.4, 18.6], "steel"), el([8.1, 11.0, 10.0], [8.4, 11.4, 18.6], "steel"),
           el([7.6, 12.8, 10.0], [7.9, 13.2, 18.0], "steel"), el([8.1, 12.8, 10.0], [8.4, 13.2, 18.0], "steel"),
           el([7.4, 10.6, 18.6], [8.6, 13.4, 19.2], "steel_dark")]                            # cantonera
    if not mag_out:
        rot = {"angle": 22.5, "axis": "x", "origin": [8, 11.0, 6.0]}
        els += [el([7.45, 6.6, 5.0], [8.55, 11.0, 6.8], "steel", rot=rot),                    # cargador inclinado
                el([7.4, 6.4, 4.9], [8.6, 6.8, 6.9], "steel_dark", rot=rot)]
    return els + trigger_group()


# ---------------------------------------------------------------- tier 3: rifle de riel
def rail_rifle(mag_out=False):
    """Rifle de riel bullpup: dos raíles de inercita al aire con el canal de aceleración brillando entre ellos,
    abrazaderas laterales, dos condensadores de cobre a los costados y la batería detrás de la empuñadura."""
    els = [el([7.2, 13.4, -15.6], [8.8, 14.0, 2.0], "inercita"),                              # raíl superior
           el([7.2, 11.2, -15.6], [8.8, 11.8, 2.0], "inercita"),                              # raíl inferior
           el([7.75, 11.8, -15.0], [8.25, 13.4, 2.0], "cyan"),                                # canal de aceleración
           el([7.0, 11.0, -16.0], [7.3, 14.2, -15.0], "steel_dark"), el([8.7, 11.0, -16.0], [9.0, 14.2, -15.0], "steel_dark"),
           el([7.3, 13.9, -16.0], [8.7, 14.2, -15.0], "steel_dark"),                          # boca abierta
           el([7.0, 10.6, 2.0], [9.0, 14.4, 21.0], "graphite"),                               # cuerpo bullpup
           el([8.95, 12.6, 4.0], [9.05, 13.2, 8.0], "hazard"),
           el([6.95, 11.4, 3.0], [7.0, 13.8, 9.0], "black"), el([9.0, 11.4, 3.0], [9.05, 13.8, 9.0], "black"),  # rejillas
           el([7.5, 14.4, 3.0], [8.5, 15.6, 9.5], "steel_dark"),                              # mira integrada
           el([7.6, 14.5, 2.6], [8.4, 15.5, 3.0], "cyan"),
           el([7.3, 14.4, 13.0], [8.7, 14.8, 19.0], "rubber"),                                # carrillera
           el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
           el([6.9, 10.2, 21.0], [9.1, 14.8, 22.0], "rubber")]                                # cantonera
    for z in (-11.0, -6.0, -1.0):                                                          # abrazaderas de los raíles
        els += [el([7.0, 11.0, z], [7.25, 14.2, z + 0.8], "steel_dark"), el([8.75, 11.0, z], [9.0, 14.2, z + 0.8], "steel_dark")]
    for x0, x1 in ((6.3, 7.0), (9.0, 9.7)):                                                # condensadores laterales
        els += [el([x0, 11.6, -7.0], [x1, 13.6, 1.0], "copper"),
                el([x0 + 0.05, 11.7, -7.4], [x1 - 0.05, 13.5, -7.0], "cyan"),
                el([x0, 12.4, 1.0], [x1, 12.8, 2.4], "steel")]
    if not mag_out:                                                                        # batería tras la empuñadura
        els += [el([7.25, 8.4, 13.0], [8.75, 10.6, 17.0], "steel"),
                el([7.2, 9.0, 13.4], [8.8, 9.6, 16.6], "cyan")]
    return els + trigger_group()


# ---------------------------------------------------------------- tier 3: fusil de asalto
def assault_rifle(mag_out=False):
    """Fusil de asalto: cañón con apagallamas, guardamanos con raíl y ranuras, cajón superior e inferior,
    culata telescópica sobre tubo, cargador curvo de polímero en dos tramos."""
    els = [el([7.65, 12.75, -14.6], [8.35, 13.45, -4.0], "steel_dark", front="black"),          # cañón
           el([7.45, 12.55, -16.0], [8.55, 13.65, -14.6], "steel", front="black"),               # apagallamas
           el([7.4, 12.5, -15.4], [8.6, 12.7, -15.0], "steel_dark"), el([7.4, 13.5, -15.4], [8.6, 13.7, -15.0], "steel_dark"),
           el([7.5, 12.6, -8.8], [8.5, 13.9, -7.8], "steel"),                                     # bloque de gases
           el([7.1, 12.0, -9.0], [8.9, 14.1, 1.0], "graphite"),                                  # guardamanos
           el([7.45, 14.1, -9.0], [8.55, 14.4, 9.6], "steel_dark")]                              # raíl superior
    for z in range(-8, 9, 2):
        els.append(el([7.38, 14.4, z], [8.62, 14.62, z + 0.9], "steel_dark"))                     # dientes del raíl
    for z in (-7.5, -5.0, -2.5):
        els += [el([8.9, 12.55, z], [8.98, 13.35, z + 1.6], "black"), el([7.02, 12.55, z], [7.1, 13.35, z + 1.6], "black")]
    els += [el([8.92, 12.95, -6.0], [9.0, 13.15, -0.5], "cyan"),                                 # vena de cinetrita
            el([7.25, 12.3, 1.0], [8.75, 14.1, 9.0], "steel_dark"),                              # cajón superior
            el([7.3, 11.2, 2.4], [8.7, 12.3, 9.6], "steel"),                                     # cajón inferior
            el([7.35, 10.4, 3.0], [8.65, 11.2, 5.8], "steel_dark"),                              # brocal
            el([8.75, 12.9, 3.6], [8.85, 13.6, 6.6], "steel"),                                   # ventana de expulsión
            el([8.75, 12.5, 7.4], [9.35, 13.2, 8.4], "steel_hi"),                                # asistente de cierre
            el([7.7, 14.1, 9.3], [8.3, 14.5, 10.5], "steel_hi"),                                 # palanca de carga
            el([7.8, 14.62, -8.5], [8.2, 15.6, -7.9], "steel_dark"),                             # punto de mira abatible
            el([7.55, 14.62, 7.4], [8.45, 15.5, 8.6], "steel_dark"),                             # alza
            el([7.25, 8.0, 9.6], [8.75, 11.6, 12.2], "rubber", rot=GRIP_ROT),                    # empuñadura
            el([7.55, 12.0, 9.6], [8.45, 12.9, 17.5], "steel_dark"),                             # tubo de la culata
            el([7.25, 10.2, 14.0], [8.75, 13.2, 20.2], "graphite"),                              # culata
            el([7.4, 13.2, 15.0], [8.6, 13.7, 19.0], "graphite"),                                # carrillera
            el([7.2, 9.6, 20.2], [8.8, 13.6, 21.0], "rubber"),                                   # cantonera
            el([7.8, 11.4, 18.0], [8.2, 11.8, 18.8], "steel")]                                   # anilla de portafusil
    if not mag_out:
        els += [el([7.4, 8.6, 3.2], [8.6, 10.4, 5.6], "graphite"),                               # cargador curvo
                el([7.4, 7.0, 3.9], [8.6, 8.8, 6.3], "graphite", rot={"angle": -22.5, "axis": "x", "origin": [8, 8.6, 4.4]}),
                el([7.35, 6.9, 4.0], [8.65, 7.3, 6.4], "steel_dark", rot={"angle": -22.5, "axis": "x", "origin": [8, 8.6, 4.4]})]
    return els + trigger_group()


# ---------------------------------------------------------------- tier 3: lanzagranadas
def grenade_launcher(open_=False):
    """Lanzagranadas de tambor: cañón corto sobre un tambor giratorio de seis recámaras entre dos placas,
    alza de escalera, empuñadura delantera y culata telescópica. Al recargar el bastidor delantero bascula."""
    hinge = {"angle": -22.5, "axis": "x", "origin": [8, 9.4, 0]} if open_ else None
    els = [el([7.1, 12.4, -9.0], [8.9, 14.2, -0.4], "steel_dark", front="black", rot=hinge),   # cañón
           el([7.05, 12.35, -7.4], [8.95, 14.25, -6.8], "hazard", rot=hinge),
           el([7.4, 9.8, -6.0], [8.6, 12.4, -4.4], "rubber", rot=hinge),                     # empuñadura delantera
           el([6.4, 9.4, -0.4], [9.6, 14.4, 0.0], "steel_dark", rot=hinge),                  # placa delantera
           el([6.6, 13.0, -2.4], [7.0, 15.8, -2.0], "steel", rot=hinge),                     # alza de escalera
           el([6.6, 13.0, -1.0], [7.0, 15.8, -0.6], "steel", rot=hinge)]
    for y in (13.8, 14.6, 15.4):
        els.append(el([6.55, y, -2.4], [7.05, y + 0.2, -0.6], "steel_hi", rot=hinge))
    els += [el([6.2, 9.8, 0.0], [9.8, 13.4, 6.0], "olive"),                                 # tambor
            el([6.6, 9.4, 0.4], [9.4, 13.8, 5.6], "olive"),
            el([6.15, 11.3, 1.0], [9.85, 11.9, 5.0], "steel_dark"),                          # estrías
            el([6.4, 9.4, 6.0], [9.6, 14.4, 6.6], "steel_dark"),                             # placa trasera
            el([7.7, 9.0, -0.4], [8.3, 9.4, 6.6], "steel"),                                  # eje inferior
            el([7.7, 14.0, 0.0], [8.3, 14.4, 6.0], "steel")]                                 # puente superior
    for (x, y) in ((9.3, 11.6), (8.65, 12.73), (7.35, 12.73), (6.7, 11.6), (7.35, 10.47), (8.65, 10.47)):
        els.append(el([x - 0.35, y - 0.35, -0.1], [x + 0.35, y + 0.35, 0.0], "black"))       # bocas de las recámaras
    els += [el([7.2, 11.4, 6.6], [8.8, 14.0, 10.0], "steel_dark"),                           # armazón
            el([8.85, 12.4, 6.8], [8.95, 12.7, 9.6], "cyan"),
            el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
            el([7.6, 12.0, 10.0], [8.4, 12.8, 16.0], "steel"),                                # tubo telescópico
            el([7.7, 11.0, 15.0], [8.3, 12.0, 17.6], "steel"),
            el([7.3, 10.2, 17.6], [8.7, 13.6, 18.6], "rubber")]
    return els + trigger_group()


# ---------------------------------------------------------------- tier 3: cañón de impulso
def pulse_cannon(cell_out=False):
    """Cañón de impulso: emisor de anillos de cobre escalonados con un núcleo luminoso, aletas disipadoras en
    el lomo, tubos de refrigeración por los costados y célula de energía bajo el cajón."""
    els = [el([6.0, 10.3, -12.4], [10.0, 15.5, -11.6], "copper"),                            # anillos del emisor
           el([6.4, 10.7, -11.0], [9.6, 15.1, -10.2], "copper"),
           el([6.8, 11.1, -9.6], [9.2, 14.7, -8.8], "copper"),
           el([6.6, 10.9, -12.5], [9.4, 14.9, -12.4], "cyan"),                              # disco del emisor
           el([7.4, 12.2, -12.4], [8.6, 13.6, -8.8], "cyan"),                               # núcleo
           el([6.9, 11.4, -8.8], [9.1, 14.4, 4.0], "graphite"),                             # cuerpo
           el([9.1, 11.7, -6.0], [9.15, 12.4, 1.0], "cyan"),                                # ventana de energía
           el([6.85, 11.7, -6.0], [6.9, 12.4, 1.0], "cyan"),
           el([7.2, 11.0, 4.0], [8.8, 14.0, 10.0], "steel_dark"),                           # cajón
           el([8.8, 12.6, 5.0], [8.9, 13.2, 9.0], "hazard"),
           el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
           el([7.2, 9.4, 10.0], [8.8, 12.6, 16.0], "graphite"),                             # culata corta
           el([7.0, 9.0, 16.0], [9.0, 13.0, 17.0], "rubber")]
    for x0, x1 in ((6.4, 6.9), (9.1, 9.6)):                                                # tubos de refrigeración
        els += [el([x0, 12.7, -8.6], [x1, 13.2, 3.4], "copper"),
                el([x0, 12.5, -8.8], [x1, 13.4, -8.2], "steel"), el([x0, 12.5, 2.8], [x1, 13.4, 3.4], "steel")]
    for z in range(-7, 4, 2):                                                              # aletas disipadoras
        els.append(el([7.1, 14.4, z], [8.9, 15.7, z + 0.4], "steel"))
    els.append(el([7.7, 14.4, -7.0], [8.3, 15.2, 3.4], "steel_dark"))                        # base de las aletas
    if not cell_out:
        els += [el([7.3, 9.0, 1.5], [8.7, 11.4, 4.5], "cyan"), el([7.2, 8.8, 1.4], [8.8, 9.2, 4.6], "steel")]  # célula
    return els + trigger_group()


def rail_slug():
    return [el([7.5, 2.0, 7.5], [8.5, 11.0, 8.5], "inercita"), el([7.6, 11.0, 7.6], [8.4, 12.2, 8.4], "steel_hi"),
            el([7.45, 6.0, 7.45], [8.55, 6.6, 8.55], "cyan")]


def kinetic_grenade():
    return [el([6.5, 3.0, 6.5], [9.5, 8.5, 9.5], "olive"), el([6.4, 5.0, 6.4], [9.6, 5.8, 9.6], "hazard"),
            el([7.0, 8.5, 7.0], [9.0, 9.4, 9.0], "steel"), el([7.6, 9.4, 7.6], [8.4, 9.8, 8.4], "cyan"),
            el([6.8, 2.4, 6.8], [9.2, 3.0, 9.2], "brass")]


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
    """Revólver a tamaño de pistola: cañón con nervadura y extractor, tambor estriado (se abre hacia la
    derecha al recargar), martillo y cachas de madera con remaches de cobre."""
    cyl_x = 1.9 if open_ else 0.0
    els = [el([7.55, 12.6, -5.0], [8.45, 13.5, 2.4], "steel_dark", front="black"),             # cañón
           el([7.45, 13.5, -5.0], [8.55, 13.85, 2.0], "steel_hi"),                            # nervadura
           el([7.6, 12.2, -4.6], [8.4, 12.6, 1.6], "steel_dark"),                             # funda del extractor
           el([7.85, 13.85, -4.6], [8.15, 14.4, -4.0], "steel_hi"),                           # punto de mira
           el([7.1 + cyl_x, 11.75, 2.4], [8.9 + cyl_x, 13.75, 6.0], "steel"),                 # tambor
           el([7.0 + cyl_x, 12.45, 2.6], [9.0 + cyl_x, 13.05, 5.8], "steel_dark"),            # estrías del tambor
           el([7.55 + cyl_x, 12.3, 1.9], [8.45 + cyl_x, 13.2, 2.4], "brass"),                 # culotes
           el([7.3, 11.4, 6.0], [8.7, 13.9, 9.0], "steel_dark"),                              # armazón
           el([7.4, 13.9, 6.4], [8.6, 14.2, 8.6], "steel_dark"),                              # puente superior
           el([7.8, 13.9, 8.4], [8.2, 14.7, 9.2], "steel_hi"),                                # martillo
           el([7.85, 12.4, 3.0], [8.15, 12.75, 5.6], "cyan"),
           el([7.25, 8.4, 9.0], [8.75, 11.8, 12.0], "wood", rot=GRIP_ROT),                    # cachas
           el([7.2, 9.6, 9.6], [8.8, 10.0, 10.0], "copper", rot=GRIP_ROT),
           el([7.3, 8.2, 9.2], [8.7, 8.6, 11.8], "steel_dark", rot=GRIP_ROT)]                 # tapa de la culata
    return scaled(els + trigger_group(), 0.72, [8, 11.5, 10.0])


def flamethrower(tank_out=False):
    """Lanzallamas: lanza con boquilla y llama piloto, protector térmico, cuerpo con válvula, depósito rojo con
    manómetro y manguera de cobre, empuñadura delantera y culata corta."""
    els = [el([7.4, 12.4, -12.0], [8.6, 13.6, 2.0], "steel_dark", front="black"),             # lanza
           el([7.1, 12.1, -13.4], [8.9, 13.9, -12.0], "copper", front="black"),               # boquilla
           el([7.7, 11.6, -13.2], [8.3, 12.1, -12.4], "shell_red"),                           # llama piloto
           el([7.0, 12.0, -10.0], [9.0, 14.0, -5.0], "steel"),                                # protector térmico
           el([7.6, 11.3, -11.0], [8.4, 12.1, -4.0], "copper"),                               # tubo de gas
           el([7.0, 11.0, 2.0], [9.0, 14.0, 10.0], "steel_dark"),                             # cuerpo
           el([8.95, 12.0, 3.0], [9.05, 12.6, 9.0], "hazard"),
           el([7.6, 14.0, 4.0], [8.4, 14.8, 5.0], "copper"), el([7.3, 14.8, 3.7], [8.7, 15.1, 5.3], "shell_red"),  # válvula
           el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
           el([7.4, 9.6, -4.0], [8.6, 12.1, -2.0], "rubber"),                                 # empuñadura delantera
           el([7.3, 10.6, 10.0], [8.7, 13.2, 17.0], "graphite"),
           el([7.2, 10.2, 17.0], [8.8, 13.6, 17.8], "rubber")]
    if not tank_out:
        els += [el([6.8, 7.0, 1.6], [9.2, 10.6, 9.4], "shell_red"),                          # depósito
                el([6.7, 7.8, 2.6], [9.3, 8.2, 8.4], "hazard"),
                el([6.7, 9.6, 2.6], [9.3, 10.0, 8.4], "steel"),                               # abrazaderas
                el([9.2, 8.4, 4.4], [9.5, 9.6, 5.6], "steel_hi"),                             # manómetro
                el([7.7, 10.6, 1.0], [8.3, 11.2, 2.4], "copper")]                             # manguera
    return els + trigger_group()


def light_machine_gun(mag_out=False):
    """Ametralladora ligera: cañón con escudo térmico perforado, tambor de munición lateral, asa de transporte,
    bípode plegado, culata de esqueleto."""
    els = [el([7.6, 12.7, -15.4], [8.4, 13.5, -1.0], "steel_dark", front="black"),
           el([7.4, 12.5, -16.0], [8.6, 13.7, -15.0], "steel", front="black"),                  # bocacha cónica
           el([7.2, 12.3, -11.0], [8.8, 13.9, -2.0], "steel"),                                 # escudo térmico
           el([7.6, 13.9, -12.6], [8.4, 14.8, -12.0], "steel_dark")]                           # punto de mira
    for z in (-10.0, -8.4, -6.8, -5.2, -3.6):
        els += [el([8.8, 12.7, z], [8.9, 13.5, z + 0.8], "black"), el([7.1, 12.7, z], [7.2, 13.5, z + 0.8], "black")]
    els += [el([7.15, 11.4, -2.0], [8.85, 14.2, 10.0], "steel_dark"),                          # cajón
            el([7.55, 14.2, 0.5], [8.45, 14.5, 8.0], "steel"),                                 # tapa de alimentación
            el([7.6, 14.5, 2.0], [8.4, 16.0, 2.6], "steel_dark"), el([7.6, 14.5, 6.0], [8.4, 16.0, 6.6], "steel_dark"),
            el([7.6, 15.6, 2.0], [8.4, 16.2, 6.6], "rubber"),                                  # asa de transporte
            el([7.7, 11.0, -14.4], [8.3, 12.6, -13.4], "steel_dark"),                          # bípode plegado
            el([7.0, 11.0, -13.9], [7.4, 11.4, -5.0], "steel"), el([8.6, 11.0, -13.9], [9.0, 11.4, -5.0], "steel"),
            el([6.95, 10.8, -5.4], [7.45, 11.2, -4.6], "rubber"), el([8.55, 10.8, -5.4], [9.05, 11.2, -4.6], "rubber"),
            el([8.85, 12.6, 1.0], [8.95, 12.9, 9.0], "cyan"),
            el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
            el([7.5, 12.4, 10.0], [8.5, 13.4, 19.5], "steel_dark"),                            # culata de esqueleto
            el([7.5, 9.6, 14.0], [8.5, 10.2, 19.5], "steel_dark"),
            el([7.55, 10.2, 18.6], [8.45, 12.4, 19.5], "steel_dark"),
            el([7.3, 9.4, 19.5], [8.7, 13.6, 20.4], "rubber")]
    if not mag_out:
        els += [el([8.85, 9.0, 0.6], [11.2, 12.6, 6.4], "olive"),                             # tambor
                el([9.4, 8.6, 1.4], [10.8, 9.0, 5.6], "olive"),
                el([11.2, 9.8, 2.4], [11.5, 11.8, 4.6], "steel"),                              # tapa del tambor
                el([8.75, 12.0, 2.8], [8.9, 12.6, 4.6], "brass")]                              # cinta
    return els + trigger_group()


def kinetic_sniper(bolt_back=False):
    """Francotirador: cañón largo estriado de inercita, freno de boca grande, chasis de fibra de carbono,
    mira enorme con torretas, carrillera ajustable, bípode y monópode trasero."""
    els = [el([7.65, 12.8, -15.0], [8.35, 13.5, 2.0], "inercita", front="black"),             # cañón
           el([7.3, 12.45, -16.0], [8.7, 13.85, -14.6], "steel_hi", front="black"),           # freno de boca
           el([7.2, 12.6, -15.6], [8.8, 13.6, -15.0], "steel_dark"),
           el([7.2, 12.0, -6.0], [8.8, 13.0, 4.0], "graphite"),                               # guardamanos
           el([7.25, 11.8, 4.0], [8.75, 14.0, 11.0], "steel_dark"),                           # cajón
           el([7.6, 14.0, 4.2], [8.4, 14.6, 4.8], "steel_dark"), el([7.6, 14.0, 9.2], [8.4, 14.6, 9.8], "steel_dark"),
           el([7.25, 14.6, 0.5], [8.75, 16.2, 12.8], "steel_dark"),                           # tubo de la mira
           el([6.95, 14.4, -1.2], [9.05, 16.4, 0.5], "steel", front="cyan"),                  # objetivo
           el([7.1, 14.5, 12.8], [8.9, 16.3, 13.6], "steel"),                                 # ocular
           el([7.6, 16.2, 5.6], [8.4, 16.9, 6.6], "steel_hi"),                                # torreta de elevación
           el([8.75, 15.0, 5.6], [9.35, 15.8, 6.6], "steel_hi"),                              # torreta de deriva
           el([7.2, 8.0, 10.5], [8.8, 12.0, 13.5], "graphite", rot=GRIP_ROT),
           el([7.1, 9.0, 13.0], [8.9, 12.4, 22.0], "graphite"),                               # culata
           el([7.3, 12.4, 14.0], [8.7, 13.6, 19.0], "rubber"),                                # carrillera
           el([7.0, 8.4, 22.0], [9.0, 12.6, 23.4], "rubber"),
           el([7.7, 8.0, 19.0], [8.3, 9.0, 19.8], "steel"),                                   # monópode
           el([7.6, 11.0, -12.0], [8.4, 11.5, -5.0], "steel"),                                # bípode plegado
           el([7.5, 10.6, -12.4], [8.5, 11.1, -11.6], "rubber")]
    z = 3.0 if bolt_back else 0.0
    els += [el([8.75, 13.0, 6.0 + z], [9.6, 13.5, 6.7 + z], "steel_hi"),                     # cerrojo
            el([9.4, 12.7, 6.0 + z], [10.1, 13.4, 6.8 + z], "cyan")]
    return els + trigger_group()


def kinetic_blade():
    """Cuchilla cinética: espada recta de inercita con acanaladura de cinetrita luminosa, doble filo pulido,
    guarda con emisor y puntas de cobre, empuñadura de goma con anillas y pomo con gema."""
    d = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}
    els = [el([7.35, 0.6, 7.45], [8.65, 4.4, 8.55], "rubber", rot=d),                              # empuñadura
           el([7.1, -0.2, 7.2], [8.9, 0.6, 8.8], "steel_hi", rot=d),                               # pomo
           el([7.6, -0.5, 7.6], [8.4, -0.2, 8.4], "cyan", rot=d),
           el([5.8, 4.4, 7.3], [10.2, 5.2, 8.7], "steel_dark", rot=d),                             # guarda
           el([5.5, 4.5, 7.4], [6.1, 6.0, 8.6], "copper", rot=d), el([9.9, 4.5, 7.4], [10.5, 6.0, 8.6], "copper", rot=d),
           el([7.4, 4.2, 7.1], [8.6, 5.4, 8.9], "steel", rot=d),                                   # emisor
           el([7.6, 4.5, 7.0], [8.4, 5.1, 9.0], "cyan", rot=d),
           el([7.0, 5.2, 7.75], [9.0, 14.6, 8.25], "inercita", rot=d),                             # hoja
           el([7.8, 5.8, 7.7], [8.2, 13.8, 8.3], "cyan", rot=d),                                   # acanaladura
           el([6.7, 5.4, 7.85], [7.0, 14.6, 8.15], "edge", rot=d), el([9.0, 5.4, 7.85], [9.3, 14.6, 8.15], "edge", rot=d),
           el([7.3, 14.6, 7.78], [8.7, 15.6, 8.22], "inercita", rot=d),                            # punta
           el([7.7, 15.6, 7.82], [8.3, 16.4, 8.18], "edge", rot=d)]
    for y in (1.2, 2.4, 3.6):
        els.append(el([7.3, y, 7.4], [8.7, y + 0.35, 8.6], "copper", rot=d))
    return els


def rocket_launcher(empty=False):
    """Lanzacohetes: tubo oliva con bocinas de acero delante y detrás, guía láser lateral, mira, asa de
    transporte, empuñadura delantera y apoyo de hombro."""
    els = [el([6.8, 11.6, -11.4], [9.2, 14.0, 15.4], "olive"),                                # tubo
           el([6.5, 11.3, -12.6], [9.5, 14.3, -11.4], "steel", front="black"),                # bocina delantera
           el([6.5, 11.3, 15.4], [9.5, 14.3, 16.6], "steel"),                                 # bocina trasera
           el([6.7, 12.4, 0.0], [9.3, 13.2, 1.0], "hazard"),
           el([6.7, 12.4, 9.0], [9.3, 13.2, 9.6], "steel_dark"),
           el([9.2, 12.4, -3.0], [9.9, 13.6, 3.0], "steel_dark"),                             # guía láser
           el([9.3, 12.7, -3.4], [9.8, 13.3, -3.0], "shell_red"),
           el([7.6, 14.0, 2.0], [8.4, 15.4, 2.6], "steel_dark"), el([7.6, 14.0, 8.0], [8.4, 15.4, 8.6], "steel_dark"),
           el([7.6, 15.0, 2.0], [8.4, 15.6, 8.6], "rubber"),                                  # asa
           el([6.4, 13.2, 4.0], [6.8, 14.6, 6.0], "steel_dark"),                              # mira lateral
           el([7.2, 8.0, 7.6], [8.8, 11.6, 10.4], "rubber", rot=GRIP_ROT),
           el([7.4, 9.6, -2.0], [8.6, 11.6, 0.0], "rubber"),
           el([7.0, 10.6, 11.0], [9.0, 11.6, 14.0], "rubber")]                                # apoyo de hombro
    if not empty:
        els += [el([7.3, 12.1, -13.0], [8.7, 13.5, -12.0], "shell_red"),                     # ojiva
                el([7.6, 12.4, -13.4], [8.4, 13.2, -13.0], "steel_hi")]
    return els + trigger_group()


def arc_caster(cell_out=False):
    """Arco voltaico: horquilla de tres puntas de cobre (bobina de Tesla) con un arco entre ellas, aisladores
    de porcelana, bobina de cobre dentro de un tubo de vidrio, bornes de latón y célula bajo el cajón."""
    els = [el([7.7, 13.7, -12.6], [8.3, 14.3, -8.6], "copper"),                               # puntas de la horquilla
           el([6.5, 11.9, -12.0], [7.1, 12.5, -8.6], "copper"), el([8.9, 11.9, -12.0], [9.5, 12.5, -8.6], "copper"),
           el([7.75, 13.75, -13.0], [8.25, 14.25, -12.6], "cyan"),
           el([6.55, 11.95, -12.4], [7.05, 12.45, -12.0], "cyan"), el([8.95, 11.95, -12.4], [9.45, 12.45, -12.0], "cyan"),
           el([7.85, 12.75, -11.8], [8.15, 13.05, -9.4], "cyan"),                             # arco
           el([6.5, 11.9, -8.6], [9.5, 12.5, -8.0], "steel_dark"), el([7.7, 12.5, -8.6], [8.3, 14.3, -8.0], "steel_dark"),
           el([7.3, 12.3, -8.0], [8.7, 13.7, -6.4], "ceramic"),                               # aislador delantero
           el([6.9, 11.8, -6.4], [9.1, 14.2, -5.8], "copper"),                                # tapas de la bobina
           el([7.1, 12.0, -5.8], [8.9, 14.0, 1.0], "glass"),                                  # bobina en tubo de vidrio
           el([6.9, 11.8, 1.0], [9.1, 14.2, 1.6], "copper"),
           el([7.0, 11.4, 1.6], [9.0, 14.4, 10.0], "steel_dark"),                             # cajón
           el([8.95, 12.0, 3.0], [9.05, 12.4, 9.0], "cyan"), el([8.95, 13.0, 3.0], [9.05, 13.6, 9.0], "hazard"),
           el([7.2, 8.0, 9.6], [8.8, 11.6, 12.4], "rubber", rot=GRIP_ROT),
           el([7.3, 10.4, 10.0], [8.7, 13.0, 16.0], "graphite"),
           el([7.1, 10.0, 16.0], [8.9, 13.4, 16.8], "rubber")]
    for z in (2.4, 6.4):                                                                   # aisladores con bornes
        els += [el([7.6, 14.4, z], [8.4, 15.6, z + 0.8], "ceramic"), el([7.5, 15.6, z - 0.1], [8.5, 15.9, z + 0.9], "brass")]
    els.append(el([7.85, 15.9, 2.9], [8.15, 16.1, 6.9], "copper"))                          # cable entre bornes
    if not cell_out:
        els += [el([7.3, 9.0, 3.0], [8.7, 11.4, 6.0], "cyan"), el([7.2, 8.8, 2.9], [8.8, 9.2, 6.1], "steel")]
    return els + trigger_group()


MINI_SPIN = [None, {"angle": 22.5, "axis": "z", "origin": [8, 13.1, -7]}, {"angle": -22.5, "axis": "z", "origin": [8, 13.1, -7]}]


MINI_SHIFT = (0.0, -8.4, 6.0)          # el asa superior queda donde la mano agarra (empuñadura de las armas largas)


def moved(els, d):
    """Traslada un grupo de elementos (y el origen de sus rotaciones)."""
    out = []
    for e in els:
        e2 = dict(e)
        e2["from"] = [round(e["from"][i] + d[i], 4) for i in range(3)]
        e2["to"] = [round(e["to"][i] + d[i], 4) for i in range(3)]
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = [round(r["origin"][i] + d[i], 4) for i in range(3)]
            e2["rotation"] = r
        out.append(e2)
    return out


def minigun(box_out=False, frame=0):
    """Minigun portátil que se lleva colgando del asa de arriba (como las miniguns de mano): 6 cañones con
    abrazaderas y corona dentada (giran: 3 fotogramas), motor con rejillas, asa metálica en D moleteada con el
    gatillo de palanca dentro, batería trasera y caja de munición con cinta. Sin empuñadura inferior."""
    r = MINI_SPIN[frame]
    els = []
    for (x, y) in ((7.0, 12.4), (8.4, 12.4), (7.0, 13.8), (8.4, 13.8), (7.7, 11.7), (7.7, 14.5)):
        els.append(el([x, y, -15.0], [x + 0.6, y + 0.6, -1.0], "steel_dark", front="black", rot=r))  # cañones
    els += [el([6.6, 11.4, -14.0], [9.4, 15.4, -13.2], "steel", rot=r),                     # abrazaderas
            el([6.6, 11.4, -8.0], [9.4, 15.4, -7.2], "steel", rot=r),
            el([6.4, 11.2, -2.4], [9.6, 15.6, -1.0], "copper", rot=r),                         # corona dentada
            el([7.5, 12.9, -15.0], [8.5, 13.3, -1.0], "steel_hi", rot=r),                      # eje
            el([6.5, 11.2, -1.0], [9.5, 15.4, 7.0], "graphite"),                               # motor
            el([6.4, 12.0, 0.0], [6.5, 14.6, 6.0], "black"), el([9.5, 12.0, 0.0], [9.6, 14.6, 6.0], "black"),  # rejillas
            el([9.5, 14.7, 0.4], [9.6, 15.1, 5.6], "hazard"),
            el([6.8, 11.4, 7.0], [9.2, 15.2, 9.0], "steel_dark"),                              # tapa trasera
            el([6.9, 11.6, 9.0], [9.1, 14.6, 13.0], "olive"),                                  # batería
            el([9.1, 13.6, 10.0], [9.2, 14.2, 12.0], "cyan"),
            el([7.2, 11.0, 9.4], [8.8, 11.6, 12.6], "steel_dark")]                             # base de la batería
    # asa metálica en D: es por donde se agarra el arma (la mano la rodea; el gatillo de palanca queda dentro)
    els += [el([7.3, 15.4, -1.0], [8.7, 15.7, 1.0], "steel_dark"), el([7.3, 15.4, 5.0], [8.7, 15.7, 7.0], "steel_dark"),
            el([7.6, 15.7, -0.6], [8.4, 17.8, 0.4], "steel"), el([7.6, 15.7, 5.6], [8.4, 17.8, 6.6], "steel"),
            el([7.55, 17.8, -0.9], [8.45, 18.7, 6.9], "steel_hi"),
            el([7.75, 16.9, 3.0], [8.25, 17.8, 4.4], "steel_dark"),                            # gatillo de palanca
            el([7.8, 16.6, 3.6], [8.2, 16.9, 5.2], "shell_red"),
            el([7.8, 18.7, 0.2], [8.2, 19.0, 0.8], "shell_red")]                               # seguro
    for z in (1.0, 2.2, 4.6, 5.8):                                                         # moleteado
        els.append(el([7.5, 17.75, z], [8.5, 18.75, z + 0.3], "steel_dark"))
    if not box_out:
        els += [el([6.6, 7.4, 0.6], [9.4, 11.2, 6.0], "olive"),                               # caja de munición
                el([6.5, 9.0, 1.0], [9.5, 9.4, 5.6], "steel"),
                el([7.6, 10.4, -1.0], [8.4, 11.2, 0.6], "brass"),                              # cinta
                el([7.6, 11.0, -1.6], [8.4, 11.4, -0.6], "brass")]
    return moved(els, MINI_SHIFT)




def fuel_canister():
    return [el([6.0, 2.0, 6.5], [10.0, 10.0, 9.5], "shell_red"), el([5.9, 5.0, 6.4], [10.1, 5.8, 9.6], "hazard"),
            el([7.4, 10.0, 7.4], [8.6, 11.4, 8.6], "steel"), el([6.6, 10.0, 7.7], [7.2, 11.0, 8.3], "steel_dark")]


def guided_rocket():
    return [el([7.4, 2.0, 7.4], [8.6, 11.0, 8.6], "olive"), el([7.5, 11.0, 7.5], [8.5, 12.6, 8.5], "shell_red"),
            el([6.6, 2.0, 7.9], [9.4, 4.0, 8.1], "steel"), el([7.9, 2.0, 6.6], [8.1, 4.0, 9.4], "steel"),
            el([7.35, 6.0, 7.35], [8.65, 6.4, 8.65], "hazard")]


# ---------------------------------------------------------------- machete (como una espada: diagonal en el plano XY)
def machete():
    """Machete de verdad: hoja ancha que se ensancha hacia la punta, lomo recto, un solo filo, punta recortada,
    virola de acero, cachas de madera con tres remaches de latón y la culata en gancho."""
    d = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}                                      # mango abajo-izq., punta arriba-der.
    els = [el([7.3, 0.6, 7.45], [8.7, 5.0, 8.55], "wood_dark", rot=d),                         # cachas
           el([7.25, 0.4, 7.95], [8.75, 5.0, 8.05], "steel_dark", rot=d),                      # espiga entre las cachas
           el([6.9, 0.0, 7.45], [8.9, 0.7, 8.55], "wood_dark", rot=d),                         # culata en gancho
           el([7.2, 5.0, 7.5], [8.9, 5.6, 8.5], "steel", rot=d),                               # virola
           el([7.5, 5.6, 7.8], [8.8, 9.0, 8.2], "blade", rot=d),                               # hoja (se ensancha)
           el([7.0, 9.0, 7.8], [8.8, 12.4, 8.2], "blade", rot=d),
           el([6.5, 12.4, 7.8], [8.8, 15.0, 8.2], "blade", rot=d),
           el([6.5, 15.0, 7.8], [8.2, 15.7, 8.2], "blade", rot=d),                             # punta recortada
           el([7.2, 15.7, 7.82], [8.0, 16.2, 8.18], "blade", rot=d),
           el([7.2, 5.8, 7.88], [7.5, 9.0, 8.12], "edge", rot=d),                              # filo
           el([6.7, 9.0, 7.88], [7.0, 12.4, 8.12], "edge", rot=d),
           el([6.2, 12.4, 7.88], [6.5, 15.5, 8.12], "edge", rot=d),
           el([8.6, 5.6, 7.75], [8.9, 15.0, 8.25], "steel_dark", rot=d),                       # lomo
           el([7.8, 10.2, 7.76], [8.5, 11.0, 8.24], "rust", rot=d),                            # manchas de óxido
           el([7.0, 13.4, 7.76], [7.6, 13.9, 8.24], "rust", rot=d),
           el([8.3, 6.5, 7.76], [8.5, 12.0, 8.24], "cyan", rot=d)]                             # vena de cinetrita
    for y in (1.4, 2.7, 4.0):                                                               # remaches
        els.append(el([7.75, y, 7.4], [8.25, y + 0.5, 8.6], "brass", rot=d))
    return els


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
    model("rifle_round", rifle_round(), SMALL)
    flat = [f"{s_}_{piece}" for s_ in ("scrap", "inercita", "tactical", "exo") for piece in ("helmet", "chestplate", "leggings", "boots")]
    flat += ["inercita_plate", "precision_mechanism", "kinetic_servo", "ballistic_fabric"]          # tools/armor_t2_gen.py
    flat += ["core_processor"]                                                                       # tools/tier3_art.py
    for name in flat:
        (ROOT / "models/item" / f"{name}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"tecnocinetico:item/{name}"}}, indent=2), encoding="utf-8")
    gunkit.flush()
    print("ok: armas (texturas propias por arma)")
