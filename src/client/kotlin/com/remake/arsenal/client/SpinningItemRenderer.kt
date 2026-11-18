package com.remake.arsenal.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ItemSupplier
import net.minecraft.world.item.ItemDisplayContext

/** Летящий предмет в виде настоящей 3D-модели, которая кувыркается в полёте (вместо плоского спрайта). */
class SpinningItemRenderer<T>(
	ctx: EntityRendererProvider.Context,
	private val scale: Float,
	private val spin: Float
) : EntityRenderer<T>(ctx) where T : Entity, T : ItemSupplier {
	private val itemRenderer = ctx.itemRenderer

	override fun getTextureLocation(entity: T): ResourceLocation = TextureAtlas.LOCATION_BLOCKS

	override fun render(e: T, yaw: Float, partial: Float, pose: PoseStack, buf: MultiBufferSource, light: Int) {
		pose.pushPose()
		pose.translate(0.0, e.bbHeight / 2.0, 0.0)
		val t = e.tickCount + partial
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.yRot)))
		pose.mulPose(Axis.ZP.rotationDegrees(t * spin))
		pose.mulPose(Axis.XP.rotationDegrees(t * spin * 0.35f))
		pose.scale(scale, scale, scale)
		itemRenderer.renderStatic(e.item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buf, e.level(), e.id)
		pose.popPose()
		super.render(e, yaw, partial, pose, buf, light)
	}
}
