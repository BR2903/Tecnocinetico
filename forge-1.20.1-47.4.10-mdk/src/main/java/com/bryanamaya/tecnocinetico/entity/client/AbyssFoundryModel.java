package com.bryanamaya.tecnocinetico.entity.client;
import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.nether.AbyssFoundryEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.WeakHashMap;
/** Arte generado por tools/abyss_foundry_gen.py; sin lógica de combate. Renderer a escala 2. */
public class AbyssFoundryModel extends EntityModel<AbyssFoundryEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Tecnocinetico.MOD_ID, "abyss_foundry"), "main");
    public final ModelPart root;
    private final ModelPart body, door_left, door_right, vent_right, lid_right, vent_left, lid_left, head, right_leg, right_knee, right_foot, left_leg, left_knee, left_foot, right_arm, right_elbow, left_arm, left_elbow, hammer, crucible, slag_stream;
    private final WeakHashMap<AbyssFoundryEntity, Visual> visuals = new WeakHashMap<>();
    private static class Visual { int attack; float progress, stopped = -1000; }
    public AbyssFoundryModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.door_left = body.getChild("door_left");
        this.door_right = body.getChild("door_right");
        this.vent_right = body.getChild("vent_right");
        this.lid_right = body.getChild("lid_right");
        this.vent_left = body.getChild("vent_left");
        this.lid_left = body.getChild("lid_left");
        this.head = body.getChild("head");
        this.right_leg = root.getChild("right_leg");
        this.right_knee = right_leg.getChild("right_knee");
        this.right_foot = right_knee.getChild("right_foot");
        this.left_leg = root.getChild("left_leg");
        this.left_knee = left_leg.getChild("left_knee");
        this.left_foot = left_knee.getChild("left_foot");
        this.right_arm = body.getChild("right_arm");
        this.right_elbow = right_arm.getChild("right_elbow");
        this.left_arm = body.getChild("left_arm");
        this.left_elbow = left_arm.getChild("left_elbow");
        this.hammer = right_elbow.getChild("hammer");
        this.crucible = left_elbow.getChild("crucible");
        this.slag_stream = crucible.getChild("slag_stream");
    }
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(35, 32).addBox(-8.000000F, -6.000000F, -5.000000F, 2.500000F, 11.000000F, 11.000000F, new CubeDeformation(-0.000200F)).texOffs(122, 17).addBox(-7.750000F, -4.800000F, -5.250000F, 2.000000F, 8.000000F, 0.500000F, new CubeDeformation(-0.000400F)).texOffs(35, 32).addBox(5.500000F, -6.000000F, -5.000000F, 2.500000F, 11.000000F, 11.000000F, new CubeDeformation(-0.000600F)).texOffs(122, 17).addBox(5.750000F, -4.800000F, -5.250000F, 2.000000F, 8.000000F, 0.500000F, new CubeDeformation(-0.000800F)).texOffs(1, 107).addBox(-5.500000F, -6.000000F, 4.000000F, 11.000000F, 12.000000F, 2.000000F, new CubeDeformation(-0.001000F)).texOffs(1, 1).addBox(-8.000000F, -8.000000F, -6.000000F, 16.000000F, 2.000000F, 12.000000F, new CubeDeformation(-0.001200F)).texOffs(59, 1).addBox(-6.000000F, 4.000000F, -5.000000F, 12.000000F, 3.000000F, 10.000000F, new CubeDeformation(-0.001400F)).texOffs(1, 72).addBox(-7.000000F, 4.000000F, -7.000000F, 14.000000F, 2.000000F, 2.000000F, new CubeDeformation(-0.001600F)).texOffs(1, 32).addBox(-5.000000F, -5.000000F, -4.000000F, 10.000000F, 9.000000F, 6.000000F, new CubeDeformation(-0.001800F)).texOffs(35, 84).addBox(-4.000000F, -4.000000F, -4.300000F, 8.000000F, 7.000000F, 0.500000F, new CubeDeformation(-0.002000F)).texOffs(55, 121).addBox(-2.000000F, -3.000000F, -4.650000F, 4.000000F, 5.000000F, 0.400000F, new CubeDeformation(-0.002200F)).texOffs(64, 97).addBox(-4.000000F, 1.000000F, -4.650000F, 1.000000F, 2.000000F, 0.400000F, new CubeDeformation(-0.002400F)).texOffs(64, 97).addBox(3.000000F, 1.000000F, -4.650000F, 1.000000F, 2.000000F, 0.400000F, new CubeDeformation(-0.002600F)).texOffs(76, 65).addBox(-7.700000F, -5.000000F, -5.450000F, 1.400000F, 8.000000F, 0.600000F, new CubeDeformation(-0.002800F)).texOffs(125, 53).addBox(-7.300000F, -4.000000F, -5.650000F, 0.600000F, 0.600000F, 0.250000F, new CubeDeformation(-0.003000F)).texOffs(125, 53).addBox(-7.300000F, 0.000000F, -5.650000F, 0.600000F, 0.600000F, 0.250000F, new CubeDeformation(-0.003200F)).texOffs(125, 53).addBox(-7.300000F, 3.000000F, -5.650000F, 0.600000F, 0.600000F, 0.250000F, new CubeDeformation(-0.003400F)).texOffs(71, 56).addBox(-6.500000F, 6.000000F, -4.000000F, 3.000000F, 3.000000F, 1.000000F, new CubeDeformation(-0.003600F)).texOffs(86, 28).addBox(-6.200000F, 6.300000F, -4.200000F, 2.400000F, 1.000000F, 0.250000F, new CubeDeformation(-0.003800F)).texOffs(55, 97).addBox(-6.000000F, -15.000000F, 2.000000F, 0.500000F, 8.000000F, 3.000000F, new CubeDeformation(-0.004000F)).texOffs(55, 97).addBox(-3.500000F, -15.000000F, 2.000000F, 0.500000F, 8.000000F, 3.000000F, new CubeDeformation(-0.004200F)).texOffs(69, 65).addBox(-5.500000F, -15.000000F, 2.000000F, 2.000000F, 8.000000F, 0.500000F, new CubeDeformation(-0.004400F)).texOffs(69, 65).addBox(-5.500000F, -15.000000F, 4.500000F, 2.000000F, 8.000000F, 0.500000F, new CubeDeformation(-0.004600F)).texOffs(35, 123).addBox(-6.200000F, -14.500000F, 1.800000F, 3.400000F, 0.600000F, 3.400000F, new CubeDeformation(-0.004800F)).texOffs(35, 123).addBox(-6.200000F, -8.000000F, 1.800000F, 3.400000F, 0.600000F, 3.400000F, new CubeDeformation(-0.005000F)).texOffs(82, 65).addBox(-6.500000F, -5.000000F, 6.000000F, 1.000000F, 8.000000F, 1.000000F, new CubeDeformation(-0.005200F)).texOffs(114, 56).addBox(-6.800000F, -4.000000F, 5.800000F, 1.600000F, 0.600000F, 1.400000F, new CubeDeformation(-0.005400F)).texOffs(114, 56).addBox(-6.800000F, 1.000000F, 5.800000F, 1.600000F, 0.600000F, 1.400000F, new CubeDeformation(-0.005600F)).texOffs(76, 65).addBox(6.300000F, -5.000000F, -5.450000F, 1.400000F, 8.000000F, 0.600000F, new CubeDeformation(-0.005800F)).texOffs(125, 53).addBox(6.700000F, -4.000000F, -5.650000F, 0.600000F, 0.600000F, 0.250000F, new CubeDeformation(-0.006000F)).texOffs(125, 53).addBox(6.700000F, 0.000000F, -5.650000F, 0.600000F, 0.600000F, 0.250000F, new CubeDeformation(-0.006200F)).texOffs(125, 53).addBox(6.700000F, 3.000000F, -5.650000F, 0.600000F, 0.600000F, 0.250000F, new CubeDeformation(-0.006400F)).texOffs(71, 56).addBox(3.500000F, 6.000000F, -4.000000F, 3.000000F, 3.000000F, 1.000000F, new CubeDeformation(-0.006600F)).texOffs(86, 28).addBox(3.800000F, 6.300000F, -4.200000F, 2.400000F, 1.000000F, 0.250000F, new CubeDeformation(-0.006800F)).texOffs(55, 110).addBox(3.000000F, -13.000000F, 2.000000F, 0.500000F, 6.000000F, 3.000000F, new CubeDeformation(-0.007000F)).texOffs(55, 110).addBox(5.500000F, -13.000000F, 2.000000F, 0.500000F, 6.000000F, 3.000000F, new CubeDeformation(-0.007200F)).texOffs(89, 56).addBox(3.500000F, -13.000000F, 2.000000F, 2.000000F, 6.000000F, 0.500000F, new CubeDeformation(-0.007400F)).texOffs(89, 56).addBox(3.500000F, -13.000000F, 4.500000F, 2.000000F, 6.000000F, 0.500000F, new CubeDeformation(-0.007600F)).texOffs(35, 123).addBox(2.800000F, -12.500000F, 1.800000F, 3.400000F, 0.600000F, 3.400000F, new CubeDeformation(-0.007800F)).texOffs(35, 123).addBox(2.800000F, -8.000000F, 1.800000F, 3.400000F, 0.600000F, 3.400000F, new CubeDeformation(-0.008000F)).texOffs(82, 65).addBox(5.500000F, -5.000000F, 6.000000F, 1.000000F, 8.000000F, 1.000000F, new CubeDeformation(-0.008200F)).texOffs(114, 56).addBox(5.200000F, -4.000000F, 5.800000F, 1.600000F, 0.600000F, 1.400000F, new CubeDeformation(-0.008400F)).texOffs(114, 56).addBox(5.200000F, 1.000000F, 5.800000F, 1.600000F, 0.600000F, 1.400000F, new CubeDeformation(-0.008600F)).texOffs(35, 94).addBox(-3.000000F, -4.000000F, 6.000000F, 6.000000F, 8.000000F, 2.000000F, new CubeDeformation(-0.008800F)).texOffs(112, 53).addBox(-2.500000F, -3.000000F, 8.050000F, 5.000000F, 0.600000F, 0.250000F, new CubeDeformation(-0.009000F)).texOffs(112, 53).addBox(-2.500000F, -1.000000F, 8.050000F, 5.000000F, 0.600000F, 0.250000F, new CubeDeformation(-0.009200F)).texOffs(112, 53).addBox(-2.500000F, 1.000000F, 8.050000F, 5.000000F, 0.600000F, 0.250000F, new CubeDeformation(-0.009400F)).texOffs(112, 53).addBox(-2.500000F, 3.000000F, 8.050000F, 5.000000F, 0.600000F, 0.250000F, new CubeDeformation(-0.009600F)), PartPose.offsetAndRotation(0.000000F, 2.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var door_left = body.addOrReplaceChild("door_left", CubeListBuilder.create().texOffs(29, 107).addBox(-5.500000F, 0.000000F, 0.000000F, 0.800000F, 9.000000F, 1.000000F, new CubeDeformation(-0.009800F)).texOffs(29, 107).addBox(-0.800000F, 0.000000F, 0.000000F, 0.800000F, 9.000000F, 1.000000F, new CubeDeformation(-0.010000F)).texOffs(52, 28).addBox(-4.700000F, 0.000000F, 0.000000F, 3.900000F, 0.800000F, 1.000000F, new CubeDeformation(-0.010200F)).texOffs(52, 28).addBox(-4.700000F, 8.200000F, 0.000000F, 3.900000F, 0.800000F, 1.000000F, new CubeDeformation(-0.010400F)).texOffs(110, 26).addBox(-4.700000F, 2.000000F, 0.100000F, 3.900000F, 0.600000F, 0.800000F, new CubeDeformation(-0.010600F)).texOffs(110, 26).addBox(-4.700000F, 4.000000F, 0.100000F, 3.900000F, 0.600000F, 0.800000F, new CubeDeformation(-0.010800F)).texOffs(110, 26).addBox(-4.700000F, 6.000000F, 0.100000F, 3.900000F, 0.600000F, 0.800000F, new CubeDeformation(-0.011000F)).texOffs(114, 60).addBox(-3.400000F, 3.400000F, -0.400000F, 1.200000F, 2.000000F, 0.400000F, new CubeDeformation(-0.011200F)), PartPose.offsetAndRotation(5.500000F, -5.000000F, -6.300000F, 0.000000F, 0.000000F, 0.000000F));
        var door_right = body.addOrReplaceChild("door_right", CubeListBuilder.create().texOffs(29, 107).addBox(0.000000F, 0.000000F, 0.000000F, 0.800000F, 9.000000F, 1.000000F, new CubeDeformation(-0.011400F)).texOffs(29, 107).addBox(4.700000F, 0.000000F, 0.000000F, 0.800000F, 9.000000F, 1.000000F, new CubeDeformation(-0.011600F)).texOffs(52, 28).addBox(0.800000F, 0.000000F, 0.000000F, 3.900000F, 0.800000F, 1.000000F, new CubeDeformation(-0.011800F)).texOffs(52, 28).addBox(0.800000F, 8.200000F, 0.000000F, 3.900000F, 0.800000F, 1.000000F, new CubeDeformation(-0.012000F)).texOffs(110, 26).addBox(0.800000F, 2.000000F, 0.100000F, 3.900000F, 0.600000F, 0.800000F, new CubeDeformation(-0.012200F)).texOffs(110, 26).addBox(0.800000F, 4.000000F, 0.100000F, 3.900000F, 0.600000F, 0.800000F, new CubeDeformation(-0.012400F)).texOffs(110, 26).addBox(0.800000F, 6.000000F, 0.100000F, 3.900000F, 0.600000F, 0.800000F, new CubeDeformation(-0.012600F)).texOffs(114, 60).addBox(2.100000F, 3.400000F, -0.400000F, 1.200000F, 2.000000F, 0.400000F, new CubeDeformation(-0.012800F)), PartPose.offsetAndRotation(-5.500000F, -5.000000F, -6.300000F, 0.000000F, 0.000000F, 0.000000F));
        var vent_right = body.addOrReplaceChild("vent_right", CubeListBuilder.create().texOffs(55, 84).addBox(-0.350000F, 0.000000F, 0.000000F, 0.700000F, 7.000000F, 4.000000F, new CubeDeformation(-0.013000F)).texOffs(1, 123).addBox(-0.550000F, 1.000000F, 0.500000F, 1.100000F, 0.800000F, 3.000000F, new CubeDeformation(-0.013200F)).texOffs(1, 123).addBox(-0.550000F, 3.000000F, 0.500000F, 1.100000F, 0.800000F, 3.000000F, new CubeDeformation(-0.013400F)).texOffs(1, 123).addBox(-0.550000F, 5.000000F, 0.500000F, 1.100000F, 0.800000F, 3.000000F, new CubeDeformation(-0.013600F)), PartPose.offsetAndRotation(-8.000000F, -4.000000F, 1.000000F, 0.000000F, 0.000000F, 0.000000F));
        var lid_right = body.addOrReplaceChild("lid_right", CubeListBuilder.create().texOffs(112, 46).addBox(-1.800000F, -0.500000F, -3.600000F, 3.600000F, 0.500000F, 3.600000F, new CubeDeformation(-0.013800F)), PartPose.offsetAndRotation(-4.500000F, -15.000000F, 5.300000F, 0.000000F, 0.000000F, 0.000000F));
        var vent_left = body.addOrReplaceChild("vent_left", CubeListBuilder.create().texOffs(55, 84).addBox(-0.350000F, 0.000000F, 0.000000F, 0.700000F, 7.000000F, 4.000000F, new CubeDeformation(-0.014000F)).texOffs(1, 123).addBox(-0.550000F, 1.000000F, 0.500000F, 1.100000F, 0.800000F, 3.000000F, new CubeDeformation(-0.014200F)).texOffs(1, 123).addBox(-0.550000F, 3.000000F, 0.500000F, 1.100000F, 0.800000F, 3.000000F, new CubeDeformation(-0.014400F)).texOffs(1, 123).addBox(-0.550000F, 5.000000F, 0.500000F, 1.100000F, 0.800000F, 3.000000F, new CubeDeformation(-0.014600F)), PartPose.offsetAndRotation(8.000000F, -4.000000F, 1.000000F, 0.000000F, 0.000000F, 0.000000F));
        var lid_left = body.addOrReplaceChild("lid_left", CubeListBuilder.create().texOffs(112, 46).addBox(-1.800000F, -0.500000F, -3.600000F, 3.600000F, 0.500000F, 3.600000F, new CubeDeformation(-0.014800F)), PartPose.offsetAndRotation(4.500000F, -13.000000F, 5.300000F, 0.000000F, 0.000000F, 0.000000F));
        var head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(96, 17).addBox(-3.500000F, -2.000000F, -3.000000F, 7.000000F, 2.000000F, 5.000000F, new CubeDeformation(-0.015000F)).texOffs(1, 98).addBox(-4.000000F, -2.500000F, -3.500000F, 8.000000F, 0.700000F, 6.000000F, new CubeDeformation(-0.015200F)).texOffs(39, 28).addBox(-2.500000F, -1.300000F, -3.250000F, 5.000000F, 0.500000F, 0.300000F, new CubeDeformation(-0.015400F)).texOffs(64, 28).addBox(-1.800000F, -1.800000F, -3.400000F, 0.900000F, 1.500000F, 0.300000F, new CubeDeformation(-0.015600F)).texOffs(64, 28).addBox(0.900000F, -1.800000F, -3.400000F, 0.900000F, 1.500000F, 0.300000F, new CubeDeformation(-0.015800F)), PartPose.offsetAndRotation(0.000000F, -8.500000F, -2.000000F, 0.000000F, 0.000000F, 0.000000F));
        var right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(35, 72).addBox(-2.200000F, 0.000000F, -2.200000F, 4.400000F, 5.500000F, 4.400000F, new CubeDeformation(-0.016000F)).texOffs(55, 77).addBox(-2.500000F, 0.400000F, -2.700000F, 5.000000F, 4.000000F, 0.700000F, new CubeDeformation(-0.016200F)).texOffs(102, 56).addBox(-2.500000F, 0.400000F, -0.400000F, 0.800000F, 5.000000F, 0.800000F, new CubeDeformation(-0.016400F)), PartPose.offsetAndRotation(-4.500000F, 12.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var right_knee = right_leg.addOrReplaceChild("right_knee", CubeListBuilder.create().texOffs(64, 41).addBox(-2.400000F, -0.800000F, -2.500000F, 4.800000F, 5.500000F, 5.000000F, new CubeDeformation(-0.016600F)).texOffs(12, 123).addBox(-1.800000F, -0.400000F, -3.000000F, 3.600000F, 2.000000F, 0.700000F, new CubeDeformation(-0.016800F)).texOffs(81, 56).addBox(-0.700000F, 0.000000F, 2.600000F, 1.400000F, 5.000000F, 1.400000F, new CubeDeformation(-0.017000F)).texOffs(124, 9).addBox(-0.350000F, 1.000000F, 4.000000F, 0.700000F, 4.000000F, 0.500000F, new CubeDeformation(-0.017200F)), PartPose.offsetAndRotation(0.000000F, 5.500000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var right_foot = right_knee.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(39, 17).addBox(-2.800000F, 0.000000F, -4.500000F, 5.600000F, 1.000000F, 8.000000F, new CubeDeformation(-0.017400F)).texOffs(69, 17).addBox(-2.500000F, -1.600000F, -4.200000F, 5.000000F, 1.600000F, 7.400000F, new CubeDeformation(-0.017600F)).texOffs(88, 65).addBox(-2.500000F, -1.300000F, -4.700000F, 1.000000F, 1.300000F, 1.000000F, new CubeDeformation(-0.017800F)).texOffs(88, 65).addBox(-0.500000F, -1.300000F, -4.700000F, 1.000000F, 1.300000F, 1.000000F, new CubeDeformation(-0.018000F)).texOffs(88, 65).addBox(1.500000F, -1.300000F, -4.700000F, 1.000000F, 1.300000F, 1.000000F, new CubeDeformation(-0.018200F)), PartPose.offsetAndRotation(0.000000F, 5.500000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(35, 72).addBox(-2.200000F, 0.000000F, -2.200000F, 4.400000F, 5.500000F, 4.400000F, new CubeDeformation(-0.018400F)).texOffs(55, 77).addBox(-2.500000F, 0.400000F, -2.700000F, 5.000000F, 4.000000F, 0.700000F, new CubeDeformation(-0.018600F)).texOffs(102, 56).addBox(1.700000F, 0.400000F, -0.400000F, 0.800000F, 5.000000F, 0.800000F, new CubeDeformation(-0.018800F)), PartPose.offsetAndRotation(4.500000F, 12.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var left_knee = left_leg.addOrReplaceChild("left_knee", CubeListBuilder.create().texOffs(64, 41).addBox(-2.400000F, -0.800000F, -2.500000F, 4.800000F, 5.500000F, 5.000000F, new CubeDeformation(-0.019000F)).texOffs(12, 123).addBox(-1.800000F, -0.400000F, -3.000000F, 3.600000F, 2.000000F, 0.700000F, new CubeDeformation(-0.019200F)).texOffs(81, 56).addBox(-0.700000F, 0.000000F, 2.600000F, 1.400000F, 5.000000F, 1.400000F, new CubeDeformation(-0.019400F)).texOffs(124, 9).addBox(-0.350000F, 1.000000F, 4.000000F, 0.700000F, 4.000000F, 0.500000F, new CubeDeformation(-0.019600F)), PartPose.offsetAndRotation(0.000000F, 5.500000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var left_foot = left_knee.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(39, 17).addBox(-2.800000F, 0.000000F, -4.500000F, 5.600000F, 1.000000F, 8.000000F, new CubeDeformation(-0.019800F)).texOffs(69, 17).addBox(-2.500000F, -1.600000F, -4.200000F, 5.000000F, 1.600000F, 7.400000F, new CubeDeformation(-0.020000F)).texOffs(88, 65).addBox(-2.500000F, -1.300000F, -4.700000F, 1.000000F, 1.300000F, 1.000000F, new CubeDeformation(-0.020200F)).texOffs(88, 65).addBox(-0.500000F, -1.300000F, -4.700000F, 1.000000F, 1.300000F, 1.000000F, new CubeDeformation(-0.020400F)).texOffs(88, 65).addBox(1.500000F, -1.300000F, -4.700000F, 1.000000F, 1.300000F, 1.000000F, new CubeDeformation(-0.020600F)), PartPose.offsetAndRotation(0.000000F, 5.500000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(112, 32).addBox(-1.800000F, 0.000000F, -1.800000F, 3.600000F, 8.000000F, 3.600000F, new CubeDeformation(-0.020800F)).texOffs(90, 32).addBox(-2.500000F, -1.200000F, -2.500000F, 5.000000F, 3.400000F, 5.000000F, new CubeDeformation(-0.021000F)).texOffs(105, 1).addBox(-2.600000F, -1.400000F, -2.650000F, 5.200000F, 0.700000F, 5.300000F, new CubeDeformation(-0.021200F)).texOffs(105, 9).addBox(-2.000000F, 2.500000F, -2.100000F, 4.000000F, 0.700000F, 4.200000F, new CubeDeformation(-0.021400F)).texOffs(105, 9).addBox(-2.000000F, 5.500000F, -2.100000F, 4.000000F, 0.700000F, 4.200000F, new CubeDeformation(-0.021600F)).texOffs(96, 56).addBox(-2.400000F, 1.000000F, -0.500000F, 0.800000F, 6.000000F, 0.800000F, new CubeDeformation(-0.021800F)), PartPose.offsetAndRotation(-9.000000F, -4.500000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var right_elbow = right_arm.addOrReplaceChild("right_elbow", CubeListBuilder.create().texOffs(55, 65).addBox(-1.500000F, 0.000000F, -1.500000F, 3.000000F, 7.000000F, 3.000000F, new CubeDeformation(-0.022000F)).texOffs(35, 106).addBox(-2.000000F, 0.000000F, -2.000000F, 4.000000F, 2.000000F, 4.000000F, new CubeDeformation(-0.022200F)).texOffs(108, 56).addBox(-0.550000F, 2.000000F, -2.000000F, 1.100000F, 5.000000F, 0.800000F, new CubeDeformation(-0.022400F)), PartPose.offsetAndRotation(0.000000F, 8.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(112, 32).addBox(-1.800000F, 0.000000F, -1.800000F, 3.600000F, 8.000000F, 3.600000F, new CubeDeformation(-0.022600F)).texOffs(90, 32).addBox(-2.500000F, -1.200000F, -2.500000F, 5.000000F, 3.400000F, 5.000000F, new CubeDeformation(-0.022800F)).texOffs(105, 1).addBox(-2.600000F, -1.400000F, -2.650000F, 5.200000F, 0.700000F, 5.300000F, new CubeDeformation(-0.023000F)).texOffs(105, 9).addBox(-2.000000F, 2.500000F, -2.100000F, 4.000000F, 0.700000F, 4.200000F, new CubeDeformation(-0.023200F)).texOffs(105, 9).addBox(-2.000000F, 5.500000F, -2.100000F, 4.000000F, 0.700000F, 4.200000F, new CubeDeformation(-0.023400F)).texOffs(96, 56).addBox(1.600000F, 1.000000F, -0.500000F, 0.800000F, 6.000000F, 0.800000F, new CubeDeformation(-0.023600F)), PartPose.offsetAndRotation(9.000000F, -4.500000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var left_elbow = left_arm.addOrReplaceChild("left_elbow", CubeListBuilder.create().texOffs(55, 65).addBox(-1.500000F, 0.000000F, -1.500000F, 3.000000F, 7.000000F, 3.000000F, new CubeDeformation(-0.023800F)).texOffs(35, 106).addBox(-2.000000F, 0.000000F, -2.000000F, 4.000000F, 2.000000F, 4.000000F, new CubeDeformation(-0.024000F)).texOffs(108, 56).addBox(-0.550000F, 2.000000F, -2.000000F, 1.100000F, 5.000000F, 0.800000F, new CubeDeformation(-0.024200F)), PartPose.offsetAndRotation(0.000000F, 8.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var hammer = right_elbow.addOrReplaceChild("hammer", CubeListBuilder.create().texOffs(1, 17).addBox(-4.500000F, -1.500000F, -4.500000F, 9.000000F, 4.000000F, 9.000000F, new CubeDeformation(-0.024400F)).texOffs(1, 88).addBox(-3.500000F, 2.500000F, -3.500000F, 7.000000F, 1.000000F, 7.000000F, new CubeDeformation(-0.024600F)).texOffs(1, 61).addBox(-4.000000F, 3.500000F, -4.000000F, 8.000000F, 1.000000F, 8.000000F, new CubeDeformation(-0.024800F)).texOffs(1, 78).addBox(-3.800000F, 3.100000F, -3.800000F, 7.600000F, 0.300000F, 7.600000F, new CubeDeformation(-0.025000F)).texOffs(64, 110).addBox(-3.000000F, -2.000000F, -4.700000F, 1.000000F, 4.500000F, 0.500000F, new CubeDeformation(-0.025200F)).texOffs(64, 110).addBox(2.000000F, -2.000000F, -4.700000F, 1.000000F, 4.500000F, 0.500000F, new CubeDeformation(-0.025400F)).texOffs(88, 70).addBox(-4.700000F, -0.800000F, -2.500000F, 0.400000F, 2.500000F, 1.000000F, new CubeDeformation(-0.025600F)).texOffs(88, 70).addBox(-4.700000F, -0.800000F, 1.500000F, 0.400000F, 2.500000F, 1.000000F, new CubeDeformation(-0.025800F)).texOffs(35, 114).addBox(-2.500000F, 4.500000F, -3.000000F, 1.000000F, 0.600000F, 6.000000F, new CubeDeformation(-0.026000F)).texOffs(35, 114).addBox(1.500000F, 4.500000F, -3.000000F, 1.000000F, 0.600000F, 6.000000F, new CubeDeformation(-0.026200F)), PartPose.offsetAndRotation(0.000000F, 8.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var crucible = left_elbow.addOrReplaceChild("crucible", CubeListBuilder.create().texOffs(1, 49).addBox(-4.000000F, 5.000000F, -4.000000F, 8.000000F, 1.300000F, 8.000000F, new CubeDeformation(-0.026400F)).texOffs(35, 56).addBox(-4.000000F, -1.000000F, -4.000000F, 1.000000F, 6.000000F, 8.000000F, new CubeDeformation(-0.026600F)).texOffs(35, 56).addBox(3.000000F, -1.000000F, -4.000000F, 1.000000F, 6.000000F, 8.000000F, new CubeDeformation(-0.026800F)).texOffs(55, 56).addBox(-3.000000F, -1.000000F, -4.000000F, 6.000000F, 6.000000F, 1.000000F, new CubeDeformation(-0.027000F)).texOffs(55, 56).addBox(-3.000000F, -1.000000F, 3.000000F, 6.000000F, 6.000000F, 1.000000F, new CubeDeformation(-0.027200F)).texOffs(64, 32).addBox(-3.000000F, 0.000000F, -3.000000F, 6.000000F, 0.400000F, 6.000000F, new CubeDeformation(-0.027400F)).texOffs(96, 26).addBox(-1.500000F, -0.100000F, -2.000000F, 3.000000F, 0.200000F, 3.000000F, new CubeDeformation(-0.027600F)).texOffs(90, 43).addBox(-4.250000F, -1.500000F, -4.250000F, 1.000000F, 0.600000F, 8.500000F, new CubeDeformation(-0.027800F)).texOffs(90, 43).addBox(3.250000F, -1.500000F, -4.250000F, 1.000000F, 0.600000F, 8.500000F, new CubeDeformation(-0.028000F)).texOffs(69, 28).addBox(-3.250000F, -1.500000F, -4.250000F, 6.500000F, 0.600000F, 1.000000F, new CubeDeformation(-0.028200F)).texOffs(69, 28).addBox(-3.250000F, -1.500000F, 3.250000F, 6.500000F, 0.600000F, 1.000000F, new CubeDeformation(-0.028400F)).texOffs(23, 123).addBox(-1.500000F, -0.800000F, -5.500000F, 3.000000F, 0.600000F, 1.400000F, new CubeDeformation(-0.028600F)).texOffs(94, 65).addBox(-2.000000F, -1.600000F, -5.500000F, 0.500000F, 1.400000F, 1.400000F, new CubeDeformation(-0.028800F)).texOffs(94, 65).addBox(1.500000F, -1.600000F, -5.500000F, 0.500000F, 1.400000F, 1.400000F, new CubeDeformation(-0.029000F)), PartPose.offsetAndRotation(0.000000F, 7.000000F, 0.000000F, 0.000000F, 0.000000F, 0.000000F));
        var slag_stream = crucible.addOrReplaceChild("slag_stream", CubeListBuilder.create().texOffs(122, 56).addBox(-0.500000F, 0.000000F, -0.500000F, 1.000000F, 4.000000F, 1.000000F, new CubeDeformation(-0.029200F)).texOffs(94, 70).addBox(-0.300000F, 3.000000F, -0.300000F, 0.600000F, 3.000000F, 0.600000F, new CubeDeformation(-0.029400F)), PartPose.offsetAndRotation(0.000000F, -0.200000F, -5.200000F, 0.000000F, 0.000000F, 0.000000F));
        return LayerDefinition.create(mesh, 128, 128);
    }
    private static float smooth(float t) { t = Mth.clamp(t, 0, 1); return t*t*(3-2*t); }
    @Override public void setupAnim(AbyssFoundryEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        Visual v = visuals.computeIfAbsent(e, key -> new Visual());
        int attack = e.attack(); float progress = e.windup(), weight = 1;
        if (attack != 0) { v.attack = attack; v.progress = progress; v.stopped = age; }
        else if (v.attack != 0 && age-v.stopped < 10 && !e.exposed()) {
            attack = v.attack; progress = Mth.lerp(Mth.clamp((age-v.stopped)/2, 0, 1), v.progress, 1);
            weight = 1-smooth((age-v.stopped-2)/8);
        }
        // andar: pasos pesados y bien visibles (el jefe es lento, así que se amplifica la cantidad de paso)
        float walk = Math.min(1.0F, limbSwingAmount * 2.2F) * (e.exposed() ? 0.0F : attack == 0 ? 1.0F : 0.5F);
        float phase = limbSwing * 0.5F;
        right_leg.xRot += Mth.cos(phase)*0.75F*walk;
        left_leg.xRot += Mth.cos(phase+Mth.PI)*0.75F*walk;
        right_knee.xRot += Math.max(0, -Mth.sin(phase))*0.9F*walk;
        left_knee.xRot += Math.max(0, Mth.sin(phase))*0.9F*walk;
        right_foot.xRot -= Mth.cos(phase)*0.35F*walk;
        left_foot.xRot -= Mth.cos(phase+Mth.PI)*0.35F*walk;
        body.y += Math.abs(Mth.sin(phase))*1.2F*walk;                                // cae el peso en cada paso
        body.zRot += Mth.sin(phase)*0.06F*walk;                                      // balanceo de lado a lado
        body.yRot += Mth.sin(phase)*0.05F*walk;
        if (attack == 0) {                                                           // brazos al ritmo de las piernas
            right_arm.xRot += Mth.cos(phase+Mth.PI)*0.35F*walk;
            left_arm.xRot += Mth.cos(phase)*0.35F*walk;
        }
        body.y += Mth.sin(age*0.12F)*0.035F;
        body.zRot += Mth.sin(age*(e.enraged() ? 0.7F : 0.18F))*(e.enraged() ? 0.008F : 0.003F);
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.15F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.15F;
        float charge = smooth(progress/0.78F), hit = smooth((progress-0.78F)/0.22F);
        if (e.exposed()) {
            root.y += 1.100000F;
            body.y += 3.100000F;
            right_leg.xRot += -0.450000F;
            left_leg.xRot += -0.450000F;
            right_knee.xRot += 0.750000F;
            left_knee.xRot += 0.750000F;
            right_foot.xRot += -0.300000F;
            left_foot.xRot += -0.300000F;
            body.xRot += 0.380000F;
            head.xRot += 0.250000F;
            door_left.yRot += -1.500000F;
            door_right.yRot += 1.500000F;
            vent_left.zRot += -1.100000F;
            vent_right.zRot += 1.100000F;
            lid_left.xRot += -1.200000F;
            lid_right.xRot += -1.200000F;
            right_arm.zRot += 0.150000F;
            left_arm.zRot += -0.200000F;
            left_arm.xRot += -0.380000F;
        } else switch (attack) {
            case 1:
                body.xRot += Mth.lerp(hit, 0.000000F*charge, 0.320000F)*weight;
                body.y += Mth.lerp(hit, 0.000000F*charge, 3.100000F)*weight;
                body.yRot += Mth.lerp(hit, -0.160000F*charge, 0.100000F)*weight;
                left_arm.zRot += Mth.lerp(hit, -0.150000F*charge, 0.000000F)*weight;
                left_foot.xRot += Mth.lerp(hit, 0.000000F*charge, -0.300000F)*weight;
                left_knee.xRot += Mth.lerp(hit, 0.000000F*charge, 0.750000F)*weight;
                left_leg.xRot += Mth.lerp(hit, 0.000000F*charge, -0.450000F)*weight;
                right_arm.xRot += Mth.lerp(hit, -2.150000F*charge, -0.600000F)*weight;
                right_elbow.xRot += Mth.lerp(hit, -0.600000F*charge, 0.100000F)*weight;
                right_foot.xRot += Mth.lerp(hit, 0.000000F*charge, -0.300000F)*weight;
                right_knee.xRot += Mth.lerp(hit, 0.000000F*charge, 0.750000F)*weight;
                right_leg.xRot += Mth.lerp(hit, 0.000000F*charge, -0.450000F)*weight;
                root.y += Mth.lerp(hit, 0.000000F*charge, 1.100000F)*weight;
                break;
            case 2:
                body.xRot += Mth.lerp(hit, -0.120000F*charge, 0.250000F)*weight;
                body.y += Mth.lerp(hit, 0.000000F*charge, 3.100000F)*weight;
                left_arm.zRot += Mth.lerp(hit, -0.250000F*charge, 0.000000F)*weight;
                left_foot.xRot += Mth.lerp(hit, 0.000000F*charge, -0.300000F)*weight;
                left_knee.xRot += Mth.lerp(hit, 0.000000F*charge, 0.750000F)*weight;
                left_leg.xRot += Mth.lerp(hit, 0.000000F*charge, -0.450000F)*weight;
                right_arm.xRot += Mth.lerp(hit, -1.450000F*charge, -0.500000F)*weight;
                right_elbow.xRot += Mth.lerp(hit, -0.500000F*charge, 0.100000F)*weight;
                right_foot.xRot += Mth.lerp(hit, 0.000000F*charge, -0.300000F)*weight;
                right_knee.xRot += Mth.lerp(hit, 0.000000F*charge, 0.750000F)*weight;
                right_leg.xRot += Mth.lerp(hit, 0.000000F*charge, -0.450000F)*weight;
                root.y += Mth.lerp(hit, 0.000000F*charge, 1.100000F)*weight;
                break;
            case 3:
                // lanzamiento: echa el crisol atrás y arriba (aviso), y lo descarga por encima del hombro hacia delante
                body.xRot += Mth.lerp(hit, -0.180000F*charge, 0.300000F)*weight;
                body.yRot += Mth.lerp(hit, 0.380000F*charge, -0.320000F)*weight;
                body.y += Mth.lerp(hit, 0.000000F*charge, 1.400000F)*weight;
                left_arm.xRot += Mth.lerp(hit, 1.350000F*charge, -2.650000F)*weight;
                left_arm.zRot += Mth.lerp(hit, -0.250000F*charge, 0.100000F)*weight;
                left_elbow.xRot += Mth.lerp(hit, -1.050000F*charge, -0.150000F)*weight;
                crucible.xRot += Mth.lerp(hit, -0.350000F*charge, 2.300000F)*weight;
                right_arm.xRot += Mth.lerp(hit, -0.400000F*charge, 0.450000F)*weight;   // contrapeso
                left_leg.xRot += Mth.lerp(hit, 0.250000F*charge, -0.550000F)*weight;    // paso adelante al lanzar
                left_knee.xRot += Mth.lerp(hit, 0.200000F*charge, 0.450000F)*weight;
                right_leg.xRot += Mth.lerp(hit, -0.150000F*charge, 0.250000F)*weight;
                break;
            case 4:
                body.xRot += Mth.lerp(hit, 0.120000F*charge, 0.300000F)*weight;
                body.y += Mth.lerp(hit, 2.170000F*charge, 3.100000F)*weight;
                door_left.yRot += Mth.lerp(hit, -0.300000F*charge, -1.500000F)*weight;
                door_right.yRot += Mth.lerp(hit, 0.300000F*charge, 1.500000F)*weight;
                left_arm.xRot += Mth.lerp(hit, 0.000000F*charge, -0.300000F)*weight;
                left_arm.zRot += Mth.lerp(hit, 0.000000F*charge, -0.320000F)*weight;
                left_foot.xRot += Mth.lerp(hit, -0.210000F*charge, -0.300000F)*weight;
                left_knee.xRot += Mth.lerp(hit, 0.525000F*charge, 0.750000F)*weight;
                left_leg.xRot += Mth.lerp(hit, -0.315000F*charge, -0.450000F)*weight;
                lid_left.xRot += Mth.lerp(hit, 0.000000F*charge, -1.200000F)*weight;
                lid_right.xRot += Mth.lerp(hit, 0.000000F*charge, -1.200000F)*weight;
                right_arm.zRot += Mth.lerp(hit, 0.000000F*charge, 0.320000F)*weight;
                right_foot.xRot += Mth.lerp(hit, -0.210000F*charge, -0.300000F)*weight;
                right_knee.xRot += Mth.lerp(hit, 0.525000F*charge, 0.750000F)*weight;
                right_leg.xRot += Mth.lerp(hit, -0.315000F*charge, -0.450000F)*weight;
                root.y += Mth.lerp(hit, 0.770000F*charge, 1.100000F)*weight;
                vent_left.zRot += Mth.lerp(hit, -0.200000F*charge, -1.100000F)*weight;
                vent_right.zRot += Mth.lerp(hit, 0.200000F*charge, 1.100000F)*weight;
                break;
            default: break;
        }
        slag_stream.visible = attack == 3 && progress > 0.84F && weight > 0.5F && !e.exposed();
        slag_stream.xRot = -(body.xRot+left_arm.xRot+left_elbow.xRot+crucible.xRot);
    }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float alpha) {
        root.render(pose, buffer, light, overlay, r, g, b, alpha);
    }
}
