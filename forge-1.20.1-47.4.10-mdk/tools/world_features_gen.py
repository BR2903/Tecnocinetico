"""Genera los detalles del terreno (features) del Overworld: configured_feature, placed_feature y biome_modifier.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/world_features_gen.py
Fase 3 de docs/PLAN_MUNDO.md. Por ahora: montones de escombros/chatarra y pedazos de escombro.

Regla para evitar el error "Feature order cycle": cada placed_feature se añade con UN solo biome_modifier
(si se comparte entre biomas, que sea en el mismo modifier y siempre en el mismo orden).
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


def state(block, **props):
    s = {"Name": rl(block)}
    if props:
        s["Properties"] = {k: str(v).lower() for k, v in props.items()}
    return s


def pile_states(block, water):
    """Montones de 1 a 4 trozos (los pequeños son más frecuentes)."""
    return {"type": "minecraft:weighted_state_provider", "entries": [
        {"weight": w, "data": state(block, pieces=n, waterlogged=water)} for n, w in ((1, 5), (2, 4), (3, 2), (4, 1))]}


def patch(name, provider, survive_state, tries, spread, water=False):
    """random_patch: intenta colocar `tries` veces en un radio `spread` (solo en aire/agua y si sobrevive)."""
    write(DATA / NS / "worldgen/configured_feature" / f"{name}.json", {
        "type": "minecraft:random_patch",
        "config": {"tries": tries, "xz_spread": spread, "y_spread": 2, "feature": {
            "feature": {"type": "minecraft:simple_block", "config": {"to_place": provider}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_fluids" if water else "minecraft:matching_blocks",
                 **({"fluids": "minecraft:water"} if water else {"blocks": "minecraft:air"})},
                {"type": "minecraft:would_survive", "state": survive_state}]}}]}}})


def placed(name, feature, per_chunk=None, rarity=None, water=False):
    """Colocación: `per_chunk` intentos por chunk o 1 de cada `rarity` chunks; en la superficie (o el lecho)."""
    mods = []
    if rarity:
        mods.append({"type": "minecraft:rarity_filter", "chance": rarity})
    if per_chunk:
        mods.append({"type": "minecraft:count", "count": per_chunk})
    mods += [{"type": "minecraft:in_square"},
             {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG" if water else "MOTION_BLOCKING_NO_LEAVES"},
             {"type": "minecraft:biome"}]
    write(DATA / NS / "worldgen/placed_feature" / f"{name}.json", {"feature": rl(feature), "placement": mods})


def modifier(name, biomes, features):
    write(DATA / NS / "forge/biome_modifier" / f"{name}.json", {
        "type": "forge:add_features", "biomes": [rl(b) for b in biomes] if len(biomes) > 1 else rl(biomes[0]),
        "features": [rl(f) for f in features], "step": "vegetal_decoration"})


def main():
    # --- configured features
    patch("rubble_piles", pile_states("rubble", False), state("rubble", pieces=1, waterlogged=False), 12, 5)
    patch("scrap_piles", pile_states("scrap_pile", False), state("scrap_pile", pieces=1, waterlogged=False), 8, 4)
    patch("debris_bits", {"type": "minecraft:simple_state_provider", "state": state("debris_layer")},
          state("debris_layer"), 24, 6)
    patch("rubble_piles_underwater", pile_states("rubble", True), state("rubble", pieces=1, waterlogged=True), 12, 5,
          water=True)
    patch("scrap_piles_underwater", pile_states("scrap_pile", True), state("scrap_pile", pieces=1, waterlogged=True), 8, 4,
          water=True)

    # --- por bioma (cada placed_feature en un único modifier)
    plan = {
        "dead_megacity": [("rubble_piles", 3, None), ("debris_bits", 3, None), ("scrap_piles", 1, None)],
        "scrap_meadows": [("rubble_piles", None, 3), ("debris_bits", 1, None), ("scrap_piles", None, 2)],
        "scrap_coast": [("scrap_piles", 2, None), ("rubble_piles", None, 2)],
        "overgrown_ruins": [("rubble_piles", None, 4), ("debris_bits", None, 2)],
        "industrial_jungle": [("rubble_piles", None, 4), ("scrap_piles", None, 4)],
        "fractured_lands": [("rubble_piles", None, 3)],
        "ruined_tundra": [("rubble_piles", None, 4), ("debris_bits", None, 3)],
        "ash_taiga": [("scrap_piles", None, 5)],
        "vitrified_desert": [("scrap_piles", None, 4)],
    }
    for biome, entries in plan.items():
        names = []
        for feature, count, rarity in entries:
            n = f"{biome}/{feature}"
            placed(n, feature, per_chunk=count, rarity=rarity)
            names.append(n)
        modifier(f"features/{biome}", [biome], names)

    # mares y río: un único modifier con el mismo orden para todos
    placed("seabed/rubble_piles", "rubble_piles_underwater", per_chunk=2, water=True)
    placed("seabed/scrap_piles", "scrap_piles_underwater", rarity=2, water=True)
    modifier("features/seabed", ["debris_sea", "deep_debris_sea", "frozen_debris_sea", "polluted_river"],
             ["seabed/rubble_piles", "seabed/scrap_piles"])
    print("ok: features del Overworld")


if __name__ == "__main__":
    main()
