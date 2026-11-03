package io.github.neoramm.fastminimessage.v2.paper;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

public final class PaperTags {

    private PaperTags() {
    }

    public static @NotNull ResolverRegistry withDefaults() {
        return withDefaults(PaperTagOptions.defaults());
    }

    public static @NotNull ResolverRegistry withDefaults(final @NotNull PaperTagOptions options) {
        Objects.requireNonNull(options, "options");
        return apply(ResolverRegistry.withDefaults(), options);
    }

    public static @NotNull ResolverRegistry apply(final @NotNull ResolverRegistry registry,
        final @NotNull PaperTagOptions options) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(options, "options");
        registry.register(new TagSpec.LeafRule(Set.of("papi"), (final var args, final var context) -> {
            args.require(1);
            final var values = args.values();
            if (values.isConstant()) {
                final @NotNull String expr = values.evaluate(RenderScope.EMPTY).string(0, "");
                if (expr.isEmpty()) throw new IllegalArgumentException("papi requires an expression");
            }
            return new ValueSource.Computed<>((final var render) -> {
                final @NotNull String expression = values.evaluate(render).string(0, "");
                if (expression.isEmpty()) throw new IllegalArgumentException("papi requires an expression");
                return Component.text(options.placeholders().render(render.viewer(), expression));
            });
        }));
        registry.register(new TagSpec.StyleRule(Set.of("item_hover"), (final var args, final var context) ->
            args.map((final var values) -> Style.style().hoverEvent(options.itemHovers()
                .create(values.string(0, ""), values.arguments().getInt(1, 1))).build())));
        registry.register(new TagSpec.StyleRule(Set.of("show_dialog"), (final var args, final var context) -> {
            args.require(1);
            args.component(0).component(1).component(2);
            return args.map((final var values) -> {
                final @NotNull Component title = values.component(0);
                final @NotNull Component button = values.size() < 3
                    ? Component.text("OK") : values.component(2);
                final @NotNull InlineDialog dialog = new InlineDialog(title,
                    values.component(1).equals(Component.empty()) ? null : values.component(1), button);
                return Style.style().clickEvent(ClickEvent.showDialog(options.inlineDialogs().create(dialog))).build();
            });
        }));
        return registry;
    }

    public static @NotNull MiniMessageCompiler compiler() {
        return compiler(PaperTagOptions.defaults());
    }

    public static @NotNull MiniMessageCompiler compiler(final @NotNull PaperTagOptions options) {
        return compiler(withDefaults(options), options);
    }

    public static @NotNull MiniMessageCompiler compiler(final @NotNull ResolverRegistry registry,
        final @NotNull PaperTagOptions options) {
        return new MiniMessageCompiler(registry).dialogResolver(options.registryDialogs());
    }
}
