"""Genera los detalles del terreno (features) del Overworld: configured_feature, placed_feature y biome_modifier.

Uso (desde forge-1.20.1-47.4.10-mdk):  python tools/world_features_gen.py
Fase 3 de docs/PLAN_MUNDO.md: montones de escombros/chatarra, pedazos, capas de ceniza, cráteres, restos de muros,
árboles muertos y cristales de cinetrita. Cráter/muro/árbol usan código propio (worldgen/feature/).

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


def modifier(name, biomes, features, step="vegetal_decoration"):
    write(DATA / NS / "forge/biome_modifier" / f"{name}.json", {
        "type": "forge:add_features", "biomes": [rl(b) for b in biomes] if len(biomes) > 1 else rl(biomes[0]),
        "features": [rl(f) for f in features], "step": step})


def remove(name, biomes, features, step="vegetal_decoration"):
    """Quita features vanilla de los biomas del mod (forge:remove_features)."""
    write(DATA / NS / "forge/biome_modifier" / f"{name}.json", {
        "type": "forge:remove_features", "biomes": [rl(b) for b in biomes] if len(biomes) > 1 else rl(biomes[0]),
        "features": [f if ":" in f else "minecraft:" + f for f in features], "steps": step})


def sickly_tree(name, tree, sapling, per_chunk=None, rarity=None):
    """Árbol vivo suelto (configurado vanilla: minecraft:oak, spruce, birch) donde sobreviviría su brote."""
    mods = []
    if rarity:
        mods.append({"type": "minecraft:rarity_filter", "chance": rarity})
    if per_chunk:
        mods.append({"type": "minecraft:count", "count": per_chunk})
    mods += [{"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
             {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"},
             {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                       "state": {"Name": sapling, "Properties": {"stage": "0"}}}}]
    write(DATA / NS / "worldgen/placed_feature" / f"{name}.json", {"feature": tree, "placement": mods})


def weighted(*pairs):
    """Proveedor de estados con pesos: weighted(("concrete", 3), ("minecraft:gravel", 1), ...)."""
    def st(b):
        return {"Name": b} if ":" in b else {"Name": rl(b)}
    return {"type": "minecraft:weighted_state_provider",
            "entries": [{"weight": w, "data": st(b)} for b, w in pairs]}


def uniform(a, b):
    return {"type": "minecraft:uniform", "value": {"min_inclusive": a, "max_inclusive": b}}


def configured(name, ftype, config):
    write(DATA / NS / "worldgen/configured_feature" / f"{name}.json", {"type": ftype, "config": config})


def placed_custom(name, feature, mods):
    write(DATA / NS / "worldgen/placed_feature" / f"{name}.json", {"feature": rl(feature), "placement": mods})


def surface_mods(per_chunk=None, rarity=None, heightmap="WORLD_SURFACE_WG", dry=True):
    mods = []
    if rarity:
        mods.append({"type": "minecraft:rarity_filter", "chance": rarity})
    if per_chunk:
        mods.append({"type": "minecraft:count", "count": per_chunk})
    mods += [{"type": "minecraft:in_square"}]
    if dry:
        mods.append({"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0})
    mods += [{"type": "minecraft:heightmap", "heightmap": heightmap}, {"type": "minecraft:biome"}]
    return mods


def crystal_mods(per_chunk, up):
    """Cuevas: busca suelo (o techo) desde una altura al azar y coloca encima (o debajo)."""
    return [{"type": "minecraft:count", "count": per_chunk},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                          "min_inclusive": {"above_bottom": 8},
                                                          "max_inclusive": {"absolute": 64}}},
            {"type": "minecraft:environment_scan", "direction_of_search": "up" if up else "down", "max_steps": 12,
             "target_condition": {"type": "minecraft:solid"},
             "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}},
            {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -1 if up else 1},
            {"type": "minecraft:block_predicate_filter",
             "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}},
            {"type": "minecraft:biome"}]


STEPS = {"crater": "local_modifications", "ruin": "surface_structures", "veg": "vegetal_decoration",
         "top": "top_layer_modification", "cave": "underground_decoration"}


def main():
    # ------------------------------------------------ montones y pedazos (random_patch)
    patch("rubble_piles", pile_states("rubble", False), state("rubble", pieces=1, waterlogged=False), 12, 5)
    patch("scrap_piles", pile_states("scrap_pile", False), state("scrap_pile", pieces=1, waterlogged=False), 8, 4)
    patch("debris_bits", {"type": "minecraft:simple_state_provider", "state": state("debris_layer")},
          state("debris_layer"), 24, 6)
    patch("rubble_piles_underwater", pile_states("rubble", True), state("rubble", pieces=1, waterlogged=True), 12, 5,
          water=True)
    patch("scrap_piles_underwater", pile_states("scrap_pile", True), state("scrap_pile", pieces=1, waterlogged=True), 8, 4,
          water=True)
    patch("dead_bushes", {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dead_bush"}},
          {"Name": "minecraft:dead_bush"}, 12, 6)
    patch("ash_layers", {"type": "minecraft:weighted_state_provider", "entries": [
        {"weight": w, "data": state("ash_layer", layers=n)} for n, w in ((1, 6), (2, 3), (3, 1))]},
        state("ash_layer", layers=1), 48, 7)

    # ------------------------------------------------ cráteres (código propio: CraterFeature)
    configured("crater_vitrified", rl("crater"), {
        "radius": uniform(4, 8), "floor": weighted(("vitrified_sand", 5), ("vitrified_glass", 1)),
        "rim": weighted(("minecraft:sand", 3), ("vitrified_sand", 1))})
    configured("crater_ash", rl("crater"), {
        "radius": uniform(3, 7), "floor": weighted(("ash_block", 3), ("minecraft:gravel", 1), ("minecraft:coarse_dirt", 1)),
        "rim": weighted(("minecraft:coarse_dirt", 2), ("minecraft:gravel", 1))})
    configured("crater_scorched", rl("crater"), {
        "radius": uniform(3, 6), "floor": weighted(("minecraft:coarse_dirt", 2), ("minecraft:gravel", 2), ("ash_block", 1)),
        "rim": weighted(("minecraft:coarse_dirt", 2), ("minecraft:dirt", 1))})

    # ------------------------------------------------ restos de muros (RuinWallFeature)
    configured("ruin_wall_concrete", rl("ruin_wall"), {
        "wall": weighted(("concrete", 3), ("cracked_concrete", 4), ("mossy_concrete", 1)),
        "height": uniform(2, 5), "length": uniform(3, 6), "vines": False})
    configured("ruin_wall_overgrown", rl("ruin_wall"), {
        "wall": weighted(("mossy_concrete", 4), ("cracked_concrete", 2), ("concrete", 1)),
        "height": uniform(2, 4), "length": uniform(3, 6), "vines": True})
    configured("ruin_wall_frozen", rl("ruin_wall"), {
        "wall": weighted(("cracked_concrete", 3), ("concrete", 2)),
        "height": uniform(1, 3), "length": uniform(2, 5), "vines": False})

    # ------------------------------------------------ árboles muertos (DeadTreeFeature)
    configured("dead_tree", rl("dead_tree"), {"log": weighted(("dead_log", 1)), "height": uniform(4, 7)})
    configured("dead_tree_short", rl("dead_tree"), {"log": weighted(("dead_log", 1)), "height": uniform(2, 4)})

    # ------------------------------------------------ cristales de cinetrita (suelo y techo de cuevas)
    configured("cinetrite_crystal_floor", "minecraft:simple_block", {"to_place": {
        "type": "minecraft:simple_state_provider",
        "state": state("cinetrite_cluster", facing="up", waterlogged=False)}})
    configured("cinetrite_crystal_ceiling", "minecraft:simple_block", {"to_place": {
        "type": "minecraft:simple_state_provider",
        "state": state("cinetrite_cluster", facing="down", waterlogged=False)}})

    # ------------------------------------------------ reparto por bioma
    # (feature, paso, por chunk, 1 de cada N chunks)
    # Densidades subidas el 2026-11-12 (el usuario: "no parece post-apocalíptico"): restos por todas partes.
    plan = {
        "dead_megacity": [("crater_scorched", "crater", None, 4), ("ruin_wall_concrete", "ruin", 3, None),
                          ("rubble_piles", "veg", 4, None), ("debris_bits", "veg", 4, None), ("scrap_piles", "veg", 2, None),
                          ("dead_bushes", "veg", 2, None)],
        "scrap_meadows": [("crater_scorched", "crater", None, 5), ("ruin_wall_concrete", "ruin", None, 2),
                          ("rubble_piles", "veg", 1, None), ("debris_bits", "veg", 2, None), ("scrap_piles", "veg", None, 2),
                          ("dead_tree", "veg", 1, None), ("dead_bushes", "veg", 2, None)],
        "scrap_coast": [("scrap_piles", "veg", 3, None), ("rubble_piles", "veg", 1, None), ("debris_bits", "veg", 1, None)],
        "overgrown_ruins": [("ruin_wall_overgrown", "ruin", 1, None), ("rubble_piles", "veg", None, 2),
                            ("debris_bits", "veg", 1, None), ("dead_tree", "veg", 2, None)],
        "industrial_jungle": [("ruin_wall_overgrown", "ruin", 2, None), ("rubble_piles", "veg", None, 2),
                              ("scrap_piles", "veg", None, 2)],
        "fractured_lands": [("crater_ash", "crater", None, 2), ("rubble_piles", "veg", None, 2),
                            ("dead_tree", "veg", 2, None), ("ash_layers", "top", 3, None), ("dead_bushes", "veg", 1, None)],
        "ruined_tundra": [("ruin_wall_frozen", "ruin", None, 2), ("rubble_piles", "veg", None, 2),
                          ("debris_bits", "veg", None, 2), ("dead_tree", "veg", None, 2)],
        "ash_taiga": [("crater_ash", "crater", None, 6), ("scrap_piles", "veg", None, 3), ("dead_tree", "veg", 4, None),
                      ("ash_layers", "top", 6, None)],
        "vitrified_desert": [("crater_vitrified", "crater", None, 3), ("scrap_piles", "veg", None, 3),
                             ("dead_tree_short", "veg", None, 4)],
        "extraction_scars": [("crater_scorched", "crater", None, 4), ("scrap_piles", "veg", None, 3),
                             ("rubble_piles", "veg", None, 2)],
        "coolant_swamp": [("rubble_piles", "veg", None, 3), ("scrap_piles", "veg", None, 3), ("dead_tree", "veg", None, 2)],
    }
    for biome, entries in plan.items():
        by_step = {}
        for feature, step, count, rarity in entries:
            n = f"{biome}/{feature}"
            if feature in ("rubble_piles", "scrap_piles", "debris_bits", "ash_layers", "dead_bushes"):
                placed(n, feature, per_chunk=count, rarity=rarity)
            else:
                placed_custom(n, feature, surface_mods(count, rarity))
            by_step.setdefault(step, []).append(n)
        for step, names in by_step.items():
            name = f"features/{biome}" if step == "veg" else f"features/{biome}_{step}"
            modifier(name, [biome], names, STEPS[step])

    # ------------------------------------------------ vegetación "mitad y mitad" (usuario, 2026-11-12)
    # Se quitan los árboles vanilla de los biomas abiertos y se ponen árboles vivos ralos junto a los muertos
    # (de "plan"). Bosque, jungla y pantano conservan sus árboles (la naturaleza se comió la ciudad) pero con
    # árboles muertos mezclados. Flores, calabazas y bayas desaparecen casi en todas partes.
    remove("vegetation/remove_trees", ["dead_megacity", "scrap_meadows", "ash_taiga", "fractured_lands", "ruined_tundra"],
           ["trees_plains", "trees_taiga", "trees_windswept_hills", "trees_snowy"])
    remove("vegetation/remove_flowers", ["dead_megacity", "scrap_meadows", "ash_taiga", "fractured_lands", "ruined_tundra",
                                         "overgrown_ruins", "industrial_jungle", "coolant_swamp", "scrap_coast", "vitrified_desert",
                                         "extraction_scars", "debris_sea", "deep_debris_sea", "frozen_debris_sea", "polluted_river"],
           ["flower_plains", "flower_default", "forest_flowers", "flower_warm", "flower_swamp", "patch_pumpkin",
            "patch_berry_common", "patch_melon", "patch_large_fern"])
    remove("vegetation/remove_tall_grass", ["dead_megacity", "scrap_meadows"], ["patch_tall_grass_2", "patch_sugar_cane"])
    remove("vegetation/remove_grass", ["dead_megacity"], ["patch_grass_plain"])
    sickly_tree("vegetation/sickly_oak_meadows", "minecraft:oak", "minecraft:oak_sapling", rarity=3)
    sickly_tree("vegetation/sickly_birch_meadows", "minecraft:birch", "minecraft:birch_sapling", rarity=6)
    sickly_tree("vegetation/sickly_spruce_taiga", "minecraft:spruce", "minecraft:spruce_sapling", per_chunk=3)
    sickly_tree("vegetation/sickly_spruce_hills", "minecraft:spruce", "minecraft:spruce_sapling", per_chunk=1)
    sickly_tree("vegetation/sickly_spruce_tundra", "minecraft:spruce", "minecraft:spruce_sapling", rarity=3)
    modifier("vegetation/sickly_trees_meadows", ["scrap_meadows"], ["vegetation/sickly_oak_meadows", "vegetation/sickly_birch_meadows"])
    modifier("vegetation/sickly_trees_taiga", ["ash_taiga"], ["vegetation/sickly_spruce_taiga"])
    modifier("vegetation/sickly_trees_hills", ["fractured_lands"], ["vegetation/sickly_spruce_hills"])
    modifier("vegetation/sickly_trees_tundra", ["ruined_tundra"], ["vegetation/sickly_spruce_tundra"])

    # ------------------------------------------------ farolas y postes eléctricos (StreetPropFeature)
    configured("street_lamps", rl("street_prop"), {"kind": "lamp", "standing": 3, "fallen": 3})
    configured("street_lamps_ruined", rl("street_prop"), {"kind": "lamp", "standing": 1, "fallen": 4})
    configured("utility_poles", rl("street_prop"), {"kind": "pole", "standing": 3, "fallen": 2})
    props = {  # (feature, por chunk, 1 de cada N chunks)
        "dead_megacity": [("street_lamps", 2, None), ("utility_poles", None, 2)],
        "scrap_meadows": [("utility_poles", None, 2), ("street_lamps_ruined", None, 4)],
        "overgrown_ruins": [("street_lamps_ruined", None, 3), ("utility_poles", None, 4)],
        "fractured_lands": [("utility_poles", None, 4)],
        "ruined_tundra": [("utility_poles", None, 3), ("street_lamps_ruined", None, 6)],
        "scrap_coast": [("street_lamps_ruined", None, 3), ("utility_poles", None, 4)],
        "ash_taiga": [("utility_poles", None, 5)],
        "vitrified_desert": [("utility_poles", None, 5)],
        "coolant_swamp": [("utility_poles", None, 4)],
    }
    for biome, entries in props.items():
        names = []
        for feature, count, rarity in entries:
            n = f"{biome}/{feature}"
            placed_custom(n, feature, surface_mods(count, rarity, heightmap="MOTION_BLOCKING_NO_LEAVES"))
            names.append(n)
        modifier(f"features/{biome}_props", [biome], names, STEPS["ruin"])

    # mares y río: un único modifier con el mismo orden para todos
    placed("seabed/rubble_piles", "rubble_piles_underwater", per_chunk=2, water=True)
    placed("seabed/scrap_piles", "scrap_piles_underwater", rarity=2, water=True)
    modifier("features/seabed", ["debris_sea", "deep_debris_sea", "frozen_debris_sea", "polluted_river"],
             ["seabed/rubble_piles", "seabed/scrap_piles"], STEPS["veg"])

    # cuevas de cinetrita
    placed_custom("cinetrite_caves/crystal_floor", "cinetrite_crystal_floor", crystal_mods(40, up=False))
    placed_custom("cinetrite_caves/crystal_ceiling", "cinetrite_crystal_ceiling", crystal_mods(25, up=True))
    modifier("features/cinetrite_caves_cave", ["cinetrite_caves"],
             ["cinetrite_caves/crystal_floor", "cinetrite_caves/crystal_ceiling"], STEPS["cave"])
    print("ok: features del Overworld")


if __name__ == "__main__":
    main()
