@file:OptIn(ExperimentalUnsignedTypes::class)

package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.assertTokensMatch
import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.lexToStream
import de.tobiasblaschke.lib.midi.adapter.grammar.LexerTestUtils.makeParser
import de.tobiasblaschke.lib.midi.adapter.grammar.ParserUtils.charHexCodesAsString
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.*
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.Token
import kotlin.test.Test
import kotlin.test.assertEquals

class Midi1FileParserTest {
    companion object {
        /** @see `should deserialize format 0 header` which tests the same */
        private val FORMAT_0_HEADER =  ubyteArrayOf(
            0x4Du, 0x54u, 0x68u, 0x64u, 0x00u, 0x00u, 0x00u, 0x06u,
            0x00u, 0x00u, 0x00u, 0x01u, 0x00u, 0x60u)
        private val FORMAT_1_HEADER = ubyteArrayOf(
            0x4Du, 0x54u, 0x68u, 0x64u,       // MThd
            0x00u, 0x00u, 0x00u, 0x06u,       // chunk length
            0x00u, 0x01u,                     // format 1
            0x00u, 0x04u,                     // four tracks
            0x00u, 0x60u,                     // 96 per quarter-note
        )
    }

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
    fun `should deserialize format 0 header`() {
        val input = ubyteArrayOf(
            0x4Du, 0x54u, 0x68u, 0x64u,       // MThd
            0x00u, 0x00u, 0x00u, 0x06u,       // chunk length
            0x00u, 0x00u,                     // format 0
            0x00u, 0x01u,                     // one track
            0x00u, 0x60u,                     // 96 per quarter-note
        )

        val tokens = lexToStream(input)
        val parser = Midi1FileParser(tokens)

        val visitor = Midi1FileChunkVisitor()
        val header = visitor.visitMidiHeader(parser.midiFile().midiHeader())

        assertEquals(MidiFileChunk.MidiFileHeader(
            midiFileTrackFormat = MidiFileTrackFormat.SINGLE_MULTI_CHANNEL_TRACK,
            numberOfTracks = 1,
            timeDivision = TimeDivision.TicksPerQuarterNote(96)),
            header)
    }

    @Test
    fun `should deserialize empty track`() {
        val input = FORMAT_0_HEADER +  // Header has length 14
                ubyteArrayOf(
                    0x4Du, 0x54u, 0x72u, 0x6Bu,       // MTrk
                    0x00u, 0x00u, 0x00u, 0x04u,       // chunk length (4)

                    0x00u,          0xFFu, 0x2Fu, 0x00u, // end of track
                )

        val tokens = lexToStream(input)
        val parser = makeParser(tokens)

        val visitor = Midi1FileChunkVisitor()
        val parsed = parser.midiFile().midiTrack()
            .map { visitor.visitMidiTrack(it) }

        assertEquals(1, parsed.size, "Expected one track to be parsed")
        assertEquals(listOf(
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MetaEvent.EndOfTrack)), parsed[0].events)
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
    fun `should deserialize format 0 track`() {
        val input = FORMAT_0_HEADER +  // Header has length 14
                ubyteArrayOf(
            0x4Du, 0x54u, 0x72u, 0x6Bu,       // MTrk
            0x00u, 0x00u, 0x00u, 0x3Bu,       // chunk length (59)

            0x00u,          0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,  // time signature
            0x00u,          0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,         // tempo
            0x00u,          0xC0u, 0x05u,
            0x00u,          0xC1u, 0x2Eu,
            0x00u,          0xC2u, 0x46u,
            0x00u,          0x92u, 0x30u, 0x60u,
            0x00u,          0x3Cu, 0x60u,           //   running status
            0x60u,          0x91u, 0x43u, 0x40u,
            0x60u,          0x90u, 0x4Cu, 0x20u,
            0x81u, 0x40u,   0x82u, 0x30u, 0x40u,    // two-byte delta-time
            0x00u,          0x3Cu, 0x40u,           // running status
            0x00u,          0x81u, 0x43u, 0x40u,
            0x00u,          0x80u, 0x4Cu, 0x40u,
            0x00u,          0xFFu, 0x2Fu, 0x00u, // end of track
        )

        val tokens = lexToStream(input)
        val parser = Midi1FileParser(tokens)

        val visitor = Midi1FileChunkVisitor()
        val parsed = parser.midiFile().midiTrack()
            .map { visitor.visitMidiTrack(it) }

        val track = parsed[0]
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MetaEvent.TimeSignature(4, 2, 0x18, 0x08)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MetaEvent.Tempo(0x07A120)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.ProgramChange(channel = 0, programNumber = 0x05)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.ProgramChange(channel = 1, programNumber = 0x2E)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.ProgramChange(channel = 2, programNumber = 0x46)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MidiEventGeneral.NoteOn(channel = 2, key = 48, velocity = 96)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MidiEventGeneral.NoteOn(channel = 2, key = 60, velocity = 96)), // from running-status
            MidiFileEvent(deltaTime=0x60, event=
                MidiEvent.MidiEventGeneral.NoteOn(channel = 1, key = 67, velocity = 64)),
            MidiFileEvent(deltaTime=0x60, event=
                MidiEvent.MidiEventGeneral.NoteOn(channel = 0, key = 76, velocity = 32)),
            MidiFileEvent(deltaTime=192, event=                                            // two-byte time-delta
                MidiEvent.MidiEventGeneral.NoteOff(channel = 2, key = 48, velocity = 64)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MidiEventGeneral.NoteOff(channel = 2, key = 60, velocity = 64)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MidiEventGeneral.NoteOff(channel = 1, key = 67, velocity = 64)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MidiEventGeneral.NoteOff(channel = 0, key = 76, velocity = 64)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MetaEvent.EndOfTrack)
        ), track.events)
    }

    /**
     *  Example from "MIDI Standard File Format" 1.0
     *
     *  A format 1 representation of the file is slightly different.  Its header
     *  chunk:
     *
     *                 4D 54 68 64     MThd
     *                 00 00 00 06     chunk length
     *                 00 01   format 1
     *                 00 04   four tracks
     *                 00 60   96 per quarter-note
     *
     */
    @Test
    fun `should deserialize format 1 header`() {
        val input = ubyteArrayOf(
            0x4Du, 0x54u, 0x68u, 0x64u,       // MThd
            0x00u, 0x00u, 0x00u, 0x06u,       // chunk length
            0x00u, 0x01u,                     // format 1
            0x00u, 0x04u,                     // four tracks
            0x00u, 0x60u,                     // 96 per quarter-note
        )

        val tokens = lexToStream(input)
        val parser = Midi1FileParser(tokens)

        val visitor = Midi1FileChunkVisitor()
        val header = visitor.visitMidiHeader(parser.midiFile().midiHeader())

        assertEquals(MidiFileChunk.MidiFileHeader(
            midiFileTrackFormat = MidiFileTrackFormat.SIMULTANEOUS_TRACKS,
            numberOfTracks = 4,
            timeDivision = TimeDivision.TicksPerQuarterNote(96)),
            header)
    }

    /**
     *  Example from "MIDI Standard File Format" 1.0
     *
     * First, the track chunk for the time signature/tempo track.  Its header,
     * followed by the events:
     *
     *                 4D 54 72 6B     MTrk
     *                 00 00 00 14     chunk length (20)
     *
     *         Delta-time      Event   Comments
     *         00      FF 58 04 04 02 18 08    time signature
     *         00      FF 51 03 07 A1 20       tempo
     *         83 00   FF 2F 00        end of track
     *
     * Then, the track chunk for the first music track.  The MIDI convention
     * for note on/off running status is used in this example:
     *
     *                 4D 54 72 6B     MTrk
     *                 00 00 00 10     chunk length (16)
     *
     *         Delta-time      Event   Comments
     *         00      C0 05
     *         81 40   90 4C 20
     *         81 40   4C 00   Running status: note on, vel = 0
     *         00      FF 2F 00        end of track
     *
     * Then, the track chunk for the second music track:
     *
     *                 4D 54 72 6B     MTrk
     *                 00 00 00 0F     chunk length (15)
     *
     *         Delta-time      Event   Comments
     *         00      C1 2E
     *         60      91 43 40
     *         82 20   43 00   running status
     *         00      FF 2F 00        end of track
     *
     * Then, the track chunk for the third music track:
     *
     *                 4D 54 72 6B     MTrk
     *                 00 00 00 15     chunk length (21)
     *
     *         Delta-time      Event   Comments
     *         00      C2 46
     *         00      92 30 60
     *         00      3C 60   running status
     *         83 00   30 00   two-byte delta-time, running status
     *         00      3C 00   running status
     *         00      FF 2F 00        end of track
     *
     */
    @Test
    fun `should deserialize format 1 track`() {
        val input = FORMAT_1_HEADER + ubyteArrayOf(
           // The track will start at byte 15 (1-based)
           0x4Du, 0x54u, 0x72u, 0x6Bu,     // MTrk
           0x00u, 0x00u, 0x00u, 0x14u,     // chunk length (20)

           // Delta-time    Event                                               Comments
           0x00u,           0xFFu, 0x58u, 0x04u, 0x04u, 0x02u, 0x18u, 0x08u,    // time signature
           0x00u,           0xFFu, 0x51u, 0x03u, 0x07u, 0xA1u, 0x20u,           // tempo
           0x83u, 0x00u,    0xFFu, 0x2Fu, 0x00u,                                // end of track

           // --------------------------------
           // The track will start at byte 35 (1-based)

           0x4Du, 0x54u, 0x72u, 0x6Bu,                           // MTrk
           0x00u, 0x00u, 0x00u, 0x10u,                           // chunk length (16)

           // Delta-time    Event                                               Comments
           0x00u,           0xC0u, 0x05u,
           0x81u, 0x40u,    0x90u, 0x4Cu, 0x20u,
           0x81u, 0x40u,    0x4Cu, 0x00u,                                       // Running status: note on, vel = 0
           0x00u,           0xFFu, 0x2Fu, 0x00u,                                // end of track

           // --------------------------------
           // The track will start at byte 51 (1-based)

           0x4Du, 0x54u, 0x72u, 0x6Bu,                           // MTrk
           0x00u, 0x00u, 0x00u, 0x0Fu,                           // chunk length (15)

           // Delta-time    Event                                               Comments
           0x00u,           0xC1u, 0x2Eu,
           0x60u,           0x91u, 0x43u, 0x40u,
           0x82u, 0x20u,    0x43u, 0x00u,                                       // running status
           0x00u,           0xFFu, 0x2Fu, 0x00u,                                // end of track

           // --------------------------------
           // The track will start at byte 66 (1-based)

           0x4Du, 0x54u, 0x72u, 0x6Bu,                           // MTrk
           0x00u, 0x00u, 0x00u, 0x15u,                           // chunk length (21)

           // Delta-time   Event                                               Comments
           0x00u,          0xC2u, 0x46u,
           0x00u,          0x92u, 0x30u, 0x60u,
           0x00u,          0x3Cu, 0x60u,                                       // running status
           0x83u, 0x00u,   0x30u, 0x00u,                                       // two-byte delta-time, running status
           0x00u,          0x3Cu, 0x00u,                                       // running status
           0x00u,          0xFFu, 0x2Fu, 0x00u,                                // end of track
        )

//        input.chunked(10)
//            .onEach { println(it.joinToString { c -> String.format("%02X", c.toInt()) }) }

        val tokens = lexToStream(input)
        val parser = makeParser(tokens)

        val visitor = Midi1FileChunkVisitor()
        val file = parser.midiFile()
        val tracks = file.midiTrack()
            .map { visitor.visitMidiTrack(it) }

        assertEquals(4, tracks.size, "Expected four tracks")

        /*
         *                 4D 54 72 6B     MTrk
         *                 00 00 00 14     chunk length (20)
         *
         *         Delta-time      Event   Comments
         *         00      FF 58 04 04 02 18 08    time signature
         *         00      FF 51 03 07 A1 20       tempo
         *         83 00   FF 2F 00        end of track
         */
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MetaEvent.TimeSignature(4, 2, 0x18, 0x08)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MetaEvent.Tempo(0x07A120)),
            MidiFileEvent(deltaTime=3 * 0x80, event=
                MidiEvent.MetaEvent.EndOfTrack)
        ), tracks[0].events, "Unable to match events in track 1")

        /*
         *                 4D 54 72 6B     MTrk
         *                 00 00 00 10     chunk length (16)
         *
         *         Delta-time      Event   Comments
         *         00      C0 05
         *         81 40   90 4C 20
         *         81 40   4C 00            Running status: note on, vel = 0
         *         00      FF 2F 00        end of track
         */
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.ProgramChange(0, 5)),
            MidiFileEvent(deltaTime = 0x80 + 0x40, event =
                MidiEvent.MidiEventGeneral.NoteOn(0, 0x4C, 0x20)),
            MidiFileEvent(deltaTime = 0x80 + 0x40, event =
                MidiEvent.MidiEventGeneral.NoteOn(0, 0x4C, 0x00)),
            MidiFileEvent(deltaTime = 0, event=
                MidiEvent.MetaEvent.EndOfTrack)
        ), tracks[1].events, "Unable to match events in track 2")

        /*
         *                 4D 54 72 6B     MTrk
         *                 00 00 00 0F     chunk length (15)
         *
         *         Delta-time      Event   Comments
         *         00      C1 2E
         *         60      91 43 40
         *         82 20   43 00   running status
         *         00      FF 2F 00        end of track
         */
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.ProgramChange(1, 0x2E)),
            MidiFileEvent(deltaTime = 0x60, event =
                MidiEvent.MidiEventGeneral.NoteOn(1, 0x43, 0x40)),
            MidiFileEvent(deltaTime = 2 * 0x80 + 0x20, event =
                MidiEvent.MidiEventGeneral.NoteOn(1, 0x43, 0x00)),
            MidiFileEvent(deltaTime = 0, event=
                MidiEvent.MetaEvent.EndOfTrack)
        ), tracks[2].events, "Unable to match events in track 3")

        /*
         * Then, the track chunk for the third music track:
         *
         *                 4D 54 72 6B     MTrk
         *                 00 00 00 15     chunk length (21)
         *
         *         Delta-time      Event   Comments
         *         00      C2 46
         *         00      92 30 60
         *         00      3C 60   running status
         *         83 00   30 00   two-byte delta-time, running status
         *         00      3C 00   running status
         *         00      FF 2F 00        end of track
         */
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.ProgramChange(2, 0x46)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.NoteOn(2, 0x30, 0x60)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.NoteOn(2, 0x3C, 0x60)),
            MidiFileEvent(deltaTime = 3 * 0x80, event =
                MidiEvent.MidiEventGeneral.NoteOn(2, 0x30, 0x00)),
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MidiEventGeneral.NoteOn(2, 0x3C, 0x00)),
            MidiFileEvent(deltaTime=0, event=
                MidiEvent.MetaEvent.EndOfTrack)
        ), tracks[3].events, "Unable to match events in track 4")
    }

    @Test
    fun `should parse text-nodes in MIDI_sample`() {
        val tokens = javaClass.getResourceAsStream("/MIDI_sample.mid")
            .use(LexerTestUtils::lexToStream)
        val parser = makeParser(tokens)

        val visitor = Midi1FileChunkVisitor()
        val file = parser.midiFile()
        val tracks = file.midiTrack()
            .map { visitor.visitMidiTrack(it) }

        assertEquals(6, tracks.size, "Expected four tracks")

        // Text in channel 1
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
                MidiEvent.MetaEvent.SequenceOrTrackName("Wikipedia MIDI (extended)")),
        ), tracks[0].events.filter { it.event is MidiEvent.TextEvent }, "Unable to match events in track 1")

        // Text in channel 2
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
            MidiEvent.MetaEvent.SequenceOrTrackName("Bass")),
        ), tracks[1].events.filter { it.event is MidiEvent.TextEvent }, "Unable to match events in track 2")

        // Text in channel 3
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
            MidiEvent.MetaEvent.SequenceOrTrackName("Piano")),
        ), tracks[2].events.filter { it.event is MidiEvent.TextEvent }, "Unable to match events in track 3")

        // Text in channel 4
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
            MidiEvent.MetaEvent.SequenceOrTrackName("Hi-hat only")),
        ), tracks[3].events.filter { it.event is MidiEvent.TextEvent }, "Unable to match events in track 4")

        // Text in channel 5
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
            MidiEvent.MetaEvent.SequenceOrTrackName("Drums")),
        ), tracks[4].events.filter { it.event is MidiEvent.TextEvent }, "Unable to match events in track 5")

        // Text in channel 6
        assertEquals(listOf(
            MidiFileEvent(deltaTime = 0, event =
            MidiEvent.MetaEvent.SequenceOrTrackName("Jazz Guitar")),
        ), tracks[5].events.filter { it.event is MidiEvent.TextEvent }, "Unable to match events in track 6")
    }
}