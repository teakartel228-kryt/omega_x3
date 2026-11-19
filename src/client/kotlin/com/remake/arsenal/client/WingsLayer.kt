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
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot

/** Меха-крылья: ранец с двигателями и два крыла из пластин; на земле сложены, в падении раскрываются веером. */
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
		val flutter = Mth.sin(ageInTicks * 0.15f) * 0.03f
		val spread = (if (gliding) 1.0f else 0.25f) + flutter
		left.yRot = -0.30f - 0.60f * spread
		right.yRot = 0.30f + 0.60f * spread
		left.zRot = -0.12f * spread
		right.zRot = 0.12f * spread
		for (i in 0..4) {
			val fan = (0.05f + 0.07f * i) * (0.4f + spread)
			left.getChild("b$i").zRot = -fan
			right.getChild("b$i").zRot = fan
		}

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
			// ранец с двигателями на спине
			root.addOrReplaceChild("pack", CubeListBuilder.create().texOffs(52, 0).addBox(-3f, 1f, 2f, 6f, 8f, 2f)
				.texOffs(68, 0).addBox(-1f, 3f, 4f, 2f, 4f, 1f)
				.texOffs(74, 0).addBox(-6f, 6f, 2f, 2f, 3f, 2f)
				.texOffs(74, 0).addBox(4f, 6f, 2f, 2f, 3f, 2f), PartPose.ZERO)
			val left_wing = root.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(36, 0).addBox(-2f, -2f, -2f, 4f, 4f, 4f)
					.texOffs(0, 0).addBox(0.0f, -1f, -1f, 16f, 2f, 2f),
				PartPose.offset(3.0f, 2f, 3f))
			left_wing.addOrReplaceChild("b0",
				CubeListBuilder.create().texOffs(82, 0).addBox(-1f, 0f, -0.5f, 2f, 15.0f, 1f)
					.texOffs(112, 0).addBox(-0.5f, 1f, 0.5f, 1f, 13.0f, 1f),
				PartPose.offset(2.5f, 1f, 0f))
			left_wing.addOrReplaceChild("b1",
				CubeListBuilder.create().texOffs(88, 0).addBox(-1f, 0f, -0.5f, 2f, 13.0f, 1f)
					.texOffs(116, 0).addBox(-0.5f, 1f, 0.5f, 1f, 11.0f, 1f),
				PartPose.offset(5.6f, 1f, 0f))
			left_wing.addOrReplaceChild("b2",
				CubeListBuilder.create().texOffs(94, 0).addBox(-1f, 0f, -0.5f, 2f, 11.0f, 1f)
					.texOffs(120, 0).addBox(-0.5f, 1f, 0.5f, 1f, 9.0f, 1f),
				PartPose.offset(8.7f, 1f, 0f))
			left_wing.addOrReplaceChild("b3",
				CubeListBuilder.create().texOffs(100, 0).addBox(-1f, 0f, -0.5f, 2f, 9.0f, 1f)
					.texOffs(124, 0).addBox(-0.5f, 1f, 0.5f, 1f, 7.0f, 1f),
				PartPose.offset(11.8f, 1f, 0f))
			left_wing.addOrReplaceChild("b4",
				CubeListBuilder.create().texOffs(106, 0).addBox(-1f, 0f, -0.5f, 2f, 7.0f, 1f)
					.texOffs(0, 16).addBox(-0.5f, 1f, 0.5f, 1f, 5.0f, 1f),
				PartPose.offset(14.9f, 1f, 0f))
			val right_wing = root.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(36, 0).addBox(-2f, -2f, -2f, 4f, 4f, 4f)
					.texOffs(0, 0).addBox(-16.0f, -1f, -1f, 16f, 2f, 2f),
				PartPose.offset(-3.0f, 2f, 3f))
			right_wing.addOrReplaceChild("b0",
				CubeListBuilder.create().texOffs(82, 0).addBox(-1f, 0f, -0.5f, 2f, 15.0f, 1f)
					.texOffs(112, 0).addBox(-0.5f, 1f, 0.5f, 1f, 13.0f, 1f),
				PartPose.offset(-2.5f, 1f, 0f))
			right_wing.addOrReplaceChild("b1",
				CubeListBuilder.create().texOffs(88, 0).addBox(-1f, 0f, -0.5f, 2f, 13.0f, 1f)
					.texOffs(116, 0).addBox(-0.5f, 1f, 0.5f, 1f, 11.0f, 1f),
				PartPose.offset(-5.6f, 1f, 0f))
			right_wing.addOrReplaceChild("b2",
				CubeListBuilder.create().texOffs(94, 0).addBox(-1f, 0f, -0.5f, 2f, 11.0f, 1f)
					.texOffs(120, 0).addBox(-0.5f, 1f, 0.5f, 1f, 9.0f, 1f),
				PartPose.offset(-8.7f, 1f, 0f))
			right_wing.addOrReplaceChild("b3",
				CubeListBuilder.create().texOffs(100, 0).addBox(-1f, 0f, -0.5f, 2f, 9.0f, 1f)
					.texOffs(124, 0).addBox(-0.5f, 1f, 0.5f, 1f, 7.0f, 1f),
				PartPose.offset(-11.8f, 1f, 0f))
			right_wing.addOrReplaceChild("b4",
				CubeListBuilder.create().texOffs(106, 0).addBox(-1f, 0f, -0.5f, 2f, 7.0f, 1f)
					.texOffs(0, 16).addBox(-0.5f, 1f, 0.5f, 1f, 5.0f, 1f),
				PartPose.offset(-14.9f, 1f, 0f))
			return LayerDefinition.create(mesh, 128, 64)
		}
	}
}
