package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.logic.ProjCat
import com.remake.arsenal.logic.ProjClassifier
import com.remake.arsenal.logic.ProjectileReceiver
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty

/**
 * Разделитель снарядов: принимает снаряд с любой грани, определяет категорию и выпускает через свою сторону.
 * combat — стрелы/трезубцы/крюки, thrown — снежки/яйца/жемчуг, special — огненные шары, заряды ветра, зелья, фейерверки.
 * Стороны выхода настраиваются палкой отладки (свойства combat / thrown / special).
 */
class ProjectileSplitterBlock(props: Properties) : Block(props), ProjectileReceiver {
	companion object {
		val COMBAT: DirectionProperty = DirectionProperty.create("combat")
		val THROWN: DirectionProperty = DirectionProperty.create("thrown")
		val SPECIAL: DirectionProperty = DirectionProperty.create("special")
	}

	init {
		registerDefaultState(stateDefinition.any().setValue(COMBAT, Direction.NORTH).setValue(THROWN, Direction.EAST).setValue(SPECIAL, Direction.UP))
	}

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { ProjectileSplitterBlock(ModBlocks.props()) }

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(COMBAT, THROWN, SPECIAL) }

	override fun exitFor(state: BlockState, face: Direction, proj: Projectile): Direction? = when (ProjClassifier.of(proj)) {
		ProjCat.COMBAT -> state.getValue(COMBAT)
		ProjCat.THROWN -> state.getValue(THROWN)
		ProjCat.SPECIAL -> state.getValue(SPECIAL)
	}

	override fun passSound(): SoundEvent = ModSounds.SPLITTER_SORT
}
