package io.github.neoramm.fastminimessage.v2.paper;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PaperItemsValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {"unknown", "UNKNOWNMATERIAL", "", "H4sI========AAAA"})
    void invalidNamesAreRejectedBeforeItemDeserialization(final @NotNull String reference) {
        final var failure = assertThrows(IllegalArgumentException.class, () -> PaperItems.hover(reference, 1));
        assertNull(failure.getCause());
    }
}
