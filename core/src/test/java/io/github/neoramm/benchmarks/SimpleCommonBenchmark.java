package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.compat.MiniMessageTags;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RuntimeTagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
@Warmup(iterations = 10, time = 3)
@Measurement(iterations = 20, time = 3)
@Fork(3)
public class SimpleCommonBenchmark {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private static final @NotNull String SAMPLE = "<green>[Server]</green> <white>Welcome!</white> "
        + "<yellow>Type <bold>/help</bold> for commands.</yellow>";

    private static final @NotNull String TAGS_SAMPLE = """
            <green>[Server]</green> <name> <rank>: <upper:hello> <tint>welcome</tint> \
            """.repeat(4);

    private static final @NotNull TagResolver NAME = Placeholder.unparsed("name", "Hector");
    private static final @NotNull TagResolver RANK = Placeholder.unparsed("rank", "Admin");
    private static final @NotNull TagResolver UPPER = TagResolver.resolver("upper",
        (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
    private static final @NotNull TagResolver TINT = TagResolver.resolver("tint",
        (Modifying) (current, depth) -> current.color(NamedTextColor.AQUA));

    private static final @NotNull TagResolver @NotNull [] PLACEHOLDERS = {NAME, RANK};
    private static final @NotNull TagResolver @NotNull [] ALL = {NAME, RANK, UPPER, TINT};

    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler compiler;
    private @NotNull PreComponentTree tree;
    private @NotNull PreComponentTree tagsTree;
    private @NotNull RuntimeTagResolver placeholderTags;
    private @NotNull RuntimeTagResolver tagsLiteralFallback;
    private @NotNull RuntimeTagResolver tagsWithDelegation;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        tree = compiler.compile(SAMPLE);
        tagsTree = compiler.compile(TAGS_SAMPLE);
        placeholderTags = MiniMessageTags.of(PLACEHOLDERS);
        tagsLiteralFallback = MiniMessageTags.of(ALL);
        tagsWithDelegation = MiniMessageTags.of(miniMessage, ALL);
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

    @Benchmark
    public @NotNull Component miniMessageTagsDeserialize() {
        return miniMessage.deserialize(TAGS_SAMPLE, ALL);
    }

    @Benchmark
    public @NotNull Component fastRenderTagsPlaceholders() {
        return tagsTree.render(NOBODY, placeholderTags);
    }

    @Benchmark
    public @NotNull Component fastRenderTagsLiteralFallback() {
        return tagsTree.render(NOBODY, tagsLiteralFallback);
    }

    @Benchmark
    public @NotNull Component fastRenderTagsWithDelegation() {
        return tagsTree.render(NOBODY, tagsWithDelegation);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderTagsWithDelegation() {
        return compiler.compile(TAGS_SAMPLE).render(NOBODY, tagsWithDelegation);
    }
}
