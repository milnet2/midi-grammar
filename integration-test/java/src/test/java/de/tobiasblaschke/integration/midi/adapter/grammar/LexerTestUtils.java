package de.tobiasblaschke.integration.midi.adapter.grammar;

import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileLexer;
import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileParser;
import org.antlr.v4.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static de.tobiasblaschke.lib.midi.adapter.grammar.ParserUtils.charHexCodesAsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class LexerTestUtils {
    private LexerTestUtils() { } // Disable instance creation

    public static CommonTokenStream lexToStream(final byte[] block) {
        try (InputStream byteStream = new ByteArrayInputStream(block)) {
            return lexToStream(byteStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CommonTokenStream lexToStream(InputStream byteStream) {
        try {
            CharStream bytesAsChar = CharStreams.fromStream(byteStream, StandardCharsets.ISO_8859_1);
            Midi1FileLexer lexer = new Midi1FileLexer(bytesAsChar);
            return new CommonTokenStream(lexer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<Token> lexToList(final byte[] block) {
        try (InputStream byteStream = new ByteArrayInputStream(block)) {
            return lexToList(byteStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Token> lexToList(InputStream byteStream) {
        return tokenStreamAsList(lexToStream(byteStream));
    }

    public static List<Token> tokenStreamAsList(CommonTokenStream tokenStream) {
        var tokenSource = tokenStream.getTokenSource();

        var ret = new ArrayList<Token>();
        var nextToken = tokenSource.nextToken();
        while (nextToken != null && nextToken.getType() != Token.EOF) {
            ret.add(nextToken);
            nextToken = tokenSource.nextToken();
        }

        return ret;
    }

    public static void assertTokensMatch(List<Token> actual, TokenMatcher... matchers) {
        IntStream.range(0, matchers.length)
                        .mapToObj(index -> new AbstractMap.SimpleEntry<Integer, TokenMatcher>(index, matchers[index]))
                        .forEach(im -> {
                            var token = actual.get(im.getKey());
                            assertThat("Mismatch on token " + im.getKey(), token, im.getValue());
                        });

        assertEquals(matchers.length, actual.size(), "Expected number of tokens does not match number of matchers");
    }

    public static Midi1FileParser makeParser(final CommonTokenStream tokens) {
        var ret = new Midi1FileParser(tokens);
        ret.setErrorHandler(new DefaultErrorStrategy() {
            @Override
            protected String getTokenErrorDisplay(final @Nullable Token t) {
                if (t == null) return "<no token>";
                var text = t.getText();
                var hex = (text == null) ? "- no text -" : charHexCodesAsString(text, " ", 16, "    ");
                var tokenName = Midi1FileLexer.VOCABULARY.getDisplayName(t.getType());
                return String.format(
                        "ERROR: on %s = '%s' at %d (0x%02X) length %d",
                        tokenName, hex,
                        t.getStartIndex(),
                        t.getStartIndex(),
                        t.getStopIndex() - t.getStartIndex() + 1);
            }
        });

        return ret;
    }

}
