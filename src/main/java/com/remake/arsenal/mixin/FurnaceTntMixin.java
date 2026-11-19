package com.remake.arsenal.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Динамит (TNT) в печке: плавка идёт как обычно, но вместо результата печка взрывается.
 * Предмета-результата никогда не появляется (рецепт нужен только затем, чтобы печка начала гореть).
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceTntMixin {
	private static final Map<AbstractFurnaceBlockEntity, Integer> remakeArsenal$fuse = new WeakHashMap<>();

	@Inject(method = "serverTick", at = @At("HEAD"), cancellable = true, require = 0)
	private static void remakeArsenal$tntSmelt(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity be, CallbackInfo ci) {
		if (level.isClientSide) return;
		ItemStack input = be.getItem(0);
		if (!input.is(Items.TNT) || !state.getValue(AbstractFurnaceBlock.LIT)) {
			remakeArsenal$fuse.remove(be);
			return;
		}
		int t = remakeArsenal$fuse.merge(be, 1, Integer::sum);
		if (t == 60) {
			level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		if (t >= 90) {
			remakeArsenal$fuse.remove(be);
			be.setItem(0, ItemStack.EMPTY);
			level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
			ci.cancel();
		}
	}
}
