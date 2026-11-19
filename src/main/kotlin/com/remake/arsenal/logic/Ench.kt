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
	val CLAWS_BLEED = key("claws_bleed")
	val CLAWS_VAMPIRE = key("claws_vampire")
	val PISTON_OVERDRIVE = key("piston_overdrive")
	val PISTON_SHOCKWAVE = key("piston_shockwave")
	val BRICK_FIRE = key("brick_fire")
	val BRICK_EXPLOSIVE = key("brick_explosive")
	val BRICK_FROST = key("brick_frost")
	val BRICK_VENOM = key("brick_venom")
	val BRICK_STORM = key("brick_storm")

	fun level(level: Level, stack: ItemStack, key: ResourceKey<Enchantment>): Int {
		if (stack.isEmpty) return 0
		val holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null) ?: return 0
		return EnchantmentHelper.getItemEnchantmentLevel(holder, stack)
	}
}
