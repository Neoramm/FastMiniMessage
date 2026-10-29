package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.TagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnparsedPreComponent extends DeferredTagPreComponent {

    private final @NotNull String unparsedTag;
    private final @Nullable String fallback;
    private volatile @Nullable PreComponent parsedComponent;

    public UnparsedPreComponent(final @NotNull String unparsedTag) {
        this(unparsedTag, null);
    }

    public UnparsedPreComponent(final @NotNull String unparsedTag, final @Nullable String fallback) {
        super(unparsedTag, "", PreComponent.EMPTY_CHILDREN, Style.empty(), false, true, false, null);
        this.unparsedTag = unparsedTag;
        this.fallback = fallback;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final PreComponent parsed = parsedComponent;
        if (parsed != null) {
            final Component rendered = parsed.render(pointered, scope);
            final Style style = computedStyle;
            if (style == null || style.isEmpty()) return rendered;
            return rendered.style(style.merge(rendered.style()));
        }
        final TagResolver resolver = scope.resolveTag(unparsedTag);
        if (resolver == null && fallback != null) {
            final Style style = computedStyle;
            return Component.text(fallback, style != null ? style : Style.empty());
        }
        return super.render(pointered, scope);
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final UnparsedPreComponent copy = new UnparsedPreComponent(unparsedTag, fallback);
        copy.parsedComponent = parsedComponent;
        copy.setStyle(computedStyle);
        return copy;
    }

    public @NotNull String getUnparsedTag() {
        return unparsedTag;
    }

    public void setParsedComponent(final @Nullable PreComponent parsedComponent) {
        this.parsedComponent = parsedComponent;
    }
}
