package io.github.neoramm.fastminimessage.v2.template;

import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public sealed interface ValueSource<T> {
    @NotNull T evaluate(final @NotNull RenderScope context);

    default boolean isConstant() {
        return this instanceof Constant<?>;
    }

    default <R> @NotNull ValueSource<R> map(final @NotNull Function<@NotNull T, @NotNull R> function) {
        Objects.requireNonNull(function, "function");
        if (this instanceof Constant<T> constant) return new Constant<>(function.apply(constant.value()));
        return new Computed<>((final var context) -> function.apply(evaluate(context)));
    }

    record Constant<T>(@NotNull T value) implements ValueSource<T> {
        public Constant {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public @NotNull T evaluate(final @NotNull RenderScope context) {
            return value;
        }
    }

    record Computed<T>(@NotNull Function<@NotNull RenderScope, @NotNull T> function) implements ValueSource<T> {
        public Computed {
            Objects.requireNonNull(function, "function");
        }

        @Override
        public @NotNull T evaluate(final @NotNull RenderScope context) {
            return Objects.requireNonNull(function.apply(context), "computed value");
        }
    }
}
