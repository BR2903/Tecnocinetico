package com.bryanamaya.tecnocinetico.map;

import com.bryanamaya.tecnocinetico.gear.GadgetItem;
import com.bryanamaya.tecnocinetico.gear.TacticalGear;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reloj de Navegación: gadget de MUÑECA (Equipo Táctico). Con el satélite en órbita abre el mapa del terreno explorado
 * (tecla del mapa, o usándolo en la mano): estructuras detectadas, búsqueda de una estructura concreta y marcadores.
 * No gasta batería (los datos vienen del satélite).
 */
public class WristMapItem extends GadgetItem {

    public WristMapItem(Properties props) {
        super(TacticalGear.WRIST, 2, "tooltip.tecnocinetico.wrist_map", props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.bryanamaya.tecnocinetico.client.map.ClientMapState::open);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}
