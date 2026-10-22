package io.github.neoramm.fastminimessage.model;

import java.util.List;

public record TagNode(
    String tagName,
    String rawArgs,
    String textContent,
    List<TagNode> children,
    boolean isClosingTag,
    boolean isSelfClosing,
    boolean explicitlyClosed
) {}