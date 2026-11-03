package io.github.neoramm.fastminimessage.v2.model;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import java.util.List;
import net.kyori.adventure.pointer.Pointered;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RuntimeTagContext(@NotNull String name, @NotNull Arguments arguments,
    @NotNull List<@NotNull TemplateNode> children, boolean hasArguments, boolean selfClosing,
    boolean explicitlyClosed, @NotNull RenderScope scope,
    @NotNull MiniMessageCompiler compiler) {
    public @Nullable Pointered pointered() {
        return scope.viewer();
    }
}
