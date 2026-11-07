package com.bryanamaya.tecnocinetico.datagen;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.worldgen.ModBiomes;
import com.bryanamaya.tecnocinetico.worldgen.ModWorldgen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generación de datos (gradlew runData → src/generated/resources): biomas, ajustes de ruido y etiquetas de biomas.
 * Los JSON generados SÍ se suben a git (el juego los lee de ahí).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators {

    private ModDataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();

        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.BIOME, ModBiomes::bootstrap)
                .add(Registries.NOISE_SETTINGS, ModWorldgen::bootstrapNoise);
        DatapackBuiltinEntriesProvider entries = new DatapackBuiltinEntriesProvider(out, event.getLookupProvider(),
                builder, Set.of(Tecnocinetico.MOD_ID));
        gen.addProvider(event.includeServer(), entries);
        gen.addProvider(event.includeServer(), new Tags(out, entries.getRegistryProvider(), files));
    }

    /** Etiquetas de biomas: qué se genera en nuestros biomas (y qué estructuras vanilla NO). */
    static final class Tags extends BiomeTagsProvider {

        Tags(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(out, lookup, Tecnocinetico.MOD_ID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            // Todo el Overworld: minerales del mod y de Create, spawn del Cuerpo de Enjambre y fortalezas (acceso al End).
            for (ResourceKey<Biome> b : ModBiomes.ALL) {
                tag(BiomeTags.IS_OVERWORLD).add(b);
            }
            // Fortalezas: preferir tierra firme.
            for (ResourceKey<Biome> b : ModBiomes.ALL) {
                if (b != ModBiomes.DEBRIS_SEA && b != ModBiomes.DEEP_DEBRIS_SEA && b != ModBiomes.FROZEN_DEBRIS_SEA
                        && b != ModBiomes.POLLUTED_RIVER && b != ModBiomes.SCRAP_COAST) {
                    tag(BiomeTags.STRONGHOLD_BIASED_TO).add(b);
                }
            }
            // Minas abandonadas: encajan con el lore (galerías de extracción). Versión "mesa" en las Cicatrices.
            for (ResourceKey<Biome> b : ModBiomes.ALL) {
                if (b != ModBiomes.EXTRACTION_SCARS) {
                    tag(BiomeTags.HAS_MINESHAFT).add(b);
                }
            }
            tag(BiomeTags.HAS_MINESHAFT_MESA).add(ModBiomes.EXTRACTION_SCARS);

            // Comportamientos vanilla que dependen de etiquetas (sin activar estructuras vanilla).
            tag(BiomeTags.WATER_ON_MAP_OUTLINES).add(ModBiomes.DEBRIS_SEA, ModBiomes.DEEP_DEBRIS_SEA,
                    ModBiomes.FROZEN_DEBRIS_SEA, ModBiomes.POLLUTED_RIVER, ModBiomes.COOLANT_SWAMP);
            tag(BiomeTags.PLAYS_UNDERWATER_MUSIC).add(ModBiomes.DEBRIS_SEA, ModBiomes.DEEP_DEBRIS_SEA,
                    ModBiomes.FROZEN_DEBRIS_SEA, ModBiomes.POLLUTED_RIVER);
            tag(BiomeTags.HAS_CLOSER_WATER_FOG).add(ModBiomes.COOLANT_SWAMP, ModBiomes.POLLUTED_RIVER);
            tag(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS).add(ModBiomes.POLLUTED_RIVER);
            tag(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS).add(ModBiomes.POLLUTED_RIVER);
            tag(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS).add(ModBiomes.COOLANT_SWAMP);
            tag(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT).add(ModBiomes.CINETRITE_CAVES);
            tag(BiomeTags.SPAWNS_SNOW_FOXES).add(ModBiomes.RUINED_TUNDRA);
            tag(BiomeTags.SPAWNS_WHITE_RABBITS).add(ModBiomes.RUINED_TUNDRA, ModBiomes.ANTENNA_PEAKS);
            tag(BiomeTags.SPAWNS_GOLD_RABBITS).add(ModBiomes.VITRIFIED_DESERT);
            tag(BiomeTags.SPAWNS_COLD_VARIANT_FROGS).add(ModBiomes.RUINED_TUNDRA, ModBiomes.ANTENNA_PEAKS,
                    ModBiomes.FROZEN_DEBRIS_SEA);
            tag(BiomeTags.SPAWNS_WARM_VARIANT_FROGS).add(ModBiomes.INDUSTRIAL_JUNGLE, ModBiomes.VITRIFIED_DESERT,
                    ModBiomes.EXTRACTION_SCARS, ModBiomes.DEEP_FACILITY);
            tag(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS).add(ModBiomes.FROZEN_DEBRIS_SEA);

            // Etiqueta propia con todos los biomas del mod (útil para fases siguientes).
            TagKey<Biome> all = TagKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "post_apocalyptic"));
            for (ResourceKey<Biome> b : ModBiomes.ALL) {
                tag(all).add(b);
            }
        }
    }
}
