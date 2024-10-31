#include <gtest/gtest.h>
#include "LexerTestUtils.h"
#include "LexerAdapter.h"
#include "Midi1FileLexer.h"

namespace de_tobiasblaschke_lib_midi_adapter_grammar {

    std::vector<std::unique_ptr<antlr4::Token>> lexToList(const std::vector<uint8_t> &input) {
        auto tokenStream = lexToStream(input);
        return tokenStreamAsList(*tokenStream);
    }

    std::unique_ptr<antlr4::CommonTokenStream> lexToStream(const std::vector<uint8_t> &input) {
        // antlr4::CharStream inputSteam = antlr4::CharStreams.

        std::string_view inputView(reinterpret_cast<const char*>(input.data()), input.size());
        antlr4::ANTLRInputStream inputStream(inputView);
        Midi1FileLexer lexer(&inputStream);

        std::unique_ptr<antlr4::CommonTokenStream> tStream(new antlr4::CommonTokenStream(&lexer));
        return tStream;
    }

    std::vector<std::unique_ptr<antlr4::Token>> tokenStreamAsList(antlr4::CommonTokenStream &tokenStream) {
        auto tokenSource = tokenStream.getTokenSource();

        std::vector<std::unique_ptr<antlr4::Token>> ret;
        auto nextToken = tokenSource->nextToken();
        while (nextToken != nullptr && nextToken->getType() != antlr4::Token::EOF) {
            ret.push_back(std::move(nextToken));
            nextToken = tokenSource->nextToken();
        }

        return ret;
    }

    void assertTokensMatch(const std::vector<std::unique_ptr<antlr4::Token>> &tokens, const std::vector<std::unique_ptr<antlr4::Token>> &expectedTokens) {
        ASSERT_EQ(tokens.size(), expectedTokens.size());
        for (size_t i = 0; i < tokens.size(); ++i) {
            EXPECT_EQ(tokens[i]->getType(), expectedTokens[i]->getType());
            EXPECT_EQ(tokens[i]->getText(), expectedTokens[i]->getText());
        }
    }
}