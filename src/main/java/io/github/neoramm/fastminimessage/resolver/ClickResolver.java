package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ClickPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ClickResolver implements ComponentResolver {
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String actionName = args.get(0, "open_url");
        final String rawValue = args.get(1, "");
        final ClickEvent event = resolveClickEvent(actionName, rawValue);
        if (children.length == 0) return new LiteralPreComponent("", inheritedStyle);
        final Style own = Style.style().clickEvent(event).build();
        final @Nullable LiteralPreComponent merged =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (merged != null) return merged;
        return new ClickPreComponent(event, children);
    }

    private @NotNull ClickEvent resolveClickEvent(final @NotNull String actionName, final @NotNull String rawValue) {
        return switch (actionName.toLowerCase(Locale.ROOT)) {
            case "run_command" -> ClickEvent.runCommand(rawValue);
            case "suggest_command" -> ClickEvent.suggestCommand(rawValue);
            case "open_file" -> ClickEvent.openFile(rawValue);
            case "copy_to_clipboard" -> ClickEvent.copyToClipboard(rawValue);
            case "change_page" -> ClickEvent.changePage(parseInt(rawValue));
            default -> ClickEvent.openUrl(rawValue);
        };
    }

    private int parseInt(final @Nullable String raw) {
        if (raw == null) return 0;
        try {
            return Integer.parseInt(raw);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean isSelfClosing() {
        return false;
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"click"};
    }
}
