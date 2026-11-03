package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.internal.TokenPatterns;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(3)
public class RegexPatternBenchmark {
    private static final @NotNull Pattern JDK = Pattern.compile("[!?#]?[a-zA-Z0-9_-]+");

    @Param({"red", "!underlined", "invalid<tag"})
    public @NotNull String input = "red";

    @Benchmark
    public boolean automaton() {
        return TokenPatterns.isTagName(input);
    }

    @Benchmark
    public boolean jdkPattern() {
        return JDK.matcher(input).matches();
    }

    @Benchmark
    public boolean previousLoop() {
        if (input.isEmpty()) return false;
        final char first = input.charAt(0);
        final int start = first == '!' || first == '?' || first == '#' ? 1 : 0;
        if (start == input.length()) return false;
        for (int i = start; i < input.length(); i++) {
            final char character = input.charAt(i);
            if (!(character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9' || character == '_' || character == '-')) return false;
        }
        return true;
    }
}
