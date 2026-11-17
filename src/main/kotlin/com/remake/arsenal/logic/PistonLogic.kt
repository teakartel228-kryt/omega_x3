package com.remake.arsenal.logic

import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.piston.PistonStructureResolver
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import java.util.function.Predicate

object PistonLogic {
	const val CHARGE_TICKS = 15

	fun shoot(player: ServerPlayer, stack: ItemStack, charged: Boolean) {
		val level = player.serverLevel()
		val dir = player.lookAngle
		val reach = if (charged) 6.0 else 3.0        // заряженный поршень в 2 раза длиннее
		val strength = if (charged) 2.0 else 1.0     // и толкает в 2 раза сильнее
		val eye = player.eyePosition
		val end = eye.add(dir.scale(reach))

		// 1) враги: урон + отталкивание
		val targets = level.getEntities(player, AABB(eye, end).inflate(1.0), Predicate<net.minecraft.world.entity.Entity> { it is LivingEntity && it.isAlive && it !== player })
		for (e in targets) {
			if (e.boundingBox.inflate(0.4).clip(eye, end).isPresent) {
				e.hurt(player.damageSources().playerAttack(player), if (charged) 6.0f else 3.0f)
				e.deltaMovement = e.deltaMovement.add(dir.x * strength, dir.y * strength * 0.8 + 0.2, dir.z * strength)
				e.hurtMarked = true
			}
		}

		// 2) блоки: толкаем так же, как обычный поршень (лимит 12 блоков, слизь/мёд, непрочные ломаются)
		val hit = level.clip(ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
		if (hit.type == HitResult.Type.BLOCK) {
			val pushDir = Direction.getNearest(dir.x, dir.y, dir.z)
			pushBlocks(level, hit.blockPos.relative(pushDir.opposite), pushDir)
		}

		// 3) отдача в противоположную сторону
		player.deltaMovement = player.deltaMovement.add(dir.scale(-0.9 * strength))
		player.hurtMarked = true
		player.fallDistance = 0.0f

		level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0f, if (charged) 0.7f else 1.1f)
		level.sendParticles(ParticleTypes.POOF, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0.02)

		player.cooldowns.addCooldown(stack.item, if (charged) 40 else 20)
		val slot = if (player.usedItemHand == InteractionHand.MAIN_HAND) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND
		stack.hurtAndBreak(1, player, slot)
	}

	private fun pushBlocks(level: ServerLevel, pistonPos: net.minecraft.core.BlockPos, dir: Direction) {
		val resolver = PistonStructureResolver(level, pistonPos, dir, true)
		if (!resolver.resolve()) return
		val toPush = resolver.toPush
		val toDestroy = resolver.toDestroy
		val states = toPush.map { level.getBlockState(it) }

		for (p in toDestroy) {
			val s = level.getBlockState(p)
			if (!s.isAir) {
				Block.dropResources(s, level, p, level.getBlockEntity(p))
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE)
			}
		}
		for (p in toPush) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE)
		for (i in toPush.indices) level.setBlock(toPush[i].relative(dir), states[i], Block.UPDATE_ALL)
	}
}
