package com.remake.arsenal.client

import com.remake.arsenal.content.ContentEntities
import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.entity.BrickShard
import com.remake.arsenal.entity.ThrownBrick
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ThrownItemRenderer

object ContentClient : ClientModInitializer {
	override fun onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STRIKE_BARRIER, RenderType.translucent())
		EntityRendererRegistry.register(ContentEntities.THROWN_BRICK) { ctx -> ThrownItemRenderer<ThrownBrick>(ctx) }
		EntityRendererRegistry.register(ContentEntities.THROWN_NETHER_BRICK) { ctx -> ThrownItemRenderer<ThrownBrick>(ctx) }
		EntityRendererRegistry.register(ContentEntities.BRICK_SHARD) { ctx -> ThrownItemRenderer<BrickShard>(ctx) }
		EntityRendererRegistry.register(ContentEntities.NETHER_BRICK_SHARD) { ctx -> ThrownItemRenderer<BrickShard>(ctx) }
	}
}
