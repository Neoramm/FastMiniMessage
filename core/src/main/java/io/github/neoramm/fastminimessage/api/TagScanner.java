package io.github.neoramm.fastminimessage.api;

import io.github.neoramm.fastminimessage.model.TagNode;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Splits a MiniMessage string into a tree of tag nodes.
 */
public interface TagScanner {

    /**
      * Scans the input and returns its root tag nodes.
      *
      * @param input the MiniMessage string to scan
      * @return the root nodes of the scanned tree
      */
    @NotNull List<@NotNull TagNode> scan(final @NotNull String input);
}