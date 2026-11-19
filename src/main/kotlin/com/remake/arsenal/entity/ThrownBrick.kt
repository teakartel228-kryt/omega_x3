package com.remake.arsenal.entity

import com.remake.arsenal.content.ContentEntities
import com.remake.arsenal.content.ContentItems
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.logic.Ench
import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.phys.AABB
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/** Брошенный кирпич (обычный или незерский): урон при попадании, затем разлетается на осколки. */
class ThrownBrick : ThrowableItemProjectile {
	constructor(type: EntityType<out ThrownBrick>, level: Level) : super(type, level)
	constructor(type: EntityType<out ThrownBrick>, owner: LivingEntity, level: Level) : super(type, owner, level)

	private var fire = 0
	private var explosive = 0
	private var frost = 0
	private var venom = 0
	private var storm = 0

	/** Зачарования с кирпича, который бросили: срабатывают, когда он разбивается. */
	fun readEnchants(level: Level, stack: ItemStack) {
		fire = Ench.level(level, stack, Ench.BRICK_FIRE)
		explosive = Ench.level(level, stack, Ench.BRICK_EXPLOSIVE)
		frost = Ench.level(level, stack, Ench.BRICK_FROST)
		venom = Ench.level(level, stack, Ench.BRICK_VENOM)
		storm = Ench.level(level, stack, Ench.BRICK_STORM)
		if (fire + explosive + frost + venom + storm > 0) {
			val s = ItemStack(getDefaultItem())
			s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			item = s
		}
	}

	private fun releaseEnchants(level: ServerLevel, at: Vec3) {
		val near = level.getEntitiesOfClass(LivingEntity::class.java, AABB(at, at).inflate(2.5))
		if (fire > 0) {
			near.forEach { it.setRemainingFireTicks(60 * fire) }
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.05)
		}
		if (frost > 0) {
			near.forEach {
				it.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80 + 60 * frost, frost))
				it.ticksFrozen = it.ticksRequiredToFreeze + 40 * frost
			}
			level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 24, 0.4, 0.3, 0.4, 0.05)
		}
		if (venom > 0) {
			val c = AreaEffectCloud(level, at.x, at.y, at.z)
			c.owner = owner as? LivingEntity
			c.radius = 2.0f
			c.duration = 80 + 60 * venom
			c.addEffect(MobEffectInstance(MobEffects.POISON, 100, venom - 1))
			level.addFreshEntity(c)
		}
		if (storm > 0) {
			val bolt = EntityType.LIGHTNING_BOLT.create(level)
			if (bolt != null) {
				bolt.moveTo(at)
				level.addFreshEntity(bolt)
			}
		}
		if (explosive > 0) {
			level.explode(owner, at.x, at.y, at.z, 1.0f + 0.7f * explosive, Level.ExplosionInteraction.NONE)
		}
	}

	val isNether: Boolean get() = type == ContentEntities.THROWN_NETHER_BRICK

	override fun getDefaultItem(): Item = if (isNether) ContentItems.NETHER_BRICK_PROJECTILE else ContentItems.BRICK_PROJECTILE

	override fun onHitEntity(result: EntityHitResult) {
		super.onHitEntity(result)
		result.entity.hurt(damageSources().thrown(this, owner), if (isNether) 5.0f else 4.0f)
	}

	override fun onHit(result: HitResult) {
		super.onHit(result)
		val lvl = level()
		if (lvl is ServerLevel) {
			shatter(lvl, result.location)
			discard()
		}
	}

	private fun shatter(level: ServerLevel, at: Vec3) {
		releaseEnchants(level, at)
		val shardType = if (isNether) ContentEntities.NETHER_BRICK_SHARD else ContentEntities.BRICK_SHARD
		repeat(4 + random.nextInt(3)) {
			val shard = BrickShard(shardType, level)
			shard.owner = owner
			shard.setPos(at.x, at.y + 0.1, at.z)
			val a = random.nextDouble() * Math.PI * 2
			val sp = 0.25 + random.nextDouble() * 0.25
			shard.deltaMovement = Vec3(cos(a) * sp, 0.2 + random.nextDouble() * 0.25, sin(a) * sp)
			level.addFreshEntity(shard)
		}
		val breakSound = if (isNether) ModSounds.NETHER_BRICK_BREAK else ModSounds.BRICK_BREAK
		level.playSound(null, at.x, at.y, at.z, breakSound, SoundSource.NEUTRAL, 1.0f, 0.9f + random.nextFloat() * 0.2f)
		level.playSound(null, at.x, at.y, at.z, ModSounds.SHARD_SCATTER, SoundSource.NEUTRAL, 0.7f, 1.0f)
		val dust = ItemStack(if (isNether) Items.NETHER_BRICK else Items.BRICK)
		level.sendParticles(ItemParticleOption(ParticleTypes.ITEM, dust), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.08)
	}
}
