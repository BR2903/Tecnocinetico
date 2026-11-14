package com.bryanamaya.tecnocinetico.entity.boss;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Multijugador: los tres jefes del Overworld sueltan su pieza del Caza del Núcleo (Motor Dimensional, Propulsor
 * Vectorial, Núcleo de Vuelo Adaptativo) <b>una por cada jugador que les hizo daño</b> (máx. 4), como los jefes del
 * Nether, para que cada amigo pueda fabricarse su caza. Los jugadores se apuntan en los datos del jefe (se guardan).
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public final class BossPartSharing {

    private static final String FIGHTERS = "TecnoFighters";
    public static final int MAX_SHARES = 4;

    private BossPartSharing() {}

    @Nullable
    private static Item part(Entity boss) {
        var type = boss.getType();
        if (type == ModEntities.SIEGE_SPIDER.get()) return ModItems.DIMENSIONAL_DRIVE.get();
        if (type == ModEntities.NETWORK_WATCHER.get()) return ModItems.VECTOR_THRUSTER.get();
        if (type == ModEntities.SWARM_MATRIARCH.get()) return ModItems.ADAPTIVE_FLIGHT_CORE.get();
        return null;
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (part(event.getEntity()) == null || !(event.getSource().getEntity() instanceof Player p)) {
            return;
        }
        ListTag list = event.getEntity().getPersistentData().getList(FIGHTERS, 8);
        String id = p.getStringUUID();
        if (list.stream().noneMatch(t -> t.getAsString().equals(id))) {
            list.add(StringTag.valueOf(id));
            event.getEntity().getPersistentData().put(FIGHTERS, list);
        }
    }

    /** Número de jugadores que le pegaron (1-{@link #MAX_SHARES}). */
    public static int fighters(Entity boss) {
        return Math.max(1, Math.min(MAX_SHARES, boss.getPersistentData().getList(FIGHTERS, 8).size()));
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        Item part = part(event.getEntity());
        if (part == null) {
            return;
        }
        int extra = fighters(event.getEntity()) - 1;                                       // el botín normal ya trae una
        for (int i = 0; i < extra; i++) {
            var e = event.getEntity();
            ItemEntity item = new ItemEntity(e.level(), e.getX(), e.getY() + 1.0, e.getZ(), new ItemStack(part));
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
    }
}
