package de.tobiasblaschke.lib.midi.adapter.grammar.domain

sealed interface MidiEvent {
    sealed interface TextEvent: MidiEvent;

    sealed interface MidiEventGeneral: MidiEvent {
        val channel: Int

        data class NoteOff(override val channel: Int, val key: Int, val velocity: Int): MidiEventGeneral
        data class NoteOn(override val channel: Int, val key: Int, val velocity: Int): MidiEventGeneral
        data class PolymorphicKeyPressure(override val channel: Int, val key: Int, val velocity: Int): MidiEventGeneral
        data class ProgramChange(override val channel: Int, val programNumber: Int): MidiEventGeneral
        data class ChannelPressure(override val channel: Int, val pressure: Int): MidiEventGeneral
        data class PitchBend(override val channel: Int, val bend: Int): MidiEventGeneral
    }

    sealed interface MidiEventControlChange: MidiEvent {
        val channel: Int
        // TODO

        data class ControlChangeController(override val channel: Int, val controller: Int, val value: Int): MidiEventControlChange
    }

    sealed interface MetaEvent: MidiEvent {
        data class SequenceNumber(val number: Int): MetaEvent
        data class Text(val text: String): MetaEvent, TextEvent
        data class CopyrightNotice(val text: String): MetaEvent, TextEvent
        data class SequenceOrTrackName(val text: String): MetaEvent, TextEvent
        data class InstrumentName(val text: String): MetaEvent, TextEvent
        data class Lyric(val text: String): MetaEvent, TextEvent
        data class Marker(val text: String): MetaEvent, TextEvent
        data class CuePoint(val text: String): MetaEvent, TextEvent
        data class ChannelPrefix(val channelNumber: Int): MetaEvent
        data object EndOfTrack: MetaEvent
        data class Tempo(val microsecondsPerQuarterNode: Int): MetaEvent
        // | SYSTEM_REAL_TIME_FF_SMPTE_OFFSET byte byte byte byte byte                  # metaSmpteOffset   // TODO: split payload
        data class TimeSignature(val numerator: Int, val denominator: Int, val numberOfMidiClocksPerMetronomeClick: Int, val numberOf32NotesIn24MidiClocks: Int): MetaEvent
        data class KeySignature(val sharpsOrFlats: SharpsAndFlats, val scale: Scale): MetaEvent
    }

    /*
metaEvent
    | SYSTEM_REAL_TIME_FF_SEQUENCER_SPECIFIC # metaSequencerSpecific // TODO: variable-length data
    | SYSTEM_REAL_TIME_FF   # metaOther
    ;
     */
}

enum class SharpsAndFlats(val midiCode: UByte) {
    FLAT_7(0xF9u),
    FLAT_6(0xFAu),
    FLAT_5(0xFBu),
    FLAT_4(0xFCu),
    FLAT_3(0xFDu),
    FLAT_2(0xFEu),
    FLAT_1(0xFFu),
    NONE(0x00u),
    SHARP_1(0x01u),
    SHARP_2(0x02u),
    SHARP_3(0x03u),
    SHARP_4(0x04u),
    SHARP_5(0x05u),
    SHARP_6(0x06u),
    SHARP_7(0x07u),
    ;

    companion object {
        fun of(visit: Int): SharpsAndFlats =
            ofOrNull(visit)
                ?: throw NoSuchElementException("No element $visit in SharpsAndFlats")

        fun ofOrNull(visit: Int): SharpsAndFlats? =
            SharpsAndFlats.entries.find { it.midiCode == visit.toUByte() }
    }
}

enum class Scale(val midiCode: UByte) {
    MAJOR(0x00u),
    MINOR(0x01u),
    ;

    companion object {
        fun of(visit: Int): Scale =
            ofOrNull(visit)
                ?: throw NoSuchElementException("No element $visit in Scale")

        fun ofOrNull(visit: Int): Scale? =
            Scale.entries.find { it.midiCode == visit.toUByte() }
    }
}