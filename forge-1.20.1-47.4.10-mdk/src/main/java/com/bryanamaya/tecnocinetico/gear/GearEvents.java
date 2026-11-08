package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.network.ModNetwork;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica del Equipo Táctico en el servidor: capacidad en cada jugador, conservar/soltar al morir, comer solo,
 * visor de visión nocturna, propulsor acuático, jetpack (el empuje lo aplica el cliente, aquí se gasta batería
 * y se anula el daño por caída) y sincronización con el cliente.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class GearEvents {

    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "tactical_gear");
    /** Jugadores usando el jetpack ahora mismo (lo avisa el cliente con JetpackPacket). */
    public static final Set<UUID> THRUSTING = ConcurrentHashMap.newKeySet();
    /** Duración del efecto de visión nocturna que pone el visor (se renueva; > 200 para que no parpadee). */
    private static final int VISOR_EFFECT = 260;

    private GearEvents() {}

    @Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(TacticalGear.class);
        }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(KEY, new TacticalGear.Provider());
        }
    }

    /** Al reaparecer (o volver del End) se copia el equipo; si murió sin keepInventory ya se soltó en drops. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        boolean keep = !event.isWasDeath() || original.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (keep) {
            TacticalGear.of(original).ifPresent(old -> TacticalGear.of(event.getEntity()).ifPresent(g -> g.load(old.save())));
        }
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        TacticalGear.of(player).ifPresent(g -> {
            for (int i = 0; i < TacticalGear.SIZE; i++) {
                ItemStack s = g.items().getStackInSlot(i);
                if (!s.isEmpty()) {
                    event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), s.copy()));
                    g.items().setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        });
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void changeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        THRUSTING.remove(event.getEntity().getUUID());
    }

    public static void sync(ServerPlayer sp) {
        TacticalGear.of(sp).ifPresent(g -> {
            g.takeDirty();
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ModNetwork.SyncGearPacket(g.save()));
        });
    }

    // ------------------------------------------------------------------ tick del servidor

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) {
            return;
        }
        TacticalGear.of(sp).ifPresent(g -> {
            serverTick(sp, g);
            if (g.takeDirty()) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ModNetwork.SyncGearPacket(g.save()));
            }
        });
    }

    /** Separado del evento para poder probarlo en los GameTests con un jugador simulado. */
    public static void serverTick(Player p, TacticalGear g) {
        int t = p.tickCount;
        if (t % 40 == 0) {
            autoEat(p, g);
        }
        // Visor de Visión Nocturna (cara)
        ItemStack face = g.get(TacticalGear.FACE);
        boolean visor = face.is(ModItems.NIGHT_VISION_VISOR.get()) && g.visorOn() && GadgetItem.hasCharge(face);
        MobEffectInstance nv = p.getEffect(MobEffects.NIGHT_VISION);
        if (visor) {
            if (t % 20 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, VISOR_EFFECT, 0, true, false, true));
                GadgetItem.drain(face, 1);
                g.setChanged();
            }
        } else if (nv != null && nv.isAmbient() && nv.getDuration() <= VISOR_EFFECT) {
            p.removeEffect(MobEffects.NIGHT_VISION);                     // solo quita la del visor, no la de pociones
        }
        // Propulsor Acuático (pies)
        ItemStack feet = g.get(TacticalGear.FEET);
        if (feet.is(ModItems.AQUA_PROPULSOR.get()) && GadgetItem.hasCharge(feet) && p.isInWater()) {
            if (t % 10 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 30, 0, true, false, true));
            }
            if (t % 20 == 0) {
                GadgetItem.drain(feet, 1);
                g.setChanged();
            }
        }
        // Jetpack (espalda): el cliente avisa de que empuja; aquí batería, caída y efectos
        if (THRUSTING.contains(p.getUUID())) {
            ItemStack back = g.get(TacticalGear.BACK);
            if (!back.is(ModItems.JETPACK.get()) || !GadgetItem.hasCharge(back) || p.onGround()) {
                THRUSTING.remove(p.getUUID());
            } else {
                p.fallDistance = 0.0F;
                if (t % 2 == 0) {
                    GadgetItem.drain(back, 1);
                    g.setChanged();
                }
                if (p.level() instanceof ServerLevel server) {
                    Vec3 nozzle = p.position().add(Vec3.directionFromRotation(0, p.yBodyRot).scale(-0.35D)).add(0, 0.7D, 0);
                    server.sendParticles(ModParticles.KINETIC_SPARK.get(), nozzle.x, nozzle.y, nozzle.z, 2, 0.08, 0.05, 0.08, 0.02);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, nozzle.x, nozzle.y - 0.3, nozzle.z,
                            1, 0.05, 0.05, 0.05, 0.01);
                    if (t % 8 == 0) {
                        server.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.JETPACK_THRUST.get(), SoundSource.PLAYERS, 0.6F,
                                0.9F + p.getRandom().nextFloat() * 0.2F);
                    }
                }
            }
        }
    }

    /** Come de las ranuras de comida si tiene hambre (sin desperdiciar: solo si cabe casi todo lo que da). */
    public static void autoEat(Player p, TacticalGear g) {
        if (p.getAbilities().instabuild) {
            return;
        }
        int level = p.getFoodData().getFoodLevel();
        if (level > 14) {
            return;
        }
        for (int i = TacticalGear.FOOD_START; i < TacticalGear.FOOD_START + TacticalGear.FOOD_COUNT; i++) {
            ItemStack s = g.items().getStackInSlot(i);
            FoodProperties food = s.getFoodProperties(p);
            if (s.isEmpty() || food == null) {
                continue;
            }
            if (level + food.getNutrition() <= 22 || level <= 6) {
                p.eat(p.level(), s);                                     // aplica la comida y gasta 1
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, 1.0F);
                g.setChanged();
                return;
            }
        }
    }
}
