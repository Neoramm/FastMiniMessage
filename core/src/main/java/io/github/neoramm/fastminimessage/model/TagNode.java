package io.github.neoramm.fastminimessage.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record TagNode(
    @NotNull String tagName,
    @NotNull String rawArgs,
    @NotNull String textContent,
    @NotNull List<@NotNull TagNode> children,
    boolean isClosingTag,
    boolean isSelfClosing,
    boolean explicitlyClosed
) {}
