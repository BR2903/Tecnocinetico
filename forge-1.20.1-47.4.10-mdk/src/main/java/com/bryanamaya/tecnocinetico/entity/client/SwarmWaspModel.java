package com.bryanamaya.tecnocinetico.entity.client;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Avispa biomecánica (también la usa el Nodo de Colmena, a mayor escala). Mira hacia -Z.
 * Textura 64x32 generada por tools/swarm_wasp_texture.py (las alas son semitransparentes → render translúcido).
 */
public class SwarmWaspModel<T extends Mob> extends HierarchicalModel<T> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "swarm_wasp"), "main");

    private final ModelPart root, body, head, abdomen;
    private final ModelPart wingLF, wingLB, wingRF, wingRB;
    private final float flapSpeed;

    public SwarmWaspModel(ModelPart root) {
        this(root, 2.1F);
    }

    public SwarmWaspModel(ModelPart root, float flapSpeed) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.abdomen = body.getChild("abdomen");
        this.wingLF = body.getChild("wing_left_front");
        this.wingLB = body.getChild("wing_left_back");
        this.wingRF = body.getChild("wing_right_front");
        this.wingRB = body.getChild("wing_right_back");
        this.flapSpeed = flapSpeed;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.5F, 4, 4, 5)
                        .texOffs(10, 17).addBox(-1.0F, -0.7F, 2.0F, 2, 2, 2),                 // cintura
                PartPose.offset(0.0F, 17.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -1.5F, -3.0F, 3, 3, 3)
                        .texOffs(36, 13).addBox(-1.5F, 0.6F, -3.8F, 1, 1, 1)                   // mandíbulas
                        .texOffs(36, 13).addBox(0.5F, 0.6F, -3.8F, 1, 1, 1),
                PartPose.offset(0.0F, -0.5F, -2.5F));
        head.addOrReplaceChild("antenna_left", CubeListBuilder.create().texOffs(26, 13).addBox(-0.5F, -0.5F, -3.0F, 1, 1, 3),
                PartPose.offsetAndRotation(0.8F, -1.5F, -2.2F, 0.7F, -0.35F, 0.0F));
        head.addOrReplaceChild("antenna_right", CubeListBuilder.create().texOffs(26, 13).mirror().addBox(-0.5F, -0.5F, -3.0F, 1, 1, 3),
                PartPose.offsetAndRotation(-0.8F, -1.5F, -2.2F, 0.7F, 0.35F, 0.0F));

        body.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.5F, -2.5F, 0.0F, 5, 5, 7)
                        .texOffs(0, 17).addBox(-0.5F, -0.5F, 7.0F, 1, 1, 3),                  // aguijón
                PartPose.offsetAndRotation(0.0F, 0.6F, 3.4F, -0.35F, 0.0F, 0.0F));

        // alas (planos de grosor 0): izquierdas hacia +X, derechas espejadas (builder nuevo: mirror() solo afecta a lo siguiente)
        body.addOrReplaceChild("wing_left_front", CubeListBuilder.create().texOffs(44, 0).addBox(0.0F, 0.0F, -1.5F, 7, 0, 3),
                PartPose.offsetAndRotation(1.2F, -2.0F, -1.0F, 0.0F, -0.25F, 0.0F));
        body.addOrReplaceChild("wing_left_back", CubeListBuilder.create().texOffs(44, 4).addBox(0.0F, 0.0F, -1.5F, 6, 0, 3),
                PartPose.offsetAndRotation(1.2F, -2.0F, 0.9F, 0.0F, 0.3F, 0.0F));
        body.addOrReplaceChild("wing_right_front", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-7.0F, 0.0F, -1.5F, 7, 0, 3),
                PartPose.offsetAndRotation(-1.2F, -2.0F, -1.0F, 0.0F, 0.25F, 0.0F));
        body.addOrReplaceChild("wing_right_back", CubeListBuilder.create().texOffs(44, 4).mirror().addBox(-6.0F, 0.0F, -1.5F, 6, 0, 3),
                PartPose.offsetAndRotation(-1.2F, -2.0F, 0.9F, 0.0F, -0.3F, 0.0F));

        // patas: 3 por lado, colgando hacia atrás
        for (int i = 0; i < 3; i++) {
            float z = -1.6F + i * 1.5F;
            body.addOrReplaceChild("leg_left_" + i, CubeListBuilder.create().texOffs(20, 13).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1),
                    PartPose.offsetAndRotation(1.4F, 1.6F, z, 0.35F + i * 0.15F, 0.0F, -0.55F));
            body.addOrReplaceChild("leg_right_" + i, CubeListBuilder.create().texOffs(20, 13).mirror().addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1),
                    PartPose.offsetAndRotation(-1.4F, 1.6F, z, 0.35F + i * 0.15F, 0.0F, 0.55F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        // aleteo rápido (se ve como un borrón) con las alas de atrás algo desfasadas
        float flap = Mth.sin(age * flapSpeed) * 0.65F;
        float flapB = Mth.sin(age * flapSpeed + 0.9F) * 0.6F;
        wingLF.zRot = -0.3F + flap;
        wingRF.zRot = 0.3F - flap;
        wingLB.zRot = -0.25F + flapB;
        wingRB.zRot = 0.25F - flapB;
        // flotar y "respirar" con el abdomen
        body.y += Mth.sin(age * 0.15F) * 0.6F;
        abdomen.xRot += Mth.sin(age * 0.2F) * 0.08F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
    }
}
