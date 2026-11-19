package com.remake.arsenal.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import com.remake.arsenal.RemakeArsenal
import com.remake.arsenal.content.ContentItems
import com.remake.arsenal.entity.GrappleHook
import com.remake.arsenal.logic.HookHead
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.min

/** Рисует голову крюка (3D-модель, повёрнутая по полёту) и железную цепь от руки игрока до крюка. */
class GrappleHookRenderer(ctx: EntityRendererProvider.Context) : EntityRenderer<GrappleHook>(ctx) {
	companion object {
		private val CHAIN: ResourceLocation = RemakeArsenal.id("textures/entity/hook_chain.png")
		private const val SEG = 0.5      // длина одного звена, блоков
		private const val HALF = 0.07    // полуширина звена
	}

	private val itemRenderer = ctx.itemRenderer
	private val heads: List<ItemStack> by lazy { ContentItems.HOOK_HEADS.map { ItemStack(it) } }
	private val special: Map<HookHead, ItemStack> by lazy {
		mapOf(
			HookHead.MACE to ItemStack(ContentItems.MACE_HEAD),
			HookHead.TRIDENT to ItemStack(ContentItems.TRIDENT_HEAD),
			HookHead.MAGNET to ItemStack(ContentItems.MAGNETITE_HEAD)
		)
	}

	override fun getTextureLocation(entity: GrappleHook): ResourceLocation = CHAIN

	// цепь может быть длинной, поэтому не отсекаем крюк по границам кадра
	override fun shouldRender(entity: GrappleHook, camera: Frustum, camX: Double, camY: Double, camZ: Double): Boolean = true

	override fun render(e: GrappleHook, yaw: Float, partial: Float, pose: PoseStack, buf: MultiBufferSource, light: Int) {
		// --- голова ---
		pose.pushPose()
		pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.yRot) + 180.0f))
		pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.xRot)))
		pose.scale(0.62f, 0.62f, 0.62f)
		val head = special[e.head] ?: heads[e.hookTier.coerceIn(0, heads.size - 1)]
		itemRenderer.renderStatic(head, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buf, e.level(), e.id)
		pose.popPose()

		// --- цепь ---
		val owner = e.owner
		if (owner != null) {
			val look = owner.getViewVector(partial)
			val right = look.cross(Vec3(0.0, 1.0, 0.0)).normalize()
			val start = owner.getEyePosition(partial)
				.add(look.scale(0.5)).add(right.scale(0.3)).add(0.0, -0.4, 0.0)
			val delta = start.subtract(e.getPosition(partial))
			drawChain(pose, buf, delta, light)
		}
		super.render(e, yaw, partial, pose, buf, light)
	}

	private fun drawChain(pose: PoseStack, buf: MultiBufferSource, delta: Vec3, light: Int) {
		val len = delta.length()
		if (len < 0.2) return
		val dir = delta.scale(1.0 / len)
		val ref = if (abs(dir.y) > 0.95) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
		val s1 = dir.cross(ref).normalize()
		val s2 = dir.cross(s1).normalize()
		val vc = buf.getBuffer(RenderType.entityCutoutNoCull(CHAIN))
		val last = pose.last()
		var t = 0.0
		var i = 0
		while (t < len - 1.0e-3 && i < 120) {
			val t2 = min(t + SEG, len)
			val a = dir.scale(t)
			val b = dir.scale(t2)
			val v1 = ((t2 - t) / SEG).toFloat()
			val faceOn = if (i % 2 == 0) s1 else s2
			val edgeOn = if (i % 2 == 0) s2 else s1
			quad(vc, last, a, b, faceOn, edgeOn, 0.0f, 0.5f, v1, light)
			quad(vc, last, a, b, edgeOn, faceOn, 0.5f, 1.0f, v1, light)
			t = t2
			i++
		}
	}

	private fun quad(
		vc: VertexConsumer, p: PoseStack.Pose, a: Vec3, b: Vec3, side: Vec3, normal: Vec3,
		u0: Float, u1: Float, v1: Float, light: Int
	) {
		val sx = side.x * HALF; val sy = side.y * HALF; val sz = side.z * HALF
		val nx = normal.x.toFloat(); val ny = normal.y.toFloat(); val nz = normal.z.toFloat()
		fun v(x: Double, y: Double, z: Double, u: Float, vv: Float) {
			vc.addVertex(p, x.toFloat(), y.toFloat(), z.toFloat())
				.setColor(255, 255, 255, 255)
				.setUv(u, vv)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(p, nx, ny, nz)
		}
		v(a.x - sx, a.y - sy, a.z - sz, u0, 0.0f)
		v(a.x + sx, a.y + sy, a.z + sz, u1, 0.0f)
		v(b.x + sx, b.y + sy, b.z + sz, u1, v1)
		v(b.x - sx, b.y - sy, b.z - sz, u0, v1)
	}
}
