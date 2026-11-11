package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.entity.custom.SurvivorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Equipamiento en relieve según oficio, sobre el esqueleto humano que sostiene sus objetos. */
public class SurvivorModel extends PlayerModel<SurvivorEntity> {
    public SurvivorModel(ModelPart root) { super(root, false); }
    @Override public void setupAnim(SurvivorEntity e, float swing, float amount, float age, float yaw, float pitch) {
        super.setupAnim(e, swing, amount, age, yaw, pitch);
        boolean scrapper = e.getRole() == SurvivorEntity.Role.SCRAPPER;
        boolean gunsmith = e.getRole() == SurvivorEntity.Role.GUNSMITH;
        body.getChild("scrapper_pack").visible = scrapper;
        head.getChild("scrapper_goggles").visible = scrapper;
        body.getChild("gunsmith_apron").visible = gunsmith;
        body.getChild("gunsmith_holster").visible = gunsmith;
        head.getChild("farmer_hat").visible = !scrapper && !gunsmith;
        body.getChild("farmer_basket").visible = !scrapper && !gunsmith;
    }
}
