package com.bryanamaya.tecnocinetico.nether;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.item.armor.Tier2ArmorItem;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.worldgen.InfernalBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;

/**
 * Peligros de ambiente del Nether (parte 4, equipo de preparación). Dos medidores por jugador, de 0 a 100, que se
 * calculan en el servidor una vez por segundo y se mandan solo a ese jugador para su barra (HUD):
 * <ul>
 * <li><b>Calor</b>: sube en todo el Nether, más en las Forjas de Escoria, los Pozos de Azufre y los Páramos de
 * Obsidiana, cerca de lava y ardiendo. A 70: lentitud; a 85: debilidad y cansancio; a 100: daño ("golpe de calor").
 * Baja fuera del Nether y en el agua. Lo frenan la armadura de piroxita (cada pieza -20 %), la de Piroxita Abisal
 * (completa: inmune) y el Refrigerador Térmico (espalda), que gasta su carga mientras trabaja.</li>
 * <li><b>Ceniza</b>: sube en los biomas de ceniza (Pozos de Azufre, Bosque Carbonizado, Canteras de Basalto, deltas
 * de basalto y valle de almas). A 50: debilidad; a 80: hambre; a 100: daño ("pulmones de ceniza"). La paran el Filtro
 * de Ceniza (cara) y el traje antirradiación completo. Baja fuera de esos biomas.</li>
 * </ul>
 * Datos en los datos persistentes del jugador (se reinician al morir). Multijugador: cada jugador lleva los suyos.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class NetherHazards {

    public static final String HEAT = "TecnoHeat", ASH = "TecnoAsh";
    public static final float MAX = 100.0F;
    public static final float BASE_HEAT = 0.6F, ASH_GAIN = 1.2F;
    public static final ResourceKey<DamageType> OVERHEAT = ResourceKey.create(Registries.DAMAGE_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "overheat"));
    public static final ResourceKey<DamageType> ASH_LUNGS = ResourceKey.create(Registries.DAMAGE_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "ash_lungs"));
    private static final Set<ResourceKey<Biome>> ASHY = Set.of(InfernalBiomes.SULFUR_PITS, InfernalBiomes.CARBON_FOREST,
            InfernalBiomes.BASALT_QUARRIES, Biomes.BASALT_DELTAS, Biomes.SOUL_SAND_VALLEY);

    private NetherHazards() {}

    public static float heat(Player p) {
        return p.getPersistentData().getFloat(HEAT);
    }

    public static float ash(Player p) {
        return p.getPersistentData().getFloat(ASH);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        Player p = event.player;
        if (event.phase != TickEvent.Phase.END || p.level().isClientSide || p.tickCount % 20 != 0) {
            return;
        }
        if (p.isCreative() || p.isSpectator()) {                                      // sin peligros: se vacían las barras
            if (heat(p) > 0 || ash(p) > 0) {
                p.getPersistentData().putFloat(HEAT, 0);
                p.getPersistentData().putFloat(ASH, 0);
                if (p instanceof ServerPlayer sp) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ModNetwork.HazardPacket(0, 0));
                }
            }
            return;
        }
        boolean nether = p.level().dimension() == Level.NETHER;
        Holder<Biome> biome = p.level().getBiome(p.blockPosition());
        float heatGain = nether ? BASE_HEAT + biomeHeat(biome) + lavaHeat(p.level(), p.blockPosition()) + (p.isOnFire() || p.isInLava() ? 3.0F : 0.0F) : 0.0F;
        boolean ashy = nether && ASHY.stream().anyMatch(biome::is);
        step(p, heatGain, ashy);
    }

    /** Calor extra del bioma (por segundo). */
    private static float biomeHeat(Holder<Biome> biome) {
        if (biome.is(InfernalBiomes.SLAG_FORGES)) return 1.0F;
        if (biome.is(InfernalBiomes.SULFUR_PITS)) return 0.6F;
        if (biome.is(InfernalBiomes.OBSIDIAN_WASTES)) return 0.4F;
        if (biome.is(Biomes.CRIMSON_FOREST) || biome.is(Biomes.WARPED_FOREST) || biome.is(Biomes.SOUL_SAND_VALLEY)) return -0.2F;
        return 0.0F;
    }

    /** Lava a menos de 4 bloques (muestreo de 9x5x9): hasta +1,5 por segundo. */
    private static float lavaHeat(Level level, BlockPos c) {
        int lava = 0;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (level.getFluidState(q.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz)).is(FluidTags.LAVA)) {
                        lava++;
                    }
                }
            }
        }
        return Math.min(1.5F, lava * 0.04F);
    }

    /**
     * Un segundo de peligros para el jugador: `heatGain` = calor del sitio (0 fuera del Nether), `ashy` = aire con
     * ceniza. Aplica protecciones, efectos y daño, y sincroniza su barra. Público para las pruebas.
     */
    public static void step(Player p, float heatGain, boolean ashy) {
        var data = p.getPersistentData();
        float heat = data.getFloat(HEAT), ash = data.getFloat(ASH);
        float oldHeat = heat, oldAsh = ash;
        {
            // ---- calor
            heatGain *= heatFactor(p);
            ItemStack cooler = gadget(p, TacticalGear.BACK, ModItems.THERMAL_COOLER.get());
            if (heatGain > 0 && GadgetItem.hasCharge(cooler)) {
                GadgetItem.drain(cooler, 1 + Math.round(heatGain));                  // trabaja más cuanto más calor
                TacticalGear.of(p).ifPresent(TacticalGear::setChanged);
                boolean mk2 = com.bryanamaya.tecnocinetico.gear.NetherGadgetItem.level(cooler) >= 2;
                heat -= mk2 ? 6.0F : 3.0F;                                            // y enfría (Mk II, el doble)
                if (mk2 && p.isInLava()) {                                            // Mk II: aguanta un baño de lava
                    p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
                }
                if (p.isOnFire() && !p.isInLava()) {
                    p.clearFire();
                }
            } else if (heatGain > 0) {
                heat += heatGain;
            } else {
                heat -= p.isInWaterOrBubble() ? 8.0F : heatGain < 0 || p.level().dimension() != Level.NETHER ? 4.0F : 0.5F;
            }
            if (p.isInWaterOrBubble()) {
                heat -= 4.0F;
            }
            heat = clamp(heat);
            // ---- ceniza
            boolean filtered = Tier2ArmorItem.hazmatPieces(p) >= 4;
            ItemStack filter = gadget(p, TacticalGear.FACE, ModItems.ASH_FILTER.get());
            if (ashy && !filtered && GadgetItem.hasCharge(filter)) {
                GadgetItem.drain(filter, 1);
                TacticalGear.of(p).ifPresent(TacticalGear::setChanged);
                filtered = true;
            }
            if (com.bryanamaya.tecnocinetico.gear.NetherGadgetItem.level(filter) >= 2 && GadgetItem.hasCharge(filter)) {
                for (var bad : java.util.List.of(MobEffects.WITHER, MobEffects.POISON, MobEffects.BLINDNESS)) {   // Mk II: filtra toxinas
                    if (p.hasEffect(bad)) {
                        p.removeEffect(bad);
                        GadgetItem.drain(filter, 6);
                    }
                }
            }
            ash = clamp(ashy && !filtered ? ash + ASH_GAIN : ash - (ashy ? 1.0F : 3.0F));
            effects(p, heat, ash);
        }
        data.putFloat(HEAT, heat);
        data.putFloat(ASH, ash);
        if (p instanceof ServerPlayer sp && (Math.round(heat) != Math.round(oldHeat) || Math.round(ash) != Math.round(oldAsh)
                || p.tickCount % 200 == 0)) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ModNetwork.HazardPacket(Math.round(heat), Math.round(ash)));
        }
    }

    /** Fracción del calor que llega al jugador según su armadura: piroxita -20 % por pieza; Piroxita Abisal -25 % y completa, nada. */
    public static float heatFactor(Player p) {
        if (Tier2ArmorItem.hasFullSet(p, Tier2ArmorItem.Kind.REFINED_PIROXITE)) {
            return 0.0F;
        }
        float f = 1.0F;
        for (ItemStack s : p.getArmorSlots()) {
            if (s.getItem() instanceof Tier2ArmorItem a) {
                if (a.kind() == Tier2ArmorItem.Kind.PIROXITE) f -= 0.2F;
                if (a.kind() == Tier2ArmorItem.Kind.REFINED_PIROXITE) f -= 0.25F;
            }
        }
        return Math.max(0.0F, f);
    }

    private static void effects(Player p, float heat, float ash) {
        if (heat >= 70) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        }
        if (heat >= 85) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, false, false, true));
        }
        if (heat >= MAX && p.tickCount % 40 == 0) {
            p.hurt(source(p, OVERHEAT), 2.0F);
        }
        if (ash >= 50) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        }
        if (ash >= 80) {
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, false, true));
            if (p.tickCount % 60 == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.5F, 0.6F);   // tos
            }
        }
        if (ash >= MAX && p.tickCount % 40 == 0) {
            p.hurt(source(p, ASH_LUNGS), 2.0F);
        }
    }

    private static DamageSource source(Player p, ResourceKey<DamageType> type) {
        return new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(MAX, v));
    }

    /** El gadget `item` si lo lleva puesto en `slot` del Equipo Táctico (si no, vacío). */
    private static ItemStack gadget(Player p, int slot, net.minecraft.world.item.Item item) {
        return TacticalGear.of(p).map(g -> g.get(slot)).filter(s -> s.is(item)).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ Botas de Anclaje

    /** Botas de Anclaje con carga: -85 % de empuje (golpes, explosiones de ghast que golpean). */
    @SubscribeEvent
    public static void knockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide) {
            ItemStack boots = gadget(p, TacticalGear.FEET, ModItems.ANCHOR_BOOTS.get());
            if (GadgetItem.hasCharge(boots)) {
                event.setStrength(event.getStrength() * (com.bryanamaya.tecnocinetico.gear.NetherGadgetItem.level(boots) >= 2 ? 0.0F : 0.15F));
                GadgetItem.drain(boots, 4);
                TacticalGear.of(p).ifPresent(TacticalGear::setChanged);
            }
        }
    }

    /** Botas de Anclaje con carga: -75 % de daño por caída. */
    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && event.getDistance() > 3.0F) {
            ItemStack boots = gadget(p, TacticalGear.FEET, ModItems.ANCHOR_BOOTS.get());
            if (GadgetItem.hasCharge(boots)) {
                event.setDamageMultiplier(event.getDamageMultiplier() * (com.bryanamaya.tecnocinetico.gear.NetherGadgetItem.level(boots) >= 2 ? 0.1F : 0.25F));
                GadgetItem.drain(boots, 8);
                TacticalGear.of(p).ifPresent(TacticalGear::setChanged);
            }
        }
    }
}
