package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.translation.Argument;
import io.github.neoramm.fastminimessage.translation.FastMiniMessageTranslationStore;

import io.github.neoramm.fastminimessage.model.TagResolver;
import io.github.neoramm.fastminimessage.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.translation.FastMiniMessageTranslator;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalizationTest {
    private final @NotNull FastMiniMessageTranslationStore store =
        FastMiniMessageTranslationStore.create(Key.key("test:localization"));

    @Test
    void matchesMiniMessageForIndexedArgumentsAndStyles() {
        final @NotNull String template = "<green>Hello <arg:0>! <argument:1> / <arg:2></green>";
        store.register("hello", Locale.US, template);
        final @NotNull MiniMessageTranslationStore reference = MiniMessageTranslationStore.create(Key.key("test:ref"));
        reference.register("hello", Locale.US, template);
        final @NotNull TranslatableComponent input = Component.translatable("hello")
            .arguments(Component.text("Alex", NamedTextColor.RED), TranslationArgument.numeric(42),
                TranslationArgument.bool(true))
            .decorate(TextDecoration.BOLD).append(Component.text(" child", NamedTextColor.RED));
        assertEquals(ComponentComparison.expression(Objects.requireNonNull(reference.translate(input, Locale.US))),
            ComponentComparison.expression(Objects.requireNonNull(store.translate(input, Locale.US))));
    }

    @Test
    void insertsNamedArgumentsAsLiteralComponents() {
        store.register("hello", Locale.US, "<name> <arg:0> <amount> <enabled>");
        final @NotNull Component input = Component.translatable("hello", Argument.string("name", "<red>Alex"),
            Argument.numeric("amount", 3), Argument.bool("enabled", true));
        assertEquals("<red>Alex <red>Alex 3 true", plain(render(input, Locale.US)));
    }

    @Test
    void resolvesExactLanguageAndDefaultLocales() {
        store.registerAll(Locale.US, Map.of("hello", "default"));
        store.register("hello", Locale.FRENCH, "langue");
        store.register("hello", Locale.CANADA_FRENCH, "exact");
        assertEquals("exact", plain(render(Component.translatable("hello"), Locale.CANADA_FRENCH)));
        assertEquals("langue", plain(render(Component.translatable("hello"), Locale.FRANCE)));
        assertEquals("default", plain(render(Component.translatable("hello"), Locale.JAPAN)));
        assertFalse(store.contains("hello", Locale.FRANCE));
        assertTrue(store.canTranslate("hello", Locale.FRANCE));
        store.defaultLocale(Locale.FRENCH);
        assertEquals("langue", plain(render(Component.translatable("hello"), Locale.JAPAN)));
    }

    @Test
    void preservesUnknownTranslationsForOtherSourcesAndClientFallback() {
        assertNull(store.translate(Component.translatable("missing"), Locale.US));
        assertNull(store.translate("missing", Locale.US));
        assertFalse(store.canTranslate("missing", Locale.US));
        assertEquals("fallback", plain(render(Component.translatable("missing").fallback("fallback"), Locale.US)));
    }

    @Test
    void rendersNestedTranslationsAndChildrenThroughGlobalTranslator() {
        store.register("hello", Locale.US, "Hello <arg:0>");
        store.register("name", Locale.US, "Alex");
        assertTrue(GlobalTranslator.translator().addSource(store));
        try {
            final @NotNull Component input = Component.translatable("hello", Component.translatable("name"))
                .append(Component.translatable("name"));
            assertEquals("Hello AlexAlex", plain(GlobalTranslator.render(input, Locale.US)));
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }

    @Test
    void loadsResourceBundlesWithoutChangingApostrophes() throws IOException {
        final @NotNull PropertyResourceBundle bundle =
            new PropertyResourceBundle(new StringReader("hello=<green>It's <arg:0>!"));
        store.registerAll(Locale.US, bundle, false);
        assertEquals("It's Alex!", plain(render(Component.translatable("hello", Component.text("Alex")), Locale.US)));
    }

    @Test
    void loadsUtf8FilesAndReportsMissingFiles(final @TempDir @NotNull Path directory) throws IOException {
        final @NotNull Path path = directory.resolve("messages.properties");
        Files.writeString(path, "hello=¡Hola!");
        store.registerAll(Locale.US, path, false);
        assertEquals("¡Hola!", plain(render(Component.translatable("hello"), Locale.US)));
        assertThrows(UncheckedIOException.class,
            () -> store.registerAll(Locale.US, directory.resolve("missing.properties"), false));
    }

    @Test
    void rejectsDuplicatesAndReleasesUnregisteredTranslations() {
        assertEquals(TriState.FALSE, store.hasAnyTranslations());
        store.register("hello", Locale.US, "first");
        assertEquals(TriState.TRUE, store.hasAnyTranslations());
        assertThrows(IllegalArgumentException.class, () -> store.register("hello", Locale.US, "second"));
        store.unregister("hello");
        assertFalse(store.contains("hello"));
        store.register("hello", Locale.US, "replacement");
        assertEquals("replacement", plain(render(Component.translatable("hello"), Locale.US)));
    }

    @Test
    void exposesLocaleToCustomResolversWithoutConsumingArgumentIndices() {
        store.register("hello", Locale.US, "<locale> <arg:0>");
        final @NotNull TagResolver resolver = TagResolver.dynamic("locale", (final var args, final var target) ->
            Component.text(Objects.requireNonNull(target).getOrDefault(Identity.LOCALE, Locale.ROOT).toLanguageTag()));
        final @NotNull Component input = Component.translatable("hello", Argument.tagResolver(resolver),
            Component.text("Alex"));
        assertEquals("fr-FR Alex", plain(render(input, Locale.FRANCE)));
    }

    @Test
    void supportsTargetOverridesAndRejectsMultipleTargets() {
        store.register("hello", Locale.US, "<player> <arg:0>");
        final @NotNull Pointered target = new TestTarget(
            Pointers.builder().withStatic(Identity.NAME, "Alex").build());
        assertEquals("Alex !", plain(render(Component.translatable("hello", Argument.target(target),
            Component.text("!")), Locale.US)));
        assertThrows(IllegalArgumentException.class, () -> store.translate(Component.translatable("hello",
            Argument.target(target), Argument.target(target)), Locale.US));
    }

    @Test
    void keepsInvalidArgumentTagsLiteral() {
        store.register("hello", Locale.US, "<arg:-1> <arg:9> <arg:nope> <arg:0:extra>");
        assertEquals("<arg:-1> <arg:9> <arg:nope> <arg:0:extra>",
            plain(render(Component.translatable("hello", Component.text("Alex")), Locale.US)));
    }

    @Test
    void supportsEmptyTranslationsAndRejectsNullInputs() {
        store.register("hello", Locale.US, "");
        assertNotNull(store.translate(Component.translatable("hello"), Locale.US));
        assertEquals("", plain(render(Component.translatable("hello"), Locale.US)));
        assertThrows(NullPointerException.class, () -> store.register("null", Locale.US, null));
        assertThrows(NullPointerException.class, () -> store.translate(Component.translatable("hello"), null));
        assertThrows(IllegalArgumentException.class, () -> Argument.string("bad:name", "value"));
    }

    @Test
    void compilesAtRegistrationAndKeepsRenderArgumentsIsolated() {
        final @NotNull AtomicInteger scans = new AtomicInteger();
        final @NotNull DefaultTagScanner scanner = new DefaultTagScanner();
        final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(),
            (final @NotNull String source) -> {
                scans.incrementAndGet();
                return scanner.scan(source);
            });
        final @NotNull FastMiniMessageTranslationStore compiled =
            FastMiniMessageTranslationStore.create(Key.key("test:compiled"), compiler);
        compiled.register("hello", Locale.US, "Hello <arg:0>");
        final int initialScans = scans.get();
        assertTrue(initialScans > 0);
        for (int i = 0; i < 10; i++) {
            final @NotNull Component result = Objects.requireNonNull(compiled.translate(
                Component.translatable("hello", Component.text(i)), Locale.US));
            assertEquals("Hello " + i, plain(result));
        }
        assertEquals(initialScans, scans.get());
    }

    @Test
    void customRuntimeModifiersApplyToTranslationContent() {
        store.register("hello", Locale.US, "<tint>Hello <arg:0></tint>");
        final @NotNull TagResolver tint = TagResolver.modifying("tint",
            (final var component, final var target) -> component.color(NamedTextColor.RED));
        final @NotNull Component actual = render(Component.translatable("hello", Component.text("Alex"),
            Argument.tagResolver(tint)), Locale.US);
        assertEquals(ComponentComparison.expression(Component.text("Hello Alex", NamedTextColor.RED)),
            ComponentComparison.expression(actual));
    }

    @Test
    void preservesOriginalStyleAndChildren() {
        store.register("hello", Locale.US, "Hello");
        final @NotNull Component child = Component.text(" child", NamedTextColor.RED);
        final @NotNull TranslatableComponent input = Component.translatable("hello")
            .color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD).append(child);
        final @NotNull Component result = Objects.requireNonNull(store.translate(input, Locale.US));
        assertEquals(input.style(), result.style());
        assertEquals(child, result.children().getLast());
    }

    @Test
    void supportsCustomPrecompiledTranslationBackends() {
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(ResolverRegistry.withDefaults()).compile("Hello <arg:0>");
        final @NotNull FastMiniMessageTranslator translator = new FastMiniMessageTranslator() {
            @Override
            public @NotNull Key name() {
                return Key.key("test:custom");
            }

            @Override
            protected @Nullable PreComponentTree getTranslation(final @NotNull String key,
                final @NotNull Locale locale) {
                return "hello".equals(key) && Locale.US.equals(locale) ? tree : null;
            }
        };
        assertEquals("Hello Alex", plain(Objects.requireNonNull(translator.translate(
            Component.translatable("hello", Component.text("Alex")), Locale.US))));
        assertNull(translator.translate(Component.translatable("hello"), Locale.FRANCE));
    }

    private @NotNull Component render(final @NotNull Component component, final @NotNull Locale locale) {
        return TranslatableComponentRenderer.usingTranslationSource(store).render(component, locale);
    }

    private static @NotNull String plain(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private record TestTarget(@NotNull Pointers pointers) implements Pointered {
    }
}
