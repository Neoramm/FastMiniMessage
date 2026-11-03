package io.github.neoramm.fastminimessage.v2.template;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lazily-built literal representation of a tag, used only when a tag cannot be resolved and must
 * fall back to its raw source.
 */
public final class TagSource {
    private final @NotNull String tagName;
    private final @NotNull String rawArgs;
    private final boolean hasArguments;
    private final boolean selfClosing;
    private final boolean explicitlyClosed;
    private final @NotNull MiniMessageCompiler compiler;
    private volatile @Nullable Component opening;
    private volatile @Nullable Component closing;

    public TagSource(final @NotNull String tagName, final @NotNull String rawArgs, final boolean hasArguments,
        final boolean selfClosing, final boolean explicitlyClosed, final @NotNull MiniMessageCompiler compiler) {
        this.tagName = Objects.requireNonNull(tagName, "tagName");
        this.rawArgs = Objects.requireNonNull(rawArgs, "rawArgs");
        this.hasArguments = hasArguments;
        this.selfClosing = selfClosing;
        this.explicitlyClosed = explicitlyClosed;
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public @NotNull String rawArgs() {
        return rawArgs;
    }

    public boolean hasArguments() {
        return hasArguments;
    }

    public boolean selfClosing() {
        return selfClosing;
    }

    public boolean explicitlyClosed() {
        return explicitlyClosed;
    }

    public @NotNull MiniMessageCompiler compiler() {
        return compiler;
    }

    public @NotNull Component opening() {
        final @Nullable Component existing = opening;
        if (existing != null) return existing;
        final @NotNull String args = hasArguments ? ":" + rawArgs : "";
        final @NotNull Component built = Component.text("<" + tagName + args + (selfClosing ? "/>" : ">"));
        opening = built;
        return built;
    }

    public @NotNull Component closing() {
        final @Nullable Component existing = closing;
        if (existing != null) return existing;
        final @NotNull Component built = explicitlyClosed && !selfClosing
            ? Component.text("</" + tagName + ">") : Component.empty();
        closing = built;
        return built;
    }
}
