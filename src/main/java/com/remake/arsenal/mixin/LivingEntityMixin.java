package com.remake.arsenal.mixin;

import com.remake.arsenal.logic.ClawsHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Когти: игрок с когтями в руке лазает по стене, как по лестнице. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void remakeArsenal$clawsClimb(CallbackInfoReturnable<Boolean> cir) {
		if (ClawsHelper.shouldClimb((LivingEntity) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
