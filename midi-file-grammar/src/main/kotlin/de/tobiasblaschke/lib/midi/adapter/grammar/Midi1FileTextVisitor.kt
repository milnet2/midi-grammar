package de.tobiasblaschke.lib.midi.adapter.grammar

import org.antlr.v4.runtime.tree.TerminalNode
import org.slf4j.LoggerFactory

class Midi1FileTextVisitor(
    private val numberVisitor: Midi1FileNumberVisitor
) : Midi1FileParserBaseVisitor<String?>() {
    companion object {
        private val log = LoggerFactory.getLogger(Midi1FileTextVisitor::class.java)
    }

    override fun defaultResult(): String? = null
    override fun aggregateResult(aggregate: String?, nextResult: String?): String? = when {
        aggregate == null -> nextResult
        nextResult == null -> aggregate
        else -> aggregate + nextResult
    }

    override fun visitMetaText(ctx: Midi1FileParser.MetaTextContext) =
        visitText(ctx.text())

    override fun visitMetaCopyrightNotice(ctx: Midi1FileParser.MetaCopyrightNoticeContext) =
        visitText(ctx.text())

    override fun visitMetaSequenceTrackName(ctx: Midi1FileParser.MetaSequenceTrackNameContext) =
        visitText(ctx.text())

    override fun visitMetaInstrumentName(ctx: Midi1FileParser.MetaInstrumentNameContext) =
        visitText(ctx.text())

    override fun visitMetaLyric(ctx: Midi1FileParser.MetaLyricContext) =
        visitText(ctx.text())

    override fun visitMetaMarker(ctx: Midi1FileParser.MetaMarkerContext) =
        visitText(ctx.text())

    override fun visitMetaCuePoint(ctx: Midi1FileParser.MetaCuePointContext) =
        visitText(ctx.text())

    override fun visitText(ctx: Midi1FileParser.TextContext): String {
        val announcedLength = numberVisitor.convertVariableLengthQuantity(ctx.length)
        val content = ctx.TEXT_BYTE()?.joinToString(separator = "") { it.text } ?: ""

        if (log.isWarnEnabled) {
            if (content.length < announcedLength) {
                log.warn("Announced length $announcedLength is shorter than actual ${content.length} on '$content'")
            }
        }

        return content
    }
}