package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record CompileContext(@NotNull String name, @NotNull MiniMessageCompiler compiler) {
    public CompileContext {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(compiler, "compiler");
    }
}
