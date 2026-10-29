package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Resolves dynamic tags at render time, supporting insertion, modification and custom logic.
 */
public interface TagResolver extends RuntimeResolver {

    @NotNull String name();

    default boolean isModifying() {
        return false;
    }

    default @Nullable PreComponent preComponent() {
        return null;
    }

    default @Nullable ComponentModifier modifier() {
        return null;
    }

    default @Nullable DynamicTag dynamicTag() {
        return null;
    }

    default @Nullable RuntimeTagResolver customResolver() {
        return null;
    }

    static @NotNull TagResolver inserting(final @NotNull String name, final @NotNull Component component) {
        return Placeholder.ofParsed(name, component);
    }

    static @NotNull TagResolver inserting(final @NotNull String name, final @NotNull PreComponent component) {
        return new Placeholder(name, component);
    }

    static @NotNull TagResolver inserting(final @NotNull String name, final @NotNull String text) {
        return Placeholder.ofUnparsed(name, text);
    }

    static @NotNull TagResolver modifying(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        return new ModifierResolver(name, modifier);
    }

    static @NotNull TagResolver resolver(final @NotNull String name, final @NotNull Component component) {
        return inserting(name, component);
    }

    static @NotNull TagResolver resolver(final @NotNull String name, final @NotNull String text) {
        return inserting(name, text);
    }

    static @NotNull TagResolver resolver(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        return modifying(name, modifier);
    }

    static @NotNull TagResolver dynamic(final @NotNull String name, final @NotNull DynamicTag dynamicTag) {
        return new DynamicTagResolver(name, dynamicTag);
    }

    static @NotNull TagResolver dynamic(final @NotNull String name,
        final @NotNull Function<Arguments, Component> function) {
        return new DynamicTagResolver(name,
            (final @NotNull Arguments args, final @Nullable Pointered context) -> function.apply(args));
    }

    static @NotNull TagResolver custom(final @NotNull RuntimeTagResolver resolver) {
        return new Custom("", resolver);
    }

    static @NotNull TagResolver custom(final @NotNull String name, final @NotNull RuntimeTagResolver resolver) {
        return new Custom(name, resolver);
    }

    /**
     * Resolves dynamic tag content using the parsed arguments and viewer context.
     */
    @FunctionalInterface
    interface DynamicTag {
        @Nullable Component resolve(final @NotNull Arguments args, final @Nullable Pointered context);
    }

    record DynamicTagResolver(@NotNull String name, @NotNull DynamicTag dynamicTag) implements TagResolver {
        @Override
        public @NotNull DynamicTag dynamicTag() {
            return dynamicTag;
        }
    }

    record Custom(@NotNull String name, @NotNull RuntimeTagResolver resolver) implements TagResolver {
        @Override
        public @NotNull RuntimeTagResolver customResolver() {
            return resolver;
        }
    }
}
