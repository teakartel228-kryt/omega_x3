package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.logic.Hit
import com.remake.arsenal.logic.HitReceiver
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.util.StringRepresentable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import kotlin.math.min
import kotlin.math.roundToInt

enum class TargetMode(private val serialized: String) : StringRepresentable {
	/** Сила сигнала зависит от урона удара. */
	DAMAGE("damage"),
	/** Каждый удар добавляет +1 к сигналу (медленно спадает). */
	COUNT("count");

	override fun getSerializedName(): String = serialized
}

/** Ударная мишень: принимает удары ближнего боя и выдаёт редстоун-сигнал. Режим и сила меняются палкой отладки (свойства mode/power). */
class StrikeTargetBlock(props: Properties) : Block(props), HitReceiver {
	companion object {
		val POWER = BlockStateProperties.POWER
		val MODE: EnumProperty<TargetMode> = EnumProperty.create("mode", TargetMode::class.java)
	}

	init {
		registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(MODE, TargetMode.DAMAGE))
	}

	override fun codec(): MapCodec<out Block> = BlockCodecs.of { StrikeTargetBlock(ModBlocks.props()) }

	override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
		builder.add(POWER, MODE)
	}

	override fun onHit(level: ServerLevel, pos: BlockPos, state: BlockState, face: Direction, hit: Hit, depth: Int) {
		val power = when (state.getValue(MODE)) {
			TargetMode.DAMAGE -> (hit.damage * 1.5f).roundToInt().coerceIn(1, 15)
			TargetMode.COUNT -> min(15, state.getValue(POWER) + 1)
		}
		level.setBlock(pos, state.setValue(POWER, power), 3)
		level.scheduleTick(pos, this, if (state.getValue(MODE) == TargetMode.DAMAGE) 20 else 40)
		level.playSound(null, pos, ModSounds.TARGET_HIT, SoundSource.BLOCKS, 1.0f, 0.8f + power / 30.0f)
		level.sendParticles(ParticleTypes.CRIT, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 6, 0.3, 0.3, 0.3, 0.1)
	}

	override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
		val p = state.getValue(POWER)
		if (p <= 0) return
		if (state.getValue(MODE) == TargetMode.DAMAGE) {
			level.setBlock(pos, state.setValue(POWER, 0), 3)
		} else {
			level.setBlock(pos, state.setValue(POWER, p - 1), 3)
			if (p - 1 > 0) level.scheduleTick(pos, this, 40)
		}
	}

	override fun isSignalSource(state: BlockState): Boolean = true

	override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int = state.getValue(POWER)

	/** Левая кнопка — это удар по мишени, а не ломание: сломать можно только с Shift (или в креативе с Shift). */
	override fun getDestroyProgress(state: BlockState, player: Player, level: BlockGetter, pos: BlockPos): Float =
		if (player.isShiftKeyDown) super.getDestroyProgress(state, player, level, pos) else 0.0f
}
