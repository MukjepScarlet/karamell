package moe.mukjep.karamell

sealed class AcceptResult(private val score: Int) : Comparable<AcceptResult> {

    override operator fun compareTo(other: AcceptResult): Int {
        return this.score.compareTo(other.score)
    }

    sealed class Success(score: Int) : AcceptResult(score)

    sealed class Failure(score: Int) : AcceptResult(score)

    /**
     * Successfully executed a command.
     *
     * @param value Result value
     */
    class Ok<T>(val value: T) : Success(1)

    /**
     * Successfully matched a leaf node of a command without execution.
     *
     * @param matches Conversion results of [Token]s
     */
    class Matched(val matches: List<Any>) : Success(1)

    /**
     * Type mismatch between [Node.Internal] and [Node.Leaf]
     */
    data object Inapplicable : Failure(-2)

    /**
     * Didn't find any matched branch.
     *
     * @param node Itself or None of its children matches the input
     * @param isSelfMatches Is [node] self matches
     * @param current Current part of [node]
     */
    class NotMatched(
        val node: Node.Internal<*>,
        val isSelfMatches: Boolean,
        val current: String
    ) : Failure(if (isSelfMatches) -1 else -2)

}