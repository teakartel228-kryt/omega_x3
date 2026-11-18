package com.remake.arsenal.mixin;

import com.remake.arsenal.logic.ProjectileRouter;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Перехватчик и разделитель снарядов: отменяем попадание в блок и отправляем снаряд через выходную сторону. */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
	@Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
	private void remake$route(HitResult hitResult, CallbackInfoReturnable<ProjectileDeflection> cir) {
		if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
			if (ProjectileRouter.tryRoute((Projectile) (Object) this, blockHit)) {
				cir.setReturnValue(ProjectileDeflection.NONE);
			}
		}
	}
}
