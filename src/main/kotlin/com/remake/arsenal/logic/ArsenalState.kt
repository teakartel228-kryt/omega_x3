package com.remake.arsenal.logic

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Anchor(val pos: Vec3, val expiresAt: Long)

/** Серверное состояние способностей одного игрока (хранится в памяти). */
class ArsenalState {
	// электро-крылья
	var energy = 3.0f
	var dashTicks = 0
	var dashDir: Vec3 = Vec3.ZERO

	// крюк
	val anchors = mutableListOf<Anchor>()
	var hookPower = 0
	var emergencyUntil = 0L
	var chainHp = -1.0f

	// шлем / мешок
	var sonicReadyAt = 0L
	var lightningReadyAt = 0L
	var summonReadyAt = 0L
	var helperUuid: UUID? = null
}

object Arsenal {
	val states = ConcurrentHashMap<UUID, ArsenalState>()

	fun of(player: ServerPlayer): ArsenalState = states.computeIfAbsent(player.uuid) { ArsenalState() }
	fun of(uuid: UUID): ArsenalState = states.computeIfAbsent(uuid) { ArsenalState() }
}
