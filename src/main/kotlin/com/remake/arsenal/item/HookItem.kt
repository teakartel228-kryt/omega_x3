package com.remake.arsenal.item

import com.remake.arsenal.logic.Ench
import com.remake.arsenal.logic.HookLogic
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks

/** Крюк. tier: 0 = базовый, 1..5 = улучшения I..V. ПКМ — выстрел, Shift — отпустить крюки. */
class HookItem(val tier: Int, props: Properties) : Item(props) {
	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
		val stack = player.getItemInHand(hand)
		if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true)
		HookLogic.fire(player as ServerPlayer, stack, tier)
		return InteractionResultHolder.sidedSuccess(stack, false)
	}

	/** Редстоун-крюк заряжается от блока редстоуна. */
	override fun useOn(ctx: UseOnContext): InteractionResult {
		val level = ctx.level
		val lvl = Ench.level(level, ctx.itemInHand, Ench.REDSTONE)
		if (lvl > 0 && level.getBlockState(ctx.clickedPos).`is`(Blocks.REDSTONE_BLOCK)) {
			if (!level.isClientSide) {
				HookLogic.setCharges(ctx.itemInHand, lvl)
				level.playSound(null, ctx.clickedPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f)
			}
			return InteractionResult.sidedSuccess(level.isClientSide)
		}
		return InteractionResult.PASS
	}
}
