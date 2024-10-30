package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiEvent
import de.tobiasblaschke.lib.midi.adapter.grammar.ParserUtils.charHexCodesAsString
import org.slf4j.LoggerFactory

class MidiEventVisitor(
    private val numberVisitor: Midi1FileNumberVisitor = Midi1FileNumberVisitor(),
    private val textVisitor: Midi1FileTextVisitor = Midi1FileTextVisitor(numberVisitor),
    private val enumVisitor: Midi1FileEnumVisitor = Midi1FileEnumVisitor(numberVisitor),
) : Midi1FileParserBaseVisitor<MidiEvent>() {
    private val log = LoggerFactory.getLogger(MidiEventVisitor::class.java)
    private var runningStatusFactory: ((List<Midi1FileParser.Byte7Context>) -> MidiEvent)? = null

    override fun visitEvent(ctx: Midi1FileParser.EventContext): MidiEvent =
        requireNotNull(super.visitEvent(ctx)) {
            val rule = Midi1FileParser.ruleNames[ctx.ruleIndex]
            val startToken = Midi1FileParser.VOCABULARY.getDisplayName(ctx.start.type)
            String.format("Should not return null on '$rule' starting with $startToken from byte ${ctx.start.startIndex} (0x%02X) containing ${ctx.text.charHexCodesAsString()}",
                ctx.start.startIndex)
        }

    override fun visitRunningStatus(ctx: Midi1FileParser.RunningStatusContext): MidiEvent? {
        try {
            val f = runningStatusFactory
            if (f == null) {
                log.error("Cannot create running-status based event as no previous event was registered. Ignoring the bytes")
                return null
            } else {
                return f(ctx.byte7())
            }
        } catch (e: RuntimeException) {
            log.error("Unable to execute running-status. Skipping it", e)
            return null
        }
    }

    override fun visitNoteOff(ctx: Midi1FileParser.NoteOffContext): MidiEvent.MidiEventGeneral.NoteOff {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventGeneral.NoteOff(channel = channel, key = numberVisitor.visit(it[0])!!, velocity = numberVisitor.visit(it[1])!!)}
        return MidiEvent.MidiEventGeneral.NoteOff(
            channel = numberVisitor.visit(ctx.CHANNEL())!!,
            key = numberVisitor.visit(ctx.noteKey)!!,
            velocity = numberVisitor.visit(ctx.velocity)!!)
    }

    override fun visitNoteOn(ctx: Midi1FileParser.NoteOnContext): MidiEvent.MidiEventGeneral.NoteOn {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventGeneral.NoteOn(channel = channel, key = numberVisitor.visit(it[0])!!, velocity = numberVisitor.visit(it[1])!!)}
        return MidiEvent.MidiEventGeneral.NoteOn(
            channel = channel,
            key = numberVisitor.visit(ctx.noteKey)!!,
            velocity = numberVisitor.visit(ctx.velocity)!!)
    }

//    override fun visitControlChangeEvent(ctx: Midi1FileParser.ControlChangeEventContext): MidiEvent {
//        val channel = numberVisitor.visit(ctx.CHANNEL())!!
//        // TODO: runningStatus
//        // TODO
//        return MidiEvent.MidiEventControlChange.ControlChangeController(
//            channel = channel,
//            controller = -1,
//            value = -1)
//    }

    override fun visitControlChangeController(ctx: Midi1FileParser.ControlChangeControllerContext): MidiEvent.MidiEventControlChange.ControlChangeController {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventControlChange.ControlChangeController(channel = channel, controller = numberVisitor.visit(it[0])!!, value = numberVisitor.visit(it[1])!!)}
        return MidiEvent.MidiEventControlChange.ControlChangeController(
            channel = channel,
            controller = numberVisitor.visit(ctx.byte_(0))!!,
            value = numberVisitor.visit(ctx.byte_(1))!!)
    }

    override fun visitProgramChange(ctx: Midi1FileParser.ProgramChangeContext): MidiEvent.MidiEventGeneral.ProgramChange {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventGeneral.ProgramChange(channel = channel, programNumber = numberVisitor.visit(it[0])!!)}
        return MidiEvent.MidiEventGeneral.ProgramChange(
            channel = channel,
            programNumber = numberVisitor.visit(ctx.programNumber)!!)
    }

    override fun visitPolyphonicKeyPressure(ctx: Midi1FileParser.PolyphonicKeyPressureContext): MidiEvent.MidiEventGeneral.PolymorphicKeyPressure {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventGeneral.PolymorphicKeyPressure(channel = channel, key = numberVisitor.visit(it[0])!!, velocity = numberVisitor.visit(it[1])!!)}
        return MidiEvent.MidiEventGeneral.PolymorphicKeyPressure(
            channel = channel,
            key = numberVisitor.visit(ctx.noteKey)!!,
            velocity = numberVisitor.visit(ctx.velocity)!!)
    }

    override fun visitChannelPressure(ctx: Midi1FileParser.ChannelPressureContext): MidiEvent.MidiEventGeneral.ChannelPressure {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventGeneral.ChannelPressure(channel = channel, pressure = numberVisitor.visit(it[0])!!)}
        return MidiEvent.MidiEventGeneral.ChannelPressure(
            channel = channel,
            pressure = numberVisitor.visit(ctx.pressure)!!)
    }

    override fun visitPitchBend(ctx: Midi1FileParser.PitchBendContext): MidiEvent.MidiEventGeneral.PitchBend {
        val channel = numberVisitor.visit(ctx.CHANNEL())!!
        runningStatusFactory = { MidiEvent.MidiEventGeneral.PitchBend(channel = channel, bend = numberVisitor.visit(it[0])!!)} // TODO: Takes a short!
        return MidiEvent.MidiEventGeneral.PitchBend(
            channel = channel,
            bend = numberVisitor.visit(ctx.bend)!!)
    }

    override fun visitSysexLength(ctx: Midi1FileParser.SysexLengthContext?): MidiEvent {
        return super.visitSysexLength(ctx)
    }

    override fun visitMetaSequenceNumber(ctx: Midi1FileParser.MetaSequenceNumberContext) =
        MidiEvent.MetaEvent.SequenceNumber(
            number = numberVisitor.visit(ctx.sequenceNumber)!!)

    override fun visitMetaText(ctx: Midi1FileParser.MetaTextContext) =
        MidiEvent.MetaEvent.Text(
            text = textVisitor.visitMetaText(ctx))

    override fun visitMetaCopyrightNotice(ctx: Midi1FileParser.MetaCopyrightNoticeContext) =
        MidiEvent.MetaEvent.CopyrightNotice(
            text = textVisitor.visitMetaCopyrightNotice(ctx))

    override fun visitMetaSequenceTrackName(ctx: Midi1FileParser.MetaSequenceTrackNameContext) =
        MidiEvent.MetaEvent.SequenceOrTrackName(
            text = textVisitor.visitMetaSequenceTrackName(ctx))

    override fun visitMetaInstrumentName(ctx: Midi1FileParser.MetaInstrumentNameContext) =
        MidiEvent.MetaEvent.InstrumentName(
            text = textVisitor.visitMetaInstrumentName(ctx))

    override fun visitMetaLyric(ctx: Midi1FileParser.MetaLyricContext) =
        MidiEvent.MetaEvent.Lyric(
            text = textVisitor.visitMetaLyric(ctx))

    override fun visitMetaMarker(ctx: Midi1FileParser.MetaMarkerContext) =
        MidiEvent.MetaEvent.Marker(
            text = textVisitor.visitMetaMarker(ctx))

    override fun visitMetaCuePoint(ctx: Midi1FileParser.MetaCuePointContext) =
        MidiEvent.MetaEvent.CuePoint(
            text = textVisitor.visitMetaCuePoint(ctx))

    override fun visitMetaMidiChannelPrefix(ctx: Midi1FileParser.MetaMidiChannelPrefixContext) =
        MidiEvent.MetaEvent.ChannelPrefix(
            channelNumber = numberVisitor.visit(ctx.channelNumber)!!)

    override fun visitMetaSetTempo(ctx: Midi1FileParser.MetaSetTempoContext) =
        MidiEvent.MetaEvent.Tempo(
            microsecondsPerQuarterNode = numberVisitor.visitMicrosecondsPerQuarterNote(ctx.microsecondsPerQuarterNote())!!)

//    override fun visitMetaSmpteOffset(ctx: Midi1FileParser.MetaSmpteOffsetContext) =

    override fun visitMetaTimeSignature(ctx: Midi1FileParser.MetaTimeSignatureContext) =
        MidiEvent.MetaEvent.TimeSignature(
            numerator = numberVisitor.visit(ctx.timeSignature().numerator)!!,
            denominator = numberVisitor.visit(ctx.timeSignature().denominator)!!,
            numberOf32NotesIn24MidiClocks = numberVisitor.visit(ctx.timeSignature().numberOf32NotesIn24MidiClocks)!!,
            numberOfMidiClocksPerMetronomeClick = numberVisitor.visit(ctx.timeSignature().numberOfMidiClocksPerMetronomeClick)!!)

    override fun visitMetaKeySignature(ctx: Midi1FileParser.MetaKeySignatureContext) =
        MidiEvent.MetaEvent.KeySignature(
            sharpsOrFlats = enumVisitor.visitSharpsOrFlats(ctx.keySignature().sharpsOrFlats),
            scale = enumVisitor.visitScale(ctx.keySignature().scale)
        )

    override fun visitMetaSequencerSpecific(ctx: Midi1FileParser.MetaSequencerSpecificContext?): MidiEvent {
        return super.visitMetaSequencerSpecific(ctx)
    }

    override fun visitMetaOther(ctx: Midi1FileParser.MetaOtherContext?): MidiEvent {
        return super.visitMetaOther(ctx)
    }
}