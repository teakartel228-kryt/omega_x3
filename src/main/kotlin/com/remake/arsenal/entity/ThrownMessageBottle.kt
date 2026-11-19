package com.remake.arsenal.entity

import com.remake.arsenal.ModItems
import com.remake.arsenal.logic.BottleMail
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult

/** Брошенная бутылка с запиской. Упала в воду — уплыла в море (ответ придёт позже), на землю — просто выпала. */
class ThrownMessageBottle : ThrowableItemProjectile {
	constructor(type: EntityType<out ThrownMessageBottle>, level: Level) : super(type, level)
	constructor(type: EntityType<out ThrownMessageBottle>, owner: LivingEntity, level: Level) : super(type, owner, level)

	private var done = false

	override fun getDefaultItem(): Item = ModItems.MESSAGE_BOTTLE

	override fun tick() {
		super.tick()
		val lvl = level()
		if (!done && lvl is ServerLevel && isInWater) sail(lvl)
	}

	private fun sail(level: ServerLevel) {
		done = true
		val text = item.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString("msg") ?: ""
		val p = owner as? ServerPlayer
		if (p != null) {
			BottleMail.send(p, text)
			p.displayClientMessage(Component.translatable("msg.remake_arsenal.bottle_sent"), true)
		}
		level.sendParticles(ParticleTypes.SPLASH, x, y, z, 14, 0.3, 0.1, 0.3, 0.1)
		level.playSound(null, x, y, z, SoundEvents.PLAYER_SPLASH, SoundSource.NEUTRAL, 0.8f, 1.2f)
		discard()
	}

	override fun onHit(result: HitResult) {
		super.onHit(result)
		val lvl = level()
		if (!done && lvl is ServerLevel) {
			done = true
			lvl.addFreshEntity(ItemEntity(lvl, x, y + 0.2, z, item.copy()))
			discard()
		}
	}
}
