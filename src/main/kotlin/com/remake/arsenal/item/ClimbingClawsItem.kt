package com.remake.arsenal.item

import com.remake.arsenal.logic.ClawsHelper
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/** Когти: 3 удара за одну атаку + лазание по стенам (см. LivingEntityMixin). */
class ClimbingClawsItem(props: Properties) : Item(props) {
	override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
		if (!attacker.level().isClientSide && !ClawsHelper.busy) {
			ClawsHelper.busy = true
			try {
				val source = if (attacker is Player) attacker.damageSources().playerAttack(attacker) else attacker.damageSources().mobAttack(attacker)
				repeat(2) {
					if (target.isAlive) {
						target.invulnerableTime = 0
						target.hurt(source, 2.0f)
					}
				}
			} finally {
				ClawsHelper.busy = false
			}
		}
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND)
		return true
	}
}
