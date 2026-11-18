package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.logic.Hit
import com.remake.arsenal.logic.HitBeam
import com.remake.arsenal.logic.HitReceiver
import com.remake.arsenal.logic.ProjectileReceiver
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties

/**
 * Перехватчик ударов и снарядов. FACING = выходная сторона, противоположная = входная.
 * Направление выхода меняется палкой отладки (свойство facing). Принимает с входной стороны удары игрока и снаряды.
 */
class HitRelayBlock(props: Properties) : Block(props), HitReceiver, ProjectileReceiver {
	companion object { val FACING = BlockStateProperties.FACING }

	init { registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)) }

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { HitRelayBlock(ModBlocks.props()) }

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(FACING) }

	override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? =
		defaultBlockState().setValue(FACING, ctx.nearestLookingDirection)

	override fun rotate(state: BlockState, rotation: Rotation): BlockState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
	override fun mirror(state: BlockState, mirror: Mirror): BlockState = state.rotate(mirror.getRotation(state.getValue(FACING)))

	override fun onHit(level: ServerLevel, pos: BlockPos, state: BlockState, face: Direction, hit: Hit, depth: Int) {
		val out = state.getValue(FACING)
		if (face != out.opposite) return // принимает только с входной стороны
		level.playSound(null, pos, ModSounds.RELAY_PASS, SoundSource.BLOCKS, 1.0f, 1.0f)
		HitBeam.cast(level, pos.relative(out), out, hit, depth)
	}

	override fun exitFor(state: BlockState, face: Direction, proj: Projectile): Direction? {
		val out = state.getValue(FACING)
		return if (face == out.opposite) out else null
	}

	override fun passSound(): SoundEvent = ModSounds.RELAY_PASS
}
