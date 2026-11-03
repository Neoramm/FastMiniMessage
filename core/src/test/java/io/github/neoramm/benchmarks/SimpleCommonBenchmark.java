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

    private static final @NotNull String UNCLOSED_CHAT_SAMPLE = "<prefix> <name><colon> <message>";

    private static final @NotNull PayloadSchema TAGS_SCHEMA;
    private static final PayloadSchema.@NotNull Key<@NotNull String> NAME_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull String> RANK_KEY;

    private static final @NotNull PayloadSchema CHAT_SCHEMA;
    private static final PayloadSchema.@NotNull Key<@NotNull Component> PREFIX_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull String> CHAT_NAME_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull String> COLON_KEY;
    private static final PayloadSchema.@NotNull Key<@NotNull String> MESSAGE_KEY;

    static {
        final @NotNull PayloadSchema.Builder tagsBuilder = PayloadSchema.builder();
        NAME_KEY = tagsBuilder.key("name", String.class);
        RANK_KEY = tagsBuilder.key("rank", String.class);
        TAGS_SCHEMA = tagsBuilder.build();

        final @NotNull PayloadSchema.Builder chatBuilder = PayloadSchema.builder();
        PREFIX_KEY = chatBuilder.key("prefix", Component.class);
        CHAT_NAME_KEY = chatBuilder.key("name", String.class);
        COLON_KEY = chatBuilder.key("colon", String.class);
        MESSAGE_KEY = chatBuilder.key("message", String.class);
        CHAT_SCHEMA = chatBuilder.build();
    }

    private static @NotNull ResolverRegistry createTagsRegistry() {
        return ResolverRegistry.withDefaults(TAGS_SCHEMA)
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(NAME_KEY)))
            .registerPlaceholder("rank", (final var scope) -> Component.text(scope.payload(RANK_KEY)))
            .register(new TagSpec.LeafRule(Set.of("upper"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> Component.text(vals.string(0, "").toUpperCase(Locale.ROOT)));
            }))
            .registerModifier("tint", (final var content, final var scope) -> content.color(NamedTextColor.AQUA));
    }

    private static @NotNull ResolverRegistry createPlaceholdersRegistry() {
        return ResolverRegistry.withDefaults(TAGS_SCHEMA)
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(NAME_KEY)))
            .registerPlaceholder("rank", (final var scope) -> Component.text(scope.payload(RANK_KEY)));
    }

    private static @NotNull ResolverRegistry createChatRegistry() {
        return ResolverRegistry.withDefaults(CHAT_SCHEMA)
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(PREFIX_KEY))
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(CHAT_NAME_KEY)))
            .registerPlaceholder("colon", (final var scope) -> Component.text(scope.payload(COLON_KEY)))
            .registerPlaceholder("message", (final var scope) -> Component.text(scope.payload(MESSAGE_KEY)));
    }

    private final @NotNull V1CoreBenchmarks.SimpleCommon v1 =
        new V1CoreBenchmarks.SimpleCommon(SAMPLE, TAGS_SAMPLE, UNCLOSED_CHAT_SAMPLE);
    private final @NotNull V1BridgeBenchmarks.Tags v1Tags =
        new V1BridgeBenchmarks.Tags(TAGS_SAMPLE, PLACEHOLDERS, ALL);
    private @NotNull MiniMessage miniMessage;
    private @NotNull MiniMessageCompiler compiler;
    private @NotNull MiniMessageCompiler tagsCompiler;
    private @NotNull PreComponentTree tree;
    private @NotNull PreComponentTree tagsTree;
    private @NotNull PreComponentTree placeholderTagsTree;
    private @NotNull PreComponentTree unclosedChatTree;
    private @NotNull RenderScope tagsScope;
    private @NotNull RenderScope chatScope;

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

        final @NotNull MiniMessageCompiler chatCompiler = new MiniMessageCompiler(createChatRegistry());
        unclosedChatTree = chatCompiler.compile(UNCLOSED_CHAT_SAMPLE);
        chatScope = RenderScope.builder(CHAT_SCHEMA)
            .viewer(NOBODY)
            .payload(PREFIX_KEY, Component.text("[VIP]", NamedTextColor.GOLD))
            .payload(CHAT_NAME_KEY, "Hector")
            .payload(COLON_KEY, ":")
            .payload(MESSAGE_KEY, "Welcome to the server!")
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
    public @NotNull Component fastRenderUnclosedChat() {
        return unclosedChatTree.render(chatScope);
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

    @Benchmark
    public @NotNull Component v1RenderUnclosedChat() {
        return v1.renderUnclosedChat();
    }
}
