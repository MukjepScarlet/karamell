package moe.mukjep.karamell

sealed class Node<T : Any>(
    val token: Token<T>,
) {
    internal abstract fun tryAccept(
        args: List<String>,
        startIndex: Int,
        values: MutableList<Any>,
        execute: Boolean
    ): AcceptResult

    abstract fun hints(): Sequence<String>

    open class Internal<T : Any> internal constructor(
        token: Token<T>,
        internal vararg val children: Node<*>
    ) : Node<T>(token) {
        init {
            require(children.isNotEmpty()) { "Internal node should have at least one child." }
        }

        override fun tryAccept(
            args: List<String>,
            startIndex: Int,
            values: MutableList<Any>,
            execute: Boolean
        ): AcceptResult {
            if (startIndex >= args.size) {
                return AcceptResult.Inapplicable
            }

            var next = startIndex
            values += token.convert(args[next++]) ?: return AcceptResult.NotMatched(this, false, args[next - 1])

            if (token is VarargToken) {
                while (next < args.size) {
                    values += token.convert(args[next++]) ?: break
                }
            }

            var result: AcceptResult = AcceptResult.Inapplicable
            for (child in children) {
                when (val childResult = child.tryAccept(args, next, values, execute)) {
                    is AcceptResult.Success -> return childResult
                    else -> result = maxOf(result, childResult)
                }
            }

            return maxOf(result, AcceptResult.NotMatched(this, true, args.getOrNull(next) ?: ""))
        }

        override fun hints(): Sequence<String> = sequence {
            val selfHint = token.hint()

            val builder = StringBuilder(selfHint)
            for (child in children) {
                for (childHint in child.hints()) {
                    yield(if (childHint.isNotBlank()) {
                        builder.append(' ').append(childHint).toString()
                    } else {
                        builder.toString()
                    })

                    builder.setLength(selfHint.length)
                }
            }
        }
    }

    class Leaf<T> internal constructor(
        val handler: (List<Any>) -> T
    ) : Node<Nothing>(EmptyToken) {
        override fun tryAccept(
            args: List<String>,
            startIndex: Int,
            values: MutableList<Any>,
            execute: Boolean
        ): AcceptResult {
            if (startIndex != args.size) {
                return AcceptResult.Inapplicable
            }

            return if (execute) {
                val result = handler(values)
                values.clear()
                AcceptResult.Ok(result)
            } else {
                AcceptResult.Matched(values)
            }
        }

        override fun hints(): Sequence<String> = sequenceOf("")
    }

    override fun toString(): String = "Node(token=$token)"
}
