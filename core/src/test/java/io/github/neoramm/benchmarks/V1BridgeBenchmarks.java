package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.compat.ExperimentalMiniMessageTags;
import io.github.neoramm.fastminimessage.compat.MiniMessageTags;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RuntimeTagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

final class V1BridgeBenchmarks {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private V1BridgeBenchmarks() {
    }

    static final class Tags {

        private final @NotNull MiniMessageCompiler compiler;
        private final @NotNull String sample;
        private final @NotNull PreComponentTree tree;
        private final @NotNull RuntimeTagResolver placeholders;
        private final @NotNull RuntimeTagResolver all;
        private final @NotNull RuntimeTagResolver resolvedAll;

        Tags(final @NotNull String sample, final TagResolver @NotNull [] placeholders,
            final TagResolver @NotNull [] all) {
            this.compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
            this.sample = sample;
            this.tree = compiler.compile(sample);
            this.placeholders = MiniMessageTags.of(placeholders);
            this.all = MiniMessageTags.of(all);
            this.resolvedAll = ExperimentalMiniMessageTags.of(all);
        }

        @NotNull Component renderPlaceholders() {
            return tree.render(NOBODY, placeholders);
        }

        @NotNull Component renderLiteralFallback() {
            return tree.render(NOBODY, all);
        }

        @NotNull Component renderResolved() {
            return tree.render(NOBODY, resolvedAll);
        }

        @NotNull Component compileAndRenderResolved() {
            return compiler.compile(sample).render(NOBODY, resolvedAll);
        }
    }
}
