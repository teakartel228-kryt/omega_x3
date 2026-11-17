package com.remake.arsenal.client

import com.mojang.blaze3d.platform.InputConstants
import com.remake.arsenal.ModEntities
import com.remake.arsenal.ModItems
import com.remake.arsenal.net.AbilityPayload
import com.remake.arsenal.net.EnergyPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.KeyMapping
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW

object RemakeArsenalClient : ClientModInitializer {
	private const val CATEGORY = "key.categories.remake_arsenal"
	private val KEY_DASH = KeyMapping("key.remake_arsenal.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY)
	private val KEY_SONIC = KeyMapping("key.remake_arsenal.sonic", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY)
	private val KEY_LIGHTNING = KeyMapping("key.remake_arsenal.lightning", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY)

	@JvmField
	var energy = 3.0f
	private var jumpWasDown = false

	override fun onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(KEY_DASH)
		KeyBindingHelper.registerKeyBinding(KEY_SONIC)
		KeyBindingHelper.registerKeyBinding(KEY_LIGHTNING)

		// помощник мешка
		EntityModelLayerRegistry.registerModelLayer(HelperModel.LAYER) { HelperModel.createLayer() }
		EntityRendererRegistry.register(ModEntities.HELPER) { ctx -> HelperRenderer(ctx) }

		// 3D-модели на теле: крылья и шлем
		EntityModelLayerRegistry.registerModelLayer(WingsLayer.LAYER) { WingsLayer.createLayer() }
		EntityModelLayerRegistry.registerModelLayer(HelmetLayer.LAYER) { HelmetLayer.createLayer() }
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register { _, renderer, helper, context ->
			if (renderer is PlayerRenderer) {
				helper.register(WingsLayer(renderer, context.modelSet))
				helper.register(HelmetLayer(renderer, context.modelSet))
			}
		}

		ClientPlayNetworking.registerGlobalReceiver(EnergyPayload.TYPE) { payload, _ -> energy = payload.energy }

		ClientTickEvents.END_CLIENT_TICK.register { mc ->
			val p = mc.player ?: return@register
			while (KEY_DASH.consumeClick()) ClientPlayNetworking.send(AbilityPayload(AbilityPayload.DASH))
			while (KEY_SONIC.consumeClick()) ClientPlayNetworking.send(AbilityPayload(AbilityPayload.SONIC))
			while (KEY_LIGHTNING.consumeClick()) ClientPlayNetworking.send(AbilityPayload(AbilityPayload.LIGHTNING))

			val jumpDown = mc.options.keyJump.isDown
			if (p.getItemBySlot(EquipmentSlot.CHEST).`is`(ModItems.ELECTRO_WINGS) && !p.isInWater && !p.abilities.flying) {
				val airborne = !p.onGround()
				// бесплатный отскок от стены (только что нажатый прыжок у стены)
				if (airborne && p.horizontalCollision && jumpDown && !jumpWasDown) {
					val look = p.lookAngle
					val away = Vec3(-look.x, 0.0, -look.z)
					if (away.lengthSqr() > 1.0e-4) {
						val v = away.normalize().scale(0.55)
						p.setDeltaMovement(v.x, 0.6, v.z)
						p.fallDistance = 0.0f
					}
				} else if (airborne && jumpDown && p.deltaMovement.y < -0.1) {
					// скольжение: падаем медленно и летим вперёд; энергию не тратит
					val look = p.lookAngle
					val m = p.deltaMovement
					p.setDeltaMovement(m.x + look.x * 0.03, -0.1, m.z + look.z * 0.03)
					p.fallDistance = 0.0f
				}
			}
			jumpWasDown = jumpDown
		}

		HudRenderCallback.EVENT.register(HudRenderCallback { g, _ ->
			val mc = net.minecraft.client.Minecraft.getInstance()
			val p = mc.player ?: return@HudRenderCallback
			if (!p.getItemBySlot(EquipmentSlot.CHEST).`is`(ModItems.ELECTRO_WINGS)) return@HudRenderCallback
			val cx = g.guiWidth() / 2
			val y = g.guiHeight() / 2 + 16
			for (i in 0 until 3) {
				val fill = (energy - i).coerceIn(0.0f, 1.0f)
				val x0 = cx - 29 + i * 20
				g.fill(x0 - 1, y - 1, x0 + 19, y + 4, 0xAA000000.toInt())
				if (fill > 0.0f) g.fill(x0, y, x0 + (18 * fill).toInt(), y + 3, if (fill >= 1.0f) 0xFF45E6FF.toInt() else 0xFF2A7F99.toInt())
			}
		})
	}
}
