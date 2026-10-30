package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.compat.ExperimentalMiniMessageTags;
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

/**
 * Measures the MiniMessage {@code TagResolver} bridge.
 *
 * <p>The placeholder benchmarks exercise the fast path that never invokes MiniMessage. The mixed
 * benchmarks add an argument-based resolver and a {@code Modifying} tag, which are delegated to
 * MiniMessage only when an instance is supplied.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 3)
@Measurement(iterations = 20, time = 3)
@Fork(3)
public class MiniMessageTagsBenchmark {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private static final @NotNull TagResolver NAME = Placeholder.unparsed("name", "Hector");
    private static final @NotNull TagResolver RANK = Placeholder.unparsed("rank", "Admin");
    private static final @NotNull TagResolver STYLED = Placeholder.styling("styled", NamedTextColor.GREEN);
    private static final @NotNull TagResolver COMPONENT =
        Placeholder.component("comp", Component.text("C", NamedTextColor.BLUE));
    private static final @NotNull TagResolver PARSED = Placeholder.parsed("parsed", "<red>P</red>");
    private static final @NotNull TagResolver UPPER = TagResolver.resolver("upper",
        (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
    private static final @NotNull TagResolver TINT = TagResolver.resolver("tint",
        (Modifying) (current, depth) -> current.color(NamedTextColor.AQUA));

    private static final @NotNull TagResolver @NotNull [] PLACEHOLDERS = {
        NAME, RANK, STYLED, COMPONENT, PARSED
    };
    private static final @NotNull TagResolver @NotNull [] ALL = {
        NAME, RANK, STYLED, COMPONENT, PARSED, UPPER, TINT
    };

    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver NATIVE_NAME =
        io.github.neoramm.fastminimessage.model.TagResolver.inserting("name", "Hector");
    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver NATIVE_RANK =
        io.github.neoramm.fastminimessage.model.TagResolver.inserting("rank", "Admin");
    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver NATIVE_COMPONENT =
        io.github.neoramm.fastminimessage.model.TagResolver.inserting("comp",
            Component.text("C", NamedTextColor.BLUE));
    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver NATIVE_UPPER =
        io.github.neoramm.fastminimessage.model.TagResolver.dynamic("upper",
            (args, ctx) -> Component.text(args.getFirst().toUpperCase()));
    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver NATIVE_TINT =
        io.github.neoramm.fastminimessage.model.TagResolver.modifying("tint",
            (current, ctx) -> current.color(NamedTextColor.AQUA));

    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver @NotNull []
        NATIVE_PLACEHOLDERS = {NATIVE_NAME, NATIVE_RANK, NATIVE_COMPONENT};
    private static final @NotNull io.github.neoramm.fastminimessage.model.TagResolver @NotNull [] NATIVE_ALL = {
        NATIVE_NAME, NATIVE_RANK, NATIVE_COMPONENT, NATIVE_UPPER, NATIVE_TINT
    };

    private static final @NotNull String PLACEHOLDER_SAMPLE = """
            <name> <rank> <styled>styled</styled> <comp> <parsed> <name/> <name> \
            """.repeat(4);

    private static final @NotNull String MIXED_SAMPLE = PLACEHOLDER_SAMPLE + """
            <upper:hello> <upper:world/> <tint>tinted <bold>text</bold></tint> \
            <upper:nested><tint>both</tint></upper> \
            """.repeat(3);

    /**
     * Exercises the compatibility fixes: fixed tags with arguments (including an empty one),
     * a self-closing parsed placeholder and the literal closing tag of a self-closing inserting
     * placeholder used with children.
     */
    private static final @NotNull String EDGE_SAMPLE = """
            <name>kids</name> <name:arg>literal</name> <rank:> <parsed/> \
            <styled>styled</styled> \
            """.repeat(4);

    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler compiler;
    private @NotNull PreComponentTree placeholderTree;
    private @NotNull PreComponentTree mixedTree;
    private @NotNull PreComponentTree edgeTree;
    private @NotNull RuntimeTagResolver placeholderTags;
    private @NotNull RuntimeTagResolver placeholderTagsWithInstance;
    private @NotNull RuntimeTagResolver mixedTags;
    private @NotNull RuntimeTagResolver mixedTagsWithInstance;
    private @NotNull RuntimeTagResolver experimentalPlaceholderTags;
    private @NotNull RuntimeTagResolver experimentalMixedTags;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        placeholderTree = compiler.compile(PLACEHOLDER_SAMPLE);
        mixedTree = compiler.compile(MIXED_SAMPLE);
        edgeTree = compiler.compile(EDGE_SAMPLE);
        placeholderTags = MiniMessageTags.of(PLACEHOLDERS);
        placeholderTagsWithInstance = MiniMessageTags.of(miniMessage, PLACEHOLDERS);
        mixedTags = MiniMessageTags.of(ALL);
        mixedTagsWithInstance = MiniMessageTags.of(miniMessage, ALL);
        experimentalPlaceholderTags = ExperimentalMiniMessageTags.of(PLACEHOLDERS);
        experimentalMixedTags = ExperimentalMiniMessageTags.of(ALL);
    }

    @Benchmark
    public @NotNull Component miniMessagePlaceholders() {
        return miniMessage.deserialize(PLACEHOLDER_SAMPLE, PLACEHOLDERS);
    }

    @Benchmark
    public @NotNull Component fastRenderPlaceholders() {
        return placeholderTree.render(NOBODY, placeholderTags);
    }

    @Benchmark
    public @NotNull Component fastRenderPlaceholdersWithInstance() {
        return placeholderTree.render(NOBODY, placeholderTagsWithInstance);
    }

    @Benchmark
    public @NotNull Component fastRenderPlaceholdersExperimental() {
        return placeholderTree.render(NOBODY, experimentalPlaceholderTags);
    }

    @Benchmark
    public @NotNull Component miniMessageEdgeCases() {
        return miniMessage.deserialize(EDGE_SAMPLE, PLACEHOLDERS);
    }

    @Benchmark
    public @NotNull Component fastRenderEdgeCases() {
        return edgeTree.render(NOBODY, placeholderTags);
    }

    @Benchmark
    public @NotNull Component fastRenderEdgeCasesExperimental() {
        return edgeTree.render(NOBODY, experimentalPlaceholderTags);
    }

    @Benchmark
    public @NotNull Component miniMessageMixed() {
        return miniMessage.deserialize(MIXED_SAMPLE, ALL);
    }

    @Benchmark
    public @NotNull Component fastRenderMixedLiteralFallback() {
        return mixedTree.render(NOBODY, mixedTags);
    }

    @Benchmark
    public @NotNull Component fastRenderMixedWithDelegation() {
        return mixedTree.render(NOBODY, mixedTagsWithInstance);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderMixedWithDelegation() {
        return compiler.compile(MIXED_SAMPLE).render(NOBODY, mixedTagsWithInstance);
    }

    @Benchmark
    public @NotNull Component fastRenderMixedExperimental() {
        return mixedTree.render(NOBODY, experimentalMixedTags);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderMixedExperimental() {
        return compiler.compile(MIXED_SAMPLE).render(NOBODY, experimentalMixedTags);
    }

    @Benchmark
    public @NotNull Component fastRenderNativePlaceholders() {
        return placeholderTree.render(NOBODY, NATIVE_PLACEHOLDERS);
    }

    @Benchmark
    public @NotNull Component fastRenderNativeMixed() {
        return mixedTree.render(NOBODY, NATIVE_ALL);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderNativeMixed() {
        return compiler.compile(MIXED_SAMPLE).render(NOBODY, NATIVE_ALL);
    }
}
