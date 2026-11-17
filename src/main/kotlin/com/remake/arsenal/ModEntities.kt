package com.remake.arsenal

import com.remake.arsenal.entity.HelperEntity
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

object ModEntities {
	val HELPER: EntityType<HelperEntity> = Registry.register(
		BuiltInRegistries.ENTITY_TYPE, RemakeArsenal.id("hate_helper"),
		EntityType.Builder.of(EntityType.EntityFactory<HelperEntity> { type, level -> HelperEntity(type, level) }, MobCategory.MISC)
			.sized(0.6f, 1.1f)
			.clientTrackingRange(8)
			.build("hate_helper")
	)

	fun init() {
		FabricDefaultAttributeRegistry.register(HELPER, HelperEntity.createAttributes())
	}
}
