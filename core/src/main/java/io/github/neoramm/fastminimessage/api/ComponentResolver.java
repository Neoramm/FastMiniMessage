package io.github.neoramm.fastminimessage.api;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a MiniMessage-style tag into a precompiled component.
 */
public interface ComponentResolver {

    /**
      * Builds the component produced by this tag.
      *
      * @param name the normalized tag name
      * @param args the parsed tag arguments
      * @param children the already-resolved child components
      * @param inheritedStyle the style inherited from enclosing tags
      * @param compiler the compiler driving the resolution
      * @return the resolved component
      */
    @NotNull PreComponent resolve(
        final @NotNull String name,
        final @NotNull Arguments args,
        final PreComponent @NotNull [] children,
        final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler
    );

    /**
      * Returns every tag name handled by this resolver.
      */
    @NotNull String @NotNull [] names();

    /**
      * Relative priority used when two resolvers claim the same name.
      */
    default int priority() {
        return 0;
    }

    /**
      * Whether the tag is self-closing and takes no children.
      */
    default boolean isSelfClosing() {
        return false;
    }

    /**
     * Returns whether the argument at {@code index} is a sub-component (a nested MiniMessage string)
     * rather than a plain scalar value.
     *
     * <p>The compiler uses this to skip placeholder substitution on that argument slot, preserving
     * any {@code <tag>} syntax it contains instead of flattening it to plain text.</p>
     *
     * <p>The default implementation returns {@code false} for every index.</p>
     *
     * @param index the zero-based argument index
     * @param args  the full argument list for the tag invocation
     * @return {@code true} when the argument at {@code index} should be treated as a MiniMessage sub-component
     */
    default boolean isComponentArgument(int index, @NotNull Arguments args) {
        return false;
    }
}