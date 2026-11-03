package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(2)
public class PayloadRenderBenchmark {
    private static final @NotNull String CHAT = "<prefix> <name><colon> <message>";
    private static final @NotNull Pointered NOBODY = new Pointered() {
    };
    private static final @NotNull ChatPayload PAYLOAD = new ChatPayload(
        Component.text("[VIP]", NamedTextColor.GOLD), Component.text("Hector"),
        Component.text(":"), Component.text("Welcome to the server!"));
    private final @NotNull PayloadSchema schema;
    private final PayloadSchema.@NotNull Key<@NotNull ChatPayload> chatKey;
    private final @NotNull RenderScope scope;
    private final @NotNull RenderScope alternateScope;
    private final @NotNull MiniMessageCompiler compiler;
    private final @NotNull PreComponentTree chat;
    private final @NotNull PreComponentTree single;
    private final @NotNull V1CoreBenchmarks.SimpleCommon v1 = new V1CoreBenchmarks.SimpleCommon("", "", CHAT);
    private int selection;

    public PayloadRenderBenchmark() {
        final var layout = PayloadSchema.builder();
        chatKey = layout.key("chat", ChatPayload.class);
        schema = layout.build();
        final var registry = ResolverRegistry.withDefaults(schema)
            .registerPlaceholder("prefix", (final var render) -> render.payload(chatKey).prefix())
            .registerPlaceholder("name", (final var render) -> render.payload(chatKey).name())
            .registerPlaceholder("colon", (final var render) -> render.payload(chatKey).colon())
            .registerPlaceholder("message", (final var render) -> render.payload(chatKey).message());
        compiler = new MiniMessageCompiler(registry);
        scope = compiler.scopeBuilder().viewer(NOBODY).payload(chatKey, PAYLOAD).build();
        alternateScope = compiler.scopeBuilder().viewer(NOBODY).payload(chatKey,
            new ChatPayload(PAYLOAD.prefix(), PAYLOAD.name(), PAYLOAD.colon(), Component.text("Another message")))
            .build();
        chat = compiler.compile(CHAT);
        single = compiler.compile("<message>");
    }

    @Benchmark
    public @NotNull Component registeredSingleReusedScope() {
        return single.render(scope);
    }

    @Benchmark
    public @NotNull Component registeredSingleAlternatingScopes() {
        selection ^= 1;
        return single.render(selection == 0 ? scope : alternateScope);
    }

    @Benchmark
    public @NotNull Component registeredChatReusedScope() {
        return chat.render(scope);
    }

    @Benchmark
    public @NotNull Component registeredChatFreshScope() {
        return chat.render(RenderScope.builder(schema).viewer(NOBODY).payload(chatKey, PAYLOAD).build());
    }

    @Benchmark
    public @NotNull Component registeredChatConsumer() {
        return chat.render((final var builder) -> builder.viewer(NOBODY).payload(chatKey, PAYLOAD));
    }

    @Benchmark
    public @NotNull Component registeredChatCompileAndRender() {
        return compiler.compile(CHAT).render(scope);
    }

    @Benchmark
    public @NotNull Component v1Chat() {
        return v1.renderUnclosedChat();
    }

    @Benchmark
    public @NotNull Component v1ChatReusedScope() {
        return v1.renderUnclosedChatReusedScope();
    }

    private record ChatPayload(@NotNull Component prefix, @NotNull Component name,
        @NotNull Component colon, @NotNull Component message) {
    }
}
