package io.github.neoramm.fastminimessage.translation;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.AbstractTranslationStore;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

public final class FastMiniMessageTranslationStore extends FastMiniMessageTranslator
    implements TranslationStore.StringBased<@NotNull String> {
    private final @NotNull MiniMessageCompiler compiler;
    private final @NotNull CompiledStore translations;

    private FastMiniMessageTranslationStore(final @NotNull Key name, final @NotNull MiniMessageCompiler compiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.translations = new CompiledStore(Objects.requireNonNull(name, "name"));
    }

    public static @NotNull FastMiniMessageTranslationStore create(final @NotNull Key name) {
        return create(name, new MiniMessageCompiler(ResolverRegistry.withDefaults()));
    }

    public static @NotNull FastMiniMessageTranslationStore create(final @NotNull Key name,
        final @NotNull MiniMessageCompiler compiler) {
        return new FastMiniMessageTranslationStore(name, compiler);
    }

    @Override
    public @NotNull Key name() {
        return translations.name();
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        return translations.hasAnyTranslations();
    }

    @Override
    public boolean contains(final @NotNull String key) {
        return translations.contains(Objects.requireNonNull(key, "key"));
    }

    @Override
    public boolean contains(final @NotNull String key, final @NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return translations.contains(Objects.requireNonNull(key, "key"), locale);
    }

    @Override
    public boolean canTranslate(final @NotNull String key, final @NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return translations.canTranslate(Objects.requireNonNull(key, "key"), locale);
    }

    @Override
    public void defaultLocale(final @NotNull Locale locale) {
        translations.defaultLocale(Objects.requireNonNull(locale, "locale"));
    }

    @Override
    public void register(final @NotNull String key, final @NotNull Locale locale, final @NotNull String translation) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(translation, "translation");
        translations.register(key, locale, compiler.compile(translation));
    }

    @Override
    public void registerAll(final @NotNull Locale locale,
        final @NotNull Map<@NotNull String, @NotNull String> translations) {
        Objects.requireNonNull(translations, "translations");
        registerAll(locale, translations.keySet(), translations::get);
    }

    @Override
    public void registerAll(final @NotNull Locale locale, final @NotNull Set<@NotNull String> keys,
        final @NotNull Function<@NotNull String, @NotNull String> function) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(function, "function");
        translations.registerAll(locale, keys, (final @NotNull String key) ->
            compiler.compile(Objects.requireNonNull(function.apply(key), "translation")));
    }

    @Override
    public void registerAll(final @NotNull Locale locale, final @NotNull ResourceBundle bundle,
        final boolean escapeSingleQuotes) {
        Objects.requireNonNull(bundle, "bundle");
        registerAll(locale, bundle.keySet(), (final @NotNull String key) -> {
            final @NotNull String value = bundle.getString(key);
            return escapeSingleQuotes ? value.replace("'", "''") : value;
        });
    }

    @Override
    public void registerAll(final @NotNull Locale locale, final @NotNull Path path, final boolean escapeSingleQuotes) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(path, "path");
        try (final @NotNull BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            registerAll(locale, new PropertyResourceBundle(reader), escapeSingleQuotes);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Cannot load translations from " + path, exception);
        }
    }

    @Override
    public void unregister(final @NotNull String key) {
        translations.unregister(Objects.requireNonNull(key, "key"));
    }

    @Override
    protected @Nullable PreComponentTree getTranslation(final @NotNull String key, final @NotNull Locale locale) {
        return translations.translationValue(key, locale);
    }

    private static final class CompiledStore extends AbstractTranslationStore<@NotNull PreComponentTree> {
        private CompiledStore(final @NotNull Key name) {
            super(name);
        }

        @Override
        protected @Nullable PreComponentTree translationValue(final @NotNull String key, final @NotNull Locale locale) {
            return super.translationValue(key, locale);
        }

        @Override
        public @Nullable MessageFormat translate(final @NotNull String key, final @NotNull Locale locale) {
            return null;
        }
    }
}
