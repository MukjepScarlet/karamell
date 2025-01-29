package moe.mukjep.karamell

interface Token<T : Any> {
    /**
     * Give a hint for this token. Basically, it's the usage or type of the parameter.
     */
    fun hint(): String = "TOKEN"

    /**
     * Give a suggest list for [current] input, depending on the Token type.
     */
    fun suggest(current: String): List<String> = emptyList()

    /**
     * A token receives a part of full command and try to parse it into a specific type.
     * @return `null`: rejected; other: accepted
     */
    fun convert(part: String): T?
}

inline fun <T : Any> Token(crossinline converter: (String) -> T?) = object : Token<T> {
    override fun convert(part: String) = converter(part)
}

fun <T : Any> Token<T>.hint(hint: String): Token<T> = hint { hint }

inline fun <T : Any> Token<T>.hint(
    crossinline hintProvider: () -> String
): Token<T> = object : Token<T> {
    override fun hint() = hintProvider()
    override fun suggest(current: String) = this@hint.suggest(current)
    override fun convert(part: String) = this@hint.convert(part)
}

fun <T : Any> Token<T>.suggest(suggestions: List<String>): Token<T> = suggest { suggestions }

inline fun <T : Any> Token<T>.suggest(
    crossinline suggestionsProvider: (current: String) -> List<String>
): Token<T> = object : Token<T> {
    override fun hint() = this@suggest.hint()
    override fun suggest(current: String) = suggestionsProvider(current)
    override fun convert(part: String) = this@suggest.convert(part)
}

inline fun <T : Any, R : Any> Token<T>.transform(crossinline transformer: (T) -> R?): Token<R> = object : Token<R> {
    override fun hint() = this@transform.hint()
    override fun suggest(current: String) = this@transform.suggest(current)
    override fun convert(part: String) = this@transform.convert(part)?.let(transformer)
}

internal class CombinedToken<T : Any>(
    private vararg val tokens: Token<T>,
) : Token<T> {
    override fun hint() = tokens.joinToString(separator = "|") { it.hint() }

    override fun suggest(current: String) = tokens.flatMap { it.suggest(current) }

    override fun convert(part: String) = tokens.firstNotNullOfOrNull { it.convert(part) }
}

operator fun <T : Any> Token<T>.plus(other: Token<T>): Token<T> = CombinedToken(this, other)

fun <T : Any> Array<out Token<T>>.combined(): Token<T> = CombinedToken(tokens = this)

fun <T : Any> Iterable<Token<T>>.combined(): Token<T> = CombinedToken(tokens = this.toList().toTypedArray())

internal class VarargToken<T : Any>(
    private val token: Token<T>,
) : Token<T> by token {
    init {
        require(token !is VarargToken)
    }

    override fun hint(): String {
        return token.hint() + "..."
    }
}

fun <T : Any> Token<T>.vararg(): Token<T> = VarargToken(this)
