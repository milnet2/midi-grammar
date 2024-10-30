parser grammar Midi1FileParser;

options {
    tokenVocab = Midi1FileLexer;
}

midiFile: midiHeader midiTrack* EOF;

midiHeader:
    BEGIN_MIDI_HEADER
    trackFormat=short8 numberOfTracks=short8 timeDivision
    END_OF_CHUNK
    ;

timeDivision
    : byte7 byte   # timeDivisionTicksPerQuarterNote
    | timeDivisionNegativeSMPTE timeDivisionTicksPerFrame # timeDivisionSMTPE
    ;

timeDivisionNegativeSMPTE
    : upperBytes
    ;

timeDivisionTicksPerFrame
    : byte
    ;

midiTrack:
    BEGIN_TRACK
    mTrkEvent*
    endOfTrack
    ;

endOfTrack
    : deltaTime SYSTEM_REAL_TIME_END_OF_TRACK END_OF_CHUNK  # properEnd // Should always this option on proper files
    | deltaTime SYSTEM_REAL_TIME_END_OF_TRACK               # trackWasHorterThanAnnounced
    | END_OF_CHUNK                                          # missingEndOfTrack
    ;

mTrkEvent
    : deltaTime event
    ;

deltaTime
    : variableLenghtQuantity
    ;



event
    : midiEvent
//    | sysexEvent
    | metaEvent
    ;

midiEvent
    // TODO: The repretitions are for "running-status"
    : COMMAND_NOTE_OFF_RANGE channel=CHANNEL (noteKey=byte7 velocity=byte7)+        # noteOff
    | COMMAND_NOTE_ON_RANGE channel=CHANNEL (noteKey=byte7 velocity=byte7)+         # noteOn
    | COMMAND_POLYPHONIC_KEY_PRESSURE_RANGE channel=CHANNEL (noteKey=byte7 velocity=byte7)+       # polyphonicKeyPressure
    | COMMAND_CC_RANGE (controlChange)+                                             # controlChangeEvent
    | COMMAND_PC_RANGE channel=CHANNEL (programNumber=byte7)+                       # programChange
    | COMMAND_CHANNEL_PRESSURE_RANGE channel=CHANNEL (pressure=byte7)+              # channelPressure
    | COMMAND_PITCH_BEND_RANGE channel=CHANNEL (bend=short7)+                       # pitchBend
    | RUNNING_STATUS (args=byte7+)                                  # runningStatus
    ;

controlChange
//    : X78                                   # allSoundsOff
//    | X79 byte                              # resetAllControllers
//    | X7a X00                               # localControlOff
//    | X7a X7f                               # localControlOn
//    | X7b                                   # allNotesOff
//    | X7c                                   # omniModeOff
//    | X7d                                   # omniModeOn
//    | X7e byte                              # monoModeOn
//    | X7f                                   # polyModeOn
    : channel=CHANNEL byte byte                             # controlChangeController
    ;

//sysexEvent
//    : Xf0 sysexLength byte Xf7  // TODO: Count of bytes?
//    | Xf7 sysexLength byte      // TODO: Count of bytes?
//    ;

sysexLength
    : variableLenghtQuantity
    ;

metaEvent
    : SYSTEM_REAL_TIME_FF_SEQUENCE_NUMBER sequenceNumber=short8        # metaSequenceNumber
    | SYSTEM_REAL_TIME_FF_TEXT text # metaText
    | SYSTEM_REAL_TIME_FF_COPYRIGHT_NOTICE text # metaCopyrightNotice
    | SYSTEM_REAL_TIME_FF_SEQUENCE_OR_TRACK_NAME text # metaSequenceTrackName
    | SYSTEM_REAL_TIME_FF_INSTRUMENT_NAME text # metaInstrumentName
    | SYSTEM_REAL_TIME_FF_LYRIC text # metaLyric
    | SYSTEM_REAL_TIME_FF_MARKER text # metaMarker
    | SYSTEM_REAL_TIME_FF_CUE_POINT text # metaCuePoint
    | SYSTEM_REAL_TIME_MIDI_CHANNEL_PREFIX channelNumber=byte7          # metaMidiChannelPrefix
    | SYSTEM_REAL_TIME_FF_TEMPO microsecondsPerQuarterNote                 # metaSetTempo
    | SYSTEM_REAL_TIME_FF_SMPTE_OFFSET byte byte byte byte byte                  # metaSmpteOffset   // TODO: split payload
    | SYSTEM_REAL_TIME_FF_TIME_SIGNATURE timeSignature                      # metaTimeSignature
    | SYSTEM_REAL_TIME_FF_KEY_SIGNATURE keySignature                        # metaKeySignature
    | SYSTEM_REAL_TIME_FF_SEQUENCER_SPECIFIC # metaSequencerSpecific // TODO: variable-length data
    | SYSTEM_REAL_TIME_FF   # metaOther
    ;

microsecondsPerQuarterNote: byte byte byte;
timeSignature: numerator=byte denominator=byte numberOfMidiClocksPerMetronomeClick=byte numberOf32NotesIn24MidiClocks=byte;
keySignature: sharpsOrFlats=byte scale=byte;

variableLenghtQuantity
    : VARIABLE_LENGTH_QUANTITY_VALUE
    ;

// ------------------------------

text: length=TEXT_LENGTH_QUANTITY_VALUE content=TEXT_BYTE*;
byte7: ARG_BYTE7;
upperBytes: ARG_BYTE_UPPER;
byte: byte7 | upperBytes;
short8: byte byte;
short7: byte7 byte7;
