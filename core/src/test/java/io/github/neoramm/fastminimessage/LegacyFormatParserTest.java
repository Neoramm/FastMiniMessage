package io.github.neoramm.fastminimessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LegacyFormatParserTest {

    @Test
    void hex() {
        assertEquals("<reset><#ff0000>", LegacyFormatParser.toMiniMessage("&#ff0000"));
        assertEquals("<reset><#FF0000>", LegacyFormatParser.toMiniMessage("&x&F&F&0&0&0&0"));
    }

    @Test
    void basic() {
        assertEquals("<reset><red>Red <bold>Bold <reset>Reset",
            LegacyFormatParser.toMiniMessage("&cRed &lBold &rReset").trim());
    }

    @Test
    void ampersandInsideTagsNotReplaced() {
        final String input = "<click:open_url:'https://example.com/api?a=1&b=2'>Click</click>";
        assertEquals(input, LegacyFormatParser.toMiniMessage(input));

        final String hover = "<hover:show_text:'Fish & Chips'>Food</hover>";
        assertEquals(hover, LegacyFormatParser.toMiniMessage(hover));
    }

    @Test
    void escapedAmpersandPreserved() {
        assertEquals("&cNotColor", LegacyFormatParser.toMiniMessage("\\&cNotColor"));
    }

    @Test
    void mathLessThanDoesNotPreventLaterColorCodes() {
        assertEquals("3 < 5 <reset><green>hello", LegacyFormatParser.toMiniMessage("3 < 5 &ahello"));
        assertEquals("\\<3 <reset><green>love", LegacyFormatParser.toMiniMessage("\\<3 &alove"));
        assertEquals("<hover:show_text:'Fish & Chips'><reset><red>Text</hover>",
            LegacyFormatParser.toMiniMessage("<hover:show_text:'Fish & Chips'>&cText</hover>"));
    }
}
