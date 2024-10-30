package de.tobiasblaschke.integration.midi.adapter.grammar;

import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileChunkVisitor;
import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileParser;
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiFileChunk;
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiFileTrackFormat;
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.TimeDivision;
import org.junit.jupiter.api.Test;

import static de.tobiasblaschke.integration.midi.adapter.grammar.LexerTestUtils.lexToStream;
import static de.tobiasblaschke.integration.midi.adapter.grammar.LexerTestUtils.makeParser;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Midi1FileParserTest {

    @Test
    void should_deserialize_format_0_header() {
        byte[] input = new byte[] {
                0x4D, 0x54, 0x68, 0x64,         // MThd
                0x00, 0x00, 0x00, 0x06,         // chunk length
                0x00, 0x00,                     // format 0
                0x00, 0x01,                     // one track
                0x00, (byte) 0x90,
        };

        var tokens = lexToStream(input);
        var parser = makeParser(tokens);

        var visitor = new Midi1FileChunkVisitor();
        var header = visitor.visitMidiHeader(parser.midiFile().midiHeader());

        assertEquals(new MidiFileChunk.MidiFileHeader(
                        MidiFileTrackFormat.SINGLE_MULTI_CHANNEL_TRACK,
                        1,
                        new TimeDivision.TicksPerQuarterNote(0x90)),
                header);
    }
}
