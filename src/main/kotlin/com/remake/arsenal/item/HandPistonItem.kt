package com.remake.arsenal.item

import com.remake.arsenal.logic.PistonLogic
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level

/** Ручной поршень: зажми ПКМ чтобы зарядить, отпусти — выстрел. */
class HandPistonItem(props: Properties) : Item(props) {
	override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.BOW
	override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int = 72000

	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
		val stack = player.getItemInHand(hand)
		if (player.cooldowns.isOnCooldown(this)) return InteractionResultHolder.fail(stack)
		player.startUsingItem(hand)
		return InteractionResultHolder.consume(stack)
	}

	override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeCharged: Int) {
		if (level.isClientSide || entity !is ServerPlayer) return
		val used = getUseDuration(stack, entity) - timeCharged
		PistonLogic.shoot(entity, stack, used >= PistonLogic.CHARGE_TICKS)
	}
}
