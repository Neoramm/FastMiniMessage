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
}
