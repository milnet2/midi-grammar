package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.assertTokensMatch
import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.dump
import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.lexToList
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test

class Midi1FileLexerTest {
    /**
     *  Example from "MIDI Standard File Format" 1.0
     *
     *  First, the header chunk:
     *                 4D 54 68 64     MThd
     *                 00 00 00 06     chunk length
     *                 00 00   format 0
     *                 00 01   one track
     *                 00 60   96 per quarter-note
     *
     */
    @Test
    fun `should lex format 0 header`() {
        val input = ubyteArrayOf(
            0x4Du, 0x54u, 0x68u, 0x64u,       // MThd
            0x00u, 0x00u, 0x00u, 0x06u,       // chunk length
            0x00u, 0x00u,                     // format 0
            0x00u, 0x01u,                     // one track
            0x00u, 0x60u,                     // 96 per quarter-note
        )

        val tokens = lexToList(input)

        assertTokensMatch(tokens,
            TokenMatcher(Midi1FileLexer.BEGIN_MIDI_HEADER, "\u004d\u0054\u0068\u0064\u0000\u0000\u0000\u0006"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0060"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),
        )
    }

    /**
     *  Example from "MIDI Standard File Format" 1.0
     *
     *  Then, the track chunk.  Its header, followed by the events (notice that
     *  running status is used in places):
     *
     *                 4D 54 72 6B     MTrk
     *                 00 00 00 3B     chunk length (59)
     *
     *         Delta-time      Event   Comments
     *         00      FF 58 04 04 02 18 08    time signature
     *         00      FF 51 03 07 A1 20       tempo
     *         00      C0 05
     *         00      C1 2E
     *         00      C2 46
     *         00      92 30 60
     *         00      3C 60   running status
     *         60      91 43 40
     *         60      90 4C 20
     *         81 40   82 30 40        two-byte delta-time
     *         00      3C 40   running status
     *         00      81 43 40
     *         00      80 4C 40
     *         00      FF 2F 00        end of track
     */
    @Test
    fun `should lex format 0 track`() {
        val input = charArrayOf(
                    '\u004D', '\u0054', '\u0072', '\u006B',       // MTrk
                    '\u0000', '\u0000', '\u0000', '\u003B',       // chunk length (59)

                    '\u0000',          '\u00FF', '\u0058', '\u0004', '\u0004', '\u0002', '\u0018', '\u0008',  // time signature
                    '\u0000',          '\u00FF', '\u0051', '\u0003', '\u0007', '\u00A1', '\u0020',         // tempo
                    '\u0000',          '\u00C0', '\u0005', // CC
                    '\u0000',          '\u00C1', '\u002E', // CC
                    '\u0000',          '\u00C2', '\u0046', // CC
                    '\u0000',          '\u0092', '\u0030', '\u0060', // Note on
                    '\u0000',          '\u003C', '\u0060',           //   running status
                    '\u0060',          '\u0091', '\u0043', '\u0040',
                    '\u0060',          '\u0090', '\u004C', '\u0020',
                    '\u0081', '\u0040',   '\u0082', '\u0030', '\u0040',    // two-byte delta-time, then note off
                    '\u0000',          '\u003C', '\u0040',           // running status
                    '\u0000',          '\u0081', '\u0043', '\u0040',
                    '\u0000',          '\u0080', '\u004C', '\u0040',
                    '\u0000',          '\u00FF', '\u002F', '\u0000', // end of track
                )

        val tokens = lexToList(String(input))

        // tokens.forEach { println("  ${it.dump()}") }

        assertTokensMatch(tokens,
            TokenMatcher(Midi1FileLexer.BEGIN_TRACK, "\u004d\u0054\u0072\u006b\u0000\u0000\u0000\u003b"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"), // byte 8
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_FF_TIME_SIGNATURE, "\u00FF\u0058\u0004"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0004"), // byte 12
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0002"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0018"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0008"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_FF_TEMPO, "\u00FF\u0051\u0003"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0007"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE_UPPER, "\u00A1"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0020"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_PC_RANGE, "\u00C0"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0005"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_PC_RANGE, "\u00C1"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u002E"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_PC_RANGE, "\u00C2"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0002"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0046"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_ON_RANGE, "\u0092"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0002"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0030"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0060"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.RUNNING_STATUS, ""),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u003C"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0060"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0060"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_ON_RANGE, "\u0091"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0043"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0040"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0060"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_ON_RANGE, "\u0090"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u004C"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0020"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0081\u0040"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_OFF_RANGE, "\u0082"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0002"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0030"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0040"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.RUNNING_STATUS, ""),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u003C"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0040"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_OFF_RANGE, "\u0081"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0043"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0040"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_OFF_RANGE, "\u0080"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u004C"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0040"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_END_OF_TRACK, "\u00FF\u002F\u0000"),

            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),
        )
    }

    @Test
    fun `should lex track after header`() {
        val input = ubyteArrayOf(
            0x4Du, 0x54u, 0x68u, 0x64u,       // MThd
            0x00u, 0x00u, 0x00u, 0x06u,       // chunk length
            0x00u, 0x00u,                     // format 0
            0x00u, 0x01u,                     // one track
            0x00u, 0x60u,                     // 96 per quarter-note

            0x4Du, 0x54u, 0x72u, 0x6Bu,       // MTrk
            0x00u, 0x00u, 0x00u, 0x04u,       // chunk length (4)

            0x00u,          0xFFu, 0x2Fu, 0x00u, // end of track
        )

        val tokens = lexToList(input)

        assertTokensMatch(tokens,
            TokenMatcher(Midi1FileLexer.BEGIN_MIDI_HEADER, "\u004d\u0054\u0068\u0064\u0000\u0000\u0000\u0006"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0060"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),

            TokenMatcher(Midi1FileLexer.BEGIN_TRACK, "\u004d\u0054\u0072\u006b\u0000\u0000\u0000\u0004"),
            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_END_OF_TRACK, "\u00FF\u002F\u0000"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),
        )
    }

    @org.junit.jupiter.api.Test
    fun `should lex consecutive tracks`() {
        val input = ubyteArrayOf(
           0x4Du, 0x54u, 0x72u, 0x6Bu,     // MTrk
           0x00u, 0x00u, 0x00u, 0x14u,     // chunk length (20)

           // Delta-time    Event                                               Comments
           0x00u,           0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,    // time signature
           0x00u,           0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,           // tempo
           0x83u, 0x00u,    0xFFu, 0x2Fu, 0x00u,                                // end of track

           // --------------------------------

           0x4Du, 0x54u, 0x72u, 0x6Bu,                           // MTrk
           0x00u, 0x00u, 0x00u, 0x10u,                           // chunk length (16)

           // Delta-time    Event                                               Comments
           0x00u,           0xC0u, 0x05u,
           0x81u, 0x40u,    0x90u, 0x4Cu, 0x20u,
           0x81u, 0x40u,    0x4Cu, 0x00u,                                       // Running status: note on, vel = 0
           0x00u,           0xFFu, 0x2Fu, 0x00u,                                // end of track
        )

        val tokens = lexToList(input)

        assertTokensMatch(tokens,
            TokenMatcher(Midi1FileLexer.BEGIN_TRACK, "\u004d\u0054\u0072\u006b\u0000\u0000\u0000\u0014"),
            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_FF_TIME_SIGNATURE, "\u00FF\u0058\u0004"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0004"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0002"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0018"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0008"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_FF_TEMPO, "\u00FF\u0051\u0003"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0007"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE_UPPER, "\u00A1"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0020"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0083\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_END_OF_TRACK, "\u00FF\u002F\u0000"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),

            // ------------

            TokenMatcher(Midi1FileLexer.BEGIN_TRACK, "\u004d\u0054\u0072\u006b\u0000\u0000\u0000\u0010"),
            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.COMMAND_PC_RANGE, "\u00C0"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0005"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0081\u0040"),
            TokenMatcher(Midi1FileLexer.COMMAND_NOTE_ON_RANGE, "\u0090"),
            TokenMatcher(Midi1FileLexer.CHANNEL, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u004C"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0020"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0081\u0040"),
            TokenMatcher(Midi1FileLexer.RUNNING_STATUS, ""),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u004C"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_END_OF_TRACK, "\u00FF\u002F\u0000"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),
        )
    }

    @Test
    fun `should lex Drum_sample2`() {
        val tokens = javaClass.getResourceAsStream("/Drum_sample2.mid")
            .use(LexerTestUtils::lexToList)

        assertTokensMatch(tokens.subList(0, 8),
            TokenMatcher(Midi1FileLexer.BEGIN_MIDI_HEADER, "\u004d\u0054\u0068\u0064\u0000\u0000\u0000\u0006"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0002"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE_UPPER, "\u00F0"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),
        )
    }

    @Test
    fun `should lex MIDI_sample`() {
        // Do not optimize/shorten the tagged code, as it's used in documentation
        // tag::read-file[]
        val tokens = javaClass.getResourceAsStream("/MIDI_sample.mid")
            .use { byteStream ->
                val bytesAsChar = CharStreams.fromStream(byteStream, StandardCharsets.ISO_8859_1)
                val lexer = Midi1FileLexer(bytesAsChar)
                CommonTokenStream(lexer) }
            .let(LexerTestUtils::tokenStreamAsList) // only for easy access, don't do that when passing the stream to a parser

        // header
        assertTokensMatch(tokens.subList(0, 8),
            TokenMatcher(Midi1FileLexer.BEGIN_MIDI_HEADER, "\u004d\u0054\u0068\u0064\u0000\u0000\u0000\u0006"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0000"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0006"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0001"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE_UPPER, "\u00E0"),
            TokenMatcher(Midi1FileLexer.END_OF_CHUNK, ""),
        )
        // end::read-file[]

        // text in track 1
        val text = "Wikipedia MIDI (extended)"
        val interestingTrackTokens = tokens.subList(8, 13 + text.length - 1 + 5)
        //interestingTrackTokens.forEach { println("  ${it.dump()}") }
        assertTokensMatch(interestingTrackTokens,
            TokenMatcher(Midi1FileLexer.BEGIN_TRACK, "\u004d\u0054\u0072\u006B\u0000\u0000\u0000\u0030"),
            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_FF_SEQUENCE_OR_TRACK_NAME, "\u00FF\u0003"),
            TokenMatcher(Midi1FileLexer.TEXT_LENGTH_QUANTITY_VALUE, String(charArrayOf(text.length.toChar()))), // 25dec = 0x19
            *text.toCharArray()
                .map { TokenMatcher(Midi1FileLexer.TEXT_BYTE, String(charArrayOf(it))) }
                .toTypedArray(),

            TokenMatcher(Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE, "\u0000"),
            TokenMatcher(Midi1FileLexer.SYSTEM_REAL_TIME_FF_TEMPO, "\u00FF\u0051\u0003"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0007"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE_UPPER, "\u00A1"),
            TokenMatcher(Midi1FileLexer.ARG_BYTE7, "\u0020"),
        )
    }
}