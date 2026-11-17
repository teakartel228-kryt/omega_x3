package com.remake.arsenal.logic

import com.remake.arsenal.ModEntities
import com.remake.arsenal.ModItems
import com.remake.arsenal.entity.HelperEntity
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.item.Item
import org.joml.Vector3f

object BagLogic {
	private val HATE_RED = DustParticleOptions(Vector3f(0.8f, 0.05f, 0.1f), 1.2f)

	/** Вытягивание руки: бьёт цель в направлении взгляда. */
	fun extendArm(player: ServerPlayer, item: Item) {
		if (player.cooldowns.isOnCooldown(item)) return
		val level = player.serverLevel()
		val dir = player.lookAngle
		val pick = Rays.pick(player, dir, 7.0) { it is LivingEntity }
		Chain.draw(level, player.eyePosition.add(0.0, -0.3, 0.0), pick.to, HATE_RED)
		val target = pick.entity
		if (target is LivingEntity) {
			target.hurt(player.damageSources().playerAttack(player), 4.0f)
			target.deltaMovement = target.deltaMovement.add(dir.x * 0.4, 0.15, dir.z * 0.4)
			target.hurtMarked = true
		}
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.6f)
		player.cooldowns.addCooldown(item, 20)
	}

	fun toggleHelper(player: ServerPlayer) {
		val level = player.serverLevel()
		val st = Arsenal.of(player)
		val existing = st.helperUuid?.let { level.getEntity(it) }
		if (existing != null && existing.isAlive) {
			existing.discard()
			st.helperUuid = null
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.helper_dismissed"), true)
			return
		}
		val now = level.gameTime
		if (now < st.summonReadyAt) {
			val sec = (st.summonReadyAt - now) / 20 + 1
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.helper_cooldown", sec), true)
			return
		}
		val helper = HelperEntity(ModEntities.HELPER, level)
		helper.ownerUuid = player.uuid
		helper.moveTo(player.x + 1.0, player.y, player.z, player.yRot, 0.0f)
		level.addFreshEntity(helper)
		st.helperUuid = helper.uuid
		level.sendParticles(ParticleTypes.SMOKE, helper.x, helper.y + 0.5, helper.z, 15, 0.3, 0.4, 0.3, 0.02)
	}

	/** Ненавистная молния: автоматически выбирает ближайшего врага. */
	fun hateLightning(player: ServerPlayer) {
		if (!player.mainHandItem.`is`(ModItems.HATE_BAG) && !player.offhandItem.`is`(ModItems.HATE_BAG)) return
		val level = player.serverLevel()
		val st = Arsenal.of(player)
		if (level.gameTime < st.lightningReadyAt) return

		val range = 20.0
		val candidates = level.getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.inflate(range)) {
			it !== player && it.isAlive && it !is HelperEntity && (it is Enemy || (player.lastHurtMob === it)) && player.hasLineOfSight(it)
		}
		val target = candidates.minByOrNull { it.distanceToSqr(player) } ?: return

		val bolt = LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level)
		bolt.moveTo(target.x, target.y, target.z)
		bolt.setVisualOnly(true)
		level.addFreshEntity(bolt)
		target.hurt(player.damageSources().playerAttack(player), 8.0f)
		st.lightningReadyAt = level.gameTime + 160
	}
}
