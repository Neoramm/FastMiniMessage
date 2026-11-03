package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.translation.FastMiniMessageTranslationStore;
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
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(3)
public class LocalizationBenchmark {

    private static final @NotNull String TEMPLATE =
        "<green>Welcome <arg:0>!</green> <gray>Today is <argument:1>.</gray>";

    private final @NotNull V1CoreBenchmarks.Localization v1 = new V1CoreBenchmarks.Localization(TEMPLATE);
    private final @NotNull FastMiniMessageTranslationStore fast =
        FastMiniMessageTranslationStore.create(Key.key("benchmark:fast"));
    private final @NotNull MiniMessageTranslationStore reference =
        MiniMessageTranslationStore.create(Key.key("benchmark:reference"));
    private final @NotNull TranslatableComponent input =
        Component.translatable("welcome", Component.text("Alex"), Component.text("Monday"));

    @Setup
    public void setup() {
        fast.register("welcome", Locale.US, TEMPLATE);
        reference.register("welcome", Locale.US, TEMPLATE);
    }

    @Benchmark
    public @Nullable Component v1Precompiled() {
        return v1.translate();
    }

    @Benchmark
    public @Nullable Component v2Precompiled() {
        return fast.translate(input, Locale.US);
    }

    @Benchmark
    public @Nullable Component miniMessage() {
        return reference.translate(input, Locale.US);
    }
}
