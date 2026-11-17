package com.remake.arsenal.net

import com.remake.arsenal.RemakeArsenal
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/** Клиент -> сервер: нажата клавиша способности. */
class AbilityPayload(val action: Int) : CustomPacketPayload {
	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

	companion object {
		const val DASH = 1
		const val SONIC = 2
		const val LIGHTNING = 3

		val TYPE = CustomPacketPayload.Type<AbilityPayload>(RemakeArsenal.id("ability"))
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, AbilityPayload> = StreamCodec.of(
			{ buf, p -> buf.writeVarInt(p.action) },
			{ buf -> AbilityPayload(buf.readVarInt()) }
		)
	}
}

/** Сервер -> клиент: сколько энергии у электро-крыльев (0..3). */
class EnergyPayload(val energy: Float) : CustomPacketPayload {
	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

	companion object {
		val TYPE = CustomPacketPayload.Type<EnergyPayload>(RemakeArsenal.id("energy"))
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, EnergyPayload> = StreamCodec.of(
			{ buf, p -> buf.writeFloat(p.energy) },
			{ buf -> EnergyPayload(buf.readFloat()) }
		)
	}
}
