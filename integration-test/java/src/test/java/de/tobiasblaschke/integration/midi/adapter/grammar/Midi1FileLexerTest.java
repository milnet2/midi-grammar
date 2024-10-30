package de.tobiasblaschke.integration.midi.adapter.grammar;

import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileLexer;
import org.junit.jupiter.api.Test;

import static de.tobiasblaschke.integration.midi.adapter.grammar.LexerTestUtils.assertTokensMatch;
import static de.tobiasblaschke.integration.midi.adapter.grammar.LexerTestUtils.lexToList;

public class Midi1FileLexerTest {

    @Test
    void should_lex_format_0_header() {
        byte[] input = new byte[] {
                0x4D, 0x54, 0x68, 0x64,         // MThd
                0x00, 0x00, 0x00, 0x06,         // chunk length
                0x00, 0x00,                     // format 0
                0x00, 0x01,                     // one track
                0x00, (byte) 0x90,
        };

        var tokens = lexToList(input);

        assertTokensMatch(tokens,
                new TokenMatcher(Midi1FileLexer.BEGIN_MIDI_HEADER, "\u004d\u0054\u0068\u0064\u0000\u0000\u0000\u0006"),
                new TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
                new TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
                new TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
                new TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0001"),
                new TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
                new TokenMatcher(Midi1FileLexer.ARG_BYTE_UPPER, "\u0090"),
                new TokenMatcher(Midi1FileLexer.END_OF_CHUNK, "")
        );
    }
}
