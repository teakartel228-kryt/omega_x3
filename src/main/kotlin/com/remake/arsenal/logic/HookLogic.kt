package com.remake.arsenal.logic

import com.remake.arsenal.ModItems
import com.remake.arsenal.content.ContentEntities
import com.remake.arsenal.entity.GrappleHook
import com.remake.arsenal.item.HookItem
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.FallingBlockEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.level.block.ShulkerBoxBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object HookLogic {
	const val EMERGENCY_BASE_TICKS = 300 // 15 секунд

	private val CRACKED: Map<Block, Block> = mapOf(
		Blocks.STONE_BRICKS to Blocks.CRACKED_STONE_BRICKS,
		Blocks.DEEPSLATE_BRICKS to Blocks.CRACKED_DEEPSLATE_BRICKS,
		Blocks.DEEPSLATE_TILES to Blocks.CRACKED_DEEPSLATE_TILES,
		Blocks.NETHER_BRICKS to Blocks.CRACKED_NETHER_BRICKS,
		Blocks.POLISHED_BLACKSTONE_BRICKS to Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
	)

	// ---------- заряды редстоун-крюка ----------
	fun charges(stack: ItemStack): Int = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getInt("charges") ?: 0

	fun setCharges(stack: ItemStack, n: Int) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack) { it.putInt("charges", n) }
	}

	// ---------- выстрел ----------
	fun fire(player: ServerPlayer, stack: ItemStack, tier: Int) {
		val level = player.serverLevel()
		val st = Arsenal.of(player)

		// Shift — отпустить все крюки
		if (player.isShiftKeyDown && Ench.level(level, stack, Ench.ARMS) == 0) {
			st.anchors.clear()
			return
		}
		// «Руки-крюки»: Shift+ПКМ — взаимодействие с блоками на расстоянии
		if (player.isShiftKeyDown && Ench.level(level, stack, Ench.ARMS) > 0) {
			remoteUse(player)
			return
		}

		val multishot = Ench.level(level, stack, Ench.MULTISHOT).coerceAtMost(1)
		val power = Ench.level(level, stack, Ench.POWER).coerceIn(0, 5)
		val quick = Ench.level(level, stack, Ench.QUICK_RECHARGE).coerceIn(0, 3)
		val head = HookHeads.of(stack)
		val extra = if (head == HookHead.TRIDENT) 2 else 0 // трезубец: +2 крюка (1→3, 2→4, 3→5)
		val shots = 1 + multishot + extra
		val cap = (if (tier >= 4) 2 else 1) + multishot + extra
		st.hookPower = power

		val spacing = if (shots == 2) 14.0 else 8.0
		for (i in 0 until shots) {
			val yawOffset = ((i - (shots - 1) / 2.0) * spacing).toFloat()
			val dir = Vec3.directionFromRotation(player.xRot, player.yRot + yawOffset)
			shootOne(player, level, st, stack, tier, dir, cap, shots, quick, head)
		}
		level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f)
		player.cooldowns.addCooldown(stack.item, 8)
	}

	private fun shootOne(
		player: ServerPlayer, level: ServerLevel, st: ArsenalState, stack: ItemStack,
		tier: Int, dir: Vec3, cap: Int, shots: Int, quick: Int, head: HookHead
	) {
		val hook = GrappleHook(ContentEntities.GRAPPLE_HOOK, level)
		hook.owner = player
		hook.hookTier = tier.coerceIn(0, 5)
		hook.head = head
		hook.maxRange = if (tier >= 4) 28.0 else 18.0
		hook.cap = cap
		hook.shots = shots
		hook.quick = quick
		hook.hand = if (player.mainHandItem === stack) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
		hook.setPos(player.eyePosition.add(0.0, -0.35, 0.0).add(dir.scale(0.6)))
		hook.launch(dir)
		level.addFreshEntity(hook)
	}

	private fun heldHook(player: ServerPlayer, hook: GrappleHook): ItemStack {
		val s = player.getItemInHand(hook.hand)
		return if (s.item is HookItem) s else ItemStack.EMPTY
	}

	/** Крюк долетел до сущности. */
	fun onHookHitEntity(hook: GrappleHook, player: ServerPlayer, target: Entity) {
		val level = player.serverLevel()
		hitEntity(player, level, Arsenal.of(player), heldHook(player, hook), hook.hookTier, target, hook.shotDir, hook.shots, hook.quick, hook.head)
	}

	/** Крюк долетел до блока. true = зацепился (игрока тянет к якорю). */
	fun onHookHitBlock(hook: GrappleHook, player: ServerPlayer, hit: BlockHitResult): Boolean {
		val level = player.serverLevel()
		return hitBlock(player, level, Arsenal.of(player), heldHook(player, hook), hook.hookTier, hit.blockPos, hit, hook.cap, hook.id)
	}

	// ---------- попадание в сущность ----------
	private fun hitEntity(
		player: ServerPlayer, level: ServerLevel, st: ArsenalState, stack: ItemStack,
		tier: Int, target: Entity, dir: Vec3, shots: Int, quick: Int, head: HookHead = HookHead.NONE
	) {
		// ловим снаряд и отправляем обратно
		if (target is Projectile) {
			target.owner = player
			target.deltaMovement = dir.scale(target.deltaMovement.length().coerceAtLeast(1.2))
			target.hurtMarked = true
			level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.5f)
			return
		}
		if (target !is LivingEntity) return
		if (target is Player && target.isFallFlying) return // нельзя поймать игрока на элитрах

		if (level.gameTime < st.emergencyUntil) {
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.hook_locked"), true)
			return
		}
		// булава: сильный удар, но цепь всегда рвётся — экстренный режим гарантирован
		if (head == HookHead.MACE) {
			target.hurt(player.damageSources().playerAttack(player), 10.0f + 2.0f * st.hookPower)
			val away = target.position().subtract(player.position()).normalize()
			target.deltaMovement = away.scale(0.8).add(0.0, 0.3, 0.0)
			target.hurtMarked = true
			level.playSound(null, target.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0f, 1.1f)
			emergency(player, level, st, stack, dir)
			return
		}
		val heavy = isHeavy(target)
		val tooHeavy = !canPull(target, tier)
		// два крюка против тяжёлой цели — риск экстренного возврата даже при достаточном уровне
		val strained = heavy && (shots >= 2 || st.anchors.isNotEmpty()) && level.random.nextFloat() < 0.5f
		if (tooHeavy || strained) {
			emergency(player, level, st, stack, dir)
			return
		}

		target.hurt(player.damageSources().playerAttack(player), 2.0f)
		val diff = player.position().subtract(target.position())
		val pull = diff.scale(0.28 + 0.04 * st.hookPower).add(0.0, 0.25, 0.0)
		val capped = if (pull.length() > 3.0) pull.normalize().scale(3.0) else pull
		target.deltaMovement = capped
		target.hurtMarked = true
	}

	private fun isHeavyArmor(p: Player): Boolean = p.armorSlots.any {
		val path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.item).path
		path.startsWith("diamond_") || path.startsWith("netherite_")
	}

	private fun isHeavy(t: LivingEntity): Boolean = if (t is Player) isHeavyArmor(t) else t.maxHealth > 30.0f

	private fun canPull(t: LivingEntity, tier: Int): Boolean {
		if (t is Player) return if (isHeavyArmor(t)) tier >= 5 else tier >= 2 // II: медь/железо, V: алмаз/незерит
		if (t.maxHealth > 150.0f) return false                              // боссы не тянутся вообще
		return if (t.maxHealth <= 30.0f) true else tier >= 5
	}

	/** Экстренное притягивание назад: цепь рвётся обратно, крюк не тянет цели 15 с (меньше с «Быстрой перезарядкой»). */
	fun emergency(player: ServerPlayer, level: ServerLevel, st: ArsenalState, stack: ItemStack?, dir: Vec3?) {
		val quick = if (stack != null) Ench.level(level, stack, Ench.QUICK_RECHARGE).coerceIn(0, 3) else quickOfHeld(player, level)
		val ticks = EMERGENCY_BASE_TICKS - 60 * quick
		st.emergencyUntil = level.gameTime + ticks
		st.chainHp = -1.0f
		if (dir != null) {
			player.deltaMovement = player.deltaMovement.add(dir.scale(-0.7)).add(0.0, 0.15, 0.0)
			player.hurtMarked = true
		}
		level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f)
		level.sendParticles(ParticleTypes.CRIT, player.x, player.eyeY - 0.3, player.z, 12, 0.3, 0.3, 0.3, 0.1)
		player.displayClientMessage(Component.translatable("msg.remake_arsenal.hook_emergency", ticks / 20), true)
	}

	private fun quickOfHeld(player: ServerPlayer, level: ServerLevel): Int =
		maxOf(Ench.level(level, player.mainHandItem, Ench.QUICK_RECHARGE), Ench.level(level, player.offhandItem, Ench.QUICK_RECHARGE)).coerceIn(0, 3)

	// ---------- попадание в блок ----------
	private fun hitBlock(
		player: ServerPlayer, level: ServerLevel, st: ArsenalState, stack: ItemStack,
		tier: Int, pos: BlockPos, hit: BlockHitResult, cap: Int, hookId: Int
	): Boolean {
		val state = level.getBlockState(pos)
		val hardness = state.getDestroySpeed(level, pos)
		if (hardness < 0.0f || hardness >= 50.0f) { // обсидиан и сверхпрочное: не зацепиться, не сломать
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.hook_too_hard"), true)
			return false
		}

		// редстоун-крюк: нажимает кнопки/рычаги на расстоянии
		val ch = charges(stack)
		if (ch > 0 && Ench.level(level, stack, Ench.REDSTONE) > 0 && (state.`is`(BlockTags.BUTTONS) || state.`is`(Blocks.LEVER))) {
			state.useWithoutItem(level, player, hit)
			setCharges(stack, ch - 1)
			level.sendParticles(net.minecraft.core.particles.DustParticleOptions(org.joml.Vector3f(1f, 0.1f, 0.1f), 1f), pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 8, 0.3, 0.3, 0.3, 0.0)
			return false
		}

		// сыпучие блоки притягиваем
		if (state.block is FallingBlock) {
			val fb = FallingBlockEntity.fall(level, pos, state)
			fb.deltaMovement = player.position().subtract(Vec3.atCenterOf(pos)).scale(0.2).add(0.0, 0.2, 0.0)
			fb.hurtMarked = true
			return false
		}

		// сундуки, бочки, шалкеры
		if (canPullContainer(state, level, pos, tier)) {
			pullContainer(player, level, pos, state)
			return false
		}

		// III: ломаем непрочное / трескаем кирпичи
		if (tier >= 3) {
			val cracked = CRACKED[state.block]
			if (cracked != null) {
				level.setBlock(pos, cracked.defaultBlockState(), Block.UPDATE_ALL)
				level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0f, 0.9f)
			} else if (hardness <= 0.6f && !state.isAir) {
				level.destroyBlock(pos, true, player)
				return false
			}
		}

		// просто цепляемся
		st.anchors.add(Anchor(Vec3.atCenterOf(pos), level.gameTime + 100, hookId))
		while (st.anchors.size > cap) st.anchors.removeAt(0)
		level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0f, 1.0f)
		return true
	}

	private fun canPullContainer(state: BlockState, level: ServerLevel, pos: BlockPos, tier: Int): Boolean {
		val block = state.block
		if (block is ChestBlock) return state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
		if (block is BarrelBlock || block is ShulkerBoxBlock) return true
		val path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).path
		if (path.contains("copper_chest")) return true
		if (tier >= 5 && block != Blocks.ENDER_CHEST) return level.getBlockEntity(pos) is Container // все хранилища кроме эндер-сундука
		return false
	}

	private fun pullContainer(player: ServerPlayer, level: ServerLevel, pos: BlockPos, state: BlockState) {
		val be = level.getBlockEntity(pos) ?: return
		val toPlayer = player.position().subtract(Vec3.atCenterOf(pos))
		val horiz = Vec3(toPlayer.x, 0.0, toPlayer.z)
		val step = if (horiz.length() < 1.0e-3) Vec3.ZERO else horiz.normalize()
		// ставим контейнер рядом с игроком, со стороны сундука
		val target = BlockPos.containing(player.x - step.x * 1.5, player.y, player.z - step.z * 1.5)
		val dest = listOf(target, target.above(), target.below()).firstOrNull { level.getBlockState(it).canBeReplaced() && it != pos } ?: run {
			player.displayClientMessage(Component.translatable("msg.remake_arsenal.hook_no_space"), true)
			return
		}
		val tag = be.saveWithoutMetadata(level.registryAccess())
		(be as? Container)?.clearContent()
		level.removeBlockEntity(pos)
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
		level.setBlock(dest, state, Block.UPDATE_ALL)
		level.getBlockEntity(dest)?.loadWithComponents(tag, level.registryAccess())
		level.getBlockEntity(dest)?.setChanged()
		level.playSound(null, dest, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f)
	}

	// ---------- «Руки-крюки» ----------
	private fun remoteUse(player: ServerPlayer) {
		val level = player.serverLevel()
		val eye = player.eyePosition
		val end = eye.add(player.lookAngle.scale(7.0)) // дальше 8 блоков меню контейнеров закрываются само
		val hit = level.clip(net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, player))
		if (hit.type != net.minecraft.world.phys.HitResult.Type.BLOCK) return
		Chain.draw(level, eye.add(0.0, -0.35, 0.0), hit.location)
		level.getBlockState(hit.blockPos).useWithoutItem(level, player, hit)
	}

	// ---------- серверный тик игрока ----------
	fun tick(player: ServerPlayer, st: ArsenalState) {
		val level = player.serverLevel()
		val now = level.gameTime

		// вышли из экстренного режима -> цепь восстановилась
		if (st.emergencyUntil != 0L && now >= st.emergencyUntil) {
			st.emergencyUntil = 0L
			st.chainHp = -1.0f
		}

		// подтягивание игрока к якорям
		if (st.anchors.isNotEmpty()) {
			st.anchors.removeIf { it.expiresAt < now }
			if (player.isShiftKeyDown || player.isSpectator) st.anchors.clear()
		}
		if (st.anchors.isNotEmpty()) {
			val eye = player.eyePosition
			var sum = Vec3.ZERO
			var arrived = false
			for (a in st.anchors) {
				val d = a.pos.subtract(eye)
				if (d.length() < 2.2) arrived = true
				sum = sum.add(d.normalize())
			}
			if (arrived) {
				player.deltaMovement = player.deltaMovement.add(0.0, 0.3, 0.0)
				player.hurtMarked = true
				st.anchors.clear()
			} else if (sum.lengthSqr() > 1.0e-6) {
				val speed = 0.85 + 0.2 * st.hookPower
				player.deltaMovement = player.deltaMovement.scale(0.5).add(sum.normalize().scale(speed * 0.5))
				player.hurtMarked = true
				player.fallDistance = 0.0f
			}
		}

		defenseTick(player, level, st)
	}

	// ---------- защитный механизм ----------
	private fun defenseHook(player: Player, level: Level): ItemStack? {
		for (s in listOf(player.mainHandItem, player.offhandItem)) {
			if (s.item is HookItem && Ench.level(level, s, Ench.DEFENSE) > 0) return s
		}
		return null
	}

	fun chainMax(player: ServerPlayer, level: ServerLevel): Float {
		val q = quickOfHeld(player, level)
		return (EMERGENCY_BASE_TICKS - 60 * q) / 20.0f // прочность цепи = длительность экстренного режима в секундах
	}

	private fun defenseTick(player: ServerPlayer, level: ServerLevel, st: ArsenalState) {
		if (defenseHook(player, level) == null) return
		if (level.gameTime < st.emergencyUntil) return // цепь разорвана, ждём восстановления
		if (st.chainHp < 0.0f) st.chainHp = chainMax(player, level)

		val incoming = level.getEntitiesOfClass(Projectile::class.java, player.boundingBox.inflate(3.0)) {
			it.owner !== player && it.deltaMovement.dot(player.position().add(0.0, 1.0, 0.0).subtract(it.position())) > 0.0
		}
		for (p in incoming) {
			absorb(player, level, st, 3.0f)
			level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 6, 0.1, 0.1, 0.1, 0.1)
			p.discard()
		}
	}

	/** Цепь поглощает урон; при нуле рвётся -> экстренный режим. */
	fun absorb(player: ServerPlayer, level: ServerLevel, st: ArsenalState, amount: Float) {
		st.chainHp -= amount
		level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0f, 1.3f)
		if (st.chainHp <= 0.0f) emergency(player, level, st, null, null)
	}

	/** Вызывается из ALLOW_DAMAGE: true = урон заблокирован цепью. */
	fun tryBlockMelee(player: ServerPlayer, st: ArsenalState, attacker: Entity, amount: Float): Boolean {
		val level = player.serverLevel()
		if (defenseHook(player, level) == null || level.gameTime < st.emergencyUntil) return false
		if (st.chainHp < 0.0f) st.chainHp = chainMax(player, level)
		val to = attacker.position().subtract(player.position()).normalize()
		if (player.lookAngle.dot(to) < 0.3) return false // цепь только спереди
		absorb(player, level, st, amount)
		if (attacker is LivingEntity) attacker.hurt(player.damageSources().thorns(player), 2.0f) // контратака
		return true
	}
}
