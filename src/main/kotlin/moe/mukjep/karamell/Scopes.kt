package moe.mukjep.karamell

@DslMarker
internal annotation class KaramellDsl

@KaramellDsl
class EndScope internal constructor(
    val parameters: List<Any?>
) {
    /**
     * @param index Can be negative or positive. result index = `(index + parameters.size) % parameters.size`
     */
    inline fun <reified T> param(
        index: Int
    ) = parameters[(index + parameters.size) % parameters.size].let {
        it as? T ?: throw ParameterTypeMismatchException("index=$index, value=$it, type=${it?.javaClass?.simpleName}")
    }

    inline fun <reified T> params(
        indexRange: IntRange
    ) = params<T>(indexRange.first, indexRange.last)

    inline fun <reified T> params(
        fromIndex: Int,
        toIndex: Int
    ) = parameters.subList(fromIndex, toIndex).mapIndexed { index, it ->
        it as? T ?: throw ParameterTypeMismatchException("index=${index + fromIndex}, value=$it, type=${it?.javaClass?.simpleName}")
    }
}

@KaramellDsl
open class SubScope internal constructor() {
    private val children = mutableListOf<Node<*>>()

    fun <T : Any> sub(token: Token<T>, block: SubScope.() -> Unit) {
        val subScope = SubScope()
        subScope.block()
        children += Node.Internal(token, children = subScope.buildNodes())
    }

    fun <T : Any> sub(vararg tokens: Token<T>, block: SubScope.() -> Unit) {
        sub(tokens.combined(), block)
    }

    fun <T : Any> end(token: Token<T>, block: EndScope.() -> Unit) {
        children += Node.Internal(token, Node.Leaf { params ->
            EndScope(params).block()
        })
    }

    fun <T : Any> end(vararg tokens: Token<T>, block: EndScope.() -> Unit) {
        end(tokens.combined(), block)
    }

    fun end(block: EndScope.() -> Unit) {
        children += Node.Leaf { params ->
            EndScope(params).block()
        }
    }

    protected fun buildNodes(): Array<Node<*>> = children.toTypedArray()
}

@KaramellDsl
class RootScope internal constructor(
    private val name: String
) : SubScope() {
    private var aliases: Array<out String>? = null
    private var ignoreCase: Boolean = true

    fun aliases(vararg aliases: String) {
        this.aliases = aliases
    }

    fun ignoreCase(value: Boolean = true) {
        this.ignoreCase = value
    }

    fun buildCommand(): Command {
        return CommandImpl(
            name,
            aliases = aliases ?: emptyArray(),
            ignoreCase = ignoreCase,
            children = buildNodes()
        )
    }
}