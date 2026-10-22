package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
@Fork(5)
public class ComplexLongBenchmark {

    private static final Pointered NOBODY = new Pointered() {
    };

    private static final String SAMPLE = """
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

    private MiniMessage miniMessage;
    private MiniMessageCompiler compiler;
    private PreComponentTree tree;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        tree = compiler.compile(SAMPLE);
    }

    @Benchmark
    public Component miniMessageDeserialize() {
        return miniMessage.deserialize(SAMPLE);
    }

    @Benchmark
    public Component miniPreCompilerRender() {
        return tree.render(NOBODY);
    }

    @Benchmark
    public Component miniPreCompilerCompileAndRender() {
        return compiler.compile(SAMPLE).render(NOBODY);
    }
}
