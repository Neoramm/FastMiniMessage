package io.github.neoramm.fastminimessage.internal;

import io.github.neoramm.fastminimessage.model.Arguments;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class TagArgumentTypes {
    private TagArgumentTypes() {
    }

    public static boolean isComponent(final @NotNull String name, final @NotNull Arguments arguments,
        final int index) {
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
