package com.remake.arsenal.net

import com.remake.arsenal.logic.Abilities
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object ModNetworking {
	fun init() {
		PayloadTypeRegistry.playC2S().register(AbilityPayload.TYPE, AbilityPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(EnergyPayload.TYPE, EnergyPayload.CODEC)

		ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.TYPE) { payload, context ->
			val player = context.player()
			when (payload.action) {
				AbilityPayload.DASH -> Abilities.dash(player)
				AbilityPayload.SONIC -> Abilities.sonicWave(player)
				AbilityPayload.LIGHTNING -> com.remake.arsenal.logic.BagLogic.hateLightning(player)
			}
		}
	}
}
