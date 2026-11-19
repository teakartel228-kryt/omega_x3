package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.content.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Пружина: ставится на пол, на стену или на потолок (FACING = куда она выталкивает).
 * Любое существо, коснувшееся её лицевой стороны, отлетает в сторону FACING.
 */
class SpringBlock(props: Properties) : Block(props) {
	companion object {
		val FACING = BlockStateProperties.FACING
		val COMPRESSED: BooleanProperty = BooleanProperty.create("compressed")

		private const val LAUNCH_UP = 1.35
		private const val LAUNCH_SIDE = 1.15
		private const val LAUNCH_DOWN = -1.0

		private fun shapeOf(dir: Direction, h: Double): VoxelShape = when (dir) {
			Direction.UP -> Block.box(0.0, 0.0, 0.0, 16.0, h, 16.0)
			Direction.DOWN -> Block.box(0.0, 16.0 - h, 0.0, 16.0, 16.0, 16.0)
			Direction.NORTH -> Block.box(0.0, 0.0, 16.0 - h, 16.0, 16.0, 16.0)
			Direction.SOUTH -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, h)
			Direction.EAST -> Block.box(0.0, 0.0, 0.0, h, 16.0, 16.0)
			Direction.WEST -> Block.box(16.0 - h, 0.0, 0.0, 16.0, 16.0, 16.0)
		}

		private val EXTENDED: Map<Direction, VoxelShape> = Direction.values().associateWith { shapeOf(it, 14.0) }
		private val PRESSED: Map<Direction, VoxelShape> = Direction.values().associateWith { shapeOf(it, 8.0) }
	}

	init {
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(COMPRESSED, false))
	}

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { SpringBlock(ModBlocks.springProps()) }

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		builder.add(FACING, COMPRESSED)
	}

	override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
		val state = defaultBlockState().setValue(FACING, ctx.clickedFace)
		return if (state.canSurvive(ctx.level, ctx.clickedPos)) state else null
	}

	override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, ctx: CollisionContext): VoxelShape {
		val dir = state.getValue(FACING)
		return if (state.getValue(COMPRESSED)) PRESSED.getValue(dir) else EXTENDED.getValue(dir)
	}

	override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
		val dir = state.getValue(FACING)
		val back = pos.relative(dir.opposite)
		return level.getBlockState(back).isFaceSturdy(level, back, dir)
	}

	override fun updateShape(
		state: BlockState, direction: Direction, neighborState: BlockState,
		level: LevelAccessor, pos: BlockPos, neighborPos: BlockPos
	): BlockState =
		if (!state.canSurvive(level, pos)) net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
		else super.updateShape(state, direction, neighborState, level, pos, neighborPos)

	override fun rotate(state: BlockState, rotation: Rotation): BlockState =
		state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

	override fun mirror(state: BlockState, mirror: Mirror): BlockState =
		state.rotate(mirror.getRotation(state.getValue(FACING)))

	override fun entityInside(state: BlockState, level: Level, pos: BlockPos, entity: Entity) {
		if (state.getValue(COMPRESSED) || entity.isSpectator || entity.isShiftKeyDown) return
		val dir = state.getValue(FACING)
		val m = entity.deltaMovement
		val nv = when (dir) {
			Direction.UP -> Vec3(m.x, LAUNCH_UP, m.z)
			Direction.DOWN -> Vec3(m.x, LAUNCH_DOWN, m.z)
			else -> Vec3(
				if (dir.stepX != 0) dir.stepX * LAUNCH_SIDE else m.x,
				Math.max(m.y, 0.42),
				if (dir.stepZ != 0) dir.stepZ * LAUNCH_SIDE else m.z
			)
		}
		entity.deltaMovement = nv
		entity.hasImpulse = true
		entity.fallDistance = 0.0f
		if (level.isClientSide) return

		entity.hurtMarked = true
		level.setBlock(pos, state.setValue(COMPRESSED, true), Block.UPDATE_ALL)
		level.scheduleTick(pos, this, 8)
		level.playSound(null, pos, ModSounds.SPRING_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f)
		val sl = level as ServerLevel
		sl.sendParticles(
			ParticleTypes.CLOUD,
			pos.x + 0.5 + dir.stepX * 0.6, pos.y + 0.5 + dir.stepY * 0.6, pos.z + 0.5 + dir.stepZ * 0.6,
			5, 0.2, 0.2, 0.2, 0.02
		)
	}

	override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
		if (state.getValue(COMPRESSED)) level.setBlock(pos, state.setValue(COMPRESSED, false), Block.UPDATE_ALL)
	}
}
