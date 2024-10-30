package de.tobiasblaschke.lib.midi.adapter.grammar

import de.tobiasblaschke.lib.midi.adapter.grammar.ParserUtils.charHexCodesAsString
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.slf4j.LoggerFactory
import kotlin.streams.toList

class Midi1FileNumberVisitor: Midi1FileParserBaseVisitor<Int?>() {
    companion object {
        private val log = LoggerFactory.getLogger(Midi1FileNumberVisitor::class.java)

        const val BASE_FOR_7BIT = 0x80;
        const val BASE_FOR_8BIT = 0x100;
    }

    override fun defaultResult(): Int? = null
    override fun aggregateResult(aggregate: Int?, nextResult: Int?): Int? = when {
        aggregate == null -> nextResult
        nextResult == null -> aggregate
        else -> {
            log.warn("Conversion should not aggregate. Still doing so using 8-bit base")
            aggregate * BASE_FOR_8BIT + nextResult
        }
    }
    override fun visitErrorNode(node: ErrorNode): Int? {
        val hex = node.text?.charHexCodesAsString() ?: " - no text -"
        val tokenName = Midi1FileLexer.VOCABULARY.getDisplayName(node.symbol.type)
        log.error("ERROR: on $node ($tokenName) = HEX $hex ")
        return super.visitErrorNode(node)
    }

    override fun visitTimeDivisionTicksPerQuarterNote(ctx: Midi1FileParser.TimeDivisionTicksPerQuarterNoteContext?): Int =
        requireNotNull(super.visitTimeDivisionTicksPerQuarterNote(ctx))

    override fun visitTimeDivisionTicksPerFrame(ctx: Midi1FileParser.TimeDivisionTicksPerFrameContext): Int =
        requireNotNull(super.visitTimeDivisionTicksPerFrame(ctx))

    override fun visitTimeDivisionNegativeSMPTE(ctx: Midi1FileParser.TimeDivisionNegativeSMPTEContext): Int =
        requireNotNull(super.visitTimeDivisionNegativeSMPTE(ctx)) // TODO: Re-calculate

    override fun visitShort8(ctx: Midi1FileParser.Short8Context): Int =
        convertToInteger(ctx.start, text = ctx.text,
            base = BASE_FOR_8BIT,
            minLength = 2, maxLength = 2)

    override fun visitByte7(ctx: Midi1FileParser.Byte7Context): Int =
        convertToInteger(ctx.start, text = ctx.text,
            base = BASE_FOR_7BIT,
            minLength = 1, maxLength = 1)

    override fun visitUpperBytes(ctx: Midi1FileParser.UpperBytesContext): Int =
        convertToInteger(ctx.start, text = ctx.text,
            base = BASE_FOR_8BIT,
            minLength = 1, maxLength = 1)

    override fun visitByte(ctx: Midi1FileParser.ByteContext): Int =
        convertToInteger(ctx.start, text = ctx.text,
            base = BASE_FOR_8BIT,
            minLength = 1, maxLength = 1)

    override fun visitDeltaTime(ctx: Midi1FileParser.DeltaTimeContext): Int =
        visitVariableLenghtQuantity(ctx.variableLenghtQuantity())

    override fun visitVariableLenghtQuantity(ctx: Midi1FileParser.VariableLenghtQuantityContext): Int =
        convertVariableLengthQuantity(ctx.VARIABLE_LENGTH_QUANTITY_VALUE().symbol)

    override fun visitTerminal(node: TerminalNode): Int? {
        val token = node.symbol

        return when(token.type) {
            Midi1FileLexer.ARG_BYTE_UPPER ->
                convertToInteger(token, base = BASE_FOR_8BIT)
            Midi1FileLexer.ARG_BYTE7 ->
                convertToInteger(token)
            Midi1FileLexer.CHANNEL ->
                convertToInteger(token, maxValueForSingleChar = '\u000F')
            Midi1FileLexer.VARIABLE_LENGTH_QUANTITY_VALUE ->
                convertVariableLengthQuantity(token)
            else -> super.visitTerminal(node)
        }
    }

    internal fun convertVariableLengthQuantity(symbol: Token, text: String = symbol.text): Int =
        convertToInteger(symbol, text, maxLength = 4, maxValueForSingleChar = '\u00FF')

    internal fun convertToInteger(symbol: Token, text: String = symbol.text, base: Int = BASE_FOR_7BIT, minLength: Int = 1, maxLength: Int? = minLength,
                                  maxValueForSingleChar: Char = (base - 1).toChar()): Int {
        val chars = text.toCharArray()

        if (log.isTraceEnabled) {
            log.trace(String.format("Reading %s from 0x%02X to 0x%02X",
                Midi1FileLexer.VOCABULARY.getDisplayName(symbol.type),
                symbol.stopIndex, symbol.startIndex))
        }
        if (log.isWarnEnabled) {
            if (text.length < minLength) {
                log.warn(
                    String.format(
                        "The token %s (%s) from 0x%02X to 0x%02X contains less characters than expected (%d)",
                        Midi1FileLexer.VOCABULARY.getDisplayName(symbol.type),
                        text.charHexCodesAsString(),
                        symbol.stopIndex, symbol.startIndex, maxLength))
            }
            if (maxLength != null && text.length > maxLength) {
                log.warn(
                    String.format(
                        "The token %s (%s) from 0x%02X to 0x%02X contains more characters than expected (%d)",
                        Midi1FileLexer.VOCABULARY.getDisplayName(symbol.type),
                        text.charHexCodesAsString(),
                        symbol.stopIndex, symbol.startIndex, maxLength))
            }
            if (chars.any { it > maxValueForSingleChar }) {
                log.warn(
                    String.format(
                        "The token %s (%s) from 0x%02X to 0x%02X contains a character with a higher value than expected than expected (0x%02X), the higher bytes will be ignored",
                        Midi1FileLexer.VOCABULARY.getDisplayName(symbol.type),
                        text.charHexCodesAsString(),
                        symbol.stopIndex, symbol.startIndex, maxValueForSingleChar))
            }
        }

        val byteMask = base - 1
        return text.chars().toList()
            .fold(0x00) { acc, addendum -> acc * base + (addendum and byteMask) }
    }
}