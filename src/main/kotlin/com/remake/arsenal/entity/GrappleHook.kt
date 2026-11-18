package com.remake.arsenal.entity

import com.remake.arsenal.logic.Arsenal
import com.remake.arsenal.logic.HookLogic
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

/**
 * Крюк-кошка как настоящая сущность: летит от игрока, цепляется за блок (цепь тянется от руки до крюка),
 * при промахе или после использования возвращается обратно к игроку.
 */
class GrappleHook(type: EntityType<out GrappleHook>, level: Level) : Projectile(type, level) {
	companion object {
		@JvmField val DATA_STATE: EntityDataAccessor<Int> = SynchedEntityData.defineId(GrappleHook::class.java, EntityDataSerializers.INT)
		@JvmField val DATA_TIER: EntityDataAccessor<Int> = SynchedEntityData.defineId(GrappleHook::class.java, EntityDataSerializers.INT)

		const val FLYING = 0
		const val STUCK = 1
		const val RETRACT = 2

		const val SPEED = 2.4
		const val RETRACT_SPEED = 2.8
	}

	/** Параметры выстрела (нужны только серверу). */
	var maxRange = 18.0
	var cap = 1
	var shots = 1
	var quick = 0
	var shotDir: Vec3 = Vec3.ZERO
	var hand: InteractionHand = InteractionHand.MAIN_HAND
	private var traveled = 0.0

	init {
		noCulling = true
	}

	var hookState: Int
		get() = entityData.get(DATA_STATE)
		set(v) { entityData.set(DATA_STATE, v) }

	var hookTier: Int
		get() = entityData.get(DATA_TIER)
		set(v) { entityData.set(DATA_TIER, v) }

	override fun defineSynchedData(builder: SynchedEntityData.Builder) {
		builder.define(DATA_STATE, FLYING)
		builder.define(DATA_TIER, 0)
	}

	/** Направить крюк вдоль вектора [dir] (поворот модели совпадает с полётом). */
	fun launch(dir: Vec3) {
		shotDir = dir
		deltaMovement = dir.scale(SPEED)
		faceMotion(deltaMovement)
		yRotO = yRot
		xRotO = xRot
	}

	private fun faceMotion(d: Vec3) {
		if (d.lengthSqr() < 1.0e-4) return
		yRot = (Mth.atan2(d.x, d.z) * 57.29578).toFloat()
		xRot = (Mth.atan2(d.y, d.horizontalDistance()) * 57.29578).toFloat()
	}

	override fun tick() {
		super.tick()
		val lvl = level()
		if (lvl.isClientSide) {
			if (hookState != STUCK) {
				val d = deltaMovement
				setPos(x + d.x, y + d.y, z + d.z)
				faceMotion(d)
			}
			return
		}
		val player = owner as? ServerPlayer
		if (player == null || !player.isAlive || player.isSpectator || player.level() !== lvl || tickCount > 600) {
			discard()
			return
		}
		val sl = lvl as ServerLevel
		val st = Arsenal.of(player)
		when (hookState) {
			FLYING -> flyStep(sl, player)
			STUCK -> {
				val linked = st.anchors.any { it.hookId == id }
				if (!linked || distanceTo(player) > maxRange + 8.0) startRetract()
			}
			RETRACT -> retractStep(player)
		}
	}

	private fun startRetract() {
		hookState = RETRACT
	}

	private fun canTarget(e: Entity, player: ServerPlayer): Boolean =
		e !== player && !e.isSpectator && e.isAlive && ((e is Projectile && e !is GrappleHook) || e is LivingEntity)

	private fun flyStep(level: ServerLevel, player: ServerPlayer) {
		val from = position()
		val v = deltaMovement
		val to = from.add(v)

		val blockHit = level.clip(ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
		val entHit = ProjectileUtil.getEntityHitResult(
			level, this, from, to, AABB(from, to).inflate(0.6),
			Predicate<Entity> { canTarget(it, player) }
		)
		val blockDist = if (blockHit.type == HitResult.Type.MISS) Double.MAX_VALUE else blockHit.location.distanceToSqr(from)
		val entDist = entHit?.location?.distanceToSqr(from) ?: Double.MAX_VALUE

		if (entHit != null && entDist < blockDist) {
			setPos(entHit.location)
			HookLogic.onHookHitEntity(this, player, entHit.entity)
			startRetract()
			return
		}
		if (blockHit.type == HitResult.Type.BLOCK) {
			setPos(blockHit.location)
			if (HookLogic.onHookHitBlock(this, player, blockHit)) {
				hookState = STUCK
				deltaMovement = Vec3.ZERO
			} else {
				startRetract()
			}
			return
		}
		setPos(to)
		traveled += v.length()
		faceMotion(v)
		if (traveled >= maxRange) startRetract()
	}

	private fun retractStep(player: ServerPlayer) {
		val target = player.position().add(0.0, player.eyeHeight * 0.6, 0.0)
		val d = target.subtract(position())
		val len = d.length()
		if (len < 1.6) {
			discard()
			return
		}
		val step = d.scale(1.0 / len).scale(Math.min(RETRACT_SPEED, len))
		deltaMovement = step
		setPos(position().add(step))
		faceMotion(step)
	}
}
