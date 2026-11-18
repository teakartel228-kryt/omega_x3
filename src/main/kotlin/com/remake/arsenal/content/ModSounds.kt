package com.remake.arsenal.content

import com.remake.arsenal.RemakeArsenal
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent

/** Собственные звуки (файлы .ogg в assets/remake_arsenal/sounds, описание в sounds.json). */
object ModSounds {
	private fun reg(name: String): SoundEvent {
		val id = RemakeArsenal.id(name)
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
	}

	@JvmField val TARGET_HIT = reg("target_hit")
	@JvmField val BARRIER_PASS = reg("barrier_pass")
	@JvmField val RELAY_PASS = reg("relay_pass")
	@JvmField val SPLITTER_SORT = reg("splitter_sort")
	@JvmField val SMART_LOAD = reg("smart_load")
	@JvmField val SMART_ERROR = reg("smart_error")
	@JvmField val BRICK_THROW = reg("brick_throw")
	@JvmField val BRICK_BREAK = reg("brick_break")
	@JvmField val NETHER_BRICK_BREAK = reg("nether_brick_break")
	@JvmField val SHARD_SCATTER = reg("shard_scatter")
	@JvmField val SHARD_LAND = reg("shard_land")

	fun init() {}
}
