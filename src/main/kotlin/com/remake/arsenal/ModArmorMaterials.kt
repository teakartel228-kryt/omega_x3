package com.remake.arsenal

import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import java.util.EnumMap
import java.util.function.Supplier

object ModArmorMaterials {
	private fun defense(helmet: Int, chest: Int): EnumMap<ArmorItem.Type, Int> {
		val m = EnumMap<ArmorItem.Type, Int>(ArmorItem.Type::class.java)
		m[ArmorItem.Type.BOOTS] = 0
		m[ArmorItem.Type.LEGGINGS] = 0
		m[ArmorItem.Type.CHESTPLATE] = chest
		m[ArmorItem.Type.HELMET] = helmet
		m[ArmorItem.Type.BODY] = 0
		return m
	}

	private fun reg(name: String, helmet: Int, chest: Int): Holder<ArmorMaterial> =
		Registry.registerForHolder(
			BuiltInRegistries.ARMOR_MATERIAL, RemakeArsenal.id(name),
			ArmorMaterial(
				defense(helmet, chest), 12, SoundEvents.ARMOR_EQUIP_IRON,
				Supplier { Ingredient.of(Items.IRON_INGOT) },
				listOf(ArmorMaterial.Layer(RemakeArsenal.id(name))), 0.0f, 0.0f
			)
		)

	// Прочность шлема: множитель 20 -> 260 (железо 165, алмаз 363).
	val NOTE_HELMET: Holder<ArmorMaterial> = reg("note_helmet", 2, 0)
	val WINGS: Holder<ArmorMaterial> = reg("electro_wings", 0, 3)
}
