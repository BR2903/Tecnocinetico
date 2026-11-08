package com.bryanamaya.tecnocinetico.block.entity;

import com.bryanamaya.tecnocinetico.block.ModBlocks;
import com.bryanamaya.tecnocinetico.entity.ModEntities;
import com.bryanamaya.tecnocinetico.entity.boss.SiegeSpiderEntity;
import com.bryanamaya.tecnocinetico.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consola del Núcleo (centro de la arena bajo la cámara del Núcleo). Al activarla aparece la Araña de Asedio y la
 * barrera de energía sella la salida (los bloques de "sello" los fija la estructura al generarse). Si la araña
 * muere, la barrera se abre y la consola queda apagada para siempre; si el combate se reinicia (nadie cerca un
 * minuto), la barrera se abre y se puede volver a intentar.
 */
public class CoreConsoleBlockEntity extends BlockEntity {

    private boolean active, beaten;
    private final List<BlockPos> seal = new ArrayList<>();
    /** Bloques que había donde se puso la barrera (NBT del estado; se lee al abrir, cuando ya hay mundo). */
    private final Map<BlockPos, CompoundTag> saved = new LinkedHashMap<>();

    public CoreConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_CONSOLE_BE.get(), pos, state);
    }

    public void setSeal(List<BlockPos> positions) {
        seal.clear();
        seal.addAll(positions);
        setChanged();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBeaten() {
        return beaten;
    }

    /** Clic derecho de un jugador: despierta al guardián. */
    public void activate(Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (beaten) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.console_beaten").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (active) {
            player.displayClientMessage(Component.translatable("message.tecnocinetico.console_active").withStyle(ChatFormatting.RED), true);
            return;
        }
        SiegeSpiderEntity boss = ModEntities.SIEGE_SPIDER.get().create(server);
        if (boss == null) {
            return;
        }
        BlockPos at = worldPosition.south(7);
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F);
        boss.finalizeSpawn(server, server.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        boss.setConsole(worldPosition);
        boss.setTarget(player);
        server.addFreshEntity(boss);
        closeSeal(server);
        active = true;
        setChanged();
        server.playSound(null, worldPosition, ModSounds.SIEGE_ROAR.get(), SoundSource.HOSTILE, 6.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.tecnocinetico.console_wake").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    private void closeSeal(ServerLevel server) {
        saved.clear();
        for (BlockPos p : seal) {
            saved.put(p, NbtUtils.writeBlockState(server.getBlockState(p)));
            server.setBlock(p, ModBlocks.CORE_BARRIER.get().defaultBlockState(), 3);
        }
    }

    private void openSeal() {
        if (level == null) {
            return;
        }
        for (Map.Entry<BlockPos, CompoundTag> e : saved.entrySet()) {
            if (level.getBlockState(e.getKey()).is(ModBlocks.CORE_BARRIER.get())) {
                level.setBlock(e.getKey(), NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), e.getValue()), 3);
            }
        }
        saved.clear();
    }

    /** El guardián cayó: se abre la salida y la consola se apaga. */
    public void defeated() {
        openSeal();
        active = false;
        beaten = true;
        setChanged();
    }

    /** Combate abandonado: se abre la salida y se puede reintentar. */
    public void reset() {
        openSeal();
        active = false;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Active", active);
        tag.putBoolean("Beaten", beaten);
        ListTag s = new ListTag();
        seal.forEach(p -> s.add(NbtUtils.writeBlockPos(p)));
        tag.put("Seal", s);
        ListTag sv = new ListTag();
        saved.forEach((p, st) -> {
            CompoundTag c = new CompoundTag();
            c.put("Pos", NbtUtils.writeBlockPos(p));
            c.put("State", st);
            sv.add(c);
        });
        tag.put("Saved", sv);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean("Active");
        beaten = tag.getBoolean("Beaten");
        seal.clear();
        for (Tag t : tag.getList("Seal", Tag.TAG_COMPOUND)) {
            seal.add(NbtUtils.readBlockPos((CompoundTag) t));
        }
        saved.clear();
        for (Tag t : tag.getList("Saved", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            saved.put(NbtUtils.readBlockPos(c.getCompound("Pos")), c.getCompound("State"));
        }
    }
}
