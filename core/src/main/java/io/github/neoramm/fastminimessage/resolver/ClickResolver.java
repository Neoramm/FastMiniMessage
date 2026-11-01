package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ClickPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ClickResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"click"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String actionName = args.nextOr("open_url");
        final @Nullable ClickEvent<?> event = resolveClickEvent(compiler, actionName, args);
        if (event == null) throw new IllegalArgumentException("A click event value is required");
        if (children.length == 0) return new LiteralPreComponent("", inheritedStyle);
        final Style own = Style.style().clickEvent(event).build();
        final @Nullable LiteralPreComponent merged =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (merged != null) return merged;
        return new ClickPreComponent<>(event, children).setStyle(inheritedStyle);
    }

    private @Nullable ClickEvent<?> resolveClickEvent(final @NotNull MiniMessageCompiler compiler,
        final @NotNull String actionName, final @NotNull Arguments args) {
        final @Subst("key") String rawValue = args.hasNext() ? args.next() : null;
        if (rawValue == null) return null;
        return switch (actionName.toLowerCase(Locale.ROOT)) {
            case "run_command" -> ClickEvent.runCommand(rawValue);
            case "suggest_command" -> ClickEvent.suggestCommand(rawValue);
            case "open_file" -> ClickEvent.openFile(rawValue);
            case "copy_to_clipboard" -> ClickEvent.copyToClipboard(rawValue);
            case "change_page" -> ClickEvent.changePage(parseInt(rawValue));
            case "custom" -> {
                final @Nullable String nbt = args.hasNext() ? args.next() : null;
                yield nbt != null && !nbt.isEmpty()
                    ? ClickEvent.custom(Key.key(rawValue), BinaryTagHolder.binaryTagHolder(nbt))
                    : ClickEvent.custom(Key.key(rawValue));
            }
            case "show_dialog" -> showDialog(compiler, rawValue);
            case "open_url" -> ClickEvent.openUrl(rawValue);
            default -> throw new IllegalArgumentException("Unknown click event action '" + actionName + "'");
        };
    }

    private @NotNull ClickEvent<?> showDialog(final @NotNull MiniMessageCompiler compiler,
        final @NotNull String reference) {
        final @Nullable DialogLike dialog = compiler.resolveDialog(reference);
        if (dialog == null) throw new UnsupportedOperationException("No dialog resolver for " + reference);
        return ClickEvent.showDialog(dialog);
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
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
