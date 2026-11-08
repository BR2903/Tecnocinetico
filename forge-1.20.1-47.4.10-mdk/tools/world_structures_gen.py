"""Genera las estructuras del Overworld (fase 4 de docs/PLAN_MUNDO.md): structure, structure_set, etiquetas
de bioma has_structure/* y tablas de botín de cofres.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/world_structures_gen.py
El código de cada tipo está en src/main/java/.../worldgen/structure/.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data"
NS = "tecnocinetico"


def rl(p):
    return f"{NS}:{p}"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


def uniform(a, b):
    return {"type": "minecraft:uniform", "value": {"min_inclusive": a, "max_inclusive": b}}


def biome_tag(name, biomes):
    write(DATA / NS / "tags/worldgen/biome/has_structure" / f"{name}.json", {"values": [rl(b) for b in biomes]})


def structure(name, stype, biomes, extra, spacing, separation, salt, adaptation="beard_thin",
              step="surface_structures", avoid=None):
    biome_tag(name, biomes)
    write(DATA / NS / "worldgen/structure" / f"{name}.json", {
        "type": rl(stype), "biomes": f"#{rl('has_structure/' + name)}", "step": step,
        "spawn_overrides": {}, "terrain_adaptation": adaptation, **extra})
    write(DATA / NS / "worldgen/structure_set" / f"{name}.json", {
        "structures": [{"structure": rl(name), "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt,
                      **({"exclusion_zone": {"other_set": rl(avoid), "chunk_count": 3}} if avoid else {})}})


def item(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ":" in name else rl(name), "weight": weight}
    if (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def item_tag(name, values):
    """Etiqueta de objetos con entradas OPCIONALES: si el mod (p. ej. Create) no está, simplemente no salen."""
    write(DATA / NS / "tags/items" / f"{name}.json",
          {"values": [{"id": v, "required": False} if not v.startswith(NS) else v for v in values]})


def tag_entry(tag, weight, lo=1, hi=1):
    """Entrada de botín que elige al azar un objeto de la etiqueta (vacía = no sale nada)."""
    e = {"type": "minecraft:tag", "name": tag if ":" in tag else rl(tag), "expand": True, "weight": weight}
    if (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def chest(name, pools):
    write(DATA / NS / "loot_tables/chests" / f"{name}.json", {"type": "minecraft:chest", "pools": pools})


def main():
    # ------------------------------------------------ edificios en ruinas
    structure("ruined_building_megacity", "ruined_building", ["dead_megacity"],
              {"floors": uniform(2, 6), "width": uniform(7, 13), "style": "urban"}, 9, 3, 74120931, avoid="survivor_settlement")
    structure("ruined_building_urban", "ruined_building", ["scrap_meadows", "fractured_lands", "ash_taiga"],
              {"floors": uniform(1, 3), "width": uniform(7, 11), "style": "urban"}, 28, 10, 74120932, avoid="survivor_settlement")
    structure("ruined_building_overgrown", "ruined_building", ["overgrown_ruins", "industrial_jungle"],
              {"floors": uniform(1, 3), "width": uniform(7, 11), "style": "overgrown"}, 20, 7, 74120933, avoid="survivor_settlement")
    structure("ruined_building_frozen", "ruined_building", ["ruined_tundra"],
              {"floors": uniform(1, 2), "width": uniform(6, 10), "style": "frozen"}, 24, 8, 74120934, avoid="survivor_settlement")

    # botín: sobre todo chatarra y materiales comunes; las piezas del mod son raras (mod difícil)
    chest("ruined_building", [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "entries": [
            item("scrap_metal", 20, 1, 4), item("minecraft:iron_nugget", 15, 2, 8), item("minecraft:copper_ingot", 10, 1, 3),
            item("cinetrite_shard", 6, 1, 3), item("minecraft:redstone", 6, 1, 4), item("minecraft:string", 6, 1, 3),
            item("minecraft:paper", 5, 1, 3), item("minecraft:bread", 6, 1, 2), item("minecraft:glass_bottle", 4),
            item("minecraft:coal", 8, 1, 4)]},
        {"rolls": 1, "entries": [
            {"type": "minecraft:empty", "weight": 40},
            item("crude_kinetic_coupler", 5), item("piezo_transducer", 4), item("deepslate_logic_substrate", 2),
            item("raw_cinetrite", 4, 1, 2), item("module_optic", 1), item("module_extended_mag", 1),
            item("module_inercita_barrel", 1), item("module_quick_reload", 1)]},
    ])
    # ------------------------------------------------ asentamiento de supervivientes (sustituye a las aldeas)
    # paso top_layer_modification: se construye DESPUÉS de los árboles, así puede despejar los de dentro
    structure("survivor_settlement", "settlement", ["scrap_meadows", "overgrown_ruins", "ash_taiga", "ruined_tundra"],
              {}, 30, 10, 74120940, adaptation="none", step="top_layer_modification")
    chest("settlement_home", [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            item("minecraft:bread", 12, 1, 3), item("minecraft:potato", 10, 1, 4), item("minecraft:carrot", 8, 1, 4),
            item("minecraft:apple", 6, 1, 2), item("minecraft:cooked_mutton", 4, 1, 2), item("minecraft:torch", 8, 2, 6),
            item("minecraft:coal", 8, 1, 4), item("minecraft:string", 5, 1, 3), item("minecraft:leather", 4, 1, 2),
            item("minecraft:paper", 5, 1, 3), item("scrap_metal", 6, 1, 3), item("minecraft:wheat_seeds", 6, 2, 5)]}])
    chest("settlement_workshop", [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            item("minecraft:iron_ingot", 6, 1, 3), item("minecraft:iron_nugget", 10, 3, 9), item("minecraft:copper_ingot", 10, 1, 4),
            item("scrap_metal", 14, 2, 6), item("minecraft:redstone", 6, 1, 5), item("minecraft:coal", 8, 2, 5),
            item("cinetrite_shard", 5, 1, 3), item("minecraft:stone_pickaxe", 3), item("minecraft:iron_shovel", 2)]},
        {"rolls": 1, "entries": [
            {"type": "minecraft:empty", "weight": 30},
            item("crude_kinetic_coupler", 6), item("piezo_transducer", 4), item("cinetrite_ingot", 3, 1, 2),
            item("module_quick_reload", 1), item("module_extended_mag", 1)]}])
    chest("settlement_storage", [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "entries": [
            item("minecraft:wheat", 10, 2, 8), item("minecraft:potato", 8, 2, 6), item("minecraft:beetroot", 6, 2, 6),
            item("minecraft:dried_kelp", 4, 2, 6), item("minecraft:spruce_log", 6, 2, 6), item("dead_log", 6, 2, 6),
            item("minecraft:coal", 6, 1, 4), item("scrap_metal", 6, 1, 4), item("minecraft:glass_bottle", 4, 1, 3)]}])
    # ------------------------------------------------ fábrica abandonada (botín con piezas de Create)
    structure("abandoned_factory", "abandoned_factory", ["industrial_jungle", "scrap_meadows", "extraction_scars"],
              {}, 36, 12, 74120950, avoid="survivor_settlement")
    item_tag("loot/create_common", ["create:andesite_alloy", "create:shaft", "create:cogwheel", "create:iron_sheet",
                                    "create:copper_sheet", "create:zinc_ingot", "create:zinc_nugget",
                                    "create:belt_connector", "createaddition:copper_wire", "createaddition:iron_rod"])
    item_tag("loot/create_rare", ["create:large_cogwheel", "create:brass_ingot", "create:electron_tube",
                                  "create:brass_sheet", "createaddition:capacitor", "createaddition:connector"])
    item_tag("loot/create_precious", ["create:precision_mechanism", "create:sturdy_sheet"])
    chest("abandoned_factory", [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            tag_entry("loot/create_common", 18, 1, 4), item("scrap_metal", 16, 2, 6),
            item("minecraft:iron_nugget", 10, 3, 9), item("minecraft:copper_ingot", 8, 1, 4), item("minecraft:redstone", 6, 2, 6),
            item("minecraft:coal", 6, 2, 6), item("minecraft:chain", 3, 1, 3)]},
        {"rolls": 1, "entries": [
            {"type": "minecraft:empty", "weight": 20}, tag_entry("loot/create_rare", 10, 1, 2),
            item("crude_kinetic_coupler", 4), item("piezo_transducer", 3), item("deepslate_logic_substrate", 2)]},
    ])
    chest("factory_office", [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
            item("minecraft:paper", 10, 2, 6), item("minecraft:book", 4), item("scrap_metal", 8, 2, 5),
            item("minecraft:redstone", 6, 2, 6), item("cinetrite_shard", 5, 1, 3), tag_entry("loot/create_common", 6, 1, 3)]},
        {"rolls": 1, "entries": [
            {"type": "minecraft:empty", "weight": 14}, tag_entry("loot/create_rare", 8, 1, 2),
            tag_entry("loot/create_precious", 3), item("deepslate_logic_substrate", 3), item("module_optic", 1),
            item("module_quick_reload", 1)]},
    ])
    print("ok: estructuras del Overworld")


if __name__ == "__main__":
    main()
