package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 20, time = 1)
@Fork(3)
public class AdventureTagsBenchmark {

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

    private static final @NotNull PayloadSchema PAYLOAD_SCHEMA;
    private static final PayloadSchema.@NotNull Key<@NotNull String> NAME_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull String> RANK_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull Component> COMP_KEY;

    static {
        final @NotNull PayloadSchema.Builder builder = PayloadSchema.builder();
        NAME_KEY = builder.key("name", String.class);
        RANK_KEY = builder.key("rank", String.class);
        COMP_KEY = builder.key("comp", Component.class);
        PAYLOAD_SCHEMA = builder.build();
    }

    private static final @NotNull String PLACEHOLDER_SAMPLE = """
            <name> <rank> <styled>styled</styled> <comp> <parsed> <name/> <name> \
            """.repeat(4);

    private static final @NotNull String MIXED_SAMPLE = PLACEHOLDER_SAMPLE + """
            <upper:hello> <upper:world/> <tint>tinted <bold>text</bold></tint> \
            <upper:nested><tint>both</tint></upper> \
            """.repeat(3);

    private static final @NotNull String EDGE_SAMPLE = """
            <name>kids</name> <name:arg>literal</name> <rank:> <parsed/> \
            <styled>styled</styled> \
            """.repeat(4);

    private static @NotNull ResolverRegistry createPlaceholdersRegistry() {
        return ResolverRegistry.withDefaults(PAYLOAD_SCHEMA)
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(NAME_KEY)))
            .registerPlaceholder("rank", (final var scope) -> Component.text(scope.payload(RANK_KEY)))
            .registerPlaceholder("comp", (final var scope) -> scope.payload(COMP_KEY))
            .registerPlaceholder("parsed", (final var scope) -> Component.text("P", NamedTextColor.RED))
            .registerModifier("styled", (final var content, final var scope) -> content.color(NamedTextColor.GREEN));
    }

    private static @NotNull ResolverRegistry createMixedRegistry() {
        return createPlaceholdersRegistry()
            .register(new TagSpec.LeafRule(Set.of("upper"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> Component.text(vals.string(0, "").toUpperCase(Locale.ROOT)));
            }))
            .registerModifier("tint", (final var content, final var scope) -> content.color(NamedTextColor.AQUA));
    }

    private final @NotNull V1BridgeBenchmarks.Tags v1Placeholders =
        new V1BridgeBenchmarks.Tags(PLACEHOLDER_SAMPLE, PLACEHOLDERS, ALL);
    private final @NotNull V1BridgeBenchmarks.Tags v1Mixed =
        new V1BridgeBenchmarks.Tags(MIXED_SAMPLE, PLACEHOLDERS, ALL);
    private final @NotNull V1BridgeBenchmarks.Tags v1Edge =
        new V1BridgeBenchmarks.Tags(EDGE_SAMPLE, PLACEHOLDERS, ALL);
    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler mixedCompiler;
    private @NotNull PreComponentTree placeholderTree;
    private @NotNull PreComponentTree mixedTree;
    private @NotNull PreComponentTree edgeTree;
    private @NotNull RenderScope scope;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        final @NotNull MiniMessageCompiler placeholderCompiler = new MiniMessageCompiler(createPlaceholdersRegistry());
        placeholderTree = placeholderCompiler.compile(PLACEHOLDER_SAMPLE);
        edgeTree = placeholderCompiler.compile(EDGE_SAMPLE);

        mixedCompiler = new MiniMessageCompiler(createMixedRegistry());
        mixedTree = mixedCompiler.compile(MIXED_SAMPLE);

        scope = RenderScope.builder(PAYLOAD_SCHEMA)
            .viewer(NOBODY)
            .payload(NAME_KEY, "Hector")
            .payload(RANK_KEY, "Admin")
            .payload(COMP_KEY, Component.text("C", NamedTextColor.BLUE))
            .build();
    }

    @Benchmark
    public @NotNull Component miniMessagePlaceholders() {
        return miniMessage.deserialize(PLACEHOLDER_SAMPLE, PLACEHOLDERS);
    }

    @Benchmark
    public @NotNull Component fastRenderPlaceholders() {
        return placeholderTree.render(scope);
    }

    @Benchmark
    public @NotNull Component miniMessageEdgeCases() {
        return miniMessage.deserialize(EDGE_SAMPLE, PLACEHOLDERS);
    }

    @Benchmark
    public @NotNull Component fastRenderEdgeCases() {
        return edgeTree.render(scope);
    }

    @Benchmark
    public @NotNull Component miniMessageMixed() {
        return miniMessage.deserialize(MIXED_SAMPLE, ALL);
    }

    @Benchmark
    public @NotNull Component fastRenderMixed() {
        return mixedTree.render(scope);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderMixed() {
        return mixedCompiler.compile(MIXED_SAMPLE).render(scope);
    }

    @Benchmark
    public @NotNull Component fastRenderNativePlaceholders() {
        return placeholderTree.render(scope);
    }

    @Benchmark
    public @NotNull Component fastRenderNativeMixed() {
        return mixedTree.render(scope);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderNativeMixed() {
        return mixedCompiler.compile(MIXED_SAMPLE).render(scope);
    }

    @Benchmark
    public @NotNull Component v1RenderPlaceholders() {
        return v1Placeholders.renderPlaceholders();
    }

    @Benchmark
    public @NotNull Component v1RenderEdgeCases() {
        return v1Edge.renderPlaceholders();
    }

    @Benchmark
    public @NotNull Component v1RenderMixedLiteralFallback() {
        return v1Mixed.renderLiteralFallback();
    }

    @Benchmark
    public @NotNull Component v1RenderMixed() {
        return v1Mixed.renderResolved();
    }

    @Benchmark
    public @NotNull Component v1CompileAndRenderMixed() {
        return v1Mixed.compileAndRenderResolved();
    }
}
