package de.tobiasblaschke.lib.midi.adapter.grammar.domain

data class MidiFileEvent(
    val deltaTime: Int,
    val event: MidiEvent)