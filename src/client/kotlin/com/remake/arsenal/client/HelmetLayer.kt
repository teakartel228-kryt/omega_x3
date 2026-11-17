package com.remake.arsenal.client

import com.mojang.blaze3d.vertex.PoseStack
import com.remake.arsenal.ModItems
import com.remake.arsenal.RemakeArsenal
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EquipmentSlot

/** 3D-детали Нотного шлема: два динамика по бокам и нота-антенна сверху. */
class HelmetLayer(
	parent: RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
	set: EntityModelSet
) : RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(parent) {
	private val root = set.bakeLayer(LAYER)

	override fun render(
		poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, player: AbstractClientPlayer,
		limbSwing: Float, limbSwingAmount: Float, partialTick: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float
	) {
		if (!player.getItemBySlot(EquipmentSlot.HEAD).`is`(ModItems.NOTE_HELMET)) return
		poseStack.pushPose()
		parentModel.head.translateAndRotate(poseStack)
		root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY)
		poseStack.popPose()
	}

	companion object {
		val LAYER = ModelLayerLocation(RemakeArsenal.id("note_helmet"), "main")
		private val TEXTURE = RemakeArsenal.id("textures/entity/note_helmet.png")

		fun createLayer(): LayerDefinition {
			val mesh = MeshDefinition()
			val root = mesh.root
			root.addOrReplaceChild("speaker_l", CubeListBuilder.create().texOffs(0, 0).addBox(5f, -7f, -3f, 2f, 6f, 6f), PartPose.ZERO)
			root.addOrReplaceChild("speaker_r", CubeListBuilder.create().texOffs(0, 0).addBox(-7f, -7f, -3f, 2f, 6f, 6f), PartPose.ZERO)
			root.addOrReplaceChild(
				"note",
				CubeListBuilder.create().texOffs(16, 0).addBox(-0.5f, -16f, -0.5f, 1f, 7f, 1f)
					.texOffs(16, 8).addBox(-2.5f, -11f, -1f, 3f, 2f, 2f)
					.texOffs(16, 12).addBox(0.5f, -16f, -0.5f, 2f, 1f, 1f),
				PartPose.ZERO
			)
			return LayerDefinition.create(mesh, 32, 16)
		}
	}
}
