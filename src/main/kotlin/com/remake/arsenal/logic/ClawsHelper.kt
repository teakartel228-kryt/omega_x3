package com.remake.arsenal.logic

import com.remake.arsenal.ModItems
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

object ClawsHelper {
	@JvmField
	var busy = false

	@JvmStatic
	fun shouldClimb(e: LivingEntity): Boolean {
		if (e !is Player || e.isSpectator) return false
		if (!e.horizontalCollision) return false
		return e.mainHandItem.`is`(ModItems.CLAWS) || e.offhandItem.`is`(ModItems.CLAWS)
	}
}
