package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.template.StringTemplate;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TagArgs {
    private static final @NotNull ValueSource<@NotNull Component> EMPTY_COMPONENT =
        new ValueSource.Constant<>(Component.empty());
    private static final @NotNull Component @NotNull [] EMPTY_COMPONENTS = new Component[0];
    private static final @NotNull ValueSource<@NotNull Values> EMPTY_VALUES =
        new ValueSource.Constant<>(new Values(Arguments.EMPTY, EMPTY_COMPONENTS));
    private final @NotNull Arguments raw;
    private final @NotNull MiniMessageCompiler compiler;
    private final @NotNull List<@Nullable ValueSource<@NotNull String>> scalars;
    private @Nullable List<@NotNull ValueSource<@NotNull Component>> components;

    public TagArgs(final @NotNull String input, final @NotNull MiniMessageCompiler compiler) {
        this.raw = Arguments.parse(Objects.requireNonNull(input, "input"));
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.scalars = raw.isEmpty() ? List.of() : new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) scalars.add(null);
    }

    public int size() {
        return raw.size();
    }

    public @NotNull String raw(final int index, final @NotNull String fallback) {
        return Objects.requireNonNull(raw.get(index, fallback));
    }

    public void require(final int count) {
        if (size() < count) throw new IllegalArgumentException("Expected at least " + count + " arguments");
    }

    public @NotNull TagArgs component(final int index) {
        if (index < 0 || index >= size()) return this;
        scalars.set(index, new ValueSource.Constant<>(raw(index, "")));
        return componentAlternative(index);
    }

    public @NotNull TagArgs componentAlternative(final int index) {
        if (index < 0 || index >= size()) return this;
        final @NotNull TemplateNode node = compiler.compileArgument(raw(index, "")).root();
        if (node instanceof final TemplateNode.Static fixed) {
            scalars.set(index, new ValueSource.Constant<>(raw(index, "")));
            components().set(index, new ValueSource.Constant<>(fixed.component()));
        } else {
            components().set(index, new ValueSource.Computed<>(node::render));
        }
        return this;
    }

    public @NotNull ValueSource<@NotNull Values> values() {
        if (raw.isEmpty()) return EMPTY_VALUES;
        final @NotNull List<@NotNull ValueSource<@NotNull String>> scalarSources = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            @Nullable ValueSource<@NotNull String> source = scalars.get(i);
            if (source == null) {
                source = StringTemplate.compile(raw.get(i, ""), compiler);
                scalars.set(i, source);
            }
            scalarSources.add(source);
        }
        final @Nullable List<@NotNull ValueSource<@NotNull Component>> componentValues = components;
        final boolean hasComponents = componentValues != null;
        final @NotNull List<@NotNull ValueSource<@NotNull Component>> componentSources =
            hasComponents ? List.copyOf(componentValues) : List.of();
        final @NotNull Arguments strings = Arguments.generate(size(), (final int index) -> {
            final var source = scalarSources.get(index);
            return source.isConstant() ? source.evaluate(RenderScope.EMPTY) : "";
        });
        final @NotNull Component @NotNull [] rendered = hasComponents ? new Component[size()] : EMPTY_COMPONENTS;
        boolean dynamicScalars = false;
        boolean dynamicComponents = false;
        for (int i = 0; i < size(); i++) {
            final var scalar = scalarSources.get(i);
            final var component = hasComponents ? componentSources.get(i) : EMPTY_COMPONENT;
            dynamicScalars |= !scalar.isConstant();
            dynamicComponents |= !component.isConstant();
            if (hasComponents) rendered[i] = component.isConstant()
                ? component.evaluate(RenderScope.EMPTY) : Component.empty();
        }
        final @NotNull Values constants = new Values(strings, rendered);
        if (!dynamicScalars && !dynamicComponents) return new ValueSource.Constant<>(constants);
        final @NotNull PreparedValues prepared = new PreparedValues(scalarSources, componentSources, constants,
            dynamicScalars, dynamicComponents);
        return new ValueSource.Computed<>(prepared::evaluate);
    }

    public <T> @NotNull ValueSource<T> map(final @NotNull Function<@NotNull Values, @NotNull T> function) {
        return values().map(function);
    }

    private @NotNull List<@NotNull ValueSource<@NotNull Component>> components() {
        final @Nullable List<@NotNull ValueSource<@NotNull Component>> existing = components;
        if (existing != null) return existing;
        final @NotNull List<@NotNull ValueSource<@NotNull Component>> created = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) created.add(EMPTY_COMPONENT);
        components = created;
        return created;
    }

    private record PreparedValues(@NotNull List<@NotNull ValueSource<@NotNull String>> scalars,
        @NotNull List<@NotNull ValueSource<@NotNull Component>> components, @NotNull Values constants,
        boolean dynamicScalars, boolean dynamicComponents) {
        private @NotNull Values evaluate(final @NotNull RenderScope context) {
            final @NotNull Arguments strings = dynamicScalars
                ? Arguments.generate(scalars.size(), (final int index) -> scalars.get(index).evaluate(context))
                : constants.scalars;
            final @NotNull Component @NotNull [] rendered = dynamicComponents
                ? constants.components.clone() : constants.components;
            if (dynamicComponents) {
                for (int i = 0, size = components.size(); i < size; i++) {
                    if (!components.get(i).isConstant()) rendered[i] = components.get(i).evaluate(context);
                }
            }
            return new Values(strings, rendered);
        }
    }

    public static final class Values {
        private final @NotNull Arguments scalars;
        private final @NotNull Component @NotNull [] components;

        private Values(final @NotNull Arguments scalars, final @NotNull Component @NotNull [] components) {
            this.scalars = scalars;
            this.components = components;
        }

        public int size() {
            return scalars.size();
        }

        public @NotNull Arguments arguments() {
            return scalars.copy();
        }

        public @NotNull Arguments rawArguments() {
            return scalars;
        }

        public @NotNull String string(final int index, final @NotNull String fallback) {
            Objects.requireNonNull(fallback, "fallback");
            return Objects.requireNonNull(scalars.get(index, fallback));
        }

        public @NotNull Component component(final int index) {
            return index >= 0 && index < components.length ? components[index] : Component.empty();
        }
    }
}
