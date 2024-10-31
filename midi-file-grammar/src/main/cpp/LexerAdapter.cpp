#include "LexerAdapter.h"
#include "Midi1FileLexer.h"

using namespace de_tobiasblaschke_lib_midi_adapter_grammar;

LexerAdapter::LexerAdapter(antlr4::CharStream *input) : Lexer(input), endOfChunk(-1), runningStatusArgumentCount(0) {}

std::unique_ptr<antlr4::Token> LexerAdapter::nextToken() {
    if (extraTokens.empty()) {
        return Lexer::nextToken();
    } else {
        auto token = std::move(extraTokens.top());
        extraTokens.pop();
        return token;
    }
}

antlr4::Token* LexerAdapter::emit() {
    auto charIndex = getCharIndex();

    if ((_type == Midi1FileLexer::BEGIN_MIDI_HEADER) || _type == Midi1FileLexer::BEGIN_TRACK) {
        std::string lengthBytes = getText().substr(4);
        size_t chunkLength = (static_cast<int>(lengthBytes[0]) * 0x1000000) +
                          (static_cast<int>(lengthBytes[1]) * 0x10000) +
                          (static_cast<int>(lengthBytes[2]) * 0x100) +
                          static_cast<int>(lengthBytes[3]);
        endOfChunk = charIndex + chunkLength;
//            spdlog::debug("Chunk-length: {} will end at position {}", chunkLength, endOfChunk);
    }

    if (endOfChunk != -1 && endOfChunk <= charIndex) {
//            spdlog::debug("Emitting END_OF_CHUNK...");
        endOfChunk = -1;
        popMode();
        extraTokens.push(_factory->create(Midi1FileLexer::END_OF_CHUNK, ""));
    }

    if (!popModePositions.empty()) {
        int popAt = popModePositions.top();
        if (popAt <= charIndex) {
            int popped = popModePositions.top();
            popModePositions.pop();
//                spdlog::trace("Emitting popMode after bytes on position {} expected was {} = {}", charIndex, popped, popAt);
            popMode();
        }
    }

    emitChannelTokenIfApplicable(_type);
    handleRunningStatus();

    return Lexer::emit();
}

void LexerAdapter::handleRunningStatus() {
    switch (_type) {
        case Midi1FileLexer::COMMAND_NOTE_OFF_RANGE:
        case Midi1FileLexer::COMMAND_NOTE_ON_RANGE:
        case Midi1FileLexer::COMMAND_POLYPHONIC_KEY_PRESSURE_RANGE:
        case Midi1FileLexer::COMMAND_CC_RANGE:
        case Midi1FileLexer::COMMAND_PITCH_BEND_RANGE:
            runningStatusArgumentCount = 2;
            break;
        case Midi1FileLexer::COMMAND_PC_RANGE:
        case Midi1FileLexer::COMMAND_CHANNEL_PRESSURE_RANGE:
            runningStatusArgumentCount = 1;
            break;
        case Midi1FileLexer::RUNNING_STATUS: {
            extraTokens.push(_factory->create(Midi1FileLexer::ARG_BYTE7, getText()));
            _text = "";

            if (runningStatusArgumentCount > 1) {
                pushMode(Midi1FileLexer::NUMERIC_ARGUMENTS);
                popModeAfterBytes(runningStatusArgumentCount - 1);
            }
            break;
        }
    }
}

void LexerAdapter::popModeAfterVariableLengthQuantity() {
    int popAfterBytes = 0;
    for (char ch : getText()) {
        popAfterBytes = (popAfterBytes * 0x80) + (ch & 0x7F);
    }
    auto endPosition = getCharIndex() + popAfterBytes;
    popModePositions.push(endPosition);
//    spdlog::debug("Popping text-mode after vlq {} which will be at {}", popAfterBytes, endPosition);
}

void LexerAdapter::popModeAfterBytes(int popAfterBytes) {
    auto endPosition = getCharIndex() + popAfterBytes;
    popModePositions.push(endPosition);
//    spdlog::trace("Popping mode after {} which will be at {}", popAfterBytes, endPosition);
}

void LexerAdapter::emitChannelTokenIfApplicable(int currentTokenType) {
    switch (currentTokenType) {
        case Midi1FileLexer::COMMAND_NOTE_OFF_RANGE:
        case Midi1FileLexer::COMMAND_NOTE_ON_RANGE:
        case Midi1FileLexer::COMMAND_POLYPHONIC_KEY_PRESSURE_RANGE:
        case Midi1FileLexer::COMMAND_CC_RANGE:
        case Midi1FileLexer::COMMAND_PC_RANGE:
        case Midi1FileLexer::COMMAND_CHANNEL_PRESSURE_RANGE:
        case Midi1FileLexer::COMMAND_PITCH_BEND_RANGE: {
            int channelNumber = getText()[0] & 0xF;
            std::string channelNumberText(1, static_cast<char>(channelNumber));
            extraTokens.push(_factory->create(Midi1FileLexer::CHANNEL, channelNumberText));
            break;
        }
    }
}