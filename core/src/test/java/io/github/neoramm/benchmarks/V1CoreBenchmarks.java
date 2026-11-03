package io.github.neoramm.benchmarks;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.TagResolver;
import io.github.neoramm.fastminimessage.translation.FastMiniMessageTranslationStore;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class V1CoreBenchmarks {

    private static final @NotNull Pointered NOBODY = new Pointered() {
    };

    private V1CoreBenchmarks() {
    }

    static final class CompiledSample {

        private final @NotNull MiniMessageCompiler compiler;
        private final @NotNull String sample;
        private final @NotNull PreComponentTree tree;

        CompiledSample(final @NotNull String sample) {
            this.compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
            this.sample = sample;
            this.tree = compiler.compile(sample);
        }

        @NotNull Component render() {
            return tree.render(NOBODY);
        }

        @NotNull Component compileAndRender() {
            return compiler.compile(sample).render(NOBODY);
        }

        @NotNull Component render(final TagResolver @NotNull ... resolvers) {
            return tree.render(NOBODY, resolvers);
        }

        @NotNull Component compileAndRender(final TagResolver @NotNull ... resolvers) {
            return compiler.compile(sample).render(NOBODY, resolvers);
        }
    }

    static final class Render {

        private final @NotNull CompiledSample sample;
        private final @NotNull PreComponentTree dynamicTree;

        Render(final @NotNull String sample, final @NotNull String dynamicSample) {
            this.sample = new CompiledSample(sample);
            final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults().register(new DynamicResolver());
            this.dynamicTree = new MiniMessageCompiler(registry).compile(dynamicSample);
        }

        @NotNull Component render() {
            return sample.render();
        }

        @NotNull Component renderDynamic() {
            return dynamicTree.render(NOBODY);
        }

        @NotNull Component compileAndRender() {
            return sample.compileAndRender();
        }
    }

    static final class SimpleCommon {

        private final @NotNull CompiledSample sample;
        private final @NotNull CompiledSample tags;
        private final @NotNull CompiledSample unclosedChat;
        private final TagResolver @NotNull [] nativeAll;
        private final TagResolver @NotNull [] chatResolvers;
        private final @NotNull RenderScope chatScope;

        SimpleCommon(final @NotNull String sample, final @NotNull String tagsSample,
            final @NotNull String unclosedChatSample) {
            this.sample = new CompiledSample(sample);
            this.tags = new CompiledSample(tagsSample);
            this.unclosedChat = new CompiledSample(unclosedChatSample);
            this.nativeAll = new TagResolver[]{
                TagResolver.inserting("name", "Hector"),
                TagResolver.inserting("rank", "Admin"),
                TagResolver.dynamic("upper", (args, ctx) -> Component.text(args.getFirst().toUpperCase())),
                TagResolver.modifying("tint", (current, ctx) -> current.color(NamedTextColor.AQUA))
            };
            this.chatResolvers = new TagResolver[]{
                TagResolver.resolver("prefix", Component.text("[VIP]", NamedTextColor.GOLD)),
                TagResolver.inserting("name", "Hector"),
                TagResolver.inserting("colon", ":"),
                TagResolver.inserting("message", "Welcome to the server!")
            };
            this.chatScope = RenderScope.of(chatResolvers);
        }

        @NotNull Component render() {
            return sample.render();
        }

        @NotNull Component compileAndRender() {
            return sample.compileAndRender();
        }

        @NotNull Component renderNativeTags() {
            return tags.render(nativeAll);
        }

        @NotNull Component compileAndRenderNativeTags() {
            return tags.compileAndRender(nativeAll);
        }

        @NotNull Component renderUnclosedChat() {
            return unclosedChat.render(chatResolvers);
        }

        @NotNull Component renderUnclosedChatReusedScope() {
            return unclosedChat.tree.render(NOBODY, chatScope);
        }
    }

    static final class ComplexLong {

        private final @NotNull CompiledSample sample;
        private final @NotNull CompiledSample tags;
        private final TagResolver @NotNull [] nativeAll;

        ComplexLong(final @NotNull String sample, final @NotNull String tagsSample) {
            this.sample = new CompiledSample(sample);
            this.tags = new CompiledSample(tagsSample);
            this.nativeAll = new TagResolver[]{
                TagResolver.inserting("name", "Hector"),
                TagResolver.inserting("rank", "Admin"),
                TagResolver.dynamic("upper", (args, ctx) -> Component.text(args.getFirst().toUpperCase())),
                TagResolver.modifying("tint", (current, ctx) -> current.color(NamedTextColor.AQUA))
            };
        }

        @NotNull Component render() {
            return sample.render();
        }

        @NotNull Component compileAndRender() {
            return sample.compileAndRender();
        }

        @NotNull Component renderNativeTags() {
            return tags.render(nativeAll);
        }

        @NotNull Component compileAndRenderNativeTags() {
            return tags.compileAndRender(nativeAll);
        }
    }

    static final class Cache {

        private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        private final @NotNull PreComponentTree tree =
            compiler.compile("<gradient:red:blue>Welcome to the server!</gradient>");
        private final @NotNull RenderScope scope = RenderScope.of(Placeholder.ofUnparsed("name", "Alex"));

        @NotNull Component withoutScope() {
            return tree.render();
        }

        @NotNull Component unusedScope() {
            return tree.render(null, scope);
        }

        @NotNull Component inspectCachedTree() {
            tree.inspect((final var snapshot) -> {
            });
            return tree.render();
        }

        @NotNull Component visitCachedTree() {
            tree.visit((final var node) -> {
            });
            return tree.render();
        }
    }

    static final class Localization {

        private final @NotNull FastMiniMessageTranslationStore store =
            FastMiniMessageTranslationStore.create(Key.key("benchmark:v1"));
        private final @NotNull TranslatableComponent input =
            Component.translatable("welcome", Component.text("Alex"), Component.text("Monday"));

        Localization(final @NotNull String template) {
            store.register("welcome", Locale.US, template);
        }

        @Nullable Component translate() {
            return store.translate(input, Locale.US);
        }
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
}
