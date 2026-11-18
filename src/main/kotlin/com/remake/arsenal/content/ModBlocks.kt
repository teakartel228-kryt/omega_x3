package com.remake.arsenal.content

import com.remake.arsenal.RemakeArsenal
import com.remake.arsenal.block.HitRelayBlock
import com.remake.arsenal.block.ProjectileSplitterBlock
import com.remake.arsenal.block.ConveyorBlock
import com.remake.arsenal.block.SmartRedstoneBE
import com.remake.arsenal.block.SpringBlock
import com.remake.arsenal.block.SmartRedstoneBlock
import com.remake.arsenal.block.StrikeBarrierBlock
import com.remake.arsenal.block.StrikeTargetBlock
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor

object ModBlocks {
	fun props(): BlockBehaviour.Properties =
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6.0f).sound(SoundType.METAL)

	fun barrierProps(): BlockBehaviour.Properties =
		props().noOcclusion().isViewBlocking { _, _, _ -> false }.isSuffocating { _, _, _ -> false }.isRedstoneConductor { _, _, _ -> false }

	fun smartProps(): BlockBehaviour.Properties =
		props().lightLevel { s -> if (s.getValue(SmartRedstoneBlock.ACTIVE)) 6 else 0 }

	fun springProps(): BlockBehaviour.Properties = props().noOcclusion()

	private fun <T : Block> reg(name: String, block: T): T {
		Registry.register(BuiltInRegistries.BLOCK, RemakeArsenal.id(name), block)
		Registry.register(BuiltInRegistries.ITEM, RemakeArsenal.id(name), BlockItem(block, Item.Properties()))
		return block
	}

	@JvmField val STRIKE_TARGET: StrikeTargetBlock = reg("strike_target", StrikeTargetBlock(props()))
	@JvmField val STRIKE_BARRIER: StrikeBarrierBlock = reg("strike_barrier", StrikeBarrierBlock(barrierProps()))
	@JvmField val HIT_RELAY: HitRelayBlock = reg("hit_relay", HitRelayBlock(props()))
	@JvmField val PROJECTILE_SPLITTER: ProjectileSplitterBlock = reg("projectile_splitter", ProjectileSplitterBlock(props()))
	@JvmField val SMART_REDSTONE: SmartRedstoneBlock = reg("smart_redstone", SmartRedstoneBlock(smartProps()))

	@JvmField val SPRING: SpringBlock = reg("spring", SpringBlock(springProps()))
	@JvmField val CONVEYOR: ConveyorBlock = reg("conveyor", ConveyorBlock(props()))

	@JvmField val SMART_BE: BlockEntityType<SmartRedstoneBE> = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE, RemakeArsenal.id("smart_redstone"),
		FabricBlockEntityTypeBuilder.create(FabricBlockEntityTypeBuilder.Factory { pos, st -> SmartRedstoneBE(pos, st) }, SMART_REDSTONE).build()
	)

	fun init() {}
}
