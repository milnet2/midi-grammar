package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.ParserUtils.charHexCodesAsString
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiEvent
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiFileChunk
import de.tobiasblaschke.lib.midi.adapter.grammar.domain.MidiFileEvent
import org.antlr.v4.runtime.tree.ErrorNode
import org.slf4j.LoggerFactory

class Midi1FileChunkVisitor(
    private val numberVisitor: Midi1FileNumberVisitor = Midi1FileNumberVisitor(),
    private val timeDivisionVisitor: Midi1FileTimeDivisionVisitor = Midi1FileTimeDivisionVisitor(numberVisitor),
    private val midiEventVisitor: MidiEventVisitor = MidiEventVisitor(numberVisitor),
    private val enumVisitor: Midi1FileEnumVisitor = Midi1FileEnumVisitor(numberVisitor),
): Midi1FileParserBaseVisitor<MidiFileChunk>() {
    companion object {
        private val log = LoggerFactory.getLogger(Midi1FileChunkVisitor::class.java)
    }

    override fun visitErrorNode(node: ErrorNode): MidiFileChunk? {
        val hex = node.text?.charHexCodesAsString() ?: " - no text -"
        val tokenName = Midi1FileLexer.VOCABULARY.getDisplayName(node.symbol.type)
        log.error("ERROR: on $node ($tokenName) = HEX $hex ")
        return super.visitErrorNode(node)
    }

    override fun visitMidiHeader(ctx: Midi1FileParser.MidiHeaderContext): MidiFileChunk.MidiFileHeader {
        val numberOfTracks = numberVisitor.visitShort8(ctx.numberOfTracks)

        if (log.isWarnEnabled) {
            if (numberOfTracks < 1) {
                log.warn("The file announced no tracks (i.e. {}) in its header", numberOfTracks)
            }
            if (ctx.text.length != 14) {
                log.warn("Unexpected header-length: {}", ctx.text.length)
            }
        }

        return MidiFileChunk.MidiFileHeader(
            midiFileTrackFormat = enumVisitor.visitTrackFormat(ctx.trackFormat),
            numberOfTracks = numberOfTracks,
            timeDivision = timeDivisionVisitor.visit(ctx.timeDivision()))
    }

    override fun visitMidiTrack(ctx: Midi1FileParser.MidiTrackContext): MidiFileChunk.MidiFileTrack {
        log.debug("Visit track")
        val events = (ctx.mTrkEvent() ?: emptyList())
            .map { MidiFileEvent(
                deltaTime = numberVisitor.visitDeltaTime(it.deltaTime()),
                event = midiEventVisitor.visitEvent(it.event())) }
        val trackEnd = eventForEndOfTrack(ctx.endOfTrack())

        if (log.isWarnEnabled) {
            if (events.isEmpty()) {
                log.warn("Track did not contain any events")
            }
            log.debug("Track complete")
        }

        return MidiFileChunk.MidiFileTrack(events + trackEnd)
    }

    private fun eventForEndOfTrack(ctx: Midi1FileParser.EndOfTrackContext): MidiFileEvent {
        var deltaTime: Int = 0

        when(ctx) {
            is Midi1FileParser.ProperEndContext ->
                deltaTime = numberVisitor.visitDeltaTime(ctx.deltaTime())
            is Midi1FileParser.MissingEndOfTrackContext ->
                log.warn("The track did not end with END_OF_TRACK (FF 2F 00) or the length announced in the header was wrong")
            is Midi1FileParser.TrackWasHorterThanAnnouncedContext -> {
                deltaTime = numberVisitor.visitDeltaTime(ctx.deltaTime())
                log.warn("The track ended itself prior to it's length announced in the track-header")
            }
        }

        return MidiFileEvent(
            deltaTime = deltaTime,
            event = MidiEvent.MetaEvent.EndOfTrack)
    }
}