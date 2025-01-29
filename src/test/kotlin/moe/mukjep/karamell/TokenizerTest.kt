package moe.mukjep.karamell

import moe.mukjep.karamell.Command.Companion.tokenize
import kotlin.test.Test
import kotlin.test.assertEquals

class TokenizerTest {

    @Test
    fun `test simple command without quotes`() {
        val command = "/set locale en_us"
        val expected = listOf("/set", "locale", "en_us")
        val result = tokenize(command)
        assertEquals(expected, result)
    }

    @Test
    fun `test command with quoted argument`() {
        val command = "/set path \"my path\""
        val expected = listOf("/set", "path", "my path")
        val result = tokenize(command)
        assertEquals(expected, result)
    }

    @Test
    fun `test command with unclosed quote`() {
        val command = "/set path \"path"
        val expected = listOf("/set", "path", "path")
        val result = tokenize(command)
        assertEquals(expected, result)
    }

    @Test
    fun `test command with consecutive spaces`() {
        val command = "/set   locale  en_us"
        val expected = listOf("/set", "locale", "en_us")
        val result = tokenize(command)
        assertEquals(expected, result)
    }

    @Test
    fun `test single word command`() {
        val command = "/set"
        val expected = listOf("/set")
        val result = tokenize(command)
        assertEquals(expected, result)
    }

    @Test
    fun `test command with escaped quote`() {
        val command = "/set message \"He said: \\\"Hello!\\\"\""
        val expected = listOf("/set", "message", "He said: \"Hello!\"")
        val result = tokenize(command)
        assertEquals(expected, result)
    }

}