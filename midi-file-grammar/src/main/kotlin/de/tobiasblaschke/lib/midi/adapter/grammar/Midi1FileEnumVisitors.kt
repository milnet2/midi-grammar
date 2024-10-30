package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileParser.Short8Context
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiFileTrackFormat
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.Scale
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.SharpsAndFlats
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.TimeDivision

class Midi1FileTimeDivisionVisitor(
    private val numberVisitor: Midi1FileNumberVisitor = Midi1FileNumberVisitor()
): Midi1FileParserBaseVisitor<TimeDivision>() {
    override fun visitTimeDivisionTicksPerQuarterNote(ctx: Midi1FileParser.TimeDivisionTicksPerQuarterNoteContext) =
        TimeDivision.TicksPerQuarterNote(
            count = numberVisitor.visitTimeDivisionTicksPerQuarterNote(ctx))

    override fun visitTimeDivisionSMTPE(ctx: Midi1FileParser.TimeDivisionSMTPEContext) =
        TimeDivision.SMPTE(
            numberVisitor.visitTimeDivisionNegativeSMPTE(ctx.timeDivisionNegativeSMPTE()),
            numberVisitor.visitTimeDivisionTicksPerFrame(ctx.timeDivisionTicksPerFrame())
        )
}

class Midi1FileEnumVisitor(
    private val numberVisitor: Midi1FileNumberVisitor = Midi1FileNumberVisitor()
): Midi1FileParserBaseVisitor<Enum<*>?>() {


    fun visitTrackFormat(trackFormat: Short8Context) =
        MidiFileTrackFormat.of(numberVisitor.visitShort8(trackFormat))

    fun visitSharpsOrFlats(sharpsOrFlats: Midi1FileParser.ByteContext): SharpsAndFlats =
        SharpsAndFlats.of(numberVisitor.visitByte(sharpsOrFlats))

    fun visitScale(scale: Midi1FileParser.ByteContext): Scale =
        Scale.of(numberVisitor.visitByte(scale))
}