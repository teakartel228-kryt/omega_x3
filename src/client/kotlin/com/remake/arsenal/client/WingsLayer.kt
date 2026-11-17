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

/** 3D-крылья на спине: сложены на земле, раскрываются в падении/скольжении. */
class WingsLayer(
	parent: RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
	set: EntityModelSet
) : RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(parent) {
	private val root = set.bakeLayer(LAYER)
	private val left = root.getChild("left_wing")
	private val right = root.getChild("right_wing")

	override fun render(
		poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, player: AbstractClientPlayer,
		limbSwing: Float, limbSwingAmount: Float, partialTick: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float
	) {
		if (!player.getItemBySlot(EquipmentSlot.CHEST).`is`(ModItems.ELECTRO_WINGS)) return
		val gliding = !player.onGround() && player.deltaMovement.y < -0.05
		val spread = if (gliding) 1.0f else 0.25f
		left.yRot = -0.35f - 0.55f * spread
		right.yRot = 0.35f + 0.55f * spread
		left.zRot = -0.15f * spread
		right.zRot = 0.15f * spread

		poseStack.pushPose()
		parentModel.body.translateAndRotate(poseStack)
		root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY)
		poseStack.popPose()
	}

	companion object {
		val LAYER = ModelLayerLocation(RemakeArsenal.id("electro_wings"), "main")
		private val TEXTURE = RemakeArsenal.id("textures/entity/electro_wings.png")

		fun createLayer(): LayerDefinition {
			val mesh = MeshDefinition()
			val root = mesh.root
			root.addOrReplaceChild(
				"left_wing",
				CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0f, 14f, 1f, 3f)
					.texOffs(0, 4).addBox(0f, 3f, 0f, 11f, 1f, 3f)
					.texOffs(0, 8).addBox(0f, 6f, 0f, 8f, 1f, 3f)
					.texOffs(0, 12).addBox(0f, 9f, 0f, 5f, 1f, 3f),
				PartPose.offset(1.5f, 1f, 2f)
			)
			root.addOrReplaceChild(
				"right_wing",
				CubeListBuilder.create().texOffs(0, 0).addBox(-14f, 0f, 0f, 14f, 1f, 3f)
					.texOffs(0, 4).addBox(-11f, 3f, 0f, 11f, 1f, 3f)
					.texOffs(0, 8).addBox(-8f, 6f, 0f, 8f, 1f, 3f)
					.texOffs(0, 12).addBox(-5f, 9f, 0f, 5f, 1f, 3f),
				PartPose.offset(-1.5f, 1f, 2f)
			)
			return LayerDefinition.create(mesh, 64, 32)
		}
	}
}
