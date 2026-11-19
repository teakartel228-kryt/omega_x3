package com.remake.arsenal.content

import com.remake.arsenal.RemakeArsenal
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import com.remake.arsenal.item.HookHeadItem
import com.remake.arsenal.logic.HookHead
import net.minecraft.world.item.Item

object ContentItems {
	private fun reg(name: String, item: Item): Item = Registry.register(BuiltInRegistries.ITEM, RemakeArsenal.id(name), item)

	@JvmField val BRICK_SHARD: Item = reg("brick_shard", Item(Item.Properties()))
	@JvmField val NETHER_BRICK_SHARD: Item = reg("nether_brick_shard", Item(Item.Properties()))
	/** «Внешний вид» летящих кирпичей (в обычной игре не выдаются — кидаются настоящие кирпичи). */
	@JvmField val BRICK_PROJECTILE: Item = reg("brick_projectile", Item(Item.Properties().stacksTo(16)))
	@JvmField val NETHER_BRICK_PROJECTILE: Item = reg("nether_brick_projectile", Item(Item.Properties().stacksTo(16)))

	@JvmField val MACE_HEAD: Item = reg("mace_head", HookHeadItem(HookHead.MACE, Item.Properties().stacksTo(1)))
	@JvmField val TRIDENT_HEAD: Item = reg("trident_head", HookHeadItem(HookHead.TRIDENT, Item.Properties().stacksTo(1)))
	@JvmField val MAGNETITE_HEAD: Item = reg("magnetite_head", HookHeadItem(HookHead.MAGNET, Item.Properties().stacksTo(1)))

	/** «Голова» летящего крюка: только модель для сущности, в игре не выдаётся. */
	@JvmField val HOOK_HEADS: List<Item> = (0..5).map { reg("hook_head_$it", Item(Item.Properties())) }

	fun init() {}
}
