package com.remake.arsenal.logic

import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

object Chain {
	val IRON = DustParticleOptions(Vector3f(0.62f, 0.62f, 0.68f), 0.7f)

	/** Рисует «цепь» частицами между двумя точками. */
	fun draw(level: ServerLevel, from: Vec3, to: Vec3, particle: DustParticleOptions = IRON) {
		val diff = to.subtract(from)
		val len = diff.length()
		if (len < 0.1) return
		val step = diff.normalize().scale(0.7)
		val n = (len / 0.7).toInt().coerceAtMost(60)
		var p = from
		for (i in 0..n) {
			level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0)
			p = p.add(step)
		}
	}
}
