package com.bryanamaya.tecnocinetico.item.weapon;

import com.bryanamaya.tecnocinetico.item.custom.WeaponModule;

/**
 * Francotirador cinético (tier 2): cerrojo, 5 balas de rifle, mucho daño a gran distancia y mira telescópica
 * integrada (visor al apuntar, zoom x6,5).
 */
public class SniperItem extends LongGunItem {

    public SniperItem(Stats stats, Properties properties) {
        super(WeaponModule.Family.HEAVY, Action.BOLT, stats, properties);
    }

    @Override
    protected boolean integratedScope() {
        return true;
    }
}
