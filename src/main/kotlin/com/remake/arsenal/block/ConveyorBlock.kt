package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.Vec3

/**
 * Конвейер — полноразмерный блок. Тащит всё, что стоит сверху, в сторону FACING (куда смотрел игрок при установке).
 * Сигнал редстоуна останавливает ленту.
 */
class ConveyorBlock(props: Properties) : Block(props) {
	companion object {
		val FACING = BlockStateProperties.HORIZONTAL_FACING
		val POWERED: BooleanProperty = BlockStateProperties.POWERED

		private const val ACCEL = 0.055
		private const val MAX_SPEED = 0.2
	}

	init {
		registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(POWERED, false))
	}

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { ConveyorBlock(ModBlocks.props()) }

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		builder.add(FACING, POWERED)
	}

	override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
		defaultBlockState()
			.setValue(FACING, ctx.horizontalDirection)
			.setValue(POWERED, ctx.level.hasNeighborSignal(ctx.clickedPos))

	override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, fromPos: BlockPos, moving: Boolean) {
		if (level.isClientSide) return
		val powered = level.hasNeighborSignal(pos)
		if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), UPDATE_CLIENTS)
	}

	override fun rotate(state: BlockState, rotation: Rotation): BlockState =
		state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

	override fun mirror(state: BlockState, mirror: Mirror): BlockState =
		state.rotate(mirror.getRotation(state.getValue(FACING)))

	override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
		if (state.getValue(POWERED) || entity.isSpectator) return
		val dir = state.getValue(FACING)
		val m = entity.deltaMovement
		val along = m.x * dir.stepX + m.z * dir.stepZ
		if (along >= MAX_SPEED) return
		entity.deltaMovement = Vec3(m.x + dir.stepX * ACCEL, m.y, m.z + dir.stepZ * ACCEL)
		entity.hasImpulse = true
	}
}
