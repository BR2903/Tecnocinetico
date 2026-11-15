package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.bryanamaya.tecnocinetico.map.MapCatalog;
import com.bryanamaya.tecnocinetico.map.MapEvents;
import com.bryanamaya.tecnocinetico.map.MapPackets;
import com.bryanamaya.tecnocinetico.map.NavigationData;
import com.bryanamaya.tecnocinetico.map.SatelliteItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Reloj de Navegación: satélite, marcadores, búsqueda de estructuras y la ranura de muñeca. Van en su propio lote (estado global). */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class MapGameTests {

    private static FakePlayer player(ServerLevel level, String name) {
        FakePlayer p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** Solo por encima de Y 320 en el Overworld; una vez en órbita vale para todos y no se lanza otro. */
    @GameTest(template = "empty", batch = "reloj_navegacion")
    public static void satelliteDeploysOnlyInOrbit(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        NavigationData data = NavigationData.get(level.getServer());
        data.reset();
        FakePlayer p = player(level, "astronauta");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SATELLITE.get()));
        p.moveTo(0.5, 100, 0.5);
        ModItems.SATELLITE.get().use(level, p, InteractionHand.MAIN_HAND);
        h.assertFalse(data.satellite(), "No debería desplegarse por debajo del cielo");
        h.assertTrue(p.getMainHandItem().is(ModItems.SATELLITE.get()), "Se gastó el satélite sin desplegarse");
        p.moveTo(0.5, SatelliteItem.ORBIT_Y + 5, 0.5);
        ModItems.SATELLITE.get().use(level, p, InteractionHand.MAIN_HAND);
        h.assertTrue(data.satellite() && "astronauta".equals(data.deployer()), "No quedó en órbita");
        h.assertTrue(p.getMainHandItem().isEmpty(), "El satélite lanzado debería gastarse");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SATELLITE.get()));
        ModItems.SATELLITE.get().use(level, p, InteractionHand.MAIN_HAND);
        h.assertFalse(p.getMainHandItem().isEmpty(), "Con uno en órbita no se lanza otro");
        // fuera del Overworld no se puede
        data.reset();
        FakePlayer nether = player(level.getServer().getLevel(Level.NETHER), "piloto");
        nether.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SATELLITE.get()));
        nether.moveTo(0.5, 400, 0.5);
        ModItems.SATELLITE.get().use(nether.level(), nether, InteractionHand.MAIN_HAND);
        h.assertFalse(data.satellite(), "Desde el Nether no debería ponerse en órbita");
        h.succeed();
    }

    /** Marcadores personales y compartidos; solo el dueño borra; sin reloj o sin satélite no se puede. */
    @GameTest(template = "empty", batch = "reloj_navegacion")
    public static void markersPersonalAndShared(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        NavigationData data = NavigationData.get(level.getServer());
        data.reset();
        FakePlayer a = player(level, "ana"), b = player(level, "beto");
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRIST_MAP.get()));
        MapEvents.action(a, MapPackets.Action.add("Base", 10, 64, 20, 0xFF0000, false));
        h.assertTrue(data.markers().isEmpty(), "Sin satélite no debería guardar marcadores");
        data.deploy("test");
        MapEvents.action(a, MapPackets.Action.add("Base", 10, 64, 20, 0xFF0000, false));
        MapEvents.action(a, MapPackets.Action.add("Mina", 50, 30, -40, 0x00FF00, true));
        MapEvents.action(b, MapPackets.Action.add("Sin reloj", 0, 0, 0, 0, true));
        h.assertTrue(data.markers().size() == 2, "Debería haber 2 marcadores (beto no lleva reloj): " + data.markers().size());
        h.assertTrue(data.visibleTo(a.getUUID()).size() == 2 && data.visibleTo(b.getUUID()).size() == 1
                && data.visibleTo(b.getUUID()).get(0).name().equals("Mina"), "Visibilidad de marcadores incorrecta");
        // la muñeca también cuenta
        TacticalGear.of(b).ifPresent(g -> g.items().setStackInSlot(TacticalGear.WRIST, new ItemStack(ModItems.WRIST_MAP.get())));
        h.assertTrue(MapEvents.hasWatch(b), "El reloj en la muñeca no cuenta");
        var mina = data.markers().stream().filter(m -> m.name().equals("Mina")).findFirst().orElseThrow();
        MapEvents.action(b, MapPackets.Action.remove(mina.id()));
        h.assertTrue(data.markers().contains(mina), "Otro jugador no debería poder borrarlo");
        MapEvents.action(a, MapPackets.Action.remove(mina.id()));
        h.assertFalse(data.markers().contains(mina), "Su dueño debería poder borrarlo");
        data.reset();
        h.succeed();
    }

    /** El satélite localiza el Puerto de Lanzamiento en el End y no repite estructuras ya apuntadas. */
    @GameTest(template = "empty", batch = "reloj_navegacion", timeoutTicks = 200)
    public static void satelliteLocatesStructures(GameTestHelper h) {
        ServerLevel end = h.getLevel().getServer().getLevel(Level.END);
        MapCatalog.Entry port = MapCatalog.byKey("launch_port");
        h.assertTrue(port != null && port.dimension() == Level.END, "Falta el Puerto en el catálogo");
        var pos = MapEvents.find(end, port, BlockPos.ZERO);
        h.assertTrue(pos.isPresent(), "El satélite no encuentra el Puerto de Lanzamiento");
        double d = Math.sqrt(pos.get().getX() * (double) pos.get().getX() + pos.get().getZ() * (double) pos.get().getZ());
        h.assertTrue(d > 900 && d < 3500, "El Puerto está a una distancia rara: " + (int) d);
        NavigationData data = new NavigationData();
        h.assertTrue(data.addFound(new NavigationData.Found("launch_port", "minecraft:the_end", 100, 100))
                && !data.addFound(new NavigationData.Found("launch_port", "minecraft:the_end", 120, 90)), "Apunta dos veces la misma estructura");
        for (MapCatalog.Entry e : MapCatalog.ALL) {
            for (var id : e.ids()) {
                h.assertTrue(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).containsKey(id),
                        "El catálogo nombra una estructura que no existe: " + id);
            }
        }
        h.succeed();
    }

    /** La ranura de muñeca: acepta el reloj, y un equipo guardado de antes (19 ranuras) se carga sin perder nada. */
    @GameTest(template = "empty")
    public static void wristSlotAndOldSaves(GameTestHelper h) {
        h.assertTrue(TacticalGear.accepts(TacticalGear.WRIST, new ItemStack(ModItems.WRIST_MAP.get()))
                && !TacticalGear.accepts(TacticalGear.BACK, new ItemStack(ModItems.WRIST_MAP.get())), "El reloj va solo en la muñeca");
        var old = new net.minecraftforge.items.ItemStackHandler(19);
        old.setStackInSlot(18, new ItemStack(ModItems.DASH_THRUSTER.get()));                  // cinturón en el formato de la v1
        CompoundTag tag = old.serializeNBT();
        tag.putByte("Enabled", (byte) 0b0111);                               // el cinturón apagado
        TacticalGear gear = new TacticalGear();
        gear.load(tag);
        h.assertTrue(gear.items().getSlots() == TacticalGear.SIZE && gear.get(TacticalGear.BELT).is(ModItems.DASH_THRUSTER.get())
                && !gear.enabled(TacticalGear.BELT) && gear.enabled(TacticalGear.WRIST), "Migración del equipo a 20 ranuras incorrecta");
        h.succeed();
    }
}
