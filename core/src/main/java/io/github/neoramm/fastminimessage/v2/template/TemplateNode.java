package io.github.neoramm.fastminimessage.v2.template;

import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.TagArgs;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

public sealed interface TemplateNode {
    @NotNull Component render(final @NotNull RenderScope context);

    default void renderInto(final @NotNull RenderScope context, final TextComponent.@NotNull Builder output) {
        output.append(render(context));
    }

    record Static(@NotNull Component component) implements TemplateNode {
        public Static {
            Objects.requireNonNull(component, "component");
        }

        @Override
        public @NotNull Component render(final @NotNull RenderScope context) {
            return component;
        }
    }

    record DynamicLeaf(@NotNull Style style,
        @NotNull Function<@NotNull RenderScope, @NotNull Component> renderer) implements TemplateNode {
        public DynamicLeaf {
            Objects.requireNonNull(style, "style");
            Objects.requireNonNull(renderer, "renderer");
        }

        @Override
        public @NotNull Component render(final @NotNull RenderScope context) {
            return RenderScope.applyStyle(renderer.apply(context), style);
        }
    }

    record RegisteredPlaceholder(@NotNull String key, @NotNull Style inheritedStyle,
        @NotNull List<@NotNull TemplateNode> children,
        @NotNull Function<@NotNull RenderScope, @NotNull Component> renderer) implements TemplateNode {
        public RegisteredPlaceholder {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(inheritedStyle, "inheritedStyle");
            children = List.copyOf(children);
            Objects.requireNonNull(renderer, "renderer");
        }

        @Override
        public @NotNull Component render(final @NotNull RenderScope context) {
            final @NotNull Component value = RenderScope.applyStyle(
                Objects.requireNonNull(renderer.apply(context), "placeholder result"), inheritedStyle);
            if (children.isEmpty()) return value;
            final TextComponent.@NotNull Builder builder = Component.text().style(inheritedStyle).append(value);
            for (int i = 0, size = children.size(); i < size; i++) children.get(i).renderInto(context, builder);
            return builder.build();
        }

        @Override
        public void renderInto(final @NotNull RenderScope context, final TextComponent.@NotNull Builder output) {
            if (!inheritedStyle.isEmpty() && !children.isEmpty()) {
                output.append(render(context));
                return;
            }
            output.append(RenderScope.applyStyle(
                Objects.requireNonNull(renderer.apply(context), "placeholder result"), inheritedStyle));
            for (int i = 0, size = children.size(); i < size; i++) children.get(i).renderInto(context, output);
        }
    }

    record DynamicTransform(@NotNull Style baseStyle, @NotNull List<@NotNull TemplateNode> children,
        @NotNull TreeTransformer transformer) implements TemplateNode {
        public DynamicTransform {
            Objects.requireNonNull(baseStyle, "baseStyle");
            children = List.copyOf(children);
            Objects.requireNonNull(transformer, "transformer");
        }

        @Override
        public @NotNull Component render(final @NotNull RenderScope context) {
            return transformer.transform(FoldPass.render(children, context), baseStyle, context);
        }

        @Override
        public void renderInto(final @NotNull RenderScope context, final TextComponent.@NotNull Builder output) {
            if (transformer != FoldPass.IDENTITY || !baseStyle.isEmpty()) {
                output.append(render(context));
                return;
            }
            for (int i = 0, size = children.size(); i < size; i++) children.get(i).renderInto(context, output);
        }
    }

    record Slot(@NotNull String key, @NotNull ValueSource<TagArgs.@NotNull Values> args,
        @NotNull Style inheritedStyle, @NotNull List<@NotNull TemplateNode> children,
        @NotNull TagSource source) implements TemplateNode {
        public Slot {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(args, "args");
            Objects.requireNonNull(inheritedStyle, "inheritedStyle");
            children = List.copyOf(children);
            Objects.requireNonNull(source, "source");
        }

        @Override
        public @NotNull Component render(final @NotNull RenderScope context) {
            return context.resolveSlot(this);
        }

        @Override
        public void renderInto(final @NotNull RenderScope context, final TextComponent.@NotNull Builder output) {
            context.renderSlotInto(this, output);
        }
    }
}
