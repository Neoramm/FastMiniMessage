package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.translation.FastMiniMessageTranslationStore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class LocalizationBenchmark {
    private final @NotNull FastMiniMessageTranslationStore fast =
        FastMiniMessageTranslationStore.create(Key.key("benchmark:fast"));
    private final @NotNull MiniMessageTranslationStore reference =
        MiniMessageTranslationStore.create(Key.key("benchmark:reference"));
    private final @NotNull TranslatableComponent input =
        Component.translatable("welcome", Component.text("Alex"), Component.text("Monday"));

    @Setup
    public void setup() {
        final @NotNull String template = "<green>Welcome <arg:0>!</green> <gray>Today is <argument:1>.</gray>";
        fast.register("welcome", Locale.US, template);
        reference.register("welcome", Locale.US, template);
    }

    @Benchmark
    public @Nullable Component precompiled() {
        return fast.translate(input, Locale.US);
    }

    @Benchmark
    public @Nullable Component miniMessage() {
        return reference.translate(input, Locale.US);
    }
}
