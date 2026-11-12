package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cargador de cualquier arma (pistola, subfusil, rifles, ametralladora, minigun). Guarda en su NBT cuántas balas
 * lleva ("Rounds") y, en los de Bala Cinética, cuántas son Munición Guiada ("Guided", se disparan primero).
 * <p>
 * Cada cargador admite un calibre ({@link #ammo()}: Bala Cinética, Bala de Rifle o Cinta de Rifle) y se llena con
 * sus paquetes ({@link AmmoPackItem}): en la mesa (cargador + paquetes, {@code MagazineLoadingRecipe}) o con el
 * cargador en la mano pulsando R (en los cinéticos, Mayús+R mete Munición Guiada). Las armas lo usan al recargar
 * (ver {@code LongGunItem} y {@code GunMagazines}).
 */
public class MagazineItem extends Item {

    public static final String TAG_ROUNDS = "Rounds";
    public static final String TAG_GUIDED = "Guided";

    /** 1 = Cargador, 2 = Cargador Ampliado (mismo código que guarda la pistola); 0 = otros (no van en la pistola). */
    private final int type;
    private final int capacity;
    private final java.util.function.Supplier<Item> ammo;

    public MagazineItem(int type, int capacity, java.util.function.Supplier<Item> ammo, Properties properties) {
        super(properties);
        this.type = type;
        this.capacity = capacity;
        this.ammo = ammo;
    }

    /** Munición (paquete) con la que se llena. */
    public Item ammo() {
        return ammo.get();
    }

    /** ¿Admite Munición Guiada? (solo los de Bala Cinética). */
    public boolean acceptsGuided() {
        return ammo.get() == ModItems.KINETIC_ROUND.get();
    }

    public int type() {
        return type;
    }

    public int capacity() {
        return capacity;
    }

    public static int capacityOfType(int type) {
        return type == 2 ? 18 : type == 1 ? 12 : 0;                          // 3 y 2 paquetes de Bala Cinética
    }

    @Nullable
    public static Item itemOfType(int type) {
        return type == 2 ? ModItems.EXTENDED_MAGAZINE.get() : type == 1 ? ModItems.MAGAZINE.get() : null;
    }

    // ------------------------------------------------------------------ NBT

    public static int getRounds(ItemStack mag) {
        CompoundTag t = mag.getTag();
        return t == null ? 0 : t.getInt(TAG_ROUNDS);
    }

    public static int getGuided(ItemStack mag) {
        CompoundTag t = mag.getTag();
        return t == null ? 0 : Math.min(t.getInt(TAG_GUIDED), getRounds(mag));
    }

    /** Fija el contenido; un cargador vacío queda sin NBT (así los vacíos se apilan). */
    public static void setContents(ItemStack mag, int rounds, int guided) {
        if (rounds <= 0) {
            mag.removeTagKey(TAG_ROUNDS);
            mag.removeTagKey(TAG_GUIDED);
            if (mag.getTag() != null && mag.getTag().isEmpty()) {
                mag.setTag(null);
            }
            return;
        }
        mag.getOrCreateTag().putInt(TAG_ROUNDS, rounds);
        if (guided > 0) {
            mag.getOrCreateTag().putInt(TAG_GUIDED, Math.min(guided, rounds));
        } else {
            mag.removeTagKey(TAG_GUIDED);
        }
    }

    /** Valor para el modelo: 0 vacío, 0.5 bala normal arriba, 1 bala guiada arriba. */
    public static float loadedProperty(ItemStack mag) {
        if (getRounds(mag) <= 0) {
            return 0.0F;
        }
        return getGuided(mag) > 0 ? 1.0F : 0.5F;
    }

    // ------------------------------------------------------------------ llenar con R

    /**
     * Llena el cargador de la mano con balas del inventario (servidor). Si hay varios cargadores apilados,
     * separa uno. Devuelve cuántas balas metió.
     */
    public static int fillFromInventory(Player player, ItemStack held, boolean guided) {
        if (!(held.getItem() instanceof MagazineItem magItem)) {
            return 0;
        }
        guided &= magItem.acceptsGuided();
        int rounds = getRounds(held);
        int free = magItem.capacity - rounds;
        Item ammo = guided ? ModItems.GUIDED_ROUND.get() : magItem.ammo();
        if (free <= 0) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.magazine_full")
                    .withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }
        int taken = AmmoPackItem.take(player, ammo, free);                   // por paquetes; el último puede quedar abierto
        if (taken == 0) {
            player.displayClientMessage(guided ? Component.translatable("message.tecnocinetico.no_guided_rounds").withStyle(ChatFormatting.RED)
                    : Component.translatable("message.tecnocinetico.no_ammo_for", ammo.getDescription()).withStyle(ChatFormatting.RED), true);
            return 0;
        }
        ItemStack target = held;
        if (held.getCount() > 1) {                                   // cargar solo uno de la pila
            target = held.split(1);
        }
        // las guiadas siempre se disparan primero, da igual el orden en que se metieron
        int newGuided = getGuided(target) + (guided ? taken : 0);
        setContents(target, rounds + taken, newGuided);
        if (target != held) {
            player.getInventory().placeItemBackInInventory(target);
        }
        com.bryanamaya.tecnocinetico.gear.TacticalGear.of(player).ifPresent(com.bryanamaya.tecnocinetico.gear.TacticalGear::setChanged);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_MAG_IN.get(),
                SoundSource.PLAYERS, 0.6F, 1.3F);
        return taken;
    }

    // ------------------------------------------------------------------ interfaz

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getRounds(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getRounds(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getGuided(stack) > 0 ? 0xD86CFF : 0x46E3F5;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.magazine_rounds", getRounds(stack), capacity)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.magazine_ammo", ammo().getDescription(),
                (capacity + AmmoPackItem.size(ammo()) - 1) / AmmoPackItem.size(ammo())).withStyle(ChatFormatting.GRAY));
        if (getGuided(stack) > 0) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.magazine_guided", getGuided(stack))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable(acceptsGuided() ? "tooltip.tecnocinetico.magazine_fill"
                : "tooltip.tecnocinetico.magazine_fill_plain").withStyle(ChatFormatting.DARK_GRAY));
        if (type == 2) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.extended_magazine").withStyle(ChatFormatting.GRAY));
        }
    }
}
