package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.PayloadInputs;
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
public class ComplexLongBenchmark {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private static final @NotNull String SAMPLE = """
            <bold>Lorem ipsum dolor sit amet</bold>, <red>consectetur adipiscing elit</red>, \
            <gradient:#ff0000:#0000ff>sed do eiusmod tempor incididunt ut labore et dolore magna aliqua</gradient>. \
            <italic>Ut enim ad minim veniam</italic>, <click:run_command:'/spawn'>\
            <hover:show_text:'<gray>hint <bold>x</bold></gray>'>quis nostrud</hover></click> \
            ullamco laboris nisi ut aliquip ex ea commodo consequat. \
            <shadow:red:0.4>Duis aute irure dolor in reprehenderit</shadow> \
            in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \
            <rainbow>Excepteur sint occaecat cupidatat non proident</rainbow>, \
            sunt in culpa qui officia deserunt mollit anim id est laborum. <newline/>\
            <bold><red><blue>nested color override</blue></red></bold> \
            <red>a</blue>b malformed</red> </unknown>leftover \
            <unknown>x</unknown> <transition:red:blue:0.5>transition content</transition> \
            <font:minecraft:default>font text</font> \
            <gradient:red:blue><key:key.jump/> mixed content</gradient> \
            <hover:show_item:'minecraft:diamond':1><bold>item hover</bold></hover> \
            <lang:chat.type.text:'<red>Steve</red>':hello/> <score:Steve:objective> <selector:@a:', '> \
            <nbt:block:0 64 0:Items:true> <pride>pride text</pride> <insert:'x'>inserted <u>underlined</u></insert> \
            <obfuscated>secret</obfuscated> <strikethrough>struck</strikethrough> \
            <gradient:red:blue><gradient:green:yellow>nested gradient</gradient> tail</gradient> \
            <gradient:0.5>single phase gradient</gradient> <colour:dark_grey>british color</colour> \
            <click:run_command:'/spawn'>closed with args</click:run_command> \
            <lang:gui.ok> unclosed translatable text \
            <red>😀 emoji</red> <reset>reset here <bold>bold after reset</bold></reset> \
            </red>unmatched close <unknown/> selfclosing unknown \
            <bold>unclosed bold span continues
            """.repeat(3);

    private static final @NotNull String TAGS_SAMPLE = """
            <bold><name></bold> <rank>, \
            <upper:lorem> <tint>ipsum dolor sit amet</tint>, \
            <gradient:#ff0000:#0000ff><name> and <upper:dolor></gradient>. \
            <click:run_command:'/spawn'><hover:show_text:'<gray><name></gray>'><upper:sit></hover></click> \
            <styled>amet <bold>consectetur</bold></styled> <parsed> \
            <name:arg>literal</name> <rank:> <name/> \
            <upper:repeated> <tint>more text <bold>with style</bold></tint> \
            """.repeat(3);

    private static final @NotNull TagResolver NAME = Placeholder.unparsed("name", "Hector");
    private static final @NotNull TagResolver RANK = Placeholder.unparsed("rank", "Admin");
    private static final @NotNull TagResolver STYLED = Placeholder.styling("styled", NamedTextColor.GREEN);
    private static final @NotNull TagResolver PARSED = Placeholder.parsed("parsed", "<red>P</red>");
    private static final @NotNull TagResolver UPPER = TagResolver.resolver("upper",
        (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
    private static final @NotNull TagResolver TINT = TagResolver.resolver("tint",
        (Modifying) (current, depth) -> current.color(NamedTextColor.AQUA));

    private static final @NotNull TagResolver @NotNull [] PLACEHOLDERS = {NAME, RANK, STYLED, PARSED};
    private static final @NotNull TagResolver @NotNull [] ALL = {
        NAME, RANK, STYLED, PARSED, UPPER, TINT
    };

    private static final @NotNull PayloadSchema TAGS_SCHEMA;
    private static final PayloadSchema.@NotNull Key<@NotNull String> NAME_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull String> RANK_KEY;

    static {
        final @NotNull PayloadSchema.Builder tagsBuilder = PayloadSchema.builder();
        NAME_KEY = tagsBuilder.key("name", String.class);
        RANK_KEY = tagsBuilder.key("rank", String.class);
        TAGS_SCHEMA = tagsBuilder.build();
    }

    private static @NotNull ResolverRegistry createTagsRegistry() {
        return ResolverRegistry.withDefaults(TAGS_SCHEMA)
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(NAME_KEY)))
            .registerPlaceholder("rank", (final var scope) -> Component.text(scope.payload(RANK_KEY)))
            .registerPlaceholder("parsed", (final var scope) -> Component.text("P", NamedTextColor.RED))
            .registerModifier("styled", (final var content, final var scope) -> content.color(NamedTextColor.GREEN))
            .register(new TagSpec.LeafRule(Set.of("upper"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> Component.text(vals.string(0, "").toUpperCase(Locale.ROOT)));
            }))
            .registerModifier("tint", (final var content, final var scope) -> content.color(NamedTextColor.AQUA));
    }

    private static @NotNull ResolverRegistry createPlaceholdersRegistry() {
        return ResolverRegistry.withDefaults(TAGS_SCHEMA)
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(NAME_KEY)))
            .registerPlaceholder("rank", (final var scope) -> Component.text(scope.payload(RANK_KEY)))
            .registerPlaceholder("parsed", (final var scope) -> Component.text("P", NamedTextColor.RED))
            .registerModifier("styled", (final var content, final var scope) -> content.color(NamedTextColor.GREEN));
    }

    private final @NotNull V1CoreBenchmarks.ComplexLong v1 =
        new V1CoreBenchmarks.ComplexLong(SAMPLE, TAGS_SAMPLE);
    private final @NotNull V1BridgeBenchmarks.Tags v1Tags =
        new V1BridgeBenchmarks.Tags(TAGS_SAMPLE, PLACEHOLDERS, ALL);
    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler compiler;
    private @NotNull MiniMessageCompiler tagsCompiler;
    private @NotNull PreComponentTree tree;
    private @NotNull PreComponentTree tagsTree;
    private @NotNull PreComponentTree placeholderTagsTree;
    private @NotNull RenderScope tagsScope;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        tree = compiler.compile(SAMPLE);

        final @NotNull MiniMessageCompiler placeholdersCompiler = new MiniMessageCompiler(createPlaceholdersRegistry());
        placeholderTagsTree = placeholdersCompiler.compile(TAGS_SAMPLE);

        tagsCompiler = new MiniMessageCompiler(createTagsRegistry());
        tagsTree = tagsCompiler.compile(TAGS_SAMPLE);
        tagsScope = RenderScope.builder(TAGS_SCHEMA)
            .viewer(NOBODY)
            .payload(NAME_KEY, "Hector")
            .payload(RANK_KEY, "Admin")
            .build();
    }

    @Benchmark
    public @NotNull Component miniMessageDeserialize() {
        return miniMessage.deserialize(SAMPLE);
    }

    @Benchmark
    public @NotNull Component miniPreCompilerRender() {
        return tree.render(PayloadInputs.viewer(NOBODY));
    }

    @Benchmark
    public @NotNull Component miniPreCompilerCompileAndRender() {
        return compiler.compile(SAMPLE).render(PayloadInputs.viewer(NOBODY));
    }

    @Benchmark
    public @NotNull Component miniMessageTagsDeserialize() {
        return miniMessage.deserialize(TAGS_SAMPLE, ALL);
    }

    @Benchmark
    public @NotNull Component fastRenderTagsPlaceholders() {
        return placeholderTagsTree.render(tagsScope);
    }

    @Benchmark
    public @NotNull Component fastRenderTagsAll() {
        return tagsTree.render(tagsScope);
    }

    @Benchmark
    public @NotNull Component fastRenderNativeTags() {
        return tagsTree.render(tagsScope);
    }

    @Benchmark
    public @NotNull Component fastCompileAndRenderNativeTags() {
        return tagsCompiler.compile(TAGS_SAMPLE).render(tagsScope);
    }

    @Benchmark
    public @NotNull Component v1Render() {
        return v1.render();
    }

    @Benchmark
    public @NotNull Component v1CompileAndRender() {
        return v1.compileAndRender();
    }

    @Benchmark
    public @NotNull Component v1RenderTagsPlaceholders() {
        return v1Tags.renderPlaceholders();
    }

    @Benchmark
    public @NotNull Component v1RenderTagsLiteralFallback() {
        return v1Tags.renderLiteralFallback();
    }

    @Benchmark
    public @NotNull Component v1RenderNativeTags() {
        return v1.renderNativeTags();
    }

    @Benchmark
    public @NotNull Component v1CompileAndRenderNativeTags() {
        return v1.compileAndRenderNativeTags();
    }
}
