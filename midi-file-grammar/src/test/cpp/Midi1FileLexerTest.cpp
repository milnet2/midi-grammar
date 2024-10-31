#include <gtest/gtest.h>
#include "Midi1FileLexer.h"
#include "LexerTestUtils.h"

using namespace de_tobiasblaschke_lib_midi_adapter_grammar;

class Midi1FileLexerTest : public ::testing::Test {
protected:
    void SetUp() override {
        // Set up any initial state here if required
    }


};

TEST_F(Midi1FileLexerTest, ShouldLexFormat0Header) {
    std::vector<uint8_t> input = {
            0x4D, 0x54, 0x68, 0x64,         // MThd
            0x00, 0x00, 0x00, 0x06,         // chunk length
            0x00, 0x00,                     // format 0
            0x00, 0x01,                     // one track
            0x00, 0x90                      // status byte
    };

    const std::vector<std::unique_ptr<antlr4::Token>> tokens = lexToList(input);

    std::vector<std::unique_ptr<antlr4::Token>> expectedTokens;
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::BEGIN_MIDI_HEADER, "\x4D\x54\x68\x64\x00\x00\x00\x06"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::ARG_BYTE7, "\x00"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::ARG_BYTE7, "\x00"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::ARG_BYTE7, "\x00"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::ARG_BYTE7, "\x01"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::ARG_BYTE7, "\x00"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::ARG_BYTE7, "\x90"}});
    expectedTokens.push_back(
            std::unique_ptr<antlr4::Token> { new antlr4::CommonToken {Midi1FileLexer::END_OF_CHUNK, ""}});

    assertTokensMatch(tokens, expectedTokens);
}
