package com.remake.arsenal.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.Block
import java.util.function.Supplier

object BlockCodecs {
	fun <B : Block> of(factory: () -> B): MapCodec<B> = MapCodec.unit(Supplier { factory() })
}
