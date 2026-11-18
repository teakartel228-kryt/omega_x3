package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.logic.HitReceiver
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Барьер для ударов. Коллизия — полный блок (игроки, мобы, предметы и снаряды не проходят),
 * а «контур» (на него смотрит луч прицеливания) — только каркас по рёбрам. Поэтому прицел и удары ближнего боя
 * проходят сквозь центр барьера и достают до цели за ним.
 */
class StrikeBarrierBlock(props: Properties) : Block(props) {
	companion object {
		val FRAME: VoxelShape = run {
			var s = Shapes.empty()
			for (x in intArrayOf(0, 14)) for (z in intArrayOf(0, 14)) s = Shapes.or(s, Block.box(x.toDouble(), 0.0, z.toDouble(), x + 2.0, 16.0, z + 2.0))
			for (y in intArrayOf(0, 14)) {
				for (z in intArrayOf(0, 14)) s = Shapes.or(s, Block.box(2.0, y.toDouble(), z.toDouble(), 14.0, y + 2.0, z + 2.0))
				for (x in intArrayOf(0, 14)) s = Shapes.or(s, Block.box(x.toDouble(), y.toDouble(), 2.0, x + 2.0, y + 2.0, 14.0))
			}
			s
		}
	}

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { StrikeBarrierBlock(ModBlocks.barrierProps()) }

	override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = FRAME
	override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = Shapes.block()
	override fun getVisualShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = Shapes.empty()
	override fun getOcclusionShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape = Shapes.empty()
	override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean = true
	override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos): Float = 1.0f
	override fun skipRendering(state: BlockState, adjacentState: BlockState, direction: Direction): Boolean = adjacentState.`is`(this)
}
