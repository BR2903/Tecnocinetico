package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Generado por tools/weapon_muzzles_gen.py desde los centros de boca de weapon_blueprints.py. */
public final class WeaponMuzzles {
    private WeaponMuzzles() {}
    public static Vec3 modelPoint(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        int m = WeaponModule.localMask(stack);
        return switch (id == null ? "" : id.getPath()) {
            case "pipe_shotgun" -> new Vec3(8, 13.15, ((m & 2) != 0 ? -4.8 : -12.6) - ((m & 1) != 0 ? 0.6 : 0));
            case "bolt_rifle" -> new Vec3(8, 13, ((m & 4) != 0 ? -16 : -15.5));
            case "pump_shotgun" -> new Vec3(8, 13, ((m & 2) != 0 ? -4.6 : -12.8) - ((m & 1) != 0 ? 0.7 : 0));
            case "semi_rifle" -> new Vec3(8, 13, ((m & 4) != 0 ? -15.8 : -14.6));
            case "kinetic_smg" -> new Vec3(8, 13, -6.2);
            case "assault_rifle" -> new Vec3(8, 13, -15.7);
            case "rail_rifle" -> new Vec3(8, 13, -15.4);
            case "grenade_launcher" -> new Vec3(8, 13.1, -10);
            case "pulse_cannon" -> new Vec3(8, 13, -12.2);
            case "revolver" -> new Vec3(8, 12.64, -2.768);
            case "flamethrower" -> new Vec3(8, 13, -14);
            case "light_machine_gun" -> new Vec3(8, 13, -15.4);
            case "kinetic_sniper" -> new Vec3(8, 13, -16);
            case "rocket_launcher" -> new Vec3(8, 12.8, -13);
            case "arc_caster" -> new Vec3(8, 13.075, -12.65);
            case "minigun" -> new Vec3(8, 5.1, -12.5);
            default -> new Vec3(8, 13, -12);
        };
    }
}
