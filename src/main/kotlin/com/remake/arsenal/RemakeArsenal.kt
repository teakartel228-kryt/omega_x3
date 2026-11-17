package com.remake.arsenal

import com.remake.arsenal.logic.ArsenalEvents
import com.remake.arsenal.net.ModNetworking
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object RemakeArsenal : ModInitializer {
	const val MOD_ID: String = "remake_arsenal"
	val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModItems.init()
		ModEntities.init()
		ModNetworking.init()
		ArsenalEvents.init()
		LOGGER.info("[REMAKE] Arsenal loaded")
	}

	fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
