package de.tobiasblaschke.lib.midi.adapter.grammar.domain

sealed interface MidiFileChunk {
    data class MidiFileHeader(
        val midiFileTrackFormat: MidiFileTrackFormat,
        val numberOfTracks: Int,
        val timeDivision: TimeDivision
    ): MidiFileChunk

    data class MidiFileTrack(
        val events: List<MidiFileEvent>
    ): MidiFileChunk
}

enum class MidiFileTrackFormat(val numeric: Short) {
    SINGLE_MULTI_CHANNEL_TRACK(0),  // the file contains a single multi-channel track
    SIMULTANEOUS_TRACKS(1), // the file contains one or more simultaneous tracks (or MIDI outputs) of a sequence
    INDEPENDENT_TRACKS(2), ;

    companion object {
        fun of(visit: Int): MidiFileTrackFormat =
            entries.find { it.numeric == visit.toShort() }
                ?: throw NoSuchElementException("No element $visit in MidiFileTrackFormat")
    }
}

sealed interface TimeDivision {
    data class TicksPerQuarterNote(val count: Int): TimeDivision
    data class SMPTE(val smpte: Int, val ticksPerFrame: Int): TimeDivision
}