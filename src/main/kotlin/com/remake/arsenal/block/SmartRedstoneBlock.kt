package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.content.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.server.level.ServerLevel

/**
 * Умный редстоун-блок. Программа на Python (подмножество, см. MiniPy) загружается из «Книги с пером»:
 * ПКМ книгой по блоку. Часы — сменить интервал запуска, Shift+ПКМ пустой рукой — старт/стоп, ПКМ пустой рукой — статус.
 */
class SmartRedstoneBlock(props: Properties) : Block(props), EntityBlock {
	companion object {
		val POWER = BlockStateProperties.POWER
		val ACTIVE: BooleanProperty = BooleanProperty.create("active")
		val INTERVALS = intArrayOf(1, 2, 4, 10, 20, 40, 100)
	}

	init { registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(ACTIVE, false)) }

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { SmartRedstoneBlock(ModBlocks.smartProps()) }

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(POWER, ACTIVE) }

	override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = SmartRedstoneBE(pos, state)

	override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
		if (level.isClientSide || type != ModBlocks.SMART_BE) null
		else BlockEntityTicker { l, p, s, be -> (be as SmartRedstoneBE).serverTick(l as ServerLevel, p, s) }

	override fun isSignalSource(state: BlockState): Boolean = true
	override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int = state.getValue(POWER)

	override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): ItemInteractionResult {
		val be = level.getBlockEntity(pos) as? SmartRedstoneBE ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
		if (stack.`is`(Items.WRITABLE_BOOK)) {
			if (level.isClientSide) return ItemInteractionResult.SUCCESS
			val content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT)
			val text = content?.pages()?.joinToString("\n") { it.raw() } ?: ""
			if (text.isBlank()) {
				player.displayClientMessage(Component.translatable("msg.remake_arsenal.smart_empty"), true)
			} else {
				val err = be.loadProgram(text)
				if (err == null) {
					level.playSound(null, pos, ModSounds.SMART_LOAD, SoundSource.BLOCKS, 1.0f, 1.0f)
					player.displayClientMessage(Component.translatable("msg.remake_arsenal.smart_loaded", text.split("\n").count { it.isNotBlank() }), true)
				} else {
					level.playSound(null, pos, ModSounds.SMART_ERROR, SoundSource.BLOCKS, 1.0f, 1.0f)
					player.sendSystemMessage(Component.translatable("msg.remake_arsenal.smart_error", err))
				}
			}
			return ItemInteractionResult.CONSUME
		}
		if (stack.`is`(Items.CLOCK)) {
			if (level.isClientSide) return ItemInteractionResult.SUCCESS
			be.cycleInterval()
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.smart_interval", be.interval), true)
			return ItemInteractionResult.CONSUME
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
	}

	override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
		if (level.isClientSide) return InteractionResult.SUCCESS
		val be = level.getBlockEntity(pos) as? SmartRedstoneBE ?: return InteractionResult.PASS
		if (player.isShiftKeyDown) {
			be.switchEnabled(!be.enabled)
			player.displayClientMessage(if (be.enabled) Component.translatable("msg.remake_arsenal.smart_on", be.interval) else Component.translatable("msg.remake_arsenal.smart_off"), true)
		} else if (be.error.isNotEmpty()) {
			player.sendSystemMessage(Component.translatable("msg.remake_arsenal.smart_error", be.error))
		} else if (be.hasProgram) {
			player.displayClientMessage(if (be.enabled) Component.translatable("msg.remake_arsenal.smart_on", be.interval) else Component.translatable("msg.remake_arsenal.smart_off"), true)
		} else {
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.smart_hint"), false)
		}
		return InteractionResult.CONSUME
	}
}
