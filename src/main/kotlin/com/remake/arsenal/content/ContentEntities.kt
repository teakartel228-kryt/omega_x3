package com.remake.arsenal.content

import com.remake.arsenal.RemakeArsenal
import com.remake.arsenal.entity.BrickShard
import com.remake.arsenal.entity.GrappleHook
import com.remake.arsenal.entity.ThrownMessageBottle
import com.remake.arsenal.entity.ThrownBrick
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

object ContentEntities {
	private fun <T : Entity> reg(name: String, factory: EntityType.EntityFactory<T>, size: Float): EntityType<T> =
		Registry.register(
			BuiltInRegistries.ENTITY_TYPE, RemakeArsenal.id(name),
			EntityType.Builder.of(factory, MobCategory.MISC).sized(size, size).clientTrackingRange(4).updateInterval(10).build(name)
		)

	@JvmField val THROWN_BRICK: EntityType<ThrownBrick> = reg("thrown_brick", EntityType.EntityFactory<ThrownBrick> { t, l -> ThrownBrick(t, l) }, 0.25f)
	@JvmField val THROWN_NETHER_BRICK: EntityType<ThrownBrick> = reg("thrown_nether_brick", EntityType.EntityFactory<ThrownBrick> { t, l -> ThrownBrick(t, l) }, 0.25f)
	@JvmField val BRICK_SHARD: EntityType<BrickShard> = reg("brick_shard", EntityType.EntityFactory<BrickShard> { t, l -> BrickShard(t, l) }, 0.2f)
	@JvmField val NETHER_BRICK_SHARD: EntityType<BrickShard> = reg("nether_brick_shard", EntityType.EntityFactory<BrickShard> { t, l -> BrickShard(t, l) }, 0.2f)

	@JvmField val THROWN_MESSAGE_BOTTLE: EntityType<ThrownMessageBottle> = reg("thrown_message_bottle", EntityType.EntityFactory<ThrownMessageBottle> { t, l -> ThrownMessageBottle(t, l) }, 0.25f)
	@JvmField val GRAPPLE_HOOK: EntityType<GrappleHook> = Registry.register(
		BuiltInRegistries.ENTITY_TYPE, RemakeArsenal.id("grapple_hook"),
		EntityType.Builder.of(EntityType.EntityFactory<GrappleHook> { t, l -> GrappleHook(t, l) }, MobCategory.MISC)
			.sized(0.3f, 0.3f).clientTrackingRange(12).updateInterval(1).noSave().noSummon().build("grapple_hook")
	)

	fun init() {}
}
