package com.remake.arsenal.block

import com.remake.arsenal.content.ModBlocks
import com.remake.arsenal.content.ModSounds
import com.remake.arsenal.logic.PyError
import com.remake.arsenal.logic.PyProgram
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/** Хранит программу Python, состояние запуска и память (save/load) умного блока. */
class SmartRedstoneBE(pos: BlockPos, state: BlockState) : BlockEntity(ModBlocks.SMART_BE, pos, state) {
	companion object {
		private const val MAX_RADIUS = 8
		private const val MAX_WORLD_OPS = 16
		private val FORBIDDEN = listOf("command_block", "structure_block", "jigsaw", "bedrock", "end_portal", "spawner", "barrier", "light")
	}

	var code = ""
	var enabled = false
	var interval = 10
	var error = ""
	private var program: PyProgram? = null
	private var phase = 0
	private val mem = HashMap<String, Any>()

	val hasProgram: Boolean get() = program != null

	/** Компилирует код. Возвращает текст ошибки или null, если всё хорошо (тогда блок запускается). */
	fun loadProgram(src: String): String? {
		return try {
			program = PyProgram.compile(src); code = src; error = ""; enabled = true; phase = 0
			syncState(); setChanged(); null
		} catch (e: PyError) {
			"строка ${e.line}: ${e.message}"
		}
	}

	fun setEnabled(v: Boolean) { enabled = v; if (v) error = ""; syncState(); setChanged() }

	fun cycleInterval() {
		val i = SmartRedstoneBlock.INTERVALS.indexOf(interval)
		interval = SmartRedstoneBlock.INTERVALS[(i + 1) % SmartRedstoneBlock.INTERVALS.size]; setChanged()
	}

	private fun syncState() {
		val lvl = level ?: return
		val st = lvl.getBlockState(worldPosition)
		if (st.block !is SmartRedstoneBlock) return
		val active = enabled && program != null && error.isEmpty()
		if (st.getValue(SmartRedstoneBlock.ACTIVE) != active || (!active && st.getValue(SmartRedstoneBlock.POWER) != 0)) {
			lvl.setBlock(worldPosition, st.setValue(SmartRedstoneBlock.ACTIVE, active).setValue(SmartRedstoneBlock.POWER, if (active) st.getValue(SmartRedstoneBlock.POWER) else 0), 3)
		}
	}

	fun serverTick(level: ServerLevel, pos: BlockPos, state: BlockState) {
		val prog = program ?: return
		if (!enabled) return
		if (++phase < interval) return
		phase = 0
		var out = state.getValue(SmartRedstoneBlock.POWER)
		var ops = 0
		fun rel(a: List<Any?>, i: Int): BlockPos {
			val dx = num(a, i); val dy = num(a, i + 1); val dz = num(a, i + 2)
			if (kotlin.math.abs(dx) > MAX_RADIUS || kotlin.math.abs(dy) > MAX_RADIUS || kotlin.math.abs(dz) > MAX_RADIUS) throw PyError("слишком далеко (максимум $MAX_RADIUS блоков)", 0)
			return pos.offset(dx, dy, dz)
		}
		fun spend() { if (++ops > MAX_WORLD_OPS) throw PyError("слишком много действий с миром за один запуск (максимум $MAX_WORLD_OPS)", 0) }

		val funcs = HashMap<String, (List<Any?>) -> Any?>()
		funcs["redstone"] = { a -> out = num(a, 0).coerceIn(0, 15); null }
		funcs["output"] = funcs["redstone"]!!
		funcs["input"] = { _ -> level.getBestNeighborSignal(pos).toDouble() }
		funcs["input_side"] = { a ->
			val d = Direction.byName(a.getOrNull(0)?.toString() ?: "") ?: throw PyError("сторона: north/south/east/west/up/down", 0)
			level.getSignal(pos.relative(d), d).toDouble()
		}
		funcs["time"] = { _ -> level.gameTime.toDouble() }
		funcs["random"] = { a -> level.random.nextInt(num(a, 0).coerceAtLeast(1)).toDouble() }
		funcs["block"] = { a -> BuiltInRegistries.BLOCK.getKey(level.getBlockState(rel(a, 0)).block).toString() }
		funcs["is_air"] = { a -> level.getBlockState(rel(a, 0)).isAir }
		funcs["power"] = { a -> level.getBestNeighborSignal(rel(a, 0)).toDouble() }
		funcs["place"] = { a ->
			spend(); val p = rel(a, 0); val id = a.getOrNull(3)?.toString() ?: throw PyError("place(dx,dy,dz,\"блок\")", 0)
			val key = ResourceLocation.tryParse(id) ?: throw PyError("плохой id блока '$id'", 0)
			val blk = BuiltInRegistries.BLOCK.getOptional(key).orElseThrow { PyError("неизвестный блок '$id'", 0) }
			if (FORBIDDEN.any { key.path.contains(it) }) throw PyError("блок '$id' запрещён", 0)
			val cur = level.getBlockState(p)
			if (cur.isAir || cur.canBeReplaced()) { level.setBlock(p, blk.defaultBlockState(), 3); true } else false
		}
		funcs["destroy"] = { a ->
			spend(); val p = rel(a, 0); val st = level.getBlockState(p)
			if (st.isAir || st.getDestroySpeed(level, p) < 0 || p == pos) false else level.destroyBlock(p, true)
		}
		funcs["set"] = { a ->
			spend(); val p = rel(a, 0); val st = level.getBlockState(p)
			val prop = st.properties.firstOrNull { it.name == a.getOrNull(3)?.toString() } ?: throw PyError("у блока нет свойства '${a.getOrNull(3)}'", 0)
			val ns = withProp<Int>(st, prop, fmtArg(a.getOrNull(4)))
			if (ns == null) false else { level.setBlock(p, ns, 3); true }
		}
		funcs["get"] = { a ->
			val st = level.getBlockState(rel(a, 0))
			val prop = st.properties.firstOrNull { it.name == a.getOrNull(3)?.toString() } ?: throw PyError("у блока нет свойства '${a.getOrNull(3)}'", 0)
			propName<Int>(st, prop)
		}
		funcs["players"] = { a -> val r = num(a, 0).coerceIn(1, 32); val c = Vec3.atCenterOf(pos); level.players().count { it.distanceToSqr(c) <= r * r }.toDouble() }
		funcs["mobs"] = { a -> val r = num(a, 0).coerceIn(1, 32); level.getEntitiesOfClass(LivingEntity::class.java, AABB(pos).inflate(r.toDouble())) { it !is Player }.size.toDouble() }
		funcs["say"] = { a ->
			val msg = Component.literal(a.joinToString(" ") { com.remake.arsenal.logic.fmt(it) })
			val c = Vec3.atCenterOf(pos)
			level.players().filter { it.distanceToSqr(c) <= 256.0 }.forEach { it.displayClientMessage(msg, true) }; null
		}
		funcs["save"] = { a ->
			val k = a.getOrNull(0)?.toString() ?: throw PyError("save(\"имя\", значение)", 0)
			val v = a.getOrNull(1) ?: throw PyError("save(\"имя\", значение)", 0)
			if (mem.size > 64 && !mem.containsKey(k)) throw PyError("слишком много сохранённых значений (максимум 64)", 0)
			mem[k] = v; null
		}
		funcs["load"] = { a -> mem[a.getOrNull(0)?.toString() ?: ""] ?: a.getOrNull(1) ?: 0.0 }

		try {
			prog.run(funcs)
		} catch (e: PyError) {
			error = "строка ${e.line}: ${e.message}"; enabled = false
			level.playSound(null, pos, ModSounds.SMART_ERROR, SoundSource.BLOCKS, 1.0f, 1.0f)
			syncState(); setChanged(); return
		}
		val cur = level.getBlockState(pos)
		if (cur.block is SmartRedstoneBlock && cur.getValue(SmartRedstoneBlock.POWER) != out) {
			level.setBlock(pos, cur.setValue(SmartRedstoneBlock.POWER, out), 3)
		}
		setChanged()
	}

	private fun num(a: List<Any?>, i: Int): Int {
		val v = a.getOrNull(i)
		return when (v) { is Double -> v.toInt(); is Boolean -> if (v) 1 else 0; else -> throw PyError("аргумент ${i + 1} должен быть числом", 0) }
	}

	private fun fmtArg(v: Any?): String = com.remake.arsenal.logic.fmt(v).let { if (it == "True") "true" else if (it == "False") "false" else it }

	@Suppress("UNCHECKED_CAST")
	private fun <T : Comparable<T>> withProp(s: BlockState, p: Property<*>, v: String): BlockState? {
		val pp = p as Property<T>
		val o = pp.getValue(v)
		return if (o.isPresent) s.setValue(pp, o.get()) else null
	}

	@Suppress("UNCHECKED_CAST")
	private fun <T : Comparable<T>> propName(s: BlockState, p: Property<*>): String {
		val pp = p as Property<T>
		return pp.getName(s.getValue(pp))
	}

	override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		tag.putString("code", code); tag.putBoolean("enabled", enabled); tag.putInt("interval", interval); tag.putString("error", error)
		val m = CompoundTag()
		for ((k, v) in mem) when (v) { is Double -> m.putDouble("n:$k", v); is Boolean -> m.putBoolean("b:$k", v); else -> m.putString("s:$k", v.toString()) }
		tag.put("mem", m)
	}

	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		code = tag.getString("code"); enabled = tag.getBoolean("enabled"); error = tag.getString("error")
		interval = tag.getInt("interval").let { if (it in SmartRedstoneBlock.INTERVALS) it else 10 }
		mem.clear()
		val m = tag.getCompound("mem")
		for (key in m.allKeys) {
			val name = key.substring(2)
			when (key[0]) { 'n' -> mem[name] = m.getDouble(key); 'b' -> mem[name] = m.getBoolean(key); 's' -> mem[name] = m.getString(key) }
		}
		program = if (code.isBlank()) null else try { PyProgram.compile(code) } catch (_: PyError) { null }
	}
}
