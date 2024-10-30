package de.tobiasblaschke.lib.midi.adapter.grammar

import kotlin.streams.toList

object ParserUtils {

    fun String.charHexCodesAsString(separator: String = " ", wrapAt: Int = 16, wrapIndent: String = "    ") =
        if (length <= wrapAt) {
            chars().toList()
                .joinToString(separator) { String.format("%02X", it) }
        } else {
            chars().toList()
                .chunked(wrapAt)
                .map { chunk -> chunk.joinToString(separator) { String.format("%02X", it) }}
                .joinToString(prefix = "\n$wrapIndent", separator = "\n$wrapIndent")
        }
}