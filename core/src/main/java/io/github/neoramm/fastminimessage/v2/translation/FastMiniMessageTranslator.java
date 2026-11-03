package io.github.neoramm.fastminimessage.v2.translation;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.model.RuntimeTagBridge;
import io.github.neoramm.fastminimessage.v2.model.TagResolver;
import io.github.neoramm.fastminimessage.v2.model.TranslationPayload;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
        if (arguments.isEmpty() && tree.isStatic()) return tree.render();
        @NotNull Pointered target = new LocaleTarget(locale);
        if (hasOnlyIndexedArguments(arguments)) return renderIndexed(tree, arguments, target);
        if (arguments.isEmpty()) return tree.render(tree.scopeBuilder().viewer(target).build());
        final @NotNull List<@NotNull TranslationArgument> indexed = new ArrayList<>(arguments.size());
        final @NotNull List<@NotNull RuntimeTagBridge> resolvers = new ArrayList<>(arguments.size());
        final @NotNull List<Argument.@NotNull Payload<?>> payloads = new ArrayList<>();
        boolean targetOverridden = false;
        for (final @NotNull TranslationArgument argument : arguments) {
            if (argument.value() instanceof final VirtualComponent virtual) {
                if (virtual.renderer() instanceof final Argument.Target override) {
                    if (targetOverridden) throw new IllegalArgumentException("Multiple translation targets");
                    target = override.value();
                    targetOverridden = true;
                    continue;
                }
                if (virtual.renderer() instanceof final Argument.Payload<?> payload) {
                    if (payload.key() == PayloadSchema.RUNTIME_TAGS_KEY) {
                        resolvers.add(RuntimeTagBridge.class.cast(payload.value()));
                    } else payloads.add(payload);
                    continue;
                }
                if (virtual.renderer() instanceof final Argument.Named named) {
                    resolvers.add(TagResolver.inserting(named.name(), named.value()));
                    indexed.add(TranslationArgument.component(named.value()));
                    continue;
                }
            }
            indexed.add(argument);
        }
        final RenderScope.@NotNull Builder builder = tree.scopeBuilder().viewer(target)
            .payload(PayloadSchema.TRANSLATION_ARGUMENTS_KEY, new TranslationPayload(indexed))
            .payload(PayloadSchema.RUNTIME_TAGS_KEY, TagResolver.combine(resolvers));
        for (final Argument.@NotNull Payload<?> payload : payloads) payload.applyTo(builder);
        return tree.render(builder.build());
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
        return tree.render(tree.scopeBuilder().viewer(target)
            .payload(PayloadSchema.TRANSLATION_ARGUMENTS_KEY, new TranslationPayload(arguments)).build());
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
