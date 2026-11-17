package com.remake.arsenal.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.remake.arsenal.RemakeArsenal
import com.remake.arsenal.entity.HelperEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

/** Кастомная 3D-модель помощника: мешок с завязанной горловиной, головой и двумя ножками. */
class HelperModel(private val root: ModelPart) : EntityModel<HelperEntity>() {
	private val head = root.getChild("head")
	private val leftLeg = root.getChild("left_leg")
	private val rightLeg = root.getChild("right_leg")

	override fun setupAnim(entity: HelperEntity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float) {
		head.yRot = netHeadYaw * (Math.PI.toFloat() / 180f)
		head.xRot = headPitch * (Math.PI.toFloat() / 180f)
		leftLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount
		rightLeg.xRot = Mth.cos(limbSwing * 0.6662f + Math.PI.toFloat()) * 1.2f * limbSwingAmount
	}

	override fun renderToBuffer(poseStack: PoseStack, buffer: VertexConsumer, packedLight: Int, packedOverlay: Int, color: Int) {
		root.render(poseStack, buffer, packedLight, packedOverlay, color)
	}

	companion object {
		val LAYER = ModelLayerLocation(RemakeArsenal.id("hate_helper"), "main")

		fun createLayer(): LayerDefinition {
			val mesh = MeshDefinition()
			val root = mesh.root
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5f, -10f, -4f, 10f, 10f, 8f), PartPose.offset(0f, 24f, 0f))
			root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3.5f, -7f, -3.5f, 7f, 7f, 7f), PartPose.offset(0f, 14f, 0f))
			root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5f, 0f, -1.5f, 3f, 2f, 3f), PartPose.offset(2.5f, 22f, 0f))
			root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5f, 0f, -1.5f, 3f, 2f, 3f), PartPose.offset(-2.5f, 22f, 0f))
			return LayerDefinition.create(mesh, 64, 32)
		}
	}
}

class HelperRenderer(ctx: EntityRendererProvider.Context) :
	MobRenderer<HelperEntity, HelperModel>(ctx, HelperModel(ctx.bakeLayer(HelperModel.LAYER)), 0.4f) {
	override fun getTextureLocation(entity: HelperEntity): ResourceLocation = TEXTURE

	companion object {
		private val TEXTURE = RemakeArsenal.id("textures/entity/hate_helper.png")
	}
}
