"""Auditoría del equilibrio de recetas de Tecnocinético.

1. Volcar las recetas:   set TECNO_AUDIT=1  y  gradlew runGameTestServer   (escribe run/recipe_audit.json)
2. Analizar:             python tools/recipe_audit.py [salida.txt]

Calcula el COSTE de cada objeto del mod en "lingotes de hierro equivalentes": a cada material base (menas, botín,
objetos vanilla) se le da un valor (BASE, abajo) y el resto se calcula siguiendo la receta más barata que lo produce
(mesa, horno, máquinas, prensas, ensambladores). Lista también los objetos que nada usa y los que no tienen origen.
Los valores BASE son una estimación de lo que cuesta conseguir cada cosa en la progresión: ajústalos si cambia.
"""
import json
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
T = 'tecnocinetico:'

# Valor de los materiales base en lingotes de hierro (la referencia es 1 hierro = 1).
BASE = {
    # vanilla
    'minecraft:raw_iron': 1, 'minecraft:raw_copper': 0.4, 'minecraft:raw_gold': 2, 'minecraft:coal': 0.15, 'minecraft:charcoal': 0.15,
    'minecraft:redstone': 0.3, 'minecraft:redstone_block': 2.7, 'minecraft:diamond': 8, 'minecraft:ender_pearl': 3,
    'minecraft:blaze_powder': 1.5, 'minecraft:lapis_lazuli': 0.3, 'minecraft:leather': 0.5, 'minecraft:string': 0.2,
    'minecraft:paper': 0.05, 'minecraft:book': 0.3, 'minecraft:glass': 0.05, 'minecraft:glass_pane': 0.02, 'minecraft:tinted_glass': 1,
    'minecraft:sand': 0.02, 'minecraft:clay_ball': 0.05, 'minecraft:cobblestone': 0.01, 'minecraft:andesite': 0.01,
    'minecraft:blackstone': 0.02, 'minecraft:smooth_stone': 0.02, 'minecraft:stone_slab': 0.01, 'minecraft:cobbled_deepslate_slab': 0.01,
    'minecraft:oak_planks': 0.05, 'minecraft:oak_slab': 0.025, 'minecraft:dark_oak_log': 0.2, 'minecraft:flint': 0.1,
    'minecraft:flint_and_steel': 1.1, 'minecraft:shears': 2, 'minecraft:bucket': 3, 'minecraft:cauldron': 7, 'minecraft:blast_furnace': 5.2,
    'minecraft:furnace': 0.1, 'minecraft:dispenser': 0.5, 'minecraft:brewing_stand': 1.6, 'minecraft:clock': 8, 'minecraft:compass': 4.3,
    'minecraft:spyglass': 1, 'minecraft:daylight_detector': 0.5, 'minecraft:iron_bars': 0.375, 'minecraft:iron_block': 9,
    'minecraft:iron_boots': 4, 'minecraft:copper_block': 3.6, 'minecraft:lightning_rod': 1.2, 'minecraft:yellow_dye': 0.05,
    'minecraft:bone_meal': 0.05, 'minecraft:honeycomb': 0.3, 'minecraft:moss_block': 0.2, 'minecraft:vine': 0.05,
    'minecraft:prismarine_shard': 0.5, 'minecraft:snow_block': 0.02, 'minecraft:snowball': 0.01, 'minecraft:soul_sand': 0.05,
    'minecraft:leather_helmet': 2.5, 'minecraft:leather_chestplate': 4, 'minecraft:leather_leggings': 3.5, 'minecraft:leather_boots': 2,
    'minecraft:coal_ore': 0.15, 'minecraft:deepslate_coal_ore': 0.15, 'minecraft:netherite_scrap': 30, 'minecraft:netherite_ingot': 130,
    'minecraft:gunpowder': 0.4, 'minecraft:quartz': 0.5, 'minecraft:obsidian': 1, 'minecraft:emerald': 4, 'minecraft:elytra': 200,
    'minecraft:piston': 1.3, 'minecraft:crafting_table': 0.2, 'minecraft:tripwire_hook': 0.3, 'minecraft:grindstone': 0.3,
    'minecraft:acacia_planks': 0.05, 'minecraft:gravel': 0.01, 'minecraft:stick': 0.02, 'minecraft:iron_nugget': 1 / 9,
    **{'minecraft:%s_wool' % c: 0.2 for c in ('white', 'black', 'blue', 'brown', 'cyan', 'gray', 'green', 'light_blue', 'light_gray',
                                             'lime', 'magenta', 'orange', 'pink', 'purple', 'red', 'yellow')},
    # Create
    'create:andesite_alloy': 0.2, 'create:andesite_casing': 0.4, 'create:shaft': 0.1,
    # menas y materiales del mod
    T + 'raw_lead': 1, T + 'raw_tin': 1, T + 'raw_titanium': 3, T + 'raw_uranium': 4, T + 'raw_cinetrite': 1.5,
    T + 'raw_inercita': 3, T + 'raw_piroxite': 5, T + 'raw_nullite': 8, T + 'scrap_metal': 0.4, T + 'titanium_dust': 3,
    T + 'lead_ore': 1, T + 'deepslate_lead_ore': 1, T + 'tin_ore': 1, T + 'deepslate_tin_ore': 1, T + 'titanium_ore': 3,
    T + 'deepslate_titanium_ore': 3, T + 'uranium_ore': 4, T + 'deepslate_uranium_ore': 4,
    T + 'power_cable': 0.25, T + 'rubble': 0.02, T + 'scrap_pile': 0.1, T + 'street_lamp_bent': 0.1, T + 'street_lamp_head': 0.1,
    T + 'street_lamp_pole': 0.1, T + 'utility_crossarm': 0.05, T + 'utility_pole': 0.1, T + 'dead_log': 0.1, T + 'vitrified_sand': 0.05,
    T + 'compacted_slag': 0.5, T + 'tar': 0.2, T + 'slag_alloy': 4, T + 'sulfur_dust': 0.5,
    # fluidos en cubo (lo que cuesta producirlos, aprox.)
    T + 'gasoline_bucket': 4, T + 'kinetic_fuel_bucket': 8, T + 'coolant_bucket': 4, T + 'igneous_plasma_bucket': 12,
    # botín de mobs y jefes (no se fabrican)
    T + 'swarm_core': 10, T + 'core_processor': 20, T + 'data_core': 40, T + 'abyssal_component': 15, T + 'forge_heart': 60,
    T + 'nether_coupler': 100, T + 'end_coupler': 150, T + 'dimensional_drive': 80, T + 'vector_thruster': 60,
    T + 'adaptive_flight_core': 60, T + 'depleted_fuel_rod': 0, T + 'empty_pulse_cell': 2,
}


def load():
    d = json.load(open(ROOT / 'run' / 'recipe_audit.json', encoding='utf-8'))
    recipes = []                                   # (tipo, [(objeto, cantidad)], salida, cantidad, id)
    for r in d['recipes']:
        if not r.get('out'):
            continue
        ins = defaultdict(int)
        for opts in r['in']:
            if opts:
                ins[min(opts, key=lambda o: (not o.startswith('minecraft:'), o))] += 1   # la opción vanilla si la hay
        recipes.append((r['type'].split(':')[-1], list(ins.items()), r['out']['item'], r['out']['count'], r['id']))
    for m in d['machines']:
        if not m.get('out'):
            continue
        recipes.append((m['type'], [(i['item'], i['count']) for i in m['in']], m['out']['item'], m['out']['count'], m['type']))
    return d['items'], recipes


def costs(recipes):
    cost = dict(BASE)
    best = {}
    for _ in range(60):
        changed = False
        for kind, ins, out, n, rid in recipes:
            if out in BASE:
                continue
            total = 0.0
            ok = True
            for item, c in ins:
                if item == out or item not in cost:
                    ok = False
                    break
                total += cost[item] * c
            if not ok:
                continue
            v = total / max(1, n)
            if v + 1e-9 < cost.get(out, float('inf')):
                cost[out] = v
                best[out] = (kind, ins, n, rid)
                changed = True
        if not changed:
            break
    return cost, best


def main():
    items, recipes = load()
    cost, best = costs(recipes)
    used = defaultdict(set)
    for kind, ins, out, n, rid in recipes:
        for item, c in ins:
            used[item].add(out)
    lines = []
    lines.append('COSTE EN LINGOTES DE HIERRO EQUIVALENTES (receta más barata)')
    lines.append('=' * 100)
    for item in sorted(items, key=lambda i: -cost.get(i, -1)):
        c = cost.get(item)
        b = best.get(item)
        how = '' if b is None else '%-20s %s -> %d' % (b[0], ', '.join('%dx %s' % (k, i.replace(T, '').replace('minecraft:', 'mc:')) for i, k in b[1]), b[2])
        src = 'BASE' if item in BASE else ('?' if c is None else '')
        lines.append('%9s %-4s %-34s usado en %-3d %s' % ('-' if c is None else '%.2f' % c, src, item.replace(T, ''), len(used[item]), how))
    lines.append('')
    lines.append('SIN ORIGEN (ni receta ni valor base): ' + ', '.join(i.replace(T, '') for i in items if i not in cost))
    lines.append('NADA LOS USA (y no son equipo final): ' + ', '.join(i.replace(T, '') for i in items if not used[i]))
    out = '\n'.join(lines)
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    if target:
        target.write_text(out, encoding='utf-8')
        print('escrito', target)
    else:
        print(out)


if __name__ == '__main__':
    main()
