package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 2)
@Measurement(iterations = 15, time = 2)
@Fork(3)
public class SimpleCommonBenchmark {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private static final @NotNull String SAMPLE = "<green>[Server]</green> <white>Welcome!</white> "
        + "<yellow>Type <bold>/help</bold> for commands.</yellow>";

    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler compiler;
    private @NotNull PreComponentTree tree;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        tree = compiler.compile(SAMPLE);
    }

    @Benchmark
    public @NotNull Component miniMessageDeserialize() {
        return miniMessage.deserialize(SAMPLE);
    }

    @Benchmark
    public @NotNull Component miniPreCompilerRender() {
        return tree.render(NOBODY);
    }

    @Benchmark
    public @NotNull Component miniPreCompilerCompileAndRender() {
        return compiler.compile(SAMPLE).render(NOBODY);
    }
}
