package com.bryanamaya.tecnocinetico.item.armor;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.item.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Armaduras tier 2 (sus piezas clave solo salen de la Prensa Cinética Automática, con electricidad). Los bonos
 * de set (4 piezas del mismo tipo) están en {@link Tier2ArmorEvents}:
 * - INERCITA: placas pesadas. Protección de diamante, resistencia al empuje; cada pieza frena un 3 %.
 *   Bono: -35 % de daño de proyectiles (balas incluidas) y explosiones.
 * - TACTICAL: chaleco y casco con visor, ligera. Protección de hierro. Bono: +15 % de velocidad y -30 % de retroceso.
 * - EXO: exoesqueleto cinético. Protección casi de diamante. Bono: acumula carga cinética al recibir golpes
 *   (agacharse + saltar = salto cinético; con la carga llena, el siguiente golpe cuerpo a cuerpo suelta una onda
 *   que empuja a los enemigos) y la mitad de daño por caída.
 * Armaduras de combate (sin habilidades; resistencia y movilidad): Mk I (tier 1), Mk II (tier 2), Mk III (tier 3,
 * Ensamblador, materiales de las tres dimensiones); cada pieza da velocidad, vida y daño de ataque.
 * Tier 3 (Ensamblador; protección de netherita, una por dimensión):
 * - CORE (Overworld, Procesador del Núcleo): escudo de 4 corazones que se recarga tras 8 s sin recibir daño.
 * - PIROXITE (Nether): inmune al fuego y la lava; quien te golpea cuerpo a cuerpo arde.
 * - NULLITE (End): 15 % de esquivar un golpe "desfasándose" unos bloques; los enderman no se enfadan; las perlas
 *   de ender no hacen daño.
 */
public class Tier2ArmorItem extends ArmorItem {

    public enum Kind {
        INERCITA("inercita", new int[]{3, 8, 6, 3}, 30, 2.5F, 0.1F, () -> Ingredient.of(ModItems.INERCITA_PLATE.get()),
                SoundEvents.ARMOR_EQUIP_NETHERITE, -0.03, 0, 0),
        TACTICAL("tactical", new int[]{2, 6, 5, 2}, 20, 1.0F, 0.0F, () -> Ingredient.of(ModItems.BALLISTIC_FABRIC.get()),
                SoundEvents.ARMOR_EQUIP_LEATHER),
        EXO("exo", new int[]{3, 7, 6, 3}, 26, 2.0F, 0.05F, () -> Ingredient.of(ModItems.KINETIC_SERVO.get()),
                SoundEvents.ARMOR_EQUIP_IRON),
        // ---- tier 3 (Ensamblador de Armas): una por dimensión, protección de netherita
        CORE("core", new int[]{3, 8, 6, 3}, 37, 3.0F, 0.1F, () -> Ingredient.of(ModItems.INERCITA_PLATE.get()),
                SoundEvents.ARMOR_EQUIP_NETHERITE),
        PIROXITE("piroxite", new int[]{3, 8, 6, 3}, 37, 3.0F, 0.1F, () -> Ingredient.of(ModItems.PIROXITE_INGOT.get()),
                SoundEvents.ARMOR_EQUIP_NETHERITE),
        NULLITE("nullite", new int[]{3, 8, 6, 3}, 37, 3.0F, 0.05F, () -> Ingredient.of(ModItems.NULLITE_INGOT.get()),
                SoundEvents.ARMOR_EQUIP_DIAMOND),
        // ---- armaduras de combate: sin habilidades; protección, vida, velocidad y daño por pieza (tiers 1-3)
        COMBAT_MK1("combat_mk1", new int[]{2, 6, 5, 2}, 18, 1.0F, 0.0F, () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT),
                SoundEvents.ARMOR_EQUIP_IRON, 0.02, 1.0, 0.0),
        COMBAT_MK2("combat_mk2", new int[]{3, 7, 6, 3}, 28, 2.0F, 0.05F, () -> Ingredient.of(ModItems.INERCITA_PLATE.get()),
                SoundEvents.ARMOR_EQUIP_NETHERITE, 0.03, 2.0, 0.04),
        COMBAT_MK3("combat_mk3", new int[]{4, 9, 7, 4}, 40, 3.5F, 0.1F, () -> Ingredient.of(ModItems.INERCITA_PLATE.get()),
                SoundEvents.ARMOR_EQUIP_NETHERITE, 0.04, 3.0, 0.06),
        // ---- equipo de protección: traje antirradiación forrado de plomo (poca protección contra golpes, pesa un poco)
        HAZMAT("hazmat", new int[]{1, 3, 2, 1}, 15, 0.0F, 0.0F, () -> Ingredient.of(ModItems.LEAD_INGOT.get()),
                SoundEvents.ARMOR_EQUIP_LEATHER, -0.02, 0, 0);

        public final String id;
        final ArmorMaterial material;
        /** Por pieza: velocidad (fracción), vida máxima (puntos) y daño de ataque (fracción). */
        final double speed, health, attack;

        Kind(String id, int[] defense, int durability, float toughness, float knockback, Supplier<Ingredient> repair, SoundEvent sound) {
            this(id, defense, durability, toughness, knockback, repair, sound, 0, 0, 0);
        }

        Kind(String id, int[] defense, int durability, float toughness, float knockback, Supplier<Ingredient> repair, SoundEvent sound,
             double speed, double health, double attack) {
            this.id = id;
            this.speed = speed;
            this.health = health;
            this.attack = attack;
            this.material = new ArmorMaterial() {
                @Override
                public int getDurabilityForType(Type type) {
                    int base = switch (type) {
                        case HELMET -> 11;
                        case CHESTPLATE -> 16;
                        case LEGGINGS -> 15;
                        case BOOTS -> 13;
                    };
                    return base * durability;
                }

                @Override
                public int getDefenseForType(Type type) {
                    return switch (type) {                                  // defense = {casco, peto, grebas, botas}
                        case HELMET -> defense[0];
                        case CHESTPLATE -> defense[1];
                        case LEGGINGS -> defense[2];
                        case BOOTS -> defense[3];
                    };
                }

                @Override
                public int getEnchantmentValue() {
                    return 12;
                }

                @Override
                public SoundEvent getEquipSound() {
                    return sound;
                }

                @Override
                public Ingredient getRepairIngredient() {
                    return repair.get();
                }

                @Override
                public String getName() {
                    return Tecnocinetico.MOD_ID + ":" + id;                 // → textures/models/armor/<id>_layer_1.png
                }

                @Override
                public float getToughness() {
                    return toughness;
                }

                @Override
                public float getKnockbackResistance() {
                    return knockback;
                }
            };
        }
    }

    private static final Map<Type, UUID> SLOW_IDS = new EnumMap<>(Map.of(
            Type.HELMET, UUID.fromString("6f2b9a40-1c3e-4d6f-9a10-5e1a7c2b3d01"),
            Type.CHESTPLATE, UUID.fromString("6f2b9a40-1c3e-4d6f-9a10-5e1a7c2b3d02"),
            Type.LEGGINGS, UUID.fromString("6f2b9a40-1c3e-4d6f-9a10-5e1a7c2b3d03"),
            Type.BOOTS, UUID.fromString("6f2b9a40-1c3e-4d6f-9a10-5e1a7c2b3d04")));

    private final Kind kind;
    private final Multimap<Attribute, AttributeModifier> modifiers;

    private static UUID id(Type type, String what) {
        return UUID.nameUUIDFromBytes(("tecnocinetico:armor_" + what + "_" + type.getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public Tier2ArmorItem(Kind kind, Type type, Properties properties) {
        super(kind.material, type, properties);
        this.kind = kind;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(super.getDefaultAttributeModifiers(type.getSlot()));
        if (kind.speed != 0) {                                               // inercita: -3 %; combate: +2..4 % por pieza
            b.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SLOW_IDS.get(type), "Armadura (velocidad)", kind.speed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (kind.health != 0) {
            b.put(Attributes.MAX_HEALTH, new AttributeModifier(id(type, "health"), "Armadura (vida)", kind.health,
                    AttributeModifier.Operation.ADDITION));
        }
        if (kind.attack != 0) {
            b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(id(type, "attack"), "Armadura (daño)", kind.attack,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        this.modifiers = b.build();
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == getType().getSlot() ? modifiers : super.getDefaultAttributeModifiers(slot);
    }

    /** ¿Lleva las 4 piezas de este tipo? */
    public static boolean hasFullSet(LivingEntity e, Kind kind) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(e.getItemBySlot(slot).getItem() instanceof Tier2ArmorItem a) || a.kind != kind) {
                return false;
            }
        }
        return true;
    }

    /** Piezas del traje antirradiación puestas (0-4). Cada una quita un 20 % de radiación; las 4, toda. */
    public static int hazmatPieces(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (e.getItemBySlot(slot).getItem() instanceof Tier2ArmorItem a && a.kind == Kind.HAZMAT) {
                n++;
            }
        }
        return n;
    }

    /** Multiplicador de retroceso de las armas (armadura táctica completa: -30 %). Lo usa el cliente. */
    public static float recoilMultiplier(LivingEntity e) {
        return hasFullSet(e, Kind.TACTICAL) ? 0.7F : 1.0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tecnocinetico." + kind.id + "_set").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tecnocinetico." + kind.id + "_set_bonus").withStyle(ChatFormatting.GRAY));
    }

    /** Modelo 3D propio (client/Armor3DModels, generado por tools/armor3d_gen.py). */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity, ItemStack stack,
                                                                                    net.minecraft.world.entity.EquipmentSlot slot,
                                                                                    net.minecraft.client.model.HumanoidModel<?> original) {
                var m = com.bryanamaya.tecnocinetico.client.Armor3DModels.get(kind.id, slot);
                return m != null ? m : original;
            }
        });
    }

    @Override
    public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.EquipmentSlot slot, String type) {
        return "tecnocinetico:textures/models/armor/3d/" + kind.id + ".png";
    }
}
