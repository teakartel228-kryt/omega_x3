package com.remake.arsenal.content

import com.remake.arsenal.RemakeArsenal
import com.remake.arsenal.entity.ThrownBrick
import com.remake.arsenal.logic.BottleAnswers
import com.remake.arsenal.logic.BottleMail
import com.remake.arsenal.logic.Hit
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import com.remake.arsenal.logic.HitReceiver
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Items

object ContentEvents {
	fun init() {
		// Удар игрока по мишени/перехватчику. Работает на сервере; на клиенте PASS, чтобы пакет удара дошёл до сервера.
		AttackBlockCallback.EVENT.register(AttackBlockCallback { player, world, hand, pos, direction ->
			if (world.isClientSide || player !is ServerPlayer || player.isSpectator) return@AttackBlockCallback InteractionResult.PASS
			val st = world.getBlockState(pos)
			val b = st.block
			if (b !is HitReceiver) return@AttackBlockCallback InteractionResult.PASS
			// Палка отладки работает как обычно. В креативе обычный клик ломает блок, а Shift+клик = только удар.
			// В выживании Shift+клик = просто ломать без удара.
			if (player.mainHandItem.`is`(Items.DEBUG_STICK)) return@AttackBlockCallback InteractionResult.PASS
			if (player.isCreative) {
				if (!player.isShiftKeyDown) return@AttackBlockCallback InteractionResult.PASS
			} else if (player.isShiftKeyDown) return@AttackBlockCallback InteractionResult.PASS
			val scale = player.getAttackStrengthScale(0.5f)
			val dmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat() * (0.2f + scale * scale * 0.8f)
			player.resetAttackStrengthTicker()
			player.swing(hand, true)
			b.onHit(world as ServerLevel, pos, st, direction, Hit(player, dmg), 0)
			// В выживании удар засчитывается, а блок ломается как обычно (долгое удержание). В креативе Shift+клик — только удар, без слома.
			if (player.isCreative) InteractionResult.SUCCESS else InteractionResult.PASS
		})

		// Книга + пустая бутылка (в разных руках) = бутылка с запиской
		UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
			val stack = player.getItemInHand(hand)
			val other = player.getItemInHand(if (hand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND)
			fun isBook(s: ItemStack) = s.`is`(Items.WRITABLE_BOOK) || s.`is`(Items.WRITTEN_BOOK)
			val pair = (stack.`is`(Items.GLASS_BOTTLE) && isBook(other)) || (isBook(stack) && other.`is`(Items.GLASS_BOTTLE))
			if (!pair) return@UseItemCallback InteractionResultHolder.pass(stack)
			if (!world.isClientSide) {
				val book = if (isBook(stack)) stack else other
				val bottle = if (isBook(stack)) other else stack
				val text = BottleAnswers.extractText(book)
				val out = ItemStack(com.remake.arsenal.ModItems.MESSAGE_BOTTLE)
				CustomData.update(DataComponents.CUSTOM_DATA, out) { it.putString("msg", text) }
				bottle.shrink(1)
				book.shrink(1)
				if (!player.addItem(out)) player.drop(out, false)
				world.playSound(null, player.x, player.y, player.z, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.0f)
			}
			InteractionResultHolder.sidedSuccess(stack, world.isClientSide)
		})

		// Раз в 5 секунд проверяем, не пора ли морю ответить
		ServerTickEvents.END_SERVER_TICK.register { server ->
			if (server.tickCount % 100 == 0) BottleMail.deliver(server)
		}

		// Метательные кирпичи: ПКМ кирпичом / незерским кирпичом в воздух.
		UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
			val stack = player.getItemInHand(hand)
			val nether = stack.`is`(Items.NETHER_BRICK)
			if (!nether && !stack.`is`(Items.BRICK)) return@UseItemCallback InteractionResultHolder.pass(stack)
			if (player.cooldowns.isOnCooldown(stack.item)) return@UseItemCallback InteractionResultHolder.fail(stack)
			if (!world.isClientSide) {
				val type = if (nether) ContentEntities.THROWN_NETHER_BRICK else ContentEntities.THROWN_BRICK
				val e = ThrownBrick(type, player, world)
				e.readEnchants(world, stack)
				e.shootFromRotation(player, player.xRot, player.yRot, 0.0f, 1.5f, 1.0f)
				world.addFreshEntity(e)
				world.playSound(null, player.x, player.y, player.z, ModSounds.BRICK_THROW, SoundSource.PLAYERS, 0.8f, 0.9f + world.random.nextFloat() * 0.2f)
			}
			player.awardStat(Stats.ITEM_USED.get(stack.item))
			if (!player.abilities.instabuild) stack.shrink(1)
			player.cooldowns.addCooldown(stack.item, 8)
			InteractionResultHolder.sidedSuccess(stack, world.isClientSide)
		})

		// Новые предметы во вкладке [REMAKE] Arsenal (старый список не трогаем)
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, RemakeArsenal.id("main"))).register { out ->
			out.accept(ModBlocks.STRIKE_TARGET)
			out.accept(ModBlocks.STRIKE_BARRIER)
			out.accept(ModBlocks.HIT_RELAY)
			out.accept(ModBlocks.PROJECTILE_SPLITTER)
			out.accept(ModBlocks.SMART_REDSTONE)
			out.accept(ContentItems.MACE_HEAD)
			out.accept(ContentItems.TRIDENT_HEAD)
			out.accept(ContentItems.MAGNETITE_HEAD)
			out.accept(ModBlocks.SPRING)
			out.accept(ModBlocks.CONVEYOR)
			out.accept(ContentItems.BRICK_SHARD)
			out.accept(ContentItems.NETHER_BRICK_SHARD)
		}
	}
}
