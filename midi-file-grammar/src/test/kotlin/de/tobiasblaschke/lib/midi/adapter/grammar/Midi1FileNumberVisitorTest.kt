package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.makeDummyToken
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Midi1FileNumberVisitorTest {

    @Test
    fun `should convert variable length quantity`() {
        val t = { text: String -> makeDummyToken(type = Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, text = text) }

        val visitor = Midi1FileNumberVisitor()
        assertEquals(0, visitor.convertVariableLengthQuantity(t("\u0000")))
        assertEquals(128, visitor.convertVariableLengthQuantity(t("\u0081\u0000")))
        assertEquals(8192, visitor.convertVariableLengthQuantity(t("\u00C0\u0000")))
        assertEquals(0x3FFF, visitor.convertVariableLengthQuantity(t("\u00FF\u007F")))
        assertEquals(0x001FFFFF, visitor.convertVariableLengthQuantity(t("\u00FF\u00FF\u007F")))
        assertEquals(0x00200000, visitor.convertVariableLengthQuantity(t("\u0081\u0080\u0080\u0000")))
        assertEquals(0x08000000, visitor.convertVariableLengthQuantity(t("\u00C0\u0080\u0080\u0000")))
        assertEquals(0x0FFFFFFF, visitor.convertVariableLengthQuantity(t("\u00FF\u00FF\u00FF\u007F")))

        // Next one should be represented by 0x00. The spec seems a bit wasteful here
        assertEquals(0, visitor.convertVariableLengthQuantity(t("\u0080\u0000")))
    }
}