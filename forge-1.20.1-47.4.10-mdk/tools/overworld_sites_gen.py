"""Datos de las cinco instalaciones finales del Overworld y botín de sus dos guardianes."""
import json
from world_structures_gen import structure, chest, item, entity_loot


def record(title, text):
    page = json.dumps({"text": text}, ensure_ascii=False)
    tag = '{title:' + json.dumps(title, ensure_ascii=False) + ',author:"Archivo de los Constructores",pages:[' + json.dumps(page, ensure_ascii=False) + ']}'
    e = item("minecraft:written_book", 1)
    e["functions"] = [{"function": "minecraft:set_nbt", "tag": tag}]
    return e


def generate():
    sites = [
        ("relay_antenna", ["antenna_peaks", "fractured_lands"], 42, 14, "none"),
        ("data_center", ["dead_megacity", "industrial_jungle", "overgrown_ruins"], 34, 11, "beard_thin"),
        ("survivor_bunker", ["scrap_meadows", "ash_taiga", "ruined_tundra", "fractured_lands"], 30, 10, "none"),
        ("swarm_laboratory", ["deep_facility"], 42, 14, "none"),
        ("sunken_city", ["debris_sea", "deep_debris_sea", "frozen_debris_sea"], 32, 10, "none"),
    ]
    for site, (name, biomes, spacing, sep, adaptation) in enumerate(sites):
        structure(name, "overworld_site", biomes, {"site": site}, spacing, sep, 74121000 + site,
                  adaptation=adaptation, step="underground_structures" if site in (2, 3) else "surface_structures",
                  avoid="survivor_settlement" if site != 4 else None)
        chest(name, [
            {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
                item("scrap_metal", 10, 2, 6), item("minecraft:copper_ingot", 8, 2, 5),
                item("minecraft:redstone", 8, 2, 7), item("minecraft:iron_ingot", 6, 1, 4),
                item("kinetic_round", 5, 3, 8), item("cinetrite_shard", 4, 1, 3),
                item("minecraft:bread", 5, 1, 3)]},
            {"rolls": 1, "entries": [
                item("piezo_transducer", 4), item("deepslate_logic_substrate", 4),
                item("crude_kinetic_coupler", 3), item("inercita_ingot", 2, 1, 2)]}])
    chest("data_archive", [{"rolls": 1, "entries": [
        record("Registro 07: retirada", "El centro de mando ya no está en el Overworld. Los guardianes ejecutan protocolos locales. Destruir uno no detendrá al Núcleo Directriz."),
        record("Registro 12: contención", "La Cinetrita proporciona energía; la Inercita permite contenerla. Los cultivos del laboratorio han integrado ambos materiales en cuerpos biomecánicos."),
        record("Registro 19: vigilancia", "El Vigía supera al guardián arácnido. Sus dos condensadores traseros pueden romperse: flanquearlo interrumpe sus ataques durante tres segundos. La Matriarca es todavía más peligrosa; su blindaje no presenta puntos débiles. Esquiven sus embestidas y las tres zonas de bombardeo.")]}])
    entity_loot("network_watcher", [
        {"rolls": 1, "entries": [item("vector_thruster", 1)]},
        {"rolls": 1, "entries": [item("core_processor", 1)]},
        {"rolls": 1, "entries": [item("precision_mechanism", 1, 2, 4)]},
        {"rolls": 1, "entries": [item("inercita_ingot", 1, 3, 6)]},
        {"rolls": 1, "entries": [item("scrap_metal", 1, 6, 12)]}])
    entity_loot("swarm_matriarch", [
        {"rolls": 1, "entries": [item("adaptive_flight_core", 1)]},
        {"rolls": 1, "entries": [item("swarm_core", 1)]},
        {"rolls": 1, "entries": [item("core_processor", 1)]},
        {"rolls": 1, "entries": [item("cinetrite_ingot", 1, 4, 8)]},
        {"rolls": 1, "entries": [item("inercita_ingot", 1, 4, 8)]}])


if __name__ == "__main__":
    generate()
