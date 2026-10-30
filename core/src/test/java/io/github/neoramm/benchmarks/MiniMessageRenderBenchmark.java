package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

import java.util.concurrent.TimeUnit;

/**
 * Compares the cost of turning the same long MiniMessage document into an Adventure component,
 * with the reference parser and with the precompiled tree used by Trigger Engine.
 *
 * <p>Every tag registered in {@link ResolverRegistry#withDefaults()} appears at least once,
 * including font, transition, score, selector, nbt, sprite, head, and gradient phase variations.</p>
 *
 * <p>{@link #miniPreCompilerRenderDynamic()} adds a value that is only known at render time inside the
 * gradients and wrappers, so their output cannot be cached; it measures the path used by templates
 * whose placeholders change between renders.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MiniMessageRenderBenchmark {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private static final @NotNull String SAMPLE = """
            <black>black</black><dark_blue>dark blue</dark_blue><dark_green>dark green</dark_green>\
            <dark_aqua>dark aqua</dark_aqua><dark_red>dark red</dark_red>\
            <dark_purple>dark purple</dark_purple><gold>gold</gold><gray>gray</gray><grey>grey</grey>\
            <dark_gray>dark gray</dark_gray><dark_grey>dark grey</dark_grey><blue>blue</blue>\
            <green>green</green><aqua>aqua</aqua><red>red</red><light_purple>light purple</light_purple>\
            <yellow>yellow</yellow><white>white</white><color:aqua>color aqua</color>\
            <color:#12abef>color hex</color><#ff00aa>hex shorthand</#ff00aa>\
            <bold>bold</bold><b>bold alias</b><italic>italic</italic><i>italic alias</i><em>emphasis</em>\
            <underlined>underlined</underlined><u>underlined alias</u>\
            <strikethrough>strikethrough</strikethrough><st>strike alias</st>\
            <obfuscated>obfuscated</obfuscated><obf>obfuscated alias</obf>\
            <bold>bold <!bold>not bold</!bold> bold again</bold>\
            <underlined>line <!underlined>plain</!underlined> line</underlined>\
            <strikethrough>strike <!strikethrough>plain</!strikethrough> strike</strikethrough>\
            <obfuscated>obf <!obfuscated>plain</!obfuscated> obf</obfuscated>\
            <red>red</red><reset>reset text \
            <click:run_command:'/spawn'>run command</click>\
            <click:suggest_command:'/msg Steve'>suggest command</click>\
            <click:open_url:'https://example.com'>open url</click>\
            <click:copy_to_clipboard:'copied value'>copy to clipboard</click>\
            <click:change_page:'2'>change page</click><insert:'shift click'>insertion</insert>\
            <hover:show_text:'<gray>A gray hint with <bold>nested style</bold></gray>'>hover text</hover>\
            <hover:show_item:'minecraft:diamond_sword':1>hover item</hover>\
            <hover:show_entity:'minecraft:zombie':00000000-0000-0000-0000-000000000000:'Zombie'>\
            hover entity</hover>\
            <gradient:#ff0000:#00ff00:#0000ff>a gradient long enough to be split into many colours\
            </gradient>\
            <gradient:red:gold:blue:white>another gradient with named colours and even more text</gradient>\
            <rainbow>a rainbow long enough to be split into many coloured components</rainbow>\
            <pride:trans>pride gradient with a translation flag</pride>\
            <shadow:#000000aa>shadow with hex</shadow>\
            <shadow:red:0.4>shadow with named colour and alpha</shadow>\
            <key:key.jump/><key:key.sneak/>\
            <lang:chat.type.text:Steve:hello/><lang_or:custom.key:'Fallback text':argument/>\
            <sprite:items:item/diamond/><head:Steve/><head/>\
            <font:minecraft:uniform>uniform font</font>\
            <transition:#ff0000:#0000ff:0.5>transition text</transition>\
            <selector:@a:', '><score:Steve:kills><nbt:block:0 64 0:Items:true>\
            <gradient:0.5>gradient phase only</gradient>\
            <colour:grey>british colour</colour>\
            <click:run_command:'/spawn'>closed with args</click:run_command>\
            <newline/><br/>
            """.repeat(2);

    /**
     * Same kinds of tags as {@link #SAMPLE}, but with a value that is not known at compile time inside
     * the gradients and wrappers, so their results cannot be cached across renders.
     */
    private static final @NotNull String DYNAMIC_SAMPLE = """
            <gradient:#ff0000:#00ff00:#0000ff>gradient with colours and <dyn/></gradient>\
            <gradient:red:gold:blue:white>another gradient with named colours, <dyn/> and even more text</gradient>\
            <rainbow>a rainbow long enough to be split into many coloured components and <dyn/></rainbow>\
            <transition:red:blue:0.5>transition with <dyn/></transition>\
            <selector:@a:'<dyn/>'>\
            <font:minecraft:uniform>font with <dyn/></font>\
            <bold>bold <dyn/> bold again</bold><red>red <dyn/> red again</red>\
            <click:run_command:'/spawn'>click with <dyn/> inside</click>\
            <insert:'shift click'>insert with <dyn/> inside</insert>\
            <shadow:#000000aa>shadow with <dyn/> inside</shadow>\
            <hover:show_text:'<gray>A gray hint with <bold>nested style</bold></gray>'>hover with <dyn/> inside</hover>\
            """.repeat(2);

    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler compiler;
    private @NotNull PreComponentTree tree;
    private @NotNull PreComponentTree dynamicTree;

    @Setup
    public void setup() {
        miniMessage = MiniMessage.builder().strict(false).build();
        compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        tree = compiler.compile(SAMPLE);

        final ResolverRegistry dynamicRegistry = ResolverRegistry.withDefaults().register(new DynamicResolver());
        dynamicTree = new MiniMessageCompiler(dynamicRegistry).compile(DYNAMIC_SAMPLE);
    }

    private static final class DynamicResolver implements ComponentResolver {

        @Override
        public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
            final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
            final @NotNull MiniMessageCompiler compiler) {
            return new DynamicPreComponent();
        }

        @Override
        public @NotNull String @NotNull [] names() {
            return new String[]{"dyn"};
        }

        @Override
        public boolean isSelfClosing() {
            return true;
        }
    }

    private static final class DynamicPreComponent extends PreComponent {

        private static final @NotNull Component VALUE = Component.text("dynamic");

        @Override
        public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
            return VALUE;
        }

        @Override
        public @NotNull PreComponent deepCopy() {
            return new DynamicPreComponent();
        }
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
    public @NotNull Component miniPreCompilerRenderDynamic() {
        return dynamicTree.render(NOBODY);
    }

    @Benchmark
    public @NotNull Component miniPreCompilerCompileAndRender() {
        return compiler.compile(SAMPLE).render(NOBODY);
    }
}
