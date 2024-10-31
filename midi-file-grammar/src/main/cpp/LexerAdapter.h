#pragma once

#include "antlr4-runtime.h"

class LexerAdapter : public antlr4::Lexer {
private:
    std::stack<std::unique_ptr<antlr4::Token>> extraTokens;
    std::stack<int> popModePositions;
    size_t endOfChunk;
    int runningStatusArgumentCount;
    int _type;
    std::string _text;

public:
	LexerAdapter(antlr4::CharStream *input);

    std::unique_ptr<antlr4::Token> nextToken() override;
	antlr4::Token* emit() override;

protected:
    void handleRunningStatus();
    void popModeAfterVariableLengthQuantity();
    void popModeAfterBytes(int popAfterBytes);
    void emitChannelTokenIfApplicable(int currentTokenType);

};