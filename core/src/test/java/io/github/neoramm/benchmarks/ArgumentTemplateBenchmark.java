package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(3)
public class ArgumentTemplateBenchmark {
    private static final @NotNull PayloadSchema SCHEMA;
    private static final PayloadSchema.@NotNull Key<@NotNull String> TARGET_KEY;
    private static final @NotNull PreComponentTree TREE;
    private static final @NotNull RenderScope @NotNull [] SCOPES;

    static {
        final var layout = PayloadSchema.builder();
        TARGET_KEY = layout.key("target", String.class);
        SCHEMA = layout.build();
        final var registry = ResolverRegistry.withDefaults(SCHEMA)
            .registerPlaceholder("target", (final var scope) -> Component.text(scope.payload(TARGET_KEY)));
        TREE = new MiniMessageCompiler(registry)
            .compile("<click:suggest_command:'/msg <target> hello'><green>Message <target></green></click>");
        SCOPES = new RenderScope[64];
        for (int i = 0; i < SCOPES.length; i++) {
            SCOPES[i] = RenderScope.builder(SCHEMA).payload(TARGET_KEY, "player" + i).build();
        }
    }

    private int index;

    @Benchmark
    public @NotNull Component changingScalarArguments() {
        return TREE.render(SCOPES[index++ & 63]);
    }
}
