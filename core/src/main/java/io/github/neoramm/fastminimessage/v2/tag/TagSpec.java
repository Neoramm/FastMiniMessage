package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.template.TreeTransformer;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

public sealed interface TagSpec {
    @NotNull Set<@NotNull String> names();

    record PlaceholderRule(@NotNull Set<@NotNull String> names,
        @NotNull Function<@NotNull RenderScope, @NotNull Component> renderer) implements TagSpec {
        public PlaceholderRule {
            names = Set.copyOf(names);
            Objects.requireNonNull(renderer, "renderer");
        }
    }

    record ModifierRule(@NotNull Set<@NotNull String> names, @NotNull ComponentModifier modifier) implements TagSpec {
        public ModifierRule {
            names = Set.copyOf(names);
            Objects.requireNonNull(modifier, "modifier");
        }
    }

    record StyleRule(@NotNull Set<@NotNull String> names,
        @NotNull BiFunction<@NotNull TagArgs, @NotNull CompileContext, @NotNull ValueSource<@NotNull Style>> rule)
        implements TagSpec {
        public StyleRule {
            names = Set.copyOf(names);
            Objects.requireNonNull(rule, "rule");
        }
    }

    record LeafRule(@NotNull Set<@NotNull String> names,
        @NotNull BiFunction<@NotNull TagArgs, @NotNull CompileContext, @NotNull ValueSource<@NotNull Component>> rule)
        implements TagSpec {
        public LeafRule {
            names = Set.copyOf(names);
            Objects.requireNonNull(rule, "rule");
        }
    }

    record TransformRule(@NotNull Set<@NotNull String> names,
        @NotNull BiFunction<@NotNull TagArgs, @NotNull CompileContext,
            @NotNull ValueSource<@NotNull TreeTransformer>> rule)
        implements TagSpec {
        public TransformRule {
            names = Set.copyOf(names);
            Objects.requireNonNull(rule, "rule");
        }
    }
}
