package moe.mukjep.karamell

sealed class CommandException(message: String? = null) : RuntimeException(message)

class ParameterTypeMismatchException(message: String? = null) : CommandException(message)
