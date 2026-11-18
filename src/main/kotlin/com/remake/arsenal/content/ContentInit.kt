package com.remake.arsenal.content

import net.fabricmc.api.ModInitializer

/** Вторая точка входа: новые блоки, кирпичи и звуки. Старый код мода не затронут. */
object ContentInit : ModInitializer {
	override fun onInitialize() {
		ModSounds.init()
		ContentItems.init()
		ModBlocks.init()
		ContentEntities.init()
		ContentEvents.init()
	}
}
