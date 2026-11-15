package com.bryanamaya.tecnocinetico.gear;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Élitros de Propulsión: élitros reforzados con los propulsores de un Jetpack Mk III. Planean como unos élitros
 * (más duraderos: {@link #DURABILITY}, se reparan con nulita) y, <b>mientras planeas y mantienes saltar</b>, empujan
 * hacia donde miras gastando combustible (NBT {@code Fuel}, máx. {@link #MAX_FUEL}: la misma gasolina que el
 * jetpack, que repostan solos del inventario al quedarse casi vacíos).
 * <p>
 * El empuje lo aplica el cliente (el movimiento del jugador es suyo, como el jetpack: {@code client/ElytraThrustClient});
 * el servidor gasta el combustible, pone las chispas y el sonido y deja volar sin expulsar.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public class ThrusterElytraItem extends ElytraItem {

    public static final int DURABILITY = 1200, MAX_FUEL = 3200;
    /** Empuje por tick hacia la mirada y velocidad a la que tiende (los cohetes vanilla: 0,1 y 1,5 con mucha más fuerza). */
    public static final double PUSH = 0.05D, TOP_SPEED = 1.8D, PULL = 0.06D;
    private static final String FUEL = "Fuel";
    /** Jugadores empujando ahora (el cliente avisa con ElytraThrustPacket). */
    public static final Set<UUID> THRUSTING = new HashSet<>();

    public ThrusterElytraItem(Properties props) {
        super(props.durability(DURABILITY));
    }

    // ------------------------------------------------------------------ combustible

    public static int fuel(ItemStack s) {
        return s.getTag() == null ? 0 : s.getTag().getInt(FUEL);
    }

    public static void setFuel(ItemStack s, int fuel) {
        s.getOrCreateTag().putInt(FUEL, Math.max(0, Math.min(MAX_FUEL, fuel)));
    }

    public static boolean canThrust(ItemStack s) {
        return s.getItem() instanceof ThrusterElytraItem && fuel(s) > 0 && isFlyEnabled(s);
    }

    /** Nueva velocidad con un tick de empuje hacia {@code look}. */
    public static Vec3 boost(Vec3 v, Vec3 look) {
        return v.add(look.scale(PUSH)).add(look.scale(TOP_SPEED).subtract(v).scale(PULL));
    }

    /** Los élitros que lleva puestos (o vacío). */
    public static ItemStack worn(LivingEntity e) {
        ItemStack s = e.getItemBySlot(EquipmentSlot.CHEST);
        return s.getItem() instanceof ThrusterElytraItem ? s : ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ élitros

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isFlyEnabled(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide) {
            int next = flightTicks + 1;
            if (next % 10 == 0) {
                if (next % 20 == 0) {
                    stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(EquipmentSlot.CHEST));
                }
                entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE);
            }
        }
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.NULLITE_INGOT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.thruster_elytra").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.thruster_elytra.fuel", fuel(stack) * 100 / MAX_FUEL)
                .withStyle(fuel(stack) > 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
    }

    // ------------------------------------------------------------------ servidor: gasto, efectos y repostaje

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || !THRUSTING.contains(p.getUUID())) {
            return;
        }
        ItemStack wings = worn(p);
        if (!p.isFallFlying() || !canThrust(wings)) {
            THRUSTING.remove(p.getUUID());
            return;
        }
        if (!p.getAbilities().instabuild) {
            setFuel(wings, fuel(wings) - 1);
        }
        if (fuel(wings) < MAX_FUEL * GadgetFuel.REFUEL_BELOW) {
            refuel(p, wings);
        }
        FlightKick.allow(p);
        p.fallDistance = 0.0F;
        ServerLevel server = p.serverLevel();
        Vec3 back = p.position().add(p.getLookAngle().scale(-0.6)).add(0, 0.6, 0);
        server.sendParticles(ModParticles.MUZZLE_FLASH.get(), back.x, back.y, back.z, 1, 0.1, 0.05, 0.1, 0.0);
        server.sendParticles(ModParticles.BULLET_SPARK.get(), back.x, back.y, back.z, 2, 0.12, 0.08, 0.12, 0.04);
        if (p.tickCount % 8 == 0) {
            server.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.JETPACK_THRUST.get(), SoundSource.PLAYERS, 0.7F,
                    1.1F + p.getRandom().nextFloat() * 0.2F);
        }
    }

    /** Gasta un objeto de gasolina del inventario (lo mismo que el jetpack). */
    public static boolean refuel(Player p, ItemStack wings) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            int amount = GadgetFuel.value(ModItems.JETPACK.get(), s);
            if (amount > 0) {
                setFuel(wings, amount == GadgetFuel.FULL ? MAX_FUEL : fuel(wings) + amount);
                if (!p.getAbilities().instabuild) {
                    s.shrink(1);
                }
                p.displayClientMessage(Component.translatable("message.tecnocinetico.gadget_refueled", wings.getHoverName(), s.getHoverName())
                        .withStyle(ChatFormatting.GOLD), true);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ receta

    /**
     * Élitros + Jetpack Mk III + Núcleo de Datos + 4 lingotes de nulita + 2 Acopladores Cinéticos Brutos (los 9 huecos)
     * = Élitros de Propulsión (conservan los encantamientos de los élitros; el combustible del jetpack pasa a ellos).
     */
    public static class Recipe extends CustomRecipe {
        public Recipe(ResourceLocation id, CraftingBookCategory cat) {
            super(id, cat);
        }

        /** Los ingredientes para mostrar la receta (JEI). */
        public static NonNullList<Ingredient> display() {
            ItemStack jet = new ItemStack(ModItems.JETPACK.get());
            JetpackItem.setLevel(jet, JetpackItem.MAX_LEVEL);
            Ingredient nul = Ingredient.of(ModItems.NULLITE_INGOT.get()), cpl = Ingredient.of(ModItems.CRUDE_KINETIC_COUPLER.get());
            return NonNullList.of(Ingredient.EMPTY, nul, cpl, nul, Ingredient.of(Items.ELYTRA),
                    net.minecraftforge.common.crafting.StrictNBTIngredient.of(jet), Ingredient.of(ModItems.DATA_CORE.get()), nul, cpl, nul);
        }

        @Nullable
        private static ItemStack[] parts(net.minecraft.world.inventory.CraftingContainer inv) {
            ItemStack elytra = null, jet = null;
            int core = 0, nul = 0, cpl = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty()) {
                    continue;
                }
                if (s.is(Items.ELYTRA) && elytra == null) {
                    elytra = s;
                } else if (s.is(ModItems.JETPACK.get()) && JetpackItem.level(s) == JetpackItem.MAX_LEVEL && jet == null) {
                    jet = s;
                } else if (s.is(ModItems.DATA_CORE.get())) {
                    core++;
                } else if (s.is(ModItems.NULLITE_INGOT.get())) {
                    nul++;
                } else if (s.is(ModItems.CRUDE_KINETIC_COUPLER.get())) {
                    cpl++;
                } else {
                    return null;
                }
            }
            return elytra != null && jet != null && core == 1 && nul == 4 && cpl == 2 ? new ItemStack[]{elytra, jet} : null;
        }

        @Override
        public boolean matches(net.minecraft.world.inventory.CraftingContainer inv, Level level) {
            return parts(inv) != null;
        }

        @Override
        public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer inv, net.minecraft.core.RegistryAccess access) {
            ItemStack[] p = parts(inv);
            if (p == null) {
                return ItemStack.EMPTY;
            }
            ItemStack out = new ItemStack(ModItems.THRUSTER_ELYTRA.get());
            EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(p[0]), out);
            if (p[0].hasCustomHoverName()) {
                out.setHoverName(p[0].getHoverName());
            }
            ItemStack jet = p[1];
            setFuel(out, (jet.getMaxDamage() - 1 - jet.getDamageValue()) * MAX_FUEL / Math.max(1, jet.getMaxDamage() - 1));
            return out;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return w * h >= 9;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return com.bryanamaya.tecnocinetico.item.ModRecipes.THRUSTER_ELYTRA.get();
        }
    }
}
