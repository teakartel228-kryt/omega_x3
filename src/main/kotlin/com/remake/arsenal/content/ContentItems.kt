package com.remake.arsenal.content

import com.remake.arsenal.RemakeArsenal
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item

object ContentItems {
	private fun reg(name: String, item: Item): Item = Registry.register(BuiltInRegistries.ITEM, RemakeArsenal.id(name), item)

	@JvmField val BRICK_SHARD: Item = reg("brick_shard", Item(Item.Properties()))
	@JvmField val NETHER_BRICK_SHARD: Item = reg("nether_brick_shard", Item(Item.Properties()))
	/** «Внешний вид» летящих кирпичей (в обычной игре не выдаются — кидаются настоящие кирпичи). */
	@JvmField val BRICK_PROJECTILE: Item = reg("brick_projectile", Item(Item.Properties().stacksTo(16)))
	@JvmField val NETHER_BRICK_PROJECTILE: Item = reg("nether_brick_projectile", Item(Item.Properties().stacksTo(16)))

	fun init() {}
}
