package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.ParserUtils.charHexCodesAsString
import org.antlr.v4.runtime.*
import org.hamcrest.Description
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.TypeSafeMatcher
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals


object LexerTestUtils {

    fun lexToStream(block: UByteArray): CommonTokenStream =
        lexToStream(block.joinToString(separator = "") { Char(it.toInt()).toString() })

    fun lexToStream(block: String): CommonTokenStream =
        block.byteInputStream(charset = Charsets.ISO_8859_1)
            .use { lexToStream(it) }

    fun lexToStream(byteStream: InputStream): CommonTokenStream {
        val bytesAsChar = CharStreams.fromStream(byteStream, StandardCharsets.ISO_8859_1)
        //val lexer = Midi1FileLexer(bytesAsChar)
        val lexer = Midi1FileLexer(bytesAsChar)
        return CommonTokenStream(lexer)
    }

    fun lexToList(block: UByteArray): List<Token> =
        lexToList(block.joinToString(separator = "") { Char(it.toInt()).toString() })


    fun lexToList(block: String): List<Token> =
        tokenStreamAsList(lexToStream(block))

    fun lexToList(inputStream: InputStream) =
        tokenStreamAsList(lexToStream(inputStream))

    fun tokenStreamAsList(tokenStream: CommonTokenStream): List<Token> {
        val tokenSource = tokenStream.tokenSource

        val ret = mutableListOf<Token>()
        var nextToken = tokenSource.nextToken()
        while (nextToken != null && nextToken.type != Token.EOF) {
            ret.add(nextToken)
            nextToken = tokenSource.nextToken()
        }
        return ret
    }

    fun assertTokensMatch(actual: List<Token>, vararg matchers: TokenMatcher) {
        matchers
            .forEachIndexed { index, matcher ->
                val token = actual.getOrNull(index)
                assertThat("Mismatch on token $index", token, matcher)
            }

        assertEquals(matchers.size, actual.size, "Expected ${matchers.size} Tokens, got ${actual.size}")
    }

    fun Token.dump(): String {
        val stringType = Midi1FileLexer.VOCABULARY.getDisplayName(type)
        return "$stringType(${text.charHexCodesAsString()} = $text) at position $startIndex to $stopIndex"
    }

    fun makeDummyToken(type: Int, text: String, startIndex: Int = 0, stopIndex: Int = startIndex + text.length): Token =
        CommonToken(type, text)
            .also {
                it.startIndex = startIndex
                it.stopIndex = stopIndex }

    fun makeParser(tokens: CommonTokenStream) =
        Midi1FileParser(tokens)
            .also {
                it.errorHandler = object : DefaultErrorStrategy() {
                    override fun getTokenErrorDisplay(t: Token?): String {
                        if (t == null) return "<no token>"
                        val hex = t.text?.charHexCodesAsString() ?: " - no text -"
                        val tokenName = Midi1FileLexer.VOCABULARY.getDisplayName(t.type)
                        return String.format(
                            "ERROR: on $tokenName = '$hex' at %d (0x%02X) length %d",
                            t.startIndex,
                            t.startIndex,
                            t.stopIndex - t.startIndex + 1
                        )
                    }
                }
            }
}

class TokenMatcher(
    private val type: Int,
    private val text: String,
    private val channel: Int? = null) : TypeSafeMatcher<Token>() {
    override fun matchesSafely(actual: Token): Boolean {
        return try {
            (actual.type == type) &&
                    (actual.text == text) &&
                    (channel == null || actual.channel == channel)
        } catch (nfe: NumberFormatException) {
            false
        }
    }

    override fun describeTo(desc: Description) {
        val expectedType = Midi1FileLexer.VOCABULARY.getDisplayName(type)

        if (channel != null) {
            val channelText = Midi1FileLexer.channelNames[channel]
            desc.appendText("Expected $expectedType(${text.charHexCodesAsString()}) on $channelText")
        } else {
            desc.appendText("Expected $expectedType(${text.charHexCodesAsString()})")
        }
    }

    override fun describeMismatchSafely(item: Token, desc: Description) {
        val actualType = Midi1FileLexer.VOCABULARY.getDisplayName(item.type)

        if (channel != null) {
            val channelText = Midi1FileLexer.channelNames[item.channel]
            desc.appendText("Got $actualType(${item.text.charHexCodesAsString()}) on $channelText at position ${item.startIndex}")
        } else {
            desc.appendText("Got $actualType(${item.text.charHexCodesAsString()}) at position ${item.startIndex}")
        }
    }


}