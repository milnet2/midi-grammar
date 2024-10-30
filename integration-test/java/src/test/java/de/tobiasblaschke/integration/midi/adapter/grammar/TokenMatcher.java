package de.tobiasblaschke.integration.midi.adapter.grammar;

import de.tobiasblaschke.lib.midi.adapter.grammar.Midi1FileLexer;
import org.antlr.v4.runtime.Token;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TokenMatcher extends TypeSafeMatcher<Token> {
    private final int type;
    private final String text;
    private final @Nullable Integer channel;

    public TokenMatcher(final int type, final String text) {
        this(type, text, null);
    }

    public TokenMatcher(final int type, final String text, final @Nullable Integer channel) {
        super(Token.class);
        this.type = type;
        this.text = text;
        this.channel = channel;
    }

    @Override
    public boolean matchesSafely(Token actual) {
        try {
            return (actual.getType() == type) &&
                    (Objects.equals(actual.getText(), text)) &&
                    (channel == null || actual.getChannel() == channel);
        } catch (NumberFormatException r) {
            return false;
        }
    }

    @Override
    public void describeTo(Description desc) {
        var expectedType = Midi1FileLexer.VOCABULARY.getDisplayName(type);

        if (channel != null) {
            var channelText = Midi1FileLexer.channelNames[channel];
            desc.appendText(String.format("Expected %s (${text.charHexCodesAsString()}) on %s"
                    , expectedType, channelText));
        } else {
            desc.appendText(String.format("Expected %s (${text.charHexCodesAsString()})"
                    , expectedType));
        }
    }

    @Override
    public void describeMismatchSafely(Token item, Description desc) {
        var actualType = Midi1FileLexer.VOCABULARY.getDisplayName(item.getType());

        if (channel != null) {
            var channelText = Midi1FileLexer.channelNames[item.getChannel()];
            desc.appendText(String.format("Got %s (${item.text.charHexCodesAsString()}) on %s at position %d"
                    , actualType, channelText, item.getStartIndex()));
        } else {
            desc.appendText(String.format("Got %s (${item.text.charHexCodesAsString()}) at position %d"
                    , actualType, item.getStartIndex()));
        }
    }
}
