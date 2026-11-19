package com.remake.arsenal.item

import com.remake.arsenal.content.ContentEntities
import com.remake.arsenal.entity.ThrownMessageBottle
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

/** Бутылка с запиской. ПКМ — бросить. Если она упадёт в воду, море пришлёт ответ. */
class MessageBottleItem(props: Properties) : Item(props) {
	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
		val stack = player.getItemInHand(hand)
		level.playSound(null, player.x, player.y, player.z, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.9f)
		if (!level.isClientSide) {
			val e = ThrownMessageBottle(ContentEntities.THROWN_MESSAGE_BOTTLE, player, level)
			e.item = stack.copyWithCount(1)
			e.shootFromRotation(player, player.xRot, player.yRot, 0.0f, 1.2f, 1.0f)
			level.addFreshEntity(e)
		}
		if (!player.abilities.instabuild) stack.shrink(1)
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
	}

	override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag) {
		val msg = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString("msg") ?: ""
		if (msg.isNotEmpty()) tooltip.add(Component.literal("«" + (if (msg.length > 40) msg.take(40) + "…" else msg) + "»").withStyle(net.minecraft.ChatFormatting.GRAY))
		tooltip.add(Component.translatable("item.remake_arsenal.message_bottle.hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
	}
}
