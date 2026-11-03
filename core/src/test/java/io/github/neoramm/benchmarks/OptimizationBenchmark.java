package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(3)
public class OptimizationBenchmark {
    private final @NotNull PayloadSchema schema;
    private final PayloadSchema.@NotNull Key<@NotNull Component> nameKey;
    private final @NotNull MiniMessageCompiler compiler;
    private final @NotNull PreComponentTree gradient;
    private final @NotNull RenderScope names;
    private final @NotNull PreComponentTree parsed;
    private final @NotNull RenderScope parsedScope;

    public OptimizationBenchmark() {
        final var layout = PayloadSchema.builder();
        nameKey = layout.key("name", Component.class);
        schema = layout.build();
        final var registry = ResolverRegistry.withDefaults(schema)
            .registerPlaceholder("name", (final var scope) -> scope.payload(nameKey))
            .registerPlaceholder("parsed", (final var scope) ->
                Component.text("Hello", NamedTextColor.RED).append(Component.text(" world", NamedTextColor.BLUE)));
        compiler = new MiniMessageCompiler(registry);
        gradient = compiler.compile("<gradient:red:blue><name></gradient>");
        names = RenderScope.builder(schema).payload(nameKey, Component.text("Alexandra")).build();
        parsed = compiler.compile("<parsed>");
        parsedScope = RenderScope.builder(schema).build();
    }

    @Benchmark
    public @NotNull PreComponentTree compileStyles() {
        return compiler.compile("<red><bold>Hello</bold></red> <blue>world</blue><italic>!</italic>");
    }

    @Benchmark
    public @NotNull Component dynamicGradient() {
        return gradient.render(names);
    }

    @Benchmark
    public @NotNull Component cachedParsedBridge() {
        return parsed.render(parsedScope);
    }
}
