package com.remake.arsenal

import com.remake.arsenal.item.ClimbingClawsItem
import com.remake.arsenal.item.HandPistonItem
import com.remake.arsenal.item.HateBagItem
import com.remake.arsenal.item.HookItem
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers

object ModItems {
	private fun <T : Item> reg(name: String, item: T): T =
		Registry.register(BuiltInRegistries.ITEM, RemakeArsenal.id(name), item)

	private fun clawAttributes(): ItemAttributeModifiers =
		ItemAttributeModifiers.builder()
			// 3 удара по 2 урона. Скорость сильно снижена (кулдаун больше, чем в старой версии).
			.add(Attributes.ATTACK_DAMAGE, AttributeModifier(RemakeArsenal.id("claws_damage"), 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_SPEED, AttributeModifier(RemakeArsenal.id("claws_speed"), -3.2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.build()

	@JvmField
	val CLAWS: Item = reg("climbing_claws", ClimbingClawsItem(Item.Properties().durability(450).attributes(clawAttributes())))

	@JvmField
	val HAND_PISTON: Item = reg("hand_piston", HandPistonItem(Item.Properties().durability(300)))

	@JvmField
	val NOTE_HELMET: Item = reg(
		"note_helmet",
		ArmorItem(ModArmorMaterials.NOTE_HELMET, ArmorItem.Type.HELMET, Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(20)))
	)

	@JvmField
	val HATE_BAG: Item = reg("hate_bag", HateBagItem(Item.Properties().stacksTo(1)))

	@JvmField
	val ELECTRO_WINGS: Item = reg(
		"electro_wings",
		ArmorItem(ModArmorMaterials.WINGS, ArmorItem.Type.CHESTPLATE, Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(25)))
	)

	/** Крюк: 0 = базовый, 1..5 = улучшения I..V. */
	@JvmField
	val HOOKS: List<HookItem> = (0..5).map { tier ->
		val props = Item.Properties().stacksTo(1)
		if (tier == 5) props.fireResistant()
		reg(hookName(tier), HookItem(tier, props))
	}

	@JvmField
	val HOOK_UPGRADE_TEMPLATE: Item = reg("hook_upgrade_template", Item(Item.Properties()))

	fun hookName(tier: Int) = if (tier == 0) "grappling_hook" else "grappling_hook_$tier"

	val TAB: CreativeModeTab = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB, RemakeArsenal.id("main"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.remake_arsenal"))
			.icon { ItemStack(HOOKS[0]) }
			.displayItems { _, out ->
				HOOKS.forEach { out.accept(it) }
				out.accept(HOOK_UPGRADE_TEMPLATE)
				out.accept(CLAWS)
				out.accept(HAND_PISTON)
				out.accept(NOTE_HELMET)
				out.accept(HATE_BAG)
				out.accept(ELECTRO_WINGS)
			}
			.build()
	)

	fun init() {}
}
