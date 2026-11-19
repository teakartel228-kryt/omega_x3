package com.remake.arsenal.logic

import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.Filterable
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import net.minecraft.world.level.saveddata.SavedData
import java.util.Random
import java.util.UUID

/** Письма в бутылках: игрок «отправляет» записку в море и через пару минут получает книгу с ответом. */
class BottleMail : SavedData() {
	class Entry(val player: UUID, val text: String, val deliverAt: Long)

	val entries = ArrayList<Entry>()

	override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
		val list = ListTag()
		for (e in entries) {
			val c = CompoundTag()
			c.putUUID("p", e.player)
			c.putString("t", e.text)
			c.putLong("at", e.deliverAt)
			list.add(c)
		}
		tag.put("entries", list)
		return tag
	}

	companion object {
		private const val NAME = "remake_arsenal_bottle_mail"

		fun load(tag: CompoundTag, registries: HolderLookup.Provider): BottleMail {
			val m = BottleMail()
			val list = tag.getList("entries", 10)
			for (i in 0 until list.size) {
				val c = list.getCompound(i)
				m.entries.add(Entry(c.getUUID("p"), c.getString("t"), c.getLong("at")))
			}
			return m
		}

		fun get(server: MinecraftServer): BottleMail =
			server.overworld().dataStorage.computeIfAbsent(SavedData.Factory<BottleMail>({ BottleMail() }, { t, r -> load(t, r) }, null), NAME)

		/** Бутылка ушла в море: ответ придёт через 1–3 минуты. */
		fun send(player: ServerPlayer, text: String) {
			val server = player.server
			val m = get(server)
			val delay = 1200L + player.random.nextInt(2400)
			m.entries.add(Entry(player.uuid, text, server.overworld().gameTime + delay))
			m.setDirty()
		}

		fun deliver(server: MinecraftServer) {
			val m = get(server)
			if (m.entries.isEmpty()) return
			val now = server.overworld().gameTime
			val it = m.entries.iterator()
			var changed = false
			while (it.hasNext()) {
				val e = it.next()
				if (e.deliverAt > now) continue
				val p = server.playerList.getPlayer(e.player) ?: continue // придёт, когда игрок зайдёт
				giveReply(p, e.text)
				it.remove()
				changed = true
			}
			if (changed) m.setDirty()
		}

		private fun giveReply(player: ServerPlayer, question: String) {
			val rnd = Random(question.hashCode().toLong() * 31 + player.level().gameTime)
			val answer = BottleAnswers.answer(question, rnd)
			val ru = BottleAnswers.isRussian(question)
			val pages = answer.chunked(240).map { Filterable.passThrough<Component>(Component.literal(it)) }
			val book = ItemStack(Items.WRITTEN_BOOK)
			book.set(
				DataComponents.WRITTEN_BOOK_CONTENT,
				WrittenBookContent(Filterable.passThrough(if (ru) "Ответ моря" else "Sea's Reply"), if (ru) "Море" else "The Sea", 0, pages, true)
			)
			if (!player.addItem(book)) {
				val drop = ItemEntity(player.level(), player.x, player.y, player.z, book)
				player.level().addFreshEntity(drop)
			}
			player.displayClientMessage(
				Component.translatable("msg.remake_arsenal.bottle_reply"), false
			)
		}
	}
}

/** Ответы моря: случайные, но подбираются по теме вопроса. */
object BottleAnswers {
	fun isRussian(q: String): Boolean {
		val cyr = q.count { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }
		val lat = q.count { it in 'a'..'z' || it in 'A'..'Z' }
		return cyr >= lat
	}

	private val RU = mapOf(
		"yesno" to listOf("Да. Волны так и шепчут.", "Нет. И не стоит надеяться.", "Скорее да, чем нет.", "Скорее нет, но чудеса случаются.", "Спроси ещё раз, когда луна будет полной.", "Море молчит, но чайки кивают: да."),
		"when" to listOf("Когда прилив коснётся третьей скалы.", "Скоро. Быстрее, чем кажется.", "Не раньше следующего рассвета.", "Когда перестанешь ждать.", "Через столько дней, сколько на дне ракушек."),
		"where" to listOf("Ищи там, где пахнет солью и ржавым железом.", "На севере, под старым деревом.", "Глубже, чем ты копал.", "Там, где ты уже был, но не смотрел внимательно.", "У воды. Всегда у воды."),
		"who" to listOf("Тот, кто громче всех молчит.", "Старый друг в новом обличье.", "Никто. Это сделал ветер.", "Ты сам, только вчерашний."),
		"how" to listOf("Терпением и крепким канатом.", "Не торопись: прилив всё сделает за тебя.", "Начни с малого, закончи большим.", "Так, как делал бы это пират: смело и с запасным планом."),
		"why" to listOf("Потому что так решили звёзды.", "Потому что иначе было бы скучно.", "Море знает, но не скажет.", "Причина проста: ты спросил."),
		"love" to listOf("Сердце знает дорогу лучше карты.", "Письмо дойдёт, если не побоишься отправить.", "Не торопи волну — она вернётся сама."),
		"wealth" to listOf("Сундук есть, но ключ у русалки.", "Богатство ближе, чем кажется: загляни под ноги.", "Блестит не всё, что золото, но тебе повезёт."),
		"howmuch" to listOf("Ровно столько, сколько нужно.", "Больше, чем ты думаешь.", "Меньше, чем ты боишься."),
		"default" to listOf("Бутылка прошла сто миль, чтобы сказать: всё будет хорошо.", "Ветер сменится — и всё встанет на места.", "Море прочитало твоё письмо и улыбнулось.", "Не отчаивайся: даже самый далёкий берег когда-нибудь виден.", "Ответ уже в твоём кармане. Поищи получше.")
	)
	private val EN = mapOf(
		"yesno" to listOf("Yes. The waves whisper it.", "No. Do not hope for it.", "More likely yes than no.", "Probably not, but miracles happen.", "Ask again under a full moon.", "The sea is silent, but the gulls nod: yes."),
		"when" to listOf("When the tide touches the third rock.", "Soon. Sooner than you think.", "Not before the next sunrise.", "When you stop waiting.", "In as many days as there are shells."),
		"where" to listOf("Look where it smells of salt and rusty iron.", "To the north, under an old tree.", "Deeper than you dug.", "Where you already were, but did not look closely.", "By the water. Always by the water."),
		"who" to listOf("The one who is loudest in their silence.", "An old friend in a new face.", "Nobody. The wind did it.", "You, only from yesterday."),
		"how" to listOf("With patience and a strong rope.", "Do not rush: the tide will do it for you.", "Start small, finish big.", "Like a pirate: boldly, with a backup plan."),
		"why" to listOf("Because the stars decided so.", "Because otherwise it would be boring.", "The sea knows, but will not tell.", "The reason is simple: you asked."),
		"love" to listOf("The heart knows the way better than a map.", "The letter will arrive if you dare to send it.", "Do not rush the wave. It will return."),
		"wealth" to listOf("There is a chest, but the mermaid holds the key.", "Wealth is closer than you think: look down.", "Not all that glitters is gold, but you will be lucky."),
		"howmuch" to listOf("Exactly as much as needed.", "More than you think.", "Less than you fear."),
		"default" to listOf("This bottle sailed a hundred miles to say: it will be fine.", "The wind will change and everything falls into place.", "The sea read your letter and smiled.", "Do not despair: even the farthest shore is visible someday.", "The answer is already in your pocket. Look harder.")
	)

	private fun category(q: String): String {
		val s = q.lowercase()
		return when {
			Regex("(любов|любит|сердц|love|crush)").containsMatchIn(s) -> "love"
			Regex("(деньг|алмаз|золот|богат|клад|сокров|money|diamond|gold|rich|treasure)").containsMatchIn(s) -> "wealth"
			Regex("(сколько|how many|how much)").containsMatchIn(s) -> "howmuch"
			Regex("(когда|when)").containsMatchIn(s) -> "when"
			Regex("(где|куда|откуда|where)").containsMatchIn(s) -> "where"
			Regex("(кто|who)").containsMatchIn(s) -> "who"
			Regex("(почему|зачем|why)").containsMatchIn(s) -> "why"
			Regex("(как |how )").containsMatchIn(s) -> "how"
			Regex("(\\?|^(ли|можно|будет|стоит|is |are |can |will |should |do |does ))").containsMatchIn(s) -> "yesno"
			else -> "default"
		}
	}

	fun answer(question: String, rnd: Random): String {
		val ru = isRussian(question)
		val pool = if (ru) RU else EN
		val q = question.trim().replace(Regex("\\s+"), " ")
		if (q.isEmpty()) {
			return if (ru) "В бутылке был пустой лист, и море ответило тишиной.\n\n" + pool.getValue("default").random(kotlin.random.Random(rnd.nextLong()))
			else "The bottle held an empty sheet, and the sea answered with silence.\n\n" + pool.getValue("default").random(kotlin.random.Random(rnd.nextLong()))
		}
		val cat = category(q)
		val line = pool.getValue(cat).random(kotlin.random.Random(rnd.nextLong()))
		val quote = if (q.length > 70) q.take(70) + "…" else q
		val extra = if (cat == "howmuch") " (" + (1 + rnd.nextInt(99)) + ")" else ""
		return if (ru) "Ты написал: «$quote»\n\n$line$extra\n\n— Море" else "You wrote: \"$quote\"\n\n$line$extra\n\n— The Sea"
	}

	/** Текст записки из книги (первая страница). */
	fun extractText(book: ItemStack): String {
		book.get(DataComponents.WRITABLE_BOOK_CONTENT)?.let { c ->
			val p = c.pages().firstOrNull()
			if (p != null) return p.raw()
		}
		book.get(DataComponents.WRITTEN_BOOK_CONTENT)?.let { c ->
			val p = c.pages().firstOrNull()
			if (p != null) return p.raw().string
		}
		return ""
	}
}
