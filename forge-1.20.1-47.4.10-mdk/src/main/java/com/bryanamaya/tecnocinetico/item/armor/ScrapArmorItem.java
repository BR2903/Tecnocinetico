package com.bryanamaya.tecnocinetico.item.armor;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Armadura de Chatarra (tier 1): placas de chatarra remachadas sobre cuero, con una célula de cinetrita en el
 * pecho. Protección algo por debajo del hierro (13 frente a 15), se repara con Chatarra.
 * Bono de set (las 4 piezas): "Blindaje improvisado", -20 % de daño de proyectiles y explosiones.
 */
@Mod.EventBusSubscriber(modid = Tecnocinetico.MOD_ID)
public class ScrapArmorItem extends ArmorItem {

    public static final float SET_BONUS = 0.8F;

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            int base = switch (type) {                                  // como el hierro (x15), algo menos (x14)
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
            return base * 14;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case HELMET -> 2;
                case CHESTPLATE -> 5;
                case LEGGINGS -> 4;
                case BOOTS -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 10;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_CHAIN;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.SCRAP_METAL.get());
        }

        @Override
        public String getName() {
            return Tecnocinetico.MOD_ID + ":scrap";                     // → textures/models/armor/scrap_layer_1.png
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public ScrapArmorItem(Type type, Properties properties) {
        super(MATERIAL, type, properties);
    }

    /** ¿Lleva las 4 piezas? */
    public static boolean hasFullSet(LivingEntity e) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof ScrapArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        var src = event.getSource();
        if ((src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_EXPLOSION)) && hasFullSet(event.getEntity())) {
            event.setAmount(event.getAmount() * SET_BONUS);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico.scrap_set").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tecnocinetico.scrap_set_bonus").withStyle(ChatFormatting.GRAY));
    }
}
