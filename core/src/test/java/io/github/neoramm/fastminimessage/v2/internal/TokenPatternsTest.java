package io.github.neoramm.fastminimessage.v2.internal;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TokenPatternsTest {
    @ParameterizedTest
    @ValueSource(strings = {"<red>", "<hover:show_text:'<red>tip</red>'>", "<click:run_command:'say > hi'>"})
    void recognizesQuotedTagBoundaries(final @NotNull String value) {
        assertEquals(value.length() - 1, TokenPatterns.tagEnd(value + "tail", 0));
        assertEquals(value.length(), TokenPatterns.tagEnd("x" + value + "tail", 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<hover:'broken <red>", "<missing", "<a<b>"})
    void malformedTokensRequireRecovery(final @NotNull String value) {
        assertEquals(-1, TokenPatterns.tagEnd(value, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"red", "!bold", "#ff0000", "PLAYER", "my-tag", "_", "player.name", "a.b"})
    void acceptsTagNames(final @NotNull String value) {
        assertTrue(TokenPatterns.isTagName(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "!", "#", "?", "a<b", "a b", "a:b", "red\n", "é"})
    void rejectsInvalidTagNames(final @NotNull String value) {
        assertFalse(TokenPatterns.isTagName(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"player.name", "theme-color", "arg", "PLAYER"})
    void acceptsScalarNames(final @NotNull String value) {
        assertTrue(TokenPatterns.isScalarKey(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"H4sIAAAAAAAAAAAA", "H4sIAAAAAAAAAAAA==", "STONE", "H4sI====AAAA===="})
    void validatesSerializedItemShape(final @NotNull String value) {
        if ("H4sIAAAAAAAAAAAA".equals(value)) assertTrue(TokenPatterns.isSerializedItem(value));
        else assertFalse(TokenPatterns.isSerializedItem(value));
    }
}
