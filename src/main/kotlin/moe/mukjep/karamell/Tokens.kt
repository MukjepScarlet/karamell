package moe.mukjep.karamell

internal val EmptyToken: Token<Nothing> = object : Token<Nothing> {
    override fun hint() = ""
    override fun convert(part: String) = null
}

private val TextToken: Token<String> = object : Token<String> {
    override fun hint() = "Text"
    override fun convert(part: String) = part
}

private class LiteralTextToken(private val example: String, private val ignoreCase: Boolean) : Token<String> {
    override fun hint() = example
    override fun convert(part: String) = part.takeIf { it.contentEquals(example, ignoreCase) }
    override fun suggest(current: String): List<String> =
        if (example.startsWith(current, ignoreCase)) {
            listOf(example)
        } else {
            emptyList()
        }
}

private class LimitedTextToken(private val lengthRange: IntRange) : Token<String> {
    override fun hint() = "Text(Length: $lengthRange)"
    override fun convert(part: String) = part.takeIf { it.length in lengthRange }
}

private class RegexToken(private val regex: Regex) : Token<MatchResult> {
    override fun hint() = "Regex(${regex.pattern})"
    override fun convert(part: String) = regex.find(part)
}

private class EnumToken<T : Enum<T>>(private val values: Array<out T>) : Token<T> {
    override fun hint() = values.joinToString(separator = "|") { it.name }
    override fun convert(part: String) = values.firstOrNull { it.name.contentEquals(part, ignoreCase = true) }
    override fun suggest(current: String): List<String> = values.map { it.name }
}

private val BoolToken = object : Token<Boolean> {
    override fun hint() = "true|t|yes|on|false|f|no|off"
    override fun convert(part: String) =
        when (part.lowercase()) {
            "true", "t", "yes", "on" -> true
            "false", "f", "no", "off" -> false
            else -> null
        }

    override fun suggest(current: String): List<String> = listOf("true", "false")
}

private class IntToken(private val radix: Int): Token<Int> {
    override fun hint() = "Int(radix=$radix)"
    override fun convert(part: String) = part.toIntOrNull(radix)
}

private class RangedIntToken(private val radix: Int, private val range: IntRange): Token<Int> {
    private val suggestion = listOf(range.first.toString(radix), range.last.toString(radix))

    override fun hint() = "Int(radix=$radix, $range)"
    override fun convert(part: String) = part.toIntOrNull(radix).takeIf { it in range }
    override fun suggest(current: String): List<String> = suggestion
}

private val FloatToken = object : Token<Float> {
    override fun hint() = "Float"
    override fun convert(part: String) = part.toFloatOrNull()
}

private class RangedFloatToken(private val range: ClosedFloatingPointRange<Float>): Token<Float> {
    private val suggestion = listOf(range.start.toString(), range.endInclusive.toString())

    override fun hint() = "Float($range)"
    override fun convert(part: String) = part.toFloatOrNull()?.takeIf { it in range }
    override fun suggest(current: String): List<String> = suggestion
}

private val DoubleToken = object : Token<Double> {
    override fun hint() = "Double"
    override fun convert(part: String) = part.toDoubleOrNull()
}

private class RangedDoubleToken(private val range: ClosedFloatingPointRange<Double>): Token<Double> {
    private val suggestion = listOf(range.start.toString(), range.endInclusive.toString())

    override fun hint() = "Double($range)"
    override fun convert(part: String) = part.toDoubleOrNull()?.takeIf { it in range }
    override fun suggest(current: String): List<String> = suggestion
}

private val INT_DEC: Token<Int> = object : Token<Int> {
    override fun hint() = "Int"
    override fun convert(part: String) = part.toIntOrNull(radix = 10)
}

private val INT_HEX: Token<Int> = object : Token<Int> {
    override fun hint() = "Int(Hex)"
    override fun convert(part: String) = part.toIntOrNull(radix = 16)
}

fun SubScope.empty(): Token<Nothing> = EmptyToken

fun SubScope.text(): Token<String> = TextToken

inline fun SubScope.text(crossinline predicate: (String) -> Boolean): Token<String> =
    Token {
        it.takeIf(predicate)
    }

fun SubScope.oneOf(ignoreCase: Boolean = true, entriesProvider: () -> Iterable<String>): Token<String> = object : Token<String> {
    override fun hint() = entriesProvider().joinToString(separator = "|")
    override fun convert(part: String) = part.takeIf { entriesProvider().any { it.contentEquals(part, ignoreCase) } }
    override fun suggest(current: String): List<String> = entriesProvider().filter { it.startsWith(current, ignoreCase) }
}

fun SubScope.oneOf(ignoreCase: Boolean = true, entries: Iterable<String>): Token<String> = object : Token<String> {
    override fun hint() = entries.joinToString(separator = "|")
    override fun convert(part: String) = part.takeIf { it in entries }
    override fun suggest(current: String): List<String> = entries.filter { it.startsWith(current, ignoreCase) }
}

fun SubScope.text(lengthRange: IntRange): Token<String> = LimitedTextToken(lengthRange)

fun SubScope.text(example: String, ignoreCase: Boolean = true): Token<String> = LiteralTextToken(example, ignoreCase)

fun SubScope.regex(regex: Regex): Token<MatchResult> = RegexToken(regex)

fun <T : Enum<T>> SubScope.enum(vararg values: T): Token<T> = EnumToken(values)

inline fun <reified T : Enum<T>> SubScope.enum(): Token<T> = enum(values = enumValues())

fun SubScope.bool(): Token<Boolean> = BoolToken

fun SubScope.int(radix: Int = 10): Token<Int> = when (radix) {
    10 -> INT_DEC
    16 -> INT_HEX
    else -> IntToken(radix)
}

fun SubScope.int(radix: Int = 10, range: IntRange): Token<Int> = RangedIntToken(radix, range)

fun SubScope.float(): Token<Float> = FloatToken

fun SubScope.float(range: ClosedFloatingPointRange<Float>): Token<Float> = RangedFloatToken(range)

fun SubScope.double(): Token<Double> = DoubleToken

fun SubScope.double(range: ClosedFloatingPointRange<Double>): Token<Double> = RangedDoubleToken(range)
