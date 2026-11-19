package com.remake.arsenal.item

import com.remake.arsenal.logic.ClawsHelper
import com.remake.arsenal.logic.Ench
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
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
		if (!attacker.level().isClientSide) {
			val lvl = attacker.level()
			val bleed = Ench.level(lvl, stack, Ench.CLAWS_BLEED)
			if (bleed > 0) target.addEffect(MobEffectInstance(MobEffects.WITHER, 40 + 30 * bleed, 0))
			val vamp = Ench.level(lvl, stack, Ench.CLAWS_VAMPIRE)
			if (vamp > 0) attacker.heal(1.0f * vamp)
		}
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND)
		return true
	}
}
