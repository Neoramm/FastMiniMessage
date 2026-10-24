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
public class ComplexLongBenchmark {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private static final @NotNull String SAMPLE = """
            <bold>Lorem ipsum dolor sit amet</bold>, <red>consectetur adipiscing elit</red>, \
            <gradient:#ff0000:#0000ff>sed do eiusmod tempor incididunt ut labore et dolore magna aliqua</gradient>. \
            <italic>Ut enim ad minim veniam</italic>, <click:run_command:'/spawn'><hover:show_text:'<gray>hint <bold>x</bold></gray>'>quis nostrud exercitation</hover></click> \
            ullamco laboris nisi ut aliquip ex ea commodo consequat. <shadow:red:0.4>Duis aute irure dolor in reprehenderit</shadow> \
            in voluptate velit esse cillum dolore eu fugiat nulla pariatur. <rainbow>Excepteur sint occaecat cupidatat non proident</rainbow>, \
            sunt in culpa qui officia deserunt mollit anim id est laborum. <newline/>\
            <bold><red><blue>nested color override</blue></red></bold> \
            <red>a</blue>b malformed</red> </unknown>leftover \
            <unknown>x</unknown> <transition:red:blue:0.5/> <font:minecraft:default>font text</font> \
            <gradient:red:blue><key:key.jump/> mixed content</gradient> \
            <hover:show_item:'minecraft:diamond':1><bold>item hover</bold></hover> \
            <lang:chat.type.text:'<red>Steve</red>':hello/> <score:Steve:objective> <selector:@a> \
            <nbt:block:~ ~ ~:Items> <pride>pride text</pride> <insert:'x'>inserted <u>underlined</u></insert> \
            <obfuscated>secret</obfuscated> <strikethrough>struck</strikethrough> \
            <gradient:red:blue><gradient:green:yellow>nested gradient</gradient> tail</gradient> \
            <red>😀 emoji</red> <reset>reset here <bold>bold after reset</bold></reset> \
            </red>unmatched close <unknown/> selfclosing unknown \
            <bold>unclosed bold span continues
            """.repeat(3);

    /**
     * Complex input mixing native tags with fixed placeholders, an argument-based resolver,
     * a {@code Modifying} tag, parsed/styling placeholders and the fixed-argument edge cases.
     */
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
