"""Exporta las bocas de los planos a Java común, sin tocar las geometrías aprobadas."""
from pathlib import Path
import json
import sys
from weapon_blueprints import MUZZLES,muzzle_point

ROOT=Path(__file__).resolve().parent.parent


def main():
    cases=[]
    for name,(x,y,z) in MUZZLES.items():
        if name in {"pipe_shotgun","pump_shotgun"}:
            cut=muzzle_point(name,2)[2]; choke=.6 if name=="pipe_shotgun" else .7
            zz=f'((m & 2) != 0 ? {cut} : {z}) - ((m & 1) != 0 ? {choke} : 0)'
        elif name in {"bolt_rifle","semi_rifle"}:
            zz=f'((m & 4) != 0 ? {muzzle_point(name,4)[2]} : {z})'
        else: zz=str(z)
        cases.append(f'            case "{name}" -> new Vec3({x}, {y}, {zz});')
    source='''package com.bryanamaya.tecnocinetico.item.weapon;

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
'''+"\n".join(cases)+'''
            default -> new Vec3(8, 13, -12);
        };
    }
}
'''
    path=ROOT/"src/main/java/com/bryanamaya/tecnocinetico/item/weapon/WeaponMuzzles.java"
    if "--profiles-only" not in sys.argv:
        path.write_text(source,encoding="utf-8")
    profiles=[]
    for name in MUZZLES:
        for mods in range(8 if name in {"pipe_shotgun","pump_shotgun","bolt_rifle","semi_rifle"} else 1):
            profiles.append({"name":name+(f"_m{mods}" if mods else ""),"point":muzzle_point(name,mods)})
    out=ROOT/"build/weapon_muzzles/profiles.json"
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(profiles,indent=2),encoding="utf-8")
    print(f"Bocas exportadas: {len(MUZZLES)} armas y sus modulos.")


if __name__=="__main__": main()
