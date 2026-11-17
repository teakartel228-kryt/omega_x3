package com.remake.arsenal.logic

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

class Pick(val entity: Entity?, val block: BlockHitResult?, val from: Vec3, val to: Vec3)

object Rays {
	/** Бросает луч из глаз игрока: возвращает ближайшее — сущность или блок. */
	fun pick(player: Player, dir: Vec3, range: Double, filter: (Entity) -> Boolean): Pick {
		val level = player.level()
		val eye = player.eyePosition
		val end = eye.add(dir.scale(range))
		val blockHit = level.clip(ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
		val blockMissed = blockHit.type == HitResult.Type.MISS
		val blockDist = if (blockMissed) Double.MAX_VALUE else blockHit.location.distanceToSqr(eye)
		val entHit = ProjectileUtil.getEntityHitResult(
			level, player, eye, end, AABB(eye, end).inflate(1.0),
			Predicate<Entity> { it !== player && !it.isSpectator && it.isAlive && filter(it) }
		)
		if (entHit != null && entHit.location.distanceToSqr(eye) < blockDist) {
			return Pick(entHit.entity, null, eye, entHit.location)
		}
		return if (blockMissed) Pick(null, null, eye, end) else Pick(null, blockHit, eye, blockHit.location)
	}
}
