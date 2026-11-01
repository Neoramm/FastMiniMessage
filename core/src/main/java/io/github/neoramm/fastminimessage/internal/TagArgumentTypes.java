package io.github.neoramm.fastminimessage.internal;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.model.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class TagArgumentTypes {
    private TagArgumentTypes() {
    }

    /**
     * Returns whether the argument at {@code index} is a sub-component for the given tag.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If a live {@link ComponentResolver} is provided it is asked first via
     *       {@link ComponentResolver#isComponentArgument(int, Arguments)}.</li>
     *   <li>Otherwise the built-in table covering all default MiniMessage tags is used as fallback.</li>
     * </ol>
     */
    public static boolean isComponent(final @Nullable ComponentResolver resolver, final @NotNull String name,
        final @NotNull Arguments arguments, final int index) {
        if (resolver != null) return resolver.isComponentArgument(index, arguments);
        return builtIn(name, arguments, index);
    }

    private static boolean builtIn(final @NotNull String name, final @NotNull Arguments arguments, final int index) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "lang", "translate", "tr" -> index >= 1;
            case "lang_or", "translate_or", "tr_or" -> index >= 2;
            case "selector", "sel" -> index == 1;
            case "hover" -> index == 1 && "show_text".equalsIgnoreCase(arguments.getFirst(""))
                || index == 3 && "show_entity".equalsIgnoreCase(arguments.getFirst(""));
            case "nbt", "data" -> index == 3 && !"interpret".equalsIgnoreCase(arguments.get(3))
                && !"true".equalsIgnoreCase(arguments.get(3));
            default -> false;
        };
    }
}
