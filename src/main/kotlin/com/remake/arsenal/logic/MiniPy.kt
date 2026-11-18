package com.remake.arsenal.logic

import kotlin.math.floor
import kotlin.math.pow

/**
 * Мини-Python для «Умного редстоун-блока».
 * Настоящий CPython/Jython в мод не встроить (нет в игре, небезопасно), поэтому здесь свой безопасный интерпретатор
 * подмножества Python: if/elif/else, while, for i in range(...), присваивания (=, +=, -=, *=, /=, //=, %=),
 * числа, строки, True/False, and/or/not, сравнения, арифметика, вызовы функций, комментарии #.
 * Скрипт запускается заново каждые N тиков; между запусками данные хранятся через save()/load().
 */
class PyError(msg: String, val line: Int) : RuntimeException(msg)

private class BreakEx : RuntimeException(null, null, false, false)
private class ContinueEx : RuntimeException(null, null, false, false)

private sealed class Expr
private class Const(val v: Any?) : Expr()
private class VarE(val name: String, val line: Int) : Expr()
private class BinE(val op: String, val l: Expr, val r: Expr, val line: Int) : Expr()
private class UnE(val op: String, val e: Expr, val line: Int) : Expr()
private class CallE(val name: String, val args: List<Expr>, val line: Int) : Expr()

private sealed class Stmt(val line: Int)
private class AssignS(line: Int, val name: String, val op: String, val e: Expr) : Stmt(line)
private class ExprS(line: Int, val e: Expr) : Stmt(line)
private class IfS(line: Int, val branches: List<Pair<Expr, List<Stmt>>>, val orelse: List<Stmt>) : Stmt(line)
private class WhileS(line: Int, val cond: Expr, val body: List<Stmt>) : Stmt(line)
private class ForS(line: Int, val name: String, val args: List<Expr>, val body: List<Stmt>) : Stmt(line)
private class FlowS(line: Int, val kind: String) : Stmt(line)

private class Tok(val kind: Char, val text: String, val num: Double = 0.0)

class PyProgram internal constructor(private val stmts: List<Any>) {
	val lineCount: Int get() = stmts.size

	fun run(funcs: Map<String, (List<Any?>) -> Any?>, maxSteps: Int = 4000) {
		@Suppress("UNCHECKED_CAST")
		Interp(funcs, maxSteps).execBlock(stmts as List<Stmt>, HashMap())
	}

	companion object {
		fun compile(src: String): PyProgram {
			val lines = ArrayList<Triple<Int, String, Int>>() // indent, text, номер строки
			src.split("\n").forEachIndexed { i, raw ->
				val text = stripComment(raw.trimEnd('\r'))
				if (text.isBlank()) return@forEachIndexed
				var ind = 0
				for (ch in text) { if (ch == ' ') ind++ else if (ch == '\t') ind += 4 else break }
				lines.add(Triple(ind, text.trim(), i + 1))
			}
			val pos = intArrayOf(0)
			val base = if (lines.isEmpty()) 0 else lines[0].first
			val block = Parser(lines).block(pos, base)
			if (pos[0] < lines.size) throw PyError("неверный отступ", lines[pos[0]].third)
			return PyProgram(block)
		}

		private fun stripComment(s: String): String {
			var q = 0.toChar()
			for (i in s.indices) {
				val c = s[i]
				if (q != 0.toChar()) { if (c == q) q = 0.toChar() } else if (c == '"' || c == '\'') q = c else if (c == '#') return s.substring(0, i)
			}
			return s
		}
	}

	// ---------------- парсер ----------------
	private class Parser(val lines: List<Triple<Int, String, Int>>) {
		fun block(pos: IntArray, indent: Int): List<Stmt> {
			val out = ArrayList<Stmt>()
			while (pos[0] < lines.size) {
				val (ind, text, no) = lines[pos[0]]
				if (ind < indent) break
				if (ind > indent) throw PyError("лишний отступ", no)
				pos[0]++
				out.add(statement(text, no, pos, indent))
			}
			return out
		}

		private fun body(pos: IntArray, indent: Int, no: Int): List<Stmt> {
			if (pos[0] >= lines.size || lines[pos[0]].first <= indent) throw PyError("ожидался блок с отступом", no)
			return block(pos, lines[pos[0]].first)
		}

		private fun cond(text: String, from: Int, no: Int): Expr {
			if (!text.endsWith(":")) throw PyError("ожидалось ':'", no)
			return expr(text.substring(from, text.length - 1), no)
		}

		private fun statement(text: String, no: Int, pos: IntArray, indent: Int): Stmt {
			when {
				text.startsWith("if ") -> {
					val branches = ArrayList<Pair<Expr, List<Stmt>>>()
					branches.add(cond(text, 3, no) to body(pos, indent, no))
					var orelse: List<Stmt> = emptyList()
					while (pos[0] < lines.size && lines[pos[0]].first == indent) {
						val t = lines[pos[0]].second; val n = lines[pos[0]].third
						if (t.startsWith("elif ")) { pos[0]++; branches.add(cond(t, 5, n) to body(pos, indent, n)) }
						else if (t == "else:") { pos[0]++; orelse = body(pos, indent, n); break }
						else break
					}
					return IfS(no, branches, orelse)
				}
				text.startsWith("while ") -> return WhileS(no, cond(text, 6, no), body(pos, indent, no))
				text.startsWith("for ") -> {
					val m = Regex("^for\\s+([A-Za-z_]\\w*)\\s+in\\s+range\\((.*)\\)\\s*:$").find(text) ?: throw PyError("поддерживается только: for i in range(...):", no)
					val args = splitArgs(m.groupValues[2]).map { expr(it, no) }
					if (args.isEmpty() || args.size > 3) throw PyError("range() принимает 1–3 аргумента", no)
					return ForS(no, m.groupValues[1], args, body(pos, indent, no))
				}
				text == "pass" -> return FlowS(no, "pass")
				text == "break" -> return FlowS(no, "break")
				text == "continue" -> return FlowS(no, "continue")
			}
			val id = Regex("^[A-Za-z_]\\w*").find(text)
			if (id != null) {
				val rest = text.substring(id.value.length).trimStart()
				for (op in listOf("//=", "+=", "-=", "*=", "/=", "%=", "=")) {
					if (rest.startsWith(op) && !(op == "=" && rest.startsWith("=="))) {
						return AssignS(no, id.value, op, expr(rest.substring(op.length), no))
					}
				}
			}
			return ExprS(no, expr(text, no))
		}

		fun splitArgs(s: String): List<String> {
			if (s.isBlank()) return emptyList()
			val out = ArrayList<String>(); var depth = 0; var q = 0.toChar(); var start = 0
			for (i in s.indices) {
				val c = s[i]
				if (q != 0.toChar()) { if (c == q) q = 0.toChar() }
				else if (c == '"' || c == '\'') q = c
				else if (c == '(') depth++ else if (c == ')') depth--
				else if (c == ',' && depth == 0) { out.add(s.substring(start, i)); start = i + 1 }
			}
			out.add(s.substring(start)); return out
		}

		fun expr(s: String, no: Int): Expr = ExprParser(tokenize(s, no), no).parse()

		private fun tokenize(s: String, no: Int): List<Tok> {
			val t = ArrayList<Tok>(); var i = 0
			while (i < s.length) {
				val c = s[i]
				when {
					c == ' ' || c == '\t' -> i++
					c.isDigit() || (c == '.' && i + 1 < s.length && s[i + 1].isDigit()) -> {
						var j = i; while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
						t.add(Tok('n', s.substring(i, j), s.substring(i, j).toDoubleOrNull() ?: throw PyError("плохое число", no))); i = j
					}
					c.isLetter() || c == '_' -> { var j = i; while (j < s.length && (s[j].isLetterOrDigit() || s[j] == '_')) j++; t.add(Tok('i', s.substring(i, j))); i = j }
					c == '"' || c == '\'' -> {
						var j = i + 1; val sb = StringBuilder()
						while (j < s.length && s[j] != c) { if (s[j] == '\\' && j + 1 < s.length) { j++; sb.append(if (s[j] == 'n') '\n' else s[j]) } else sb.append(s[j]); j++ }
						if (j >= s.length) throw PyError("незакрытая строка", no)
						t.add(Tok('s', sb.toString())); i = j + 1
					}
					else -> {
						val two = if (i + 1 < s.length) s.substring(i, i + 2) else ""
						if (two in listOf("==", "!=", "<=", ">=", "//", "**")) { t.add(Tok('o', two)); i += 2 }
						else if (c in "+-*/%<>(),") { t.add(Tok('o', c.toString())); i++ }
						else throw PyError("неожиданный символ '$c'", no)
					}
				}
			}
			return t
		}
	}

	private class ExprParser(val t: List<Tok>, val line: Int) {
		var p = 0
		fun parse(): Expr { val e = or(); if (p < t.size) throw PyError("лишнее в выражении: '${t[p].text}'", line); return e }
		private fun isOp(x: String) = p < t.size && t[p].kind == 'o' && t[p].text == x
		private fun isKw(x: String) = p < t.size && t[p].kind == 'i' && t[p].text == x
		private fun or(): Expr { var l = and(); while (isKw("or")) { p++; l = BinE("or", l, and(), line) }; return l }
		private fun and(): Expr { var l = not(); while (isKw("and")) { p++; l = BinE("and", l, not(), line) }; return l }
		private fun not(): Expr { if (isKw("not")) { p++; return UnE("not", not(), line) }; return cmp() }
		private fun cmp(): Expr {
			var l = add()
			while (p < t.size && t[p].kind == 'o' && t[p].text in listOf("==", "!=", "<", ">", "<=", ">=")) { val op = t[p++].text; l = BinE(op, l, add(), line) }
			return l
		}
		private fun add(): Expr { var l = mul(); while (isOp("+") || isOp("-")) { val op = t[p++].text; l = BinE(op, l, mul(), line) }; return l }
		private fun mul(): Expr {
			var l = unary()
			while (isOp("*") || isOp("/") || isOp("//") || isOp("%")) { val op = t[p++].text; l = BinE(op, l, unary(), line) }
			return l
		}
		private fun unary(): Expr { if (isOp("-")) { p++; return UnE("-", unary(), line) }; if (isOp("+")) { p++; return unary() }; return pow() }
		private fun pow(): Expr { val l = atom(); if (isOp("**")) { p++; return BinE("**", l, unary(), line) }; return l }
		private fun atom(): Expr {
			if (p >= t.size) throw PyError("неожиданный конец выражения", line)
			val k = t[p++]
			when (k.kind) {
				'n' -> return Const(k.num)
				's' -> return Const(k.text)
				'i' -> {
					when (k.text) { "True" -> return Const(true); "False" -> return Const(false); "None" -> return Const(null) }
					if (isOp("(")) {
						p++; val args = ArrayList<Expr>()
						if (!isOp(")")) { while (true) { args.add(or()); if (isOp(",")) p++ else break } }
						if (!isOp(")")) throw PyError("ожидалось ')'", line); p++
						return CallE(k.text, args, line)
					}
					return VarE(k.text, line)
				}
				else -> if (k.text == "(") { val e = or(); if (!isOp(")")) throw PyError("ожидалось ')'", line); p++; return e }
			}
			throw PyError("неожиданный '${k.text}'", line)
		}
	}

	// ---------------- исполнение ----------------
	private class Interp(val funcs: Map<String, (List<Any?>) -> Any?>, val maxSteps: Int) {
		var steps = 0
		fun tick(line: Int) { if (++steps > maxSteps) throw PyError("программа слишком долгая (лимит $maxSteps шагов)", line) }

		fun execBlock(b: List<Stmt>, vars: HashMap<String, Any?>) { for (s in b) exec(s, vars) }

		fun exec(s: Stmt, v: HashMap<String, Any?>) {
			tick(s.line)
			when (s) {
				is AssignS -> {
					val rhs = eval(s.e, v)
					v[s.name] = if (s.op == "=") rhs else {
						val cur = v[s.name] ?: throw PyError("переменная '${s.name}' не определена", s.line)
						bin(s.op.dropLast(1), cur, rhs, s.line)
					}
				}
				is ExprS -> eval(s.e, v)
				is IfS -> {
					for ((c, b) in s.branches) if (truth(eval(c, v))) { execBlock(b, v); return }
					execBlock(s.orelse, v)
				}
				is WhileS -> while (truth(eval(s.cond, v))) {
					tick(s.line)
					try { execBlock(s.body, v) } catch (_: BreakEx) { break } catch (_: ContinueEx) { }
				}
				is ForS -> {
					val a = s.args.map { num(eval(it, v), s.line) }
					val (from, to, step) = when (a.size) { 1 -> Triple(0.0, a[0], 1.0); 2 -> Triple(a[0], a[1], 1.0); else -> Triple(a[0], a[1], a[2]) }
					if (step == 0.0) throw PyError("шаг range() не может быть 0", s.line)
					var i = from
					while (if (step > 0) i < to else i > to) {
						tick(s.line); v[s.name] = i
						try { execBlock(s.body, v) } catch (_: BreakEx) { break } catch (_: ContinueEx) { }
						i += step
					}
				}
				is FlowS -> when (s.kind) { "break" -> throw BreakEx(); "continue" -> throw ContinueEx() }
			}
		}

		fun eval(e: Expr, v: HashMap<String, Any?>): Any? = when (e) {
			is Const -> e.v
			is VarE -> if (v.containsKey(e.name)) v[e.name] else throw PyError("переменная '${e.name}' не определена", e.line)
			is UnE -> when (e.op) { "not" -> !truth(eval(e.e, v)); else -> -num(eval(e.e, v), e.line) }
			is BinE -> when (e.op) {
				"and" -> { val l = eval(e.l, v); if (!truth(l)) l else eval(e.r, v) }
				"or" -> { val l = eval(e.l, v); if (truth(l)) l else eval(e.r, v) }
				else -> bin(e.op, eval(e.l, v), eval(e.r, v), e.line)
			}
			is CallE -> call(e, v)
		}

		fun call(e: CallE, v: HashMap<String, Any?>): Any? {
			tick(e.line)
			val args = e.args.map { eval(it, v) }
			funcs[e.name]?.let { f -> try { return f(args) } catch (ex: PyError) { throw PyError(ex.message ?: "ошибка", e.line) } }
			return when (e.name) {
				"abs" -> kotlin.math.abs(num(args.getOrNull(0), e.line))
				"min" -> args.map { num(it, e.line) }.minOrNull() ?: throw PyError("min() без аргументов", e.line)
				"max" -> args.map { num(it, e.line) }.maxOrNull() ?: throw PyError("max() без аргументов", e.line)
				"int" -> { val d = num(args.getOrNull(0), e.line); if (d < 0) kotlin.math.ceil(d) else floor(d) }
				"round" -> Math.round(num(args.getOrNull(0), e.line)).toDouble()
				"str" -> fmt(args.getOrNull(0))
				"len" -> (args.getOrNull(0) as? String)?.length?.toDouble() ?: throw PyError("len() только для строк", e.line)
				else -> throw PyError("неизвестная функция '${e.name}'", e.line)
			}
		}

		fun truth(x: Any?): Boolean = when (x) { null -> false; is Boolean -> x; is Double -> x != 0.0; is String -> x.isNotEmpty(); else -> true }
		fun num(x: Any?, line: Int): Double = when (x) { is Double -> x; is Boolean -> if (x) 1.0 else 0.0; else -> throw PyError("ожидалось число, получено ${fmt(x)}", line) }

		fun bin(op: String, a: Any?, b: Any?, line: Int): Any? {
			if (op == "+" && (a is String || b is String)) return fmt(a) + fmt(b)
			if (op == "==") return eq(a, b)
			if (op == "!=") return !eq(a, b)
			if ((op == "<" || op == ">" || op == "<=" || op == ">=") && a is String && b is String) {
				val c = a.compareTo(b); return when (op) { "<" -> c < 0; ">" -> c > 0; "<=" -> c <= 0; else -> c >= 0 }
			}
			val x = num(a, line); val y = num(b, line)
			return when (op) {
				"+" -> x + y; "-" -> x - y; "*" -> x * y
				"/" -> if (y == 0.0) throw PyError("деление на ноль", line) else x / y
				"//" -> if (y == 0.0) throw PyError("деление на ноль", line) else floor(x / y)
				"%" -> if (y == 0.0) throw PyError("деление на ноль", line) else x - floor(x / y) * y
				"**" -> x.pow(y)
				"<" -> x < y; ">" -> x > y; "<=" -> x <= y; ">=" -> x >= y
				else -> throw PyError("неизвестный оператор $op", line)
			}
		}

		fun eq(a: Any?, b: Any?): Boolean = when {
			a is Double && b is Double -> a == b
			a is Boolean && b is Double -> (if (a) 1.0 else 0.0) == b
			a is Double && b is Boolean -> a == (if (b) 1.0 else 0.0)
			else -> a == b
		}
	}
}

fun fmt(v: Any?): String = when (v) {
	null -> "None"
	is Boolean -> if (v) "True" else "False"
	is Double -> if (v == floor(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()
	else -> v.toString()
}
