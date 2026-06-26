package com.bryanamaya.tecnocinetico.item.custom;

import com.bryanamaya.tecnocinetico.item.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class KineticPistolItem extends Item {

    public KineticPistolItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack pistolStack = pPlayer.getItemInHand(pHand);

        // 1. Buscar munición
        boolean hasAmmo = pPlayer.isCreative() || pPlayer.getInventory().contains(new ItemStack(ModItems.CINETRITE_SHARD.get()));

        if (hasAmmo) {
            if (!pLevel.isClientSide()) {
                // 2. SISTEMA HITSCAN: Calcular el rayo instantáneo (Rango: 25 bloques)
                double range = 25.0D;
                Vec3 eyePos = pPlayer.getEyePosition();
                Vec3 lookVec = pPlayer.getLookAngle();
                Vec3 endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
                AABB searchBox = pPlayer.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);

                // Trazar el láser para ver a qué entidad golpea
                EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(pLevel, pPlayer, eyePos, endPos, searchBox, (entity) -> !entity.isSpectator() && entity.isPickable());

                if (hitResult != null && hitResult.getEntity() != null) {
                    // ¡Impacto confirmado! Aplicamos daño directo del jugador (Esto burla al Enderman)
                    // Daño: 8.0F (Equivale a 4 corazones de golpe)
                    hitResult.getEntity().hurt(pLevel.damageSources().playerAttack(pPlayer), 8.0F);
                }

                // Efecto visual: Trazadora de partículas para simular el disparo supersónico
                if (pLevel instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, eyePos.x + lookVec.x, eyePos.y + lookVec.y, eyePos.z + lookVec.z, 15, lookVec.x, lookVec.y, lookVec.z, 0.5D);
                }

                // 3. Consumir la munición
                if (!pPlayer.isCreative()) {
                    findAndConsumeAmmo(pPlayer);
                    pistolStack.hurtAndBreak(1, pPlayer, (player) -> player.broadcastBreakEvent(pHand));
                }
            }

            // 4. Sonido de disparo electromagnético
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 2.0f);

            // 5. Cooldown (Retroceso del arma)
            pPlayer.getCooldowns().addCooldown(this, 10);

            pPlayer.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.consume(pistolStack);
        }

        // Si no hay balas, sonido de percutor vacío
        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.2f);

        return InteractionResultHolder.fail(pistolStack);
    }

    // Algoritmo para buscar y remover exactamente 1 fragmento de Cinetrita del inventario
    private void findAndConsumeAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.CINETRITE_SHARD.get())) {
                stack.shrink(1); // Reduce la cantidad de munición en 1
                break;
            }
        }
    }
}