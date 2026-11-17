package com.remake.arsenal.item

import com.remake.arsenal.logic.BagLogic
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/**
 * ПКМ — вытянуть руку (атака на расстоянии).
 * Shift+ПКМ — призвать/отозвать помощника.
 * Клавиша G (при мешке в руке) — Ненавистная молния.
 */
class HateBagItem(props: Properties) : Item(props) {
	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
		val stack = player.getItemInHand(hand)
		if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true)
		val sp = player as ServerPlayer
		if (player.isShiftKeyDown) BagLogic.toggleHelper(sp) else BagLogic.extendArm(sp, this)
		return InteractionResultHolder.sidedSuccess(stack, false)
	}
}
