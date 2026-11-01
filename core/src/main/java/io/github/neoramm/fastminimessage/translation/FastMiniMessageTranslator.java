package io.github.neoramm.fastminimessage.translation;

import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.TagResolver;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.VirtualComponent;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public abstract class FastMiniMessageTranslator implements Translator {
    protected abstract @Nullable PreComponentTree getTranslation(final @NotNull String key,
        final @NotNull Locale locale);

    @Override
    public final @Nullable MessageFormat translate(final @NotNull String key, final @NotNull Locale locale) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(locale, "locale");
        return null;
    }

    @Override
    public final @Nullable Component translate(final @NotNull TranslatableComponent component,
        final @NotNull Locale locale) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(locale, "locale");
        final @Nullable PreComponentTree tree = getTranslation(component.key(), locale);
        if (tree == null) return null;
        final @NotNull Component translated = render(tree, component.arguments(), locale);
        return translated.applyFallbackStyle(component.style()).append(component.children());
    }

    private static @NotNull Component render(final @NotNull PreComponentTree tree,
        final @NotNull List<@NotNull TranslationArgument> arguments, final @NotNull Locale locale) {
        @NotNull Pointered target = new LocaleTarget(locale);
        if (hasOnlyIndexedArguments(arguments)) return renderIndexed(tree, arguments, target);
        if (arguments.isEmpty()) return tree.render(target);
        final @NotNull List<@NotNull Component> indexed = new ArrayList<>(arguments.size());
        final @NotNull List<@NotNull TagResolver> resolvers = new ArrayList<>(arguments.size() + 2);
        boolean targetOverridden = false;
        for (final @NotNull TranslationArgument argument : arguments) {
            if (argument.value() instanceof final VirtualComponent virtual) {
                if (virtual.renderer() instanceof final Argument.Target override) {
                    if (targetOverridden) throw new IllegalArgumentException("Multiple translation targets");
                    target = override.value();
                    targetOverridden = true;
                    continue;
                }
                if (virtual.renderer() instanceof final Argument.Resolvers custom) {
                    resolvers.addAll(custom.values());
                    continue;
                }
                if (virtual.renderer() instanceof final Argument.Named named) {
                    resolvers.add(TagResolver.inserting(named.name(), named.value()));
                    indexed.add(named.value());
                    continue;
                }
            }
            indexed.add(argument.asComponent());
        }
        final @NotNull TagResolver.DynamicTag indexedResolver =
            (final @NotNull Arguments args, final @Nullable Pointered viewer) -> resolveArgument(args, indexed);
        resolvers.addFirst(TagResolver.dynamic("argument", indexedResolver));
        resolvers.addFirst(TagResolver.dynamic("arg", indexedResolver));
        return tree.render(target, RenderScope.of(resolvers));
    }

    private static boolean hasOnlyIndexedArguments(final @NotNull List<@NotNull TranslationArgument> arguments) {
        if (arguments.isEmpty()) return false;
        for (final TranslationArgument argument : arguments) {
            if (argument.value() instanceof VirtualComponent) return false;
        }
        return true;
    }

    private static @NotNull Component renderIndexed(final @NotNull PreComponentTree tree,
        final @NotNull List<@NotNull TranslationArgument> arguments, final @NotNull Pointered target) {
        final @NotNull TagResolver.DynamicTag resolver = (final var args, final var viewer) -> {
            final int index = args.size() == 1 ? args.getInt(0, -1) : -1;
            return index >= 0 && index < arguments.size() ? arguments.get(index).asComponent() : null;
        };
        return tree.render(target, RenderScope.of(TagResolver.dynamic("arg", resolver),
            TagResolver.dynamic("argument", resolver)));
    }

    private static @Nullable Component resolveArgument(final @NotNull Arguments args,
        final @NotNull List<@NotNull Component> indexed) {
        if (args.size() != 1) return null;
        final int index = args.getInt(0, -1);
        return index >= 0 && index < indexed.size() ? indexed.get(index) : null;
    }

    private static final class LocaleTarget implements Pointered {
        private final @NotNull Locale locale;
        private volatile @Nullable Pointers pointers;

        private LocaleTarget(final @NotNull Locale locale) {
            this.locale = locale;
        }

        @Override
        public <T> @NotNull Optional<T> get(final @NotNull Pointer<T> pointer) {
            Objects.requireNonNull(pointer, "pointer");
            return Identity.LOCALE.equals(pointer) ? Optional.of(pointer.type().cast(locale)) : Optional.empty();
        }

        @Override
        public @NotNull Pointers pointers() {
            final @Nullable Pointers cached = pointers;
            if (cached != null) return cached;
            final @NotNull Pointers built = Pointers.builder().withStatic(Identity.LOCALE, locale).build();
            pointers = built;
            return built;
        }
    }
}
