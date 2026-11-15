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

    /** La radiación de bloques se salta las secciones sin uranio, pero sigue notando un bloque cercano. */
    @GameTest(template = "empty")
    public static void radiationSkipsCleanSections(GameTestHelper h) {
        var player = h.makeMockPlayer();
        player.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        float clean = com.bryanamaya.tecnocinetico.effect.RadiationEvents.exposure(player);
        h.setBlock(new net.minecraft.core.BlockPos(3, 2, 1), com.bryanamaya.tecnocinetico.block.ModBlocks.URANIUM_BLOCK.get());
        float hot = com.bryanamaya.tecnocinetico.effect.RadiationEvents.exposure(player);
        h.assertTrue(clean == 0.0F && hot > 1.0F, "Radiación sin/con bloque de uranio: " + clean + " / " + hot);
        h.succeed();
    }

    /** La luz de estado no cambia hasta que el nuevo valor se mantiene unos ticks (sin reconstruir el chunk cada tick). */
    @GameTest(template = "empty")
    public static void statusLightDebounce(GameTestHelper h) {
        var pos = new net.minecraft.core.BlockPos(1, 2, 1);
        h.setBlock(pos, Blocks.REDSTONE_LAMP.defaultBlockState());                // cualquier bloque con un booleano sirve de muestra
        var lit = net.minecraft.world.level.block.RedstoneLampBlock.LIT;
        var abs = h.absolutePos(pos);
        int pending = 0;
        for (int i = 0; i < com.bryanamaya.tecnocinetico.util.StatusLight.SETTLE - 1; i++) {
            pending = com.bryanamaya.tecnocinetico.util.StatusLight.update(h.getLevel(), abs, h.getBlockState(pos), lit, true, pending);
        }
        h.assertTrue(!h.getBlockState(pos).getValue(lit), "La luz cambió antes de tiempo");
        h.assertTrue(com.bryanamaya.tecnocinetico.util.StatusLight.update(h.getLevel(), abs, h.getBlockState(pos), lit, false, pending) == 0,
                "Volver al valor actual debería reiniciar la espera");
        pending = 0;
        for (int i = 0; i < com.bryanamaya.tecnocinetico.util.StatusLight.SETTLE; i++) {
            pending = com.bryanamaya.tecnocinetico.util.StatusLight.update(h.getLevel(), abs, h.getBlockState(pos), lit, true, pending);
        }
        h.assertTrue(h.getBlockState(pos).getValue(lit), "La luz no se encendió tras mantenerse");
        h.succeed();
    }
}
