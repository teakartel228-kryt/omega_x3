package com.remake.arsenal.logic

import com.remake.arsenal.content.ContentItems
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/** Сменные головки крюка. Хранятся в custom_data крюка под ключом "head". */
enum class HookHead(val key: String) {
	NONE("none"), MACE("mace"), TRIDENT("trident"), MAGNET("magnet");

	companion object {
		fun byKey(k: String?): HookHead = values().firstOrNull { it.key == k } ?: NONE
		fun byIndex(i: Int): HookHead = values().getOrElse(i) { NONE }
	}
}

object HookHeads {
	fun of(stack: ItemStack): HookHead =
		HookHead.byKey(stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString("head"))

	fun set(stack: ItemStack, head: HookHead) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack) { it.putString("head", head.key) }
	}

	/** Предмет-головка, соответствующий типу (для возврата при замене). */
	fun item(head: HookHead): Item? = when (head) {
		HookHead.MACE -> ContentItems.MACE_HEAD
		HookHead.TRIDENT -> ContentItems.TRIDENT_HEAD
		HookHead.MAGNET -> ContentItems.MAGNETITE_HEAD
		HookHead.NONE -> null
	}
}
