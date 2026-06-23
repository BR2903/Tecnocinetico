package com.bryanamaya.tecnocinetico;
import com.bryanamaya.tecnocinetico.block.entity.ModBlockEntities;
import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.item.ModCreativeModeTabs;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Tecnocinetico.MOD_ID)
public class Tecnocinetico {

    public static final String MOD_ID = "tecnocinetico";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Tecnocinetico() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // --- EL ÚNICO REGISTRO CIVIL DE TECNOCINÉTICO ---
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("INICIALIZANDO EL NÚCLEO TECNOCINÉTICO...");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Servidor de la IA arrancando...");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Cargando interfaces visuales y texturas...");
        }
    }
}