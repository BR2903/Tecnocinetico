"""Auditoría del equilibrio de combate de Tecnocinético.

1. Volcar las estadísticas:  set TECNO_AUDIT=1  y  gradlew runGameTestServer   (escribe run/combat_audit.json)
2. Analizar:                 python tools/combat_audit.py

Para cada etapa (tier 1, tier 2, tier 3) toma la armadura y las armas de esa etapa y calcula, contra los enemigos de esa
etapa: cuántos golpes de cada ataque aguanta el jugador y cuántos segundos tarda en matarlos (daño por segundo
sostenido tras la armadura del enemigo; con balas normales y con cinéticas). Fórmula de armadura de Minecraft 1.20.
Los ataques especiales (disparos, áreas) están en ATTACKS: copiados del código; actualízalos si cambian.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
T = 'tecnocinetico:'


def after_armor(dmg, armor, tough):
    """Daño tras la armadura (vanilla 1.20)."""
    f = min(20.0, max(armor / 5.0, armor - dmg / (2.0 + tough / 4.0)))
    return dmg * (1 - f / 25.0)


# armaduras de cada etapa: (nombre, defensa, dureza, vida extra)
ARMOR = {
    1: [('Chatarra', 13, 0, 0), ('Combate Mk I', 15, 1.0, 4)],
    2: [('Inercita', 20, 2.5, 0), ('Combate Mk II', 19, 2.0, 8)],
    3: [('Núcleo (+8 escudo)', 20, 3.0, 8), ('Piroxita Abisal', 24, 3.5, 0), ('Combate Mk III', 24, 3.5, 12)],
}
# armas de cada etapa: (nombre, daño por disparo, disparos por segundo, perdigones)
WEAPONS = {
    1: [('Pistola Cinética', 8, 2.5, 1), ('Revólver', 9, 2.0, 1), ('Escopeta de caño', 2.5, 20 / 14, 8), ('Rifle de cerrojo', 16, 20 / 24, 1)],
    2: [('Subfusil', 3.5, 10, 1), ('Semiautomático', 13, 2.5, 1), ('Ametralladora', 4, 10, 1), ('Francotirador', 26, 20 / 30, 1),
        ('Escopeta corredera', 2.8, 1.25, 8)],
    3: [('Fusil de Asalto', 8, 20 / 3, 1), ('Minigun', 4, 20, 1), ('Rifle de Riel', 32, 1, 1)],
}
KINETIC_PIERCE = 0.35        # KineticRounds.PIERCE: ese % del daño atraviesa la armadura

# ataques de cada enemigo (los que no son el cuerpo a cuerpo de su atributo): nombre -> daño
ATTACKS = {
    T + 'corrupted_gunner': {'ráfaga': 2.0, 'disparo': 3.0, 'disparo cargado': 11.0},
    T + 'drone': {'ráfaga (x6)': 2.0, 'disparo pesado': 10.0},
    T + 'sentry_turret': {'ráfaga (x8)': 2.5, 'disparo pesado': 14.0},
    T + 'anchor': {'pisotón': 6.0},
    T + 'siege_spider': {'pisotón': 12.0, 'cañón': 18.0, 'metralla': 4.0},
    T + 'network_watcher': {'ráfaga (fase 1-3)': 16.0, 'bombardeo': 20.0, 'EMP': 18.0},
    T + 'swarm_matriarch': {'golpe en área': 23.0},
    T + 'ash_sentinel': {'disparo': 9.0},
    T + 'abyss_foundry': {'sobrecalentamiento': 32.0, 'bola de escoria': 18.0},
    T + 'slag_devourer': {'mordisco': 28.0, 'embestida': 26.0, 'emerge': 32.0, 'escupitajo': 18.0},
    'minecraft:piglin#nether': {'ráfaga de fusil': 10.0},
    T + 'data_specter': {'garras': 12.0, 'pulso': 9.0},
    T + 'end_relay_turret': {'disparo cargado': 8.0},
    'minecraft:shulker#end': {'bala (x3)': 4.0},
    'dragon': {'lluvia de datos': 10.0, 'golpe de ala': 10.0},
}
# en qué etapa se enfrenta cada enemigo
STAGE = {
    1: ['corrupted', 'corrupted_gunner', 'corrupted_spider', 'corrupted_cave_spider', 'corrupted_drowned', 'drone', 'sentry_turret',
        'swarm_body', 'swarm_wasp', 'hive_node', 'anchor', 'minecraft:zombie', 'minecraft:skeleton'],
    2: ['siege_spider', 'network_watcher', 'swarm_matriarch', 'minecraft:piglin#nether', 'minecraft:piglin_brute#nether',
        'minecraft:zombified_piglin#nether', 'minecraft:blaze#nether', 'minecraft:wither_skeleton#nether', 'slag_mite', 'ash_sentinel'],
    3: ['abyss_foundry', 'slag_devourer', 'minecraft:enderman#end', 'minecraft:shulker#end', 'minecraft:endermite#end', 'data_specter',
        'end_relay_turret', 'dragon'],
}


def main():
    stats = {e['id']: e for e in json.load(open(ROOT / 'run' / 'combat_audit.json', encoding='utf-8'))}
    stats['dragon'] = {'id': 'dragon', 'health': 600, 'armor': 0, 'toughness': 0, 'attack': 10}
    for stage in (1, 2, 3):
        print('=' * 110)
        print('ETAPA %d · armaduras: %s · armas: %s' % (stage, ', '.join(a[0] for a in ARMOR[stage]), ', '.join(w[0] for w in WEAPONS[stage])))
        print('=' * 110)
        for key in STAGE[stage]:
            eid = key if ':' in key or key == 'dragon' else T + key
            s = stats.get(eid)
            if s is None:
                print('  ?? falta', eid)
                continue
            hp, arm, tgh = s['health'], s['armor'], s['toughness']
            attacks = {}
            if s['attack'] > 0:
                attacks['cuerpo a cuerpo'] = s['attack']
            attacks.update(ATTACKS.get(eid, {}))
            # golpes que aguanta el jugador con cada armadura de la etapa
            survive = []
            for name, d, t, extra in ARMOR[stage]:
                cells = ['%s %d' % (an[:10], -(-int((20 + extra) * 10) // max(1, int(after_armor(dmg, d, t) * 10)))) for an, dmg in attacks.items()]
                survive.append('%s: %s' % (name.split(' (')[0], ', '.join(cells)))
            # segundos para matarlo con cada arma (normal / cinética)
            kills = []
            for name, dmg, rate, pellets in WEAPONS[stage]:
                dps = after_armor(dmg, arm, tgh) * rate * pellets
                kdps = dps + dmg * KINETIC_PIERCE * rate * pellets
                kills.append('%s %.0fs/%.0fs' % (name.split(' ')[0], hp / max(dps, 1e-6), hp / kdps))
            print('%-34s vida %5.0f  armadura %4.1f/%-4.1f' % (eid.replace(T, ''), hp, arm, tgh))
            print('    matarlo (normal/cinética):', ' · '.join(kills))
            for line in survive:
                print('    golpes hasta morir ·', line)


if __name__ == '__main__':
    main()
