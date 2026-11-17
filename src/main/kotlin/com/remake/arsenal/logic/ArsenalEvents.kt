package com.remake.arsenal.logic

import com.remake.arsenal.ModItems
import com.remake.arsenal.net.EnergyPayload
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile

object ArsenalEvents {
	/** Энергия электро-крыльев: ~1 рывок за 2.5 с, восстанавливается автоматически (как стамина в ULTRAKILL). */
	private const val ENERGY_REGEN_PER_TICK = 0.02f

	fun init() {
		ServerTickEvents.END_SERVER_TICK.register { server ->
			for (p in server.playerList.players) tickPlayer(p)
		}
		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> Arsenal.states.remove(handler.player.uuid) }

		ServerLivingEntityEvents.ALLOW_DAMAGE.register(object : ServerLivingEntityEvents.AllowDamage {
			override fun allowDamage(entity: LivingEntity, source: DamageSource, amount: Float): Boolean {
				if (entity !is ServerPlayer) return true
				if (source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true
				val st = Arsenal.states[entity.uuid] ?: return true
				if (st.dashTicks > 0) return false // неуязвимость во время рывка
				val attacker = source.directEntity
				if (attacker != null && (attacker is LivingEntity || attacker is Projectile)) {
					if (HookLogic.tryBlockMelee(entity, st, attacker, amount)) return false
				}
				return true
			}
		})
	}

	private fun tickPlayer(p: ServerPlayer) {
		val st = Arsenal.of(p)
		val hasWings = p.getItemBySlot(EquipmentSlot.CHEST).`is`(ModItems.ELECTRO_WINGS)

		if (st.dashTicks > 0) {
			st.dashTicks--
			p.deltaMovement = st.dashDir.scale(1.4)
			p.hurtMarked = true
			p.fallDistance = 0.0f
		}
		if (hasWings) {
			if (st.energy < 3.0f) st.energy = (st.energy + ENERGY_REGEN_PER_TICK).coerceAtMost(3.0f)
			if (p.tickCount % 2 == 0) ServerPlayNetworking.send(p, EnergyPayload(st.energy))
		}

		HookLogic.tick(p, st)
	}
}
