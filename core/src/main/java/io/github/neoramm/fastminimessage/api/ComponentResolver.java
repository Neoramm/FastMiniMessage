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
    PreComponent resolve(
        final @NotNull String name,
        final @NotNull Arguments args,
        final @NotNull PreComponent[] children,
        final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler
    );

    /**
      * Returns every tag name handled by this resolver.
      */
    @NotNull String[] names();

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
}