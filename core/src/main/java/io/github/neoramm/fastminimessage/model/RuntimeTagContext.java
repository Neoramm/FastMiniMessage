package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import net.kyori.adventure.pointer.Pointered;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inputs available to a {@link RuntimeTagResolver} when a deferred tag is rendered.
 *
 * @param name the tag name as written in the source
 * @param rawArgs the raw argument text, empty when the tag has no arguments
 * @param children the already compiled children of the tag
 * @param hasArguments whether the source tag included an argument separator
 * @param selfClosing whether the source tag was written in self-closing form
 * @param explicitlyClosed whether the source tag had an explicit closing tag
 * @param pointered the rendering target, usually the viewer
 * @param scope the runtime placeholders and modifiers for this render
 * @param compiler the compiler that owns the tag
 */
public record RuntimeTagContext(
    @NotNull String name,
    @NotNull String rawArgs,
    PreComponent @NotNull [] children,
    boolean hasArguments,
    boolean selfClosing,
    boolean explicitlyClosed,
    @Nullable Pointered pointered,
    @NotNull RenderScope scope,
    @NotNull MiniMessageCompiler compiler
) {
}
