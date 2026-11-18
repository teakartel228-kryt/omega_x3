package com.remake.arsenal.entity

import com.remake.arsenal.content.ContentEntities
import com.remake.arsenal.content.ContentItems
import com.remake.arsenal.content.ModSounds
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
