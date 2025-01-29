package moe.mukjep.karamell

private class CommandToken(
    private val name: String,
    private val aliases: Array<out String>,
    private val ignoreCase: Boolean
): Token<String> {
    private val hintText = buildString(name.length) {
        append(name)
        if (aliases.isNotEmpty()) {
            aliases.joinTo(buffer = this, separator = "|", prefix = "(", postfix = ")")
        }
    }

    private val allNames = buildList(aliases.size + 1) {
        add(name)
        addAll(aliases)
    }

    override fun hint() = hintText
    override fun convert(part: String) = part.takeIf { p -> allNames.any { p.contentEquals(it, ignoreCase) } }
    override fun suggest(current: String): List<String> = allNames
}

interface Command {
    fun execute(args: List<String>): AcceptResult

    fun suggest(args: List<String>): List<String>

    companion object {
        /**
         * A simple tokenize function, to split [command] into a [List] of [String].
         * Features:
         * 1. Basically split with spaces.
         * 2. Ignore multiple spaces.
         * 3. `"` is used for string literals.
         */
        @JvmStatic
        fun tokenize(command: String): List<String> {
            val result = mutableListOf<String>()
            val currentToken = StringBuilder()
            var inQuotes = false
            var i = 0

            while (i < command.length) {
                val char = command[i]
                when {
                    char == '\\' && i + 1 < command.length && command[i + 1] == '"' -> {
                        currentToken.append('"')
                        i += 2
                        continue
                    }
                    char == '"' -> {
                        inQuotes = !inQuotes
                        if (!inQuotes && currentToken.isNotEmpty()) {
                            result.add(currentToken.toString())
                            currentToken.clear()
                        }
                    }
                    char.isWhitespace() && !inQuotes -> {
                        if (currentToken.isNotEmpty()) {
                            result.add(currentToken.toString())
                            currentToken.clear()
                        }
                    }
                    else -> {
                        currentToken.append(char)
                    }
                }
                i++
            }

            if (currentToken.isNotEmpty()) {
                result.add(currentToken.toString().removeSurrounding("\""))
            }

            return result
        }
    }
}

internal class CommandImpl internal constructor(
    name: String,
    aliases: Array<out String>,
    ignoreCase: Boolean,
    children: Array<out Node<*>>
) : Node.Internal<String>(CommandToken(name, aliases, ignoreCase), children = children), Command {

    override fun execute(args: List<String>): AcceptResult {
        return tryAccept(args, 0, mutableListOf(), true)
    }

    override fun suggest(args: List<String>): List<String> {
        return when (val acc = tryAccept(args, 0, mutableListOf(), false)) {
            is AcceptResult.Success -> emptyList()
            is AcceptResult.NotMatched -> if (acc.isSelfMatches) {
                buildList {
                    for (child in acc.node.children) {
                        addAll(child.token.suggest(acc.current))
                    }
                }
            } else {
                acc.node.token.suggest(acc.current)
            }
            else -> error("Unexpected accepted result: $acc")
        }
    }

}

fun Command(name: String, block: RootScope.() -> Unit): Command {
    return RootScope(name).apply(block).buildCommand()
}
