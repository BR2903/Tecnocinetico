"""Auditoría del ritmo de progresión de Tecnocinético.

1. Volcar datos:  set TECNO_AUDIT=1  y  gradlew runGameTestServer   (escribe run/recipe_audit.json y run/ore_audit.json)
2. Analizar:      python tools/progression_audit.py

Para cada etapa toma un "kit" típico (armas, armadura y máquinas de esa etapa), lo descompone en materias primas
siguiendo la receta más barata (la misma que tools/recipe_audit.py) y lo compara con lo que sale del mundo:
menas por chunk (medidas en chunks recién generados) -> "chunks enteros que habría que minar" para cada material.
Referencias vanilla: armadura de diamante completa (24 diamantes) y un set de netherita (16 restos).
Los materiales que no salen de menas (botín de jefes, chatarra, fluidos, azufre: también polvo de blaze + carbón)
se listan aparte. Las Élitras propulsadas son una receta especial (Java) y no entran.
"""
import json
from collections import defaultdict
from pathlib import Path

import recipe_audit as ra

ROOT = Path(__file__).resolve().parent.parent
T = 'tecnocinetico:'


def armor(prefix):
    return [T + prefix + '_' + p for p in ('helmet', 'chestplate', 'leggings', 'boots')]


KITS = {
    '1 · Superficie (chatarra, pistola, prensa)': [T + 'kinetic_pistol', T + 'pipe_shotgun', T + 'bolt_rifle', T + 'kinetic_assembly_press',
                                                    T + 'weapon_bench', *armor('scrap'), *armor('combat_mk1')],
    '2 · Inercita y electricidad': [T + 'kinetic_smg', T + 'semi_rifle', T + 'pump_shotgun', T + 'auto_kinetic_press', T + 'electric_refiner',
                                    T + 'chemical_lab', T + 'ammo_fabricator', T + 'cell_charger', T + 'jetpack', *armor('inercita'),
                                    *armor('combat_mk2')],
    '3 · Núcleo y ensambladores': [T + 'assault_rifle', T + 'rail_rifle', T + 'weapon_assembler', T + 'reaction_chamber',
                                   T + 'industrial_assembler'],
    '4 · Nether (piroxita)': [T + 'thermal_lance', T + 'refinery', T + 'oil_pump', *armor('piroxite'), *armor('refined_piroxite')],
    '5 · End (nulita; la Combate Mk III pide nulita y piroxita)': [*armor('combat_mk3'), T + 'network_intruder', T + 'satellite', T + 'wrist_map', *armor('nullite')],
    'Reactor de fisión (kit)': [T + 'fission_reactor_kit'],
}

# materia prima -> (dimensión, menas que la dan, objetos por bloque de mena sin fortuna)
ORE = {
    'minecraft:raw_iron': ('minecraft:overworld', ['minecraft:iron_ore', 'minecraft:deepslate_iron_ore'], 1),
    'minecraft:raw_copper': ('minecraft:overworld', ['minecraft:copper_ore', 'minecraft:deepslate_copper_ore'], 3.5),
    'minecraft:raw_gold': ('minecraft:overworld', ['minecraft:gold_ore', 'minecraft:deepslate_gold_ore'], 1),
    'minecraft:diamond': ('minecraft:overworld', ['minecraft:diamond_ore', 'minecraft:deepslate_diamond_ore'], 1),
    'minecraft:redstone': ('minecraft:overworld', ['minecraft:redstone_ore', 'minecraft:deepslate_redstone_ore'], 4.5),
    'minecraft:coal': ('minecraft:overworld', ['minecraft:coal_ore', 'minecraft:deepslate_coal_ore'], 1),
    'minecraft:quartz': ('minecraft:the_nether', ['minecraft:nether_quartz_ore'], 1),
    'minecraft:netherite_scrap': ('minecraft:the_nether', ['minecraft:ancient_debris'], 1),
    T + 'raw_cinetrite': ('minecraft:overworld', [T + 'cinetrite_ore', T + 'deepslate_cinetrite_ore'], 1),
    T + 'raw_inercita': ('minecraft:overworld', [T + 'inercita_ore', T + 'deepslate_inercita_ore'], 1),
    T + 'raw_lead': ('minecraft:overworld', [T + 'lead_ore', T + 'deepslate_lead_ore'], 1),
    T + 'raw_tin': ('minecraft:overworld', [T + 'tin_ore', T + 'deepslate_tin_ore'], 1),
    T + 'raw_titanium': ('minecraft:overworld', [T + 'titanium_ore', T + 'deepslate_titanium_ore'], 1),
    T + 'raw_uranium': ('minecraft:overworld', [T + 'uranium_ore', T + 'deepslate_uranium_ore'], 1),
    T + 'raw_piroxite': ('minecraft:the_nether', [T + 'piroxite_ore'], 1),
    T + 'raw_nullite': ('minecraft:the_end', [T + 'nullite_ore'], 1),
}
# lingotes/menas que equivalen a una materia prima (las recetas a veces piden el bloque de mena o el lingote vanilla)
ALIAS = {'minecraft:iron_ingot': 'minecraft:raw_iron', 'minecraft:copper_ingot': 'minecraft:raw_copper',
         'minecraft:gold_ingot': 'minecraft:raw_gold', 'minecraft:coal_ore': 'minecraft:coal', 'minecraft:deepslate_coal_ore': 'minecraft:coal',
         'minecraft:charcoal': 'minecraft:coal', 'minecraft:redstone_block': ('minecraft:redstone', 9),
         'minecraft:iron_block': ('minecraft:raw_iron', 9), 'minecraft:copper_block': ('minecraft:raw_copper', 9),
         'minecraft:iron_nugget': ('minecraft:raw_iron', 1 / 9), 'minecraft:iron_bars': ('minecraft:raw_iron', 0.375),
         'minecraft:bucket': ('minecraft:raw_iron', 3), 'minecraft:cauldron': ('minecraft:raw_iron', 7),
         'minecraft:iron_boots': ('minecraft:raw_iron', 4), 'minecraft:shears': ('minecraft:raw_iron', 2),
         'minecraft:compass': ('minecraft:raw_iron', 4), 'minecraft:clock': ('minecraft:raw_gold', 4),
         'minecraft:netherite_ingot': ('minecraft:netherite_scrap', 4), 'minecraft:lightning_rod': ('minecraft:raw_copper', 3),
         'minecraft:blast_furnace': ('minecraft:raw_iron', 5)}
for raw, (_, ores, _) in list(ORE.items()):
    for o in ores:
        ALIAS.setdefault(o, raw)


FURNACE = {}          # lingote -> materia prima 1:1 (sin máquinas: horno y nada más)
for _raw in ORE:
    if _raw.startswith(T + 'raw_'):
        FURNACE[_raw.replace('raw_', '') + '_ingot'] = _raw
FURNACE.update({'minecraft:iron_ingot': 'minecraft:raw_iron', 'minecraft:copper_ingot': 'minecraft:raw_copper',
                'minecraft:gold_ingot': 'minecraft:raw_gold'})


def expand(item, qty, best, out, seen=(), furnace=False):
    """Suma en out las materias primas (o lo que no tenga receta) que hacen falta para qty de item.
    furnace=True: los lingotes salen 1:1 de la mena (horno), sin el x3 del laboratorio químico."""
    if furnace and item in FURNACE:
        out[FURNACE[item]] += qty
        return
    if item in ALIAS:
        a = ALIAS[item]
        raw, k = (a, 1) if isinstance(a, str) else a
        out[raw] += qty * k
        return
    if item in ORE or item in ra.BASE or item not in best or item in seen:
        out[item] += qty
        return
    kind, ins, n, rid = best[item]
    for i, c in ins:
        expand(i, qty * c / max(1, n), best, out, seen + (item,), furnace)


def main():
    items, recipes = ra.load()
    cost, best = ra.costs(recipes)
    ores = json.load(open(ROOT / 'run' / 'ore_audit.json', encoding='utf-8'))
    supply = {}
    for raw, (dim, blocks, per) in ORE.items():
        supply[raw] = sum(ores.get(dim, {}).get(b, 0) for b in blocks) * per
    print('MATERIAS PRIMAS POR CHUNK (sin fortuna):')
    print('  ' + ' · '.join('%s %.1f' % (r.split(':')[1].replace('raw_', ''), s) for r, s in sorted(supply.items(), key=lambda x: -x[1])))
    print('  referencia: armadura de diamante = %.1f chunks; set de netherita = %.1f chunks' % (
        24 / supply['minecraft:diamond'], 16 / max(1e-6, supply['minecraft:netherite_scrap'])))
    for name, kit in KITS.items():
        need, oven = defaultdict(float), defaultdict(float)
        for it in kit:
            if it not in cost:
                print('  ?? sin receta', it)
            expand(it, 1, best, need)
            expand(it, 1, best, oven, furnace=True)
        print('=' * 100)
        print('ETAPA %s  (coste total %.0f hierro-eq)' % (name, sum(cost.get(i, 0) for i in kit)))
        mined = [(r, q, q / supply[r]) for r, q in need.items() if r in supply and supply[r] > 0]
        print('   %-28s %16s   %s' % ('', 'con laboratorio', 'solo horno'))
        for r, q, ch in sorted(mined, key=lambda x: -x[2]):
            print('   %-28s %6.1f -> %4.1f ch   %6.1f -> %4.1f ch   (%s)' % (r.replace(T, '').replace('minecraft:', 'mc:'), q, ch,
                                                                      oven[r], oven[r] / supply[r], ORE[r][0].split(':')[1]))
        other = sorted(((r, q) for r, q in need.items() if r not in supply), key=lambda x: -x[1] * ra.BASE.get(x[0], 1))
        print('   otros: ' + ', '.join('%.1f %s' % (q, r.replace(T, '').replace('minecraft:', 'mc:')) for r, q in other if q > 0.05))


if __name__ == '__main__':
    main()
