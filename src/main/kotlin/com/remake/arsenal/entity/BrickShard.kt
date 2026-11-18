package com.remake.arsenal.entity

import com.remake.arsenal.content.ContentEntities
import com.remake.arsenal.content.ContentItems
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.mixin.ItemEntityAccessor
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/** Осколок кирпича: летит, наносит урон, после падения превращается в предмет, который можно подобрать (исчезает через ~20 с). */
class BrickShard : ThrowableItemProjectile {
	constructor(type: EntityType<out BrickShard>, level: Level) : super(type, level)
	constructor(type: EntityType<out BrickShard>, owner: LivingEntity, level: Level) : super(type, owner, level)

	val isNether: Boolean get() = type == ContentEntities.NETHER_BRICK_SHARD

	override fun getDefaultItem(): Item = if (isNether) ContentItems.NETHER_BRICK_SHARD else ContentItems.BRICK_SHARD

	override fun onHitEntity(result: EntityHitResult) {
		super.onHitEntity(result)
		result.entity.hurt(damageSources().thrown(this, owner), if (isNether) 3.0f else 2.0f)
	}

	override fun onHit(result: HitResult) {
		super.onHit(result)
		val lvl = level()
		if (lvl is ServerLevel) land(lvl)
	}

	private fun land(level: ServerLevel) {
		val item = ItemEntity(level, x, y, z, ItemStack(getDefaultItem()))
		(item as ItemEntityAccessor).`remake$setAge`(6000 - 400) // исчезнет через 400 тиков (~20 секунд)
		item.setDefaultPickUpDelay()
		item.deltaMovement = Vec3((random.nextDouble() - 0.5) * 0.06, 0.12, (random.nextDouble() - 0.5) * 0.06)
		level.addFreshEntity(item)
		level.playSound(null, x, y, z, ModSounds.SHARD_LAND, SoundSource.NEUTRAL, 0.6f, 0.9f + random.nextFloat() * 0.3f)
		discard()
	}
}
