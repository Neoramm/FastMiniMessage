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
public class RenderCacheBenchmark {
    private final @NotNull V1CoreBenchmarks.Cache v1 = new V1CoreBenchmarks.Cache();
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull PreComponentTree tree =
        compiler.compile("<gradient:red:blue>Welcome to the server!</gradient>");
    private final @NotNull RenderScope scope = RenderScope.builder(PayloadSchema.withDefaults()).build();

    @Benchmark
    public @NotNull Component withoutScope() {
        return tree.render();
    }

    @Benchmark
    public @NotNull Component unusedScope() {
        return tree.render((scope).withViewer(null));
    }
    @Benchmark
    public @NotNull Component inspectCachedTree() {
        tree.inspect((final var snapshot) -> { });
        return tree.render();
    }

    @Benchmark
    public @NotNull Component mapCachedTree() {
        tree.map((final var node) -> node);
        return tree.render();
    }

    @Benchmark
    public @NotNull Component v1WithoutScope() {
        return v1.withoutScope();
    }

    @Benchmark
    public @NotNull Component v1UnusedScope() {
        return v1.unusedScope();
    }

    @Benchmark
    public @NotNull Component v1InspectCachedTree() {
        return v1.inspectCachedTree();
    }

    @Benchmark
    public @NotNull Component v1VisitCachedTree() {
        return v1.visitCachedTree();
    }

}
