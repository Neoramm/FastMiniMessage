package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.Placeholder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RenderCacheBenchmark {
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull PreComponentTree tree = compiler.compile("<gradient:red:blue>Welcome to the server!</gradient>");
    private final @NotNull RenderScope scope = RenderScope.of(Placeholder.ofUnparsed("name", "Alex"));

    @Benchmark
    public @NotNull Component withoutScope() {
        return tree.render();
    }

    @Benchmark
    public @NotNull Component unusedScope() {
        return tree.render(null, scope);
    }
    @Benchmark
    public @NotNull Component inspectCachedTree() {
        tree.inspect((final var snapshot) -> { });
        return tree.render();
    }

    @Benchmark
    public @NotNull Component visitCachedTree() {
        tree.visit((final var node) -> { });
        return tree.render();
    }

}
