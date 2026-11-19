package com.remake.arsenal.item

import com.remake.arsenal.logic.HookHead
import com.remake.arsenal.logic.HookHeads
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/** Сменная головка крюка. Держишь головку в одной руке, крюк в другой, жмёшь ПКМ — головка надевается. */
class HookHeadItem(val head: HookHead, props: Properties) : Item(props) {
	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
		val stack = player.getItemInHand(hand)
		val other = if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
		val hook = player.getItemInHand(other)
		if (hook.item !is HookItem) {
			if (!level.isClientSide) player.displayClientMessage(Component.translatable("msg.remake_arsenal.head_need_hook"), true)
			return InteractionResultHolder.fail(stack)
		}
		if (!level.isClientSide) {
			val old = HookHeads.of(hook)
			if (old == head) return InteractionResultHolder.fail(stack)
			HookHeads.set(hook, head)
			val oldItem = HookHeads.item(old)
			if (!player.abilities.instabuild) stack.shrink(1)
			if (oldItem != null) player.addItem(ItemStack(oldItem))
			level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.4f)
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.head_attached", Component.translatable("hook.head.${head.key}")), true)
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
	}
}
