#pragma once

#include <vector>
#include <memory>
#include "Token.h"

namespace de_tobiasblaschke_lib_midi_adapter_grammar {
    std::unique_ptr<antlr4::CommonTokenStream> lexToStream(const std::vector<uint8_t> &input);
    std::vector<std::unique_ptr<antlr4::Token>> lexToList(const std::vector<uint8_t> &input);
    std::vector<std::unique_ptr<antlr4::Token>> tokenStreamAsList(antlr4::CommonTokenStream &tokenStream);
    void assertTokensMatch(const std::vector<std::unique_ptr<antlr4::Token>> &tokens, const std::vector<std::unique_ptr<antlr4::Token>> &expectedTokens);
}

