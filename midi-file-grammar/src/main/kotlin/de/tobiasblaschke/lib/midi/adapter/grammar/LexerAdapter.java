package de.tobiasblaschke.lib.midi.adapter.grammar;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

public abstract class LexerAdapter extends Lexer {
    private final Logger log = LoggerFactory.getLogger(LexerAdapter.class);

    private final Stack<Token> extraTokens = new Stack<>();
    private final Stack<Integer> popModePositions = new Stack<>();
    private int endOfChunk = -1;
    private int runningStatusArgumentCount = 0;

    public LexerAdapter(CharStream input) {
        super(input);
    }

    @Override
    public Token nextToken() {
        if (extraTokens.isEmpty()) {
            return super.nextToken();
        } else {
            return extraTokens.pop();
        }
    }

    @Override
    public Token emit() {
        int charIndex = getCharIndex();

        if ((_type == Midi1FileLexer.BEGIN_MIDI_HEADER) || _type == Midi1FileLexer.BEGIN_TRACK) {
            String lengthBytes = getText().substring(4);
            int chunkLength = ((int) lengthBytes.charAt(0)) * 0x1000000 + ((int) lengthBytes.charAt(1)) * 0x10000 + ((int) lengthBytes.charAt(2)) * 0x100 + ((int) lengthBytes.charAt(3));
            endOfChunk = getCharIndex() + chunkLength;
            log.debug("Chunk-length: {} will end at position {}", chunkLength, endOfChunk);
        }

        if (endOfChunk != -1 && endOfChunk <= getCharIndex()) {
            log.debug("Emitting END_OF_CHUNK...");
            endOfChunk = -1;
            popMode();
            extraTokens.push(_factory.create(_tokenFactorySourcePair, Midi1FileLexer.END_OF_CHUNK, "", _channel, _tokenStartCharIndex, getCharIndex()-1,
                    _tokenStartLine, _tokenStartCharPositionInLine));
        }

        if (!popModePositions.isEmpty()) {
            int popAt = popModePositions.peek();
            if (popAt <= charIndex) {
                int popped = popModePositions.pop();
                log.trace("Emitting popMode after bytes on position {} expected was {} = {}", charIndex, popped, popAt);
                popMode();
            }
        }

        emitChannelTokenIfApplicable(_type);
        handleRunningStatus();

        return super.emit();
    }

    private void handleRunningStatus() {
        switch (_type) {
            case Midi1FileLexer.COMMAND_NOTE_OFF_RANGE:
            case Midi1FileLexer.COMMAND_NOTE_ON_RANGE:
            case Midi1FileLexer.COMMAND_POLYPHONIC_KEY_PRESSURE_RANGE:
            case Midi1FileLexer.COMMAND_CC_RANGE:
            case Midi1FileLexer.COMMAND_PITCH_BEND_RANGE:
                runningStatusArgumentCount = 2;
                break;
            case Midi1FileLexer.COMMAND_PC_RANGE:
            case Midi1FileLexer.COMMAND_CHANNEL_PRESSURE_RANGE:
                runningStatusArgumentCount = 1;
                break;
            case Midi1FileLexer.RUNNING_STATUS: {
                // consume runningStatusArgumentCount - 1 further symbols
                extraTokens.push(_factory.create(_tokenFactorySourcePair, Midi1FileLexer.ARG_BYTE7, getText(),
                        _channel, _tokenStartCharIndex, getCharIndex() - 1,
                        _tokenStartLine, _tokenStartCharPositionInLine));
                _text = "";

                if (runningStatusArgumentCount > 1) {
                    pushMode(Midi1FileLexer.NUMERIC_ARGUMENTS);
                    popModeAfterBytes(runningStatusArgumentCount - 1);
                }
                break;
            }
        }
    }

    protected void popModeAfterVariableLengthQuantity() {
        int popAfterBytes = getText().chars()
                // .peek(ch -> System.out.println("character " + ch + " as " + (ch & 0x7F)))
                .reduce(0x00, (agg, add) -> (agg * 0x80) + (add & 0x7F));
        int endPosition = getCharIndex() + popAfterBytes;
        popModePositions.push(endPosition);
        log.debug("Popping text-mode after vlq {} which will be at {}", popAfterBytes, endPosition);
    }

    protected void popModeAfterBytes(int popAfterBytes) {
        int endPosition = getCharIndex() + popAfterBytes;
        popModePositions.push(endPosition);
        log.trace("Popping mode after {} which will be at {}", popAfterBytes, endPosition);
    }

    private void emitChannelTokenIfApplicable(int currentTokenType) {
        switch (currentTokenType) {
            case Midi1FileLexer.COMMAND_NOTE_OFF_RANGE:
            case Midi1FileLexer.COMMAND_NOTE_ON_RANGE:
            case Midi1FileLexer.COMMAND_POLYPHONIC_KEY_PRESSURE_RANGE:
            case Midi1FileLexer.COMMAND_CC_RANGE:
            case Midi1FileLexer.COMMAND_PC_RANGE:
            case Midi1FileLexer.COMMAND_CHANNEL_PRESSURE_RANGE:
            case Midi1FileLexer.COMMAND_PITCH_BEND_RANGE:
                int channelNumber = getText().charAt(0) & 0xF;
                String channelNumberText = "_".replace('_', (char) channelNumber);
                extraTokens.push(_factory.create(_tokenFactorySourcePair, Midi1FileLexer.CHANNEL,
                        channelNumberText,
                        _channel, _tokenStartCharIndex, getCharIndex() - 1,
                        _tokenStartLine, _tokenStartCharPositionInLine));
        }
    }
}