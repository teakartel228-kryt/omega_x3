package com.remake.arsenal.entity

import com.remake.arsenal.logic.Arsenal
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import java.util.EnumSet
import java.util.UUID

/** Помощник Ненавистного мешка: ~10 HP, ходит рядом с игроком, атакует, бросает голову. */
class HelperEntity(type: EntityType<out PathfinderMob>, level: Level) : PathfinderMob(type, level) {
	var ownerUuid: UUID? = null
	private var headCooldown = 60

	fun owner(): Player? = ownerUuid?.let { level().getPlayerByUUID(it) }

	override fun registerGoals() {
		goalSelector.addGoal(0, FloatGoal(this))
		goalSelector.addGoal(1, MeleeAttackGoal(this, 1.3, true))
		goalSelector.addGoal(2, FollowOwnerGoal(this))
		goalSelector.addGoal(3, LookAtPlayerGoal(this, Player::class.java, 8.0f))
		targetSelector.addGoal(1, OwnerTargetGoal(this))
		targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Monster::class.java, true))
	}

	override fun tick() {
		super.tick()
		if (level().isClientSide) return
		if (headCooldown > 0) headCooldown--
		val t = target
		if (t != null && t.isAlive && headCooldown == 0 && distanceTo(t) in 3.0f..8.0f && hasLineOfSight(t)) throwHead(t)
		if (tickCount % 20 == 0 && tickCount > 40 && owner() == null) discard()
	}

	/** «Бросок головы»: частицы летят к цели и обратно, цель получает урон. */
	private fun throwHead(t: LivingEntity) {
		val sl = level() as ServerLevel
		val from = position().add(0.0, 0.9, 0.0)
		val to = t.position().add(0.0, t.bbHeight * 0.6, 0.0)
		for (i in 0..12) {
			val f = if (i <= 6) i / 6.0 else (12 - i) / 6.0
			val p = from.lerp(to, f)
			sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0)
		}
		t.hurt(damageSources().mobAttack(this), 3.0f)
		sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0f, 0.5f)
		headCooldown = 100
	}

	override fun die(source: DamageSource) {
		super.die(source)
		if (!level().isClientSide) {
			ownerUuid?.let {
				val st = Arsenal.of(it)
				st.summonReadyAt = level().gameTime + 600 // повторный призыв через 30 секунд
				st.helperUuid = null
			}
		}
	}

	override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean = false

	override fun addAdditionalSaveData(tag: CompoundTag) {
		super.addAdditionalSaveData(tag)
		ownerUuid?.let { tag.putUUID("Owner", it) }
	}

	override fun readAdditionalSaveData(tag: CompoundTag) {
		super.readAdditionalSaveData(tag)
		if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner")
	}

	private class FollowOwnerGoal(val mob: HelperEntity) : Goal() {
		init { setFlags(EnumSet.of(Goal.Flag.MOVE)) }
		override fun canUse(): Boolean {
			val o = mob.owner() ?: return false
			return mob.target == null && mob.distanceToSqr(o) > 16.0
		}
		override fun canContinueToUse(): Boolean = canUse()
		override fun tick() {
			val o = mob.owner() ?: return
			if (mob.distanceToSqr(o) > 24.0 * 24.0) mob.teleportTo(o.x, o.y, o.z)
			else mob.navigation.moveTo(o, 1.2)
		}
	}

	private class OwnerTargetGoal(val mob: HelperEntity) : Goal() {
		init { setFlags(EnumSet.of(Goal.Flag.TARGET)) }
		private fun wanted(): LivingEntity? {
			val o = mob.owner() ?: return null
			val t = o.lastHurtMob ?: o.lastHurtByMob
			return if (t != null && t.isAlive && t !== mob) t else null
		}
		override fun canUse(): Boolean = wanted() != null
		override fun start() { mob.target = wanted() }
	}

	companion object {
		@JvmStatic
		fun createAttributes(): AttributeSupplier.Builder =
			net.minecraft.world.entity.Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 24.0)
	}
}
