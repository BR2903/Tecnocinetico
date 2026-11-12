package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Munición en paquetes: UN objeto contiene varias balas ({@link #rounds()}): Bala Cinética 6, Bala de Rifle 5,
 * Cartuchos 6, Cinta de Rifle 15. El modelo muestra el paquete entero.
 * <p>
 * Un paquete abierto (se gastaron algunas balas, p. ej. al recargar el revólver a medias o al llenar un cargador
 * casi lleno) guarda lo que le queda en NBT ({@value #TAG_LEFT}) y muestra una barra; los paquetes llenos no llevan
 * NBT y se apilan normal. Al sacar balas se gastan primero los paquetes abiertos.
 */
public class AmmoPackItem extends Item {

    public static final String TAG_LEFT = "RoundsLeft";
    private final int rounds;

    public AmmoPackItem(int rounds, Properties properties) {
        super(properties);
        this.rounds = rounds;
    }

    /** Balas de un paquete lleno. */
    public int rounds() {
        return rounds;
    }

    /** Balas por objeto: las de un paquete, o 1 si no es un paquete (granadas, cohetes, células...). */
    public static int size(Item item) {
        return item instanceof AmmoPackItem pack ? pack.rounds : 1;
    }

    /** Balas que quedan en el objeto de arriba de esta pila (un paquete abierto siempre va solo). */
    public static int left(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.contains(TAG_LEFT) ? t.getInt(TAG_LEFT) : size(s.getItem());
    }

    /** Balas que hay en toda la pila. */
    public static int roundsIn(ItemStack s) {
        return s.isEmpty() ? 0 : left(s) + (s.getCount() - 1) * size(s.getItem());
    }

    /** Balas de este tipo que lleva el jugador (inventario y ranuras de munición del Equipo Táctico). */
    public static int available(Player player, Item ammo) {
        int n = 0;
        for (ItemStack s : TacticalGear.ammoSources(player)) {
            if (s.is(ammo)) {
                n += roundsIn(s);
            }
        }
        return n;
    }

    /**
     * Saca hasta {@code want} balas (servidor). Primero de los paquetes abiertos; si hay que abrir uno de una pila,
     * se separa y lo que sobra vuelve al jugador como paquete abierto. En creativo no gasta nada.
     * Devuelve cuántas sacó.
     */
    public static int take(Player player, Item ammo, int want) {
        if (want <= 0) {
            return 0;
        }
        if (player.getAbilities().instabuild) {
            return want;
        }
        int size = size(ammo), got = 0;
        List<ItemStack> sources = TacticalGear.ammoSources(player);
        for (int pass = 0; pass < 2 && got < want; pass++) {               // 0: paquetes abiertos; 1: llenos
            for (ItemStack s : sources) {
                if (got >= want || !s.is(ammo) || (s.getTag() != null && s.getTag().contains(TAG_LEFT)) != (pass == 0)) {
                    continue;
                }
                while (got < want && !s.isEmpty()) {
                    int inTop = left(s), use = Math.min(inTop, want - got);
                    got += use;
                    if (use == inTop) {
                        s.shrink(1);                                          // paquete gastado entero
                    } else if (s.getCount() == 1) {
                        s.getOrCreateTag().putInt(TAG_LEFT, inTop - use);     // queda abierto
                    } else {
                        s.shrink(1);                                          // abre uno de la pila
                        ItemStack open = new ItemStack(ammo);
                        open.getOrCreateTag().putInt(TAG_LEFT, inTop - use);
                        TacticalGear.giveBackAmmo(player, open);
                    }
                }
            }
        }
        if (size > 0 && got > 0) {
            TacticalGear.of(player).ifPresent(TacticalGear::setChanged);
        }
        return got;
    }

    // ------------------------------------------------------------------ interfaz

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(TAG_LEFT);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * left(stack) / rounds);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0B040;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isBarVisible(stack)) {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.ammo_pack_open", left(stack), rounds).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.tecnocinetico.ammo_pack", rounds).withStyle(ChatFormatting.GRAY));
        }
    }
}
