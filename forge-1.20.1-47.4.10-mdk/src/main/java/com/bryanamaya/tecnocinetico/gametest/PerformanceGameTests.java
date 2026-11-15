package com.bryanamaya.tecnocinetico.gametest;

import com.bryanamaya.tecnocinetico.Tecnocinetico;
import com.bryanamaya.tecnocinetico.network.ParticleShapePacket;
import com.bryanamaya.tecnocinetico.particle.ModParticles;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

/** Rendimiento: las figuras de partículas viajan en un solo paquete y llegan intactas. */
@GameTestHolder(Tecnocinetico.MOD_ID)
@PrefixGameTestTemplate(false)
public class PerformanceGameTests {

    @GameTest(template = "empty")
    public static void particleShapePacketRoundTrip(GameTestHelper h) {
        ParticleOptions[] particles = {ModParticles.DATA_GLITCH.get(), new DustParticleOptions(new Vector3f(1.0F, 0.3F, 0.85F), 0.7F),
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState())};
        for (ParticleOptions p : particles) {
            var in = new ParticleShapePacket(p, ParticleShapePacket.DISK, new Vec3(1.5, 64.25, -3), new Vec3(0, 1, 0), new Vec3(4, 5, 6),
                    48, 1.5F, 6.0F, 1.5F, 0.25F, 2, 0.1F, 0.05F, 0.1F, 0.2F);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            ParticleShapePacket.encode(in, buf);
            var out = ParticleShapePacket.decode(buf);
            h.assertTrue(out.particle().writeToString().equals(p.writeToString()) && out.shape() == in.shape() && out.a().equals(in.a())
                            && out.c().equals(in.c()) && out.n() == in.n() && out.r1() == in.r1() && out.count() == in.count()
                            && out.speed() == in.speed() && buf.readableBytes() == 0,
                    "La figura de partículas no llega igual: " + p.writeToString());
        }
        h.succeed();
    }
}
