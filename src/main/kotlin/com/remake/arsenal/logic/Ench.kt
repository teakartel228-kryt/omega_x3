package com.remake.arsenal.logic

import com.remake.arsenal.RemakeArsenal
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level

object Ench {
	private fun key(name: String): ResourceKey<Enchantment> = ResourceKey.create(Registries.ENCHANTMENT, RemakeArsenal.id(name))

	val MULTISHOT = key("hook_multishot")
	val QUICK_RECHARGE = key("hook_quick_recharge")
	val POWER = key("hook_power")
	val ARMS = key("hook_arms")
	val REDSTONE = key("redstone_hook")
	val DEFENSE = key("defense_mechanism")

	fun level(level: Level, stack: ItemStack, key: ResourceKey<Enchantment>): Int {
		if (stack.isEmpty) return 0
		val holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null) ?: return 0
		return EnchantmentHelper.getItemEnchantmentLevel(holder, stack)
	}
}
