package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.RuntimeTagContext;
import io.github.neoramm.fastminimessage.model.RuntimeTagResolver;
import io.github.neoramm.fastminimessage.model.TagResolver;
import io.github.neoramm.fastminimessage.model.TagResolver.DynamicTag;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeferredTagPreComponent extends PreComponent {

    private final @NotNull String tagName;
    private final @NotNull String rawArgsTemplate;
    private final @Nullable Arguments staticArgs;
    private final boolean hasArguments;
    private final boolean selfClosing;
    private final boolean explicitlyClosed;
    private final @Nullable MiniMessageCompiler compiler;
    private volatile @Nullable CachedResolution cachedResolution;

    public DeferredTagPreComponent(
        final @NotNull String tagName,
        final @NotNull String rawArgsTemplate,
        final PreComponent @NotNull [] children,
        final @Nullable Style inheritedStyle,
        final boolean hasArguments,
        final boolean selfClosing,
        final boolean explicitlyClosed,
        final @Nullable MiniMessageCompiler compiler
    ) {
        this.tagName = tagName;
        this.rawArgsTemplate = rawArgsTemplate;
        this.staticArgs = rawArgsTemplate.isEmpty()
            ? Arguments.EMPTY
            : (rawArgsTemplate.indexOf('<') == -1 ? Arguments.parse(rawArgsTemplate) : null);
        this.hasArguments = hasArguments;
        this.selfClosing = selfClosing;
        this.explicitlyClosed = explicitlyClosed;
        this.compiler = compiler;
        this.children = children;
        this.computedStyle = inheritedStyle;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public boolean isExplicitlyClosed() {
        return explicitlyClosed;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TagResolver resolver = scope.resolveTag(tagName);
        if (resolver != null) {
            if (resolver.isModifying()) {
                final ComponentModifier modifier = resolver.modifier();
                if (modifier != null) {
                    final Component childComponent = renderChildren(pointered, scope);
                    final Component modified = modifier.modify(childComponent, pointered);
                    final Style style = computedStyle;
                    if (style == null || style.isEmpty()) return modified;
                    return modified.style(style.merge(modified.style()));
                }
            }
            final PreComponent pre = resolver.preComponent();
            if (pre != null) {
                final Component rendered = pre.render(pointered, scope);
                final Style style = computedStyle;
                final Component styled = style == null || style.isEmpty()
                    ? rendered
                    : rendered.style(style.merge(rendered.style()));
                final PreComponent @Nullable [] kids = children;
                if (kids == null || kids.length == 0) return styled;
                final TextComponent.Builder builder = Component.text();
                if (style != null && !style.isEmpty()) builder.style(style);
                builder.append(styled);
                renderChildrenInto(pointered, builder, scope);
                return builder.build();
            }
            final DynamicTag dynamic = resolver.dynamicTag();
            if (dynamic != null) {
                final Arguments args = staticArgs != null ? staticArgs.copy() : Arguments.parse(resolveArgs(scope, pointered));
                final Component resolved = dynamic.resolve(args, pointered);
                if (resolved != null) {
                    final Style style = computedStyle;
                    final Component styled = style == null || style.isEmpty()
                        ? resolved
                        : resolved.style(style.merge(resolved.style()));
                    final PreComponent @Nullable [] kids = children;
                    if (kids == null || kids.length == 0) return styled;
                    final TextComponent.Builder builder = Component.text();
                    if (style != null && !style.isEmpty()) builder.style(style);
                    builder.append(styled);
                    renderChildrenInto(pointered, builder, scope);
                    return builder.build();
                }
            }
            final RuntimeTagResolver custom = resolver.customResolver();
            if (custom != null && compiler != null) {
                final String substituted = resolveArgs(scope, pointered);
                final RuntimeTagContext context = new RuntimeTagContext(tagName, substituted, getChildren(),
                    hasArguments, selfClosing, explicitlyClosed, pointered, scope, compiler);
                final Component resolved = custom.render(context);
                if (resolved != null) {
                    final Style style = computedStyle;
                    if (style == null || style.isEmpty()) return resolved;
                    return resolved.style(style.merge(resolved.style()));
                }
            }
        }
        if (pointered != null && ("player".equalsIgnoreCase(tagName) || "name".equalsIgnoreCase(tagName))) {
            final java.util.Optional<String> name = pointered.get(Identity.NAME);
            if (name.isPresent()) {
                final Style style = computedStyle;
                final Component component = Component.text(name.get());
                final Component styled = style == null || style.isEmpty() ? component : component.style(style);
                final PreComponent @Nullable [] kids = children;
                if (kids == null || kids.length == 0) return styled;
                final TextComponent.Builder builder = Component.text();
                if (style != null && !style.isEmpty()) builder.style(style);
                builder.append(styled);
                renderChildrenInto(pointered, builder, scope);
                return builder.build();
            }
        }
        final String substituted = resolveArgs(scope, pointered);
        if (compiler != null && compiler.findResolver(tagName) != null) {
            final CachedResolution cache = this.cachedResolution;
            final PreComponent resolved;
            if (cache != null && cache.args().equals(substituted)) {
                resolved = cache.resolved();
            } else {
                resolved = compiler.resolveTag(tagName, substituted, getChildren(), computedStyle, hasArguments,
                    selfClosing, explicitlyClosed);
                this.cachedResolution = new CachedResolution(substituted, resolved);
            }
            return resolved.render(pointered, scope);
        }
        final Style style = computedStyle != null ? computedStyle : Style.empty();
        final String open = openTag(tagName, substituted, hasArguments, selfClosing);
        final PreComponent @Nullable [] kids = children;
        final boolean closing = explicitlyClosed && !selfClosing;
        if ((kids == null || kids.length == 0) && !closing) return Component.text(open, style);
        final TextComponent.Builder builder = Component.text();
        if (!style.isEmpty()) builder.style(style);
        builder.append(Component.text(open));
        renderChildrenInto(pointered, builder, scope);
        if (closing) builder.append(Component.text("</" + tagName + ">"));
        return builder.build();
    }

    @Override
    public void renderInto(final @Nullable Pointered pointered, final @NotNull TextComponent.Builder builder,
        final @NotNull RenderScope scope) {
        final TagResolver resolver = scope.resolveTag(tagName);
        if (resolver != null) {
            if (resolver.isModifying() || resolver.customResolver() != null) {
                builder.append(render(pointered, scope));
                return;
            }
            final PreComponent pre = resolver.preComponent();
            if (pre != null) {
                final Component rendered = pre.render(pointered, scope);
                final Style style = computedStyle;
                final Component styled = style == null || style.isEmpty()
                    ? rendered
                    : rendered.style(style.merge(rendered.style()));
                builder.append(styled);
                renderChildrenInto(pointered, builder, scope);
                return;
            }
            final DynamicTag dynamic = resolver.dynamicTag();
            if (dynamic != null) {
                final Arguments args = staticArgs != null ? staticArgs.copy() : Arguments.parse(resolveArgs(scope, pointered));
                final Component resolved = dynamic.resolve(args, pointered);
                if (resolved != null) {
                    final Style style = computedStyle;
                    final Component styled = style == null || style.isEmpty()
                        ? resolved
                        : resolved.style(style.merge(resolved.style()));
                    builder.append(styled);
                    renderChildrenInto(pointered, builder, scope);
                    return;
                }
            }
        }
        if (pointered != null && ("player".equalsIgnoreCase(tagName) || "name".equalsIgnoreCase(tagName))) {
            final java.util.Optional<String> name = pointered.get(Identity.NAME);
            if (name.isPresent()) {
                final Style style = computedStyle;
                final Component component = Component.text(name.get());
                final Component styled = style == null || style.isEmpty() ? component : component.style(style);
                builder.append(styled);
                renderChildrenInto(pointered, builder, scope);
                return;
            }
        }
        if (compiler != null && compiler.findResolver(tagName) != null) {
            final String substituted = resolveArgs(scope, pointered);
            final CachedResolution cache = this.cachedResolution;
            final PreComponent resolved;
            if (cache != null && cache.args().equals(substituted)) {
                resolved = cache.resolved();
            } else {
                resolved = compiler.resolveTag(tagName, substituted, getChildren(), computedStyle, hasArguments,
                    selfClosing, explicitlyClosed);
                this.cachedResolution = new CachedResolution(substituted, resolved);
            }
            resolved.renderInto(pointered, builder, scope);
            return;
        }
        builder.append(render(pointered, scope));
    }

    private @NotNull String resolveArgs(final @NotNull RenderScope scope, final @Nullable Pointered pointered) {
        if (rawArgsTemplate.isEmpty() || rawArgsTemplate.indexOf('<') == -1) return rawArgsTemplate;
        return substitute(rawArgsTemplate, scope, pointered);
    }

    private @NotNull Component renderChildren(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final PreComponent @Nullable [] kids = children;
        if (kids == null || kids.length == 0) return Component.empty();
        if (kids.length == 1) return kids[0].render(pointered, scope);
        final TextComponent.Builder builder = Component.text();
        renderChildrenInto(pointered, builder, scope);
        return builder.build();
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final DeferredTagPreComponent copy = new DeferredTagPreComponent(tagName, rawArgsTemplate,
            copyNonNullChildren(), computedStyle, hasArguments, selfClosing, explicitlyClosed, compiler);
        copy.cachedResolution = this.cachedResolution;
        return copy;
    }

    private static @NotNull String openTag(final @NotNull String name, final @NotNull String rawArgs,
        final boolean hasArguments, final boolean selfClosing) {
        if (name.startsWith("<") && name.endsWith(">")) return name;
        final String colonArgs = hasArguments || !rawArgs.isEmpty() ? ":" + rawArgs : "";
        return selfClosing ? "<" + name + colonArgs + "/>" : "<" + name + colonArgs + ">";
    }

    private record CachedResolution(@NotNull String args, @NotNull PreComponent resolved) {
    }

    private static @NotNull String substitute(
        final @NotNull String template, final @NotNull RenderScope scope, final @Nullable Pointered pointered
    ) {
        if (template.indexOf('<') == -1) return template;
        final StringBuilder builder = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            final int open = template.indexOf('<', i);
            if (open == -1) {
                builder.append(template, i, template.length());
                break;
            }
            builder.append(template, i, open);
            final int close = template.indexOf('>', open);
            if (close == -1) {
                builder.append(template, open, template.length());
                break;
            }
            final String candidate = template.substring(open + 1, close);
            final TagResolver resolver = scope.resolveTag(candidate);
            if (resolver != null) {
                final PreComponent pre = resolver.preComponent();
                if (pre != null) {
                    final Component rendered = pre.render(pointered, scope);
                    builder.append(plainText(rendered));
                    i = close + 1;
                    continue;
                }
                final DynamicTag dynamic = resolver.dynamicTag();
                if (dynamic != null) {
                    final Component rendered = dynamic.resolve(Arguments.EMPTY, pointered);
                    if (rendered != null) {
                        builder.append(plainText(rendered));
                        i = close + 1;
                        continue;
                    }
                }
            }
            if (pointered != null && ("player".equalsIgnoreCase(candidate) || "name".equalsIgnoreCase(candidate))) {
                final java.util.Optional<String> name = pointered.get(Identity.NAME);
                if (name.isPresent()) {
                    builder.append(name.get());
                    i = close + 1;
                    continue;
                }
            }
            builder.append(template, open, close + 1);
            i = close + 1;
        }
        return builder.toString();
    }

    private static @NotNull String plainText(final @NotNull Component component) {
        final StringBuilder builder = new StringBuilder();
        appendPlain(builder, component);
        return builder.toString();
    }

    private static void appendPlain(final @NotNull StringBuilder builder, final @NotNull Component component) {
        if (component instanceof final TextComponent text) builder.append(text.content());
        for (final Component child : component.children()) appendPlain(builder, child);
    }
}
