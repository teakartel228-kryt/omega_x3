package com.remake.arsenal.logic

import com.remake.arsenal.ModItems
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

object Abilities {
	const val SONIC_RADIUS = 6.0
	const val SONIC_COOLDOWN = 160

	// ---------- Электро-крылья: рывок ----------
	fun dash(player: ServerPlayer) {
		if (!player.getItemBySlot(EquipmentSlot.CHEST).`is`(ModItems.ELECTRO_WINGS)) return
		val st = Arsenal.of(player)
		if (st.energy < 1.0f) {
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.no_energy"), true)
			return
		}
		val level = player.serverLevel()
		st.energy -= 1.0f
		st.dashTicks = 7                      // неуязвимость только пока длится рывок
		st.dashDir = player.lookAngle.normalize()
		player.deltaMovement = st.dashDir.scale(1.6)
		player.hurtMarked = true
		player.fallDistance = 0.0f
		level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 2.0f)
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.x, player.y + 1.0, player.z, 20, 0.4, 0.6, 0.4, 0.2)
	}

	// ---------- Нотный шлем: звуковая волна ----------
	fun sonicWave(player: ServerPlayer) {
		if (!player.getItemBySlot(EquipmentSlot.HEAD).`is`(ModItems.NOTE_HELMET)) return
		val level = player.serverLevel()
		val st = Arsenal.of(player)
		if (level.gameTime < st.sonicReadyAt) {
			val sec = (st.sonicReadyAt - level.gameTime) / 20 + 1
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.sonic_cooldown", sec), true)
			return
		}
		st.sonicReadyAt = level.gameTime + SONIC_COOLDOWN

		val center = player.eyePosition
		val targets = level.getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.inflate(SONIC_RADIUS)) {
			it !== player && it.isAlive && it.distanceToSqr(player) <= SONIC_RADIUS * SONIC_RADIUS
		}
		for (t in targets) {
			// стены и блоки блокируют волну
			val to = t.position().add(0.0, t.bbHeight * 0.5, 0.0)
			val clip = level.clip(ClipContext(center, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
			if (clip.type != HitResult.Type.MISS) continue
			t.hurt(player.damageSources().sonicBoom(player), 6.0f)
			val away = t.position().subtract(player.position()).multiply(1.0, 0.0, 1.0)
			if (away.lengthSqr() > 1.0e-4) {
				t.deltaMovement = t.deltaMovement.add(away.normalize().scale(1.4)).add(0.0, 0.25, 0.0)
				t.hurtMarked = true
			}
		}

		// визуальное кольцо из нот
		for (i in 0 until 36) {
			val a = Math.toRadians(i * 10.0)
			for (r in intArrayOf(2, 4, 6)) {
				level.sendParticles(ParticleTypes.NOTE, player.x + Math.cos(a) * r, player.y + 1.0, player.z + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0)
			}
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, player.x, player.y + 1.0, player.z, 1, 0.0, 0.0, 0.0, 0.0)
		level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2.0f, 0.7f)
		level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.6f)
	}
}
