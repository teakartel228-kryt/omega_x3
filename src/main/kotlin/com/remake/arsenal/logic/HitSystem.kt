package com.remake.arsenal.logic

import com.remake.arsenal.block.StrikeBarrierBlock
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.entity.ThrownBrick
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.entity.projectile.DragonFireball
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.entity.projectile.LargeFireball
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ShulkerBullet
import net.minecraft.world.entity.projectile.SmallFireball
import net.minecraft.world.entity.projectile.Snowball
import net.minecraft.world.entity.projectile.ThrownEgg
import net.minecraft.world.entity.projectile.ThrownEnderpearl
import net.minecraft.world.entity.projectile.ThrownExperienceBottle
import net.minecraft.world.entity.projectile.ThrownPotion
import net.minecraft.world.entity.projectile.WitherSkull
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

/** Удар игрока (по блоку напрямую или переданный по цепочке перехватчиков). */
class Hit(val player: ServerPlayer, val damage: Float)

/** Блок, принимающий удары ближнего боя. [face] — грань, с которой пришёл удар. */
interface HitReceiver {
	fun onHit(level: ServerLevel, pos: BlockPos, state: BlockState, face: Direction, hit: Hit, depth: Int)
}

/** Блок, принимающий снаряды. [exitFor] — куда выпустить снаряд, пришедший на грань [face]; null — блок не принимает. */
interface ProjectileReceiver {
	fun exitFor(state: BlockState, face: Direction, proj: Projectile): Direction?
	fun passSound(): SoundEvent
}

enum class ProjCat { COMBAT, THROWN, SPECIAL }

object ProjClassifier {
	fun of(p: Projectile): ProjCat = when (p) {
		// Категория 1: стрелы, трезубцы (наследуют AbstractArrow), крюки (удочка)
		is AbstractArrow, is FishingHook -> ProjCat.COMBAT
		// Категория 3 проверяется до «метательных»: зелья тоже метательные, но особые
		is ThrownPotion, is FireworkRocketEntity, is LargeFireball, is SmallFireball, is AbstractWindCharge,
		is DragonFireball, is WitherSkull, is ShulkerBullet -> ProjCat.SPECIAL
		// Категория 2: снежки, яйца, эндер-жемчуг (+ мод. кирпичи и пузырёк опыта)
		is Snowball, is ThrownEgg, is ThrownEnderpearl, is ThrownExperienceBottle, is ThrownBrick -> ProjCat.THROWN
		else -> ProjCat.SPECIAL
	}
}

object HitBeam {
	const val RANGE = 12
	const val MAX_DEPTH = 16

	/** Пускает «удар» от клетки [from] в сторону [dir]: бьёт первую цель, проходит сквозь барьеры, передаёт дальше другим приёмникам. */
	fun cast(level: ServerLevel, from: BlockPos, dir: Direction, hit: Hit, depth: Int) {
		if (depth > MAX_DEPTH) return
		var p = from
		for (i in 0 until RANGE) {
			val st = level.getBlockState(p)
			val b = st.block
			if (b is HitReceiver) { b.onHit(level, p, st, dir.opposite, hit, depth + 1); return }
			val target = level.getEntitiesOfClass(LivingEntity::class.java, AABB(p)) { it !== hit.player && it.isAlive && it.isAttackable }.firstOrNull()
			if (target != null) {
				target.hurt(level.damageSources().playerAttack(hit.player), hit.damage)
				target.knockback(0.5, -dir.stepX.toDouble(), -dir.stepZ.toDouble())
				level.sendParticles(ParticleTypes.CRIT, target.x, target.y + target.bbHeight / 2, target.z, 8, 0.2, 0.2, 0.2, 0.2)
				return
			}
			if (!(b is StrikeBarrierBlock || st.getCollisionShape(level, p).isEmpty)) return
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + 0.5, p.y + 0.5, p.z + 0.5, 1, 0.1, 0.1, 0.1, 0.0)
			p = p.relative(dir)
		}
	}
}

object ProjectileRouter {
	/** Вызывается из миксина при попадании снаряда в блок. true — попадание отменено, снаряд перенаправлен. */
	@JvmStatic
	fun tryRoute(proj: Projectile, hit: BlockHitResult): Boolean {
		val level = proj.level()
		val startPos = hit.blockPos
		val startState = level.getBlockState(startPos)
		val first = startState.block as? ProjectileReceiver ?: return false
		var exit = first.exitFor(startState, hit.direction, proj) ?: return false
		var cur = startPos
		var hops = 0
		var next: BlockPos
		while (true) {
			next = cur.relative(exit)
			val ns = level.getBlockState(next)
			val nb = ns.block
			if (nb is ProjectileReceiver && hops < 8) {
				val e2 = nb.exitFor(ns, exit.opposite, proj)
				if (e2 != null) { cur = next; exit = e2; hops++; continue }
			}
			if (!ns.getCollisionShape(level, next).isEmpty) return false // выход заблокирован — обычное попадание
			break
		}
		if (level.isClientSide) return true // двигает снаряд только сервер
		val speed = max(proj.deltaMovement.length(), 0.35)
		val c = Vec3.atCenterOf(next)
		proj.setPos(c.x, c.y, c.z)
		proj.deltaMovement = Vec3(exit.stepX * speed, exit.stepY * speed, exit.stepZ * speed)
		proj.hasImpulse = true
		proj.hurtMarked = true
		(level as ServerLevel).playSound(null, startPos, first.passSound(), SoundSource.BLOCKS, 1.0f, 1.0f)
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 6, 0.15, 0.15, 0.15, 0.05)
		return true
	}
}
