package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the precompiled tree renders the same expression as MiniMessage for every tag whose
 * semantics both implementations share, independently of the component structure.
 *
 * <p>Unknown tags and unmatched closing tags are excluded because the tag scanner renders them
 * differently from MiniMessage.</p>
 */
final class MiniMessageEquivalenceTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull MiniMessage miniMessage = MiniMessage.builder().strict(false).build();

    @Test
    void rendersTheSameExpressionAsMiniMessage() {
        final String[] samples = {
            "plain",
            "<red>a</red><blue>b</blue>",
            "<red><blue>a</blue>b</red>",
            "<bold>a <red>b</red> c</bold>",
            "<bold><red><click:run_command:'/spawn'>x</click></red></bold>",
            "<gradient:red:blue>abc</gradient>",
            "<red>a<reset>b</reset>",
            "<bold><red>a<reset>b</reset>",
            "<reset>abc",
            "<red>a<reset/>b",
            "<red>a</red><reset>b<bold>c</bold>",
            "<bold><gradient:red:blue>abc</gradient></bold>",
            "<gradient:red:blue><bold>a</bold>b</gradient>",
            "<red><click:run_command:'/spawn'>a <bold>b</bold></click></red>",
            "<insert:'x'>a <underlined>b</underlined></insert>",
            "<shadow:red:0.4>a <bold>b</bold></shadow>",
            "<hover:show_text:'<gray>hint <bold>x</bold></gray>'>a <red>b</red></hover>",
            "<hover:show_item:'minecraft:diamond':1>a</hover>",
            "<red><hover:show_item:'minecraft:diamond':1>a</hover></red>",
            "<hover:show_entity:'minecraft:zombie':00000000-0000-0000-0000-000000000000:'Zombie'>a</hover>",
            "<bold><hover:show_entity:'minecraft:zombie':00000000-0000-0000-0000-000000000000:"
                + "'<red>Zombie</red>'>a</hover></bold>",
            "<red><hover:show_text:'hint'>a <bold>b</bold></hover></red>",
            "<key:key.jump/>",
            "<red><key:key.jump/></red>",
            "<lang:chat.type.text:Steve:hello/>",
            "<lang:chat.type.text:'<red>Steve</red>':hello/>",
            "<sprite:items:item/diamond/>",
            "<head:Steve/>",
            "<gold><head:Steve/></gold>",
            "<rainbow>abc</rainbow>",
            "<rainbow:5>abc</rainbow>",
            "<rainbow:0>abc</rainbow>",
            "<rainbow:!5>abc</rainbow>",
            "<bold>a</bold><bold>b</bold>",
            "<red>a</red><red>b</red>",
            "<red>a</red>b<red>c</red>",
            "<click:run_command:'/a'>a</click><click:run_command:'/a'>b</click>",
            "<bold><red>a</red><red>b</red></bold>",
            "<hover:show_text:'<red>x</red>'><bold>a</bold></hover>",
            "<click:run_command:'/x'><hover:show_text:'h'>a <red>b</red></hover></click>",
            "<hover:show_text:'h'><click:run_command:'/x'>a</click></hover>",
            "<click:run_command:'/x'><gradient:red:blue>abc</gradient></click>",
            "<gradient:red:blue><click:run_command:'/x'>abc</click></gradient>",
            "<bold><italic><underlined><strikethrough><obfuscated>a</obfuscated>"
                + "</strikethrough></underlined></italic></bold>",
            "<bold>a<!bold>b</!bold>c</bold>",
            "<red>a<blue>b</blue>c</red>",
            "<shadow:red:0.4><bold>a</bold><red>b</red></shadow>",
            "<insert:'x'><red>a</red></insert>",
            "<red><lang:chat.type.text:Steve:hello/></red>",
            "<red><sprite:items:item/diamond/></red>",
            "<red><head:Steve/></red>",
            "<gradient:red:blue>a <red>b</red> c</gradient>",
            "<red><gradient:red:blue>abc</gradient></red>",
            "<gradient:red:blue><gradient:green:yellow>abc</gradient>def</gradient>",
            "<gradient:red:blue><red>a</red>b</gradient>",
            "<gradient:red:blue><rainbow>abc</rainbow>def</gradient>",
            "<rainbow><gradient:red:blue>abc</gradient>def</rainbow>",
            "<newline/>",
            "<bold>a<br/>b</bold>",
            "<red>😀a</red>",
            "<gradient:red:blue>😀a</gradient>",
        };
        final List<String> failures = new ArrayList<>();
        for (final String sample : samples) {
            final String failure = compare(sample);
            if (failure != null) failures.add(failure);
        }
        assertEquals(List.of(), failures, "expression differences");
    }

    private @Nullable String compare(final @NotNull String miniMessageSource) {
        final Component reference = miniMessage.deserialize(miniMessageSource);
        final PreComponentTree tree = compiler.compile(miniMessageSource);
        final Component actual = tree.render(NOBODY);
        final List<String> expected = signature(reference);
        final List<String> found = signature(actual);
        if (expected.equals(found)) return null;
        final int size = Math.min(expected.size(), found.size());
        int index = 0;
        while (index < size && expected.get(index).equals(found.get(index))) index++;
        final String expectedToken = index < expected.size() ? expected.get(index) : "<end>";
        final String foundToken = index < found.size() ? found.get(index) : "<end>";
        return "expression differs for: " + miniMessageSource
            + "\n  first difference at " + index
            + "\n  miniMessage: " + expectedToken
            + "\n  precompiler: " + foundToken
            + "\n  sizes: " + expected.size() + " vs " + found.size();
    }

    private static @NotNull List<String> signature(final @NotNull Component component) {
        final List<String> out = new ArrayList<>();
        append(component, Style.empty(), out);
        return out;
    }

    private static void append(final @NotNull Component component, final @NotNull Style inherited,
        final @NotNull List<String> out) {
        final Style style = inherited.merge(component.style());
        if (component instanceof TextComponent text) {
            final String suffix = styleSignature(style);
            for (int i = 0; i < text.content().length(); i++) out.add("'" + text.content().charAt(i) + "'" + suffix);
        } else if (component instanceof TranslatableComponent translatable) {
            final StringBuilder args = new StringBuilder();
            for (final TranslationArgument argument : translatable.arguments()) {
                args.append(signature(argument.asComponent())).append(';');
            }
            out.add("<translatable:" + translatable.key() + ':' + translatable.fallback() + ':' + args + '>'
                + styleSignature(style));
        } else if (component instanceof KeybindComponent keybind) {
            out.add("<keybind:" + keybind.keybind() + '>' + styleSignature(style));
        } else if (component instanceof ObjectComponent object) {
            out.add("<object:" + object.contents() + '>' + styleSignature(style));
        } else {
            out.add("<" + component.getClass().getSimpleName() + '>' + styleSignature(style));
        }
        for (final Component child : component.children()) append(child, style, out);
    }

    private static @NotNull String styleSignature(final @NotNull Style style) {
        final StringBuilder out = new StringBuilder(96);
        out.append(" color=").append(color(style.color()));
        for (final TextDecoration decoration : TextDecoration.values()) {
            out.append(' ').append(decoration).append('=').append(style.decoration(decoration));
        }
        out.append(" insert=").append(style.insertion());
        out.append(" click=").append(click(style.clickEvent()));
        out.append(" hover=").append(hover(style.hoverEvent()));
        out.append(" font=").append(style.font());
        out.append(" shadow=").append(shadow(style.shadowColor()));
        return out.toString();
    }

    private static @NotNull String color(final @Nullable TextColor color) {
        return color == null ? "-" : color.asHexString();
    }

    private static @NotNull String shadow(final @Nullable ShadowColor color) {
        return color == null ? "-" : color.asHexString();
    }

    private static @NotNull String click(final @Nullable ClickEvent event) {
        if (event == null) return "-";
        final ClickEvent.Payload payload = event.payload();
        final String value;
        if (payload instanceof ClickEvent.Payload.Text text) value = text.value();
        else if (payload instanceof ClickEvent.Payload.Int integer) value = Integer.toString(integer.integer());
        else value = String.valueOf(payload);
        return event.action().name() + ':' + value;
    }

    private static @NotNull String hover(final @Nullable HoverEvent<?> event) {
        if (event == null) return "-";
        final String action = String.valueOf(event.action());
        if (event.action() == HoverEvent.Action.SHOW_TEXT) return action + ':' + signature((Component) event.value());
        if (event.action() == HoverEvent.Action.SHOW_ITEM) {
            final HoverEvent.ShowItem item = (HoverEvent.ShowItem) event.value();
            return action + ':' + item.item() + ':' + item.count() + ':' + item.dataComponents();
        }
        if (event.action() == HoverEvent.Action.SHOW_ENTITY) {
            final HoverEvent.ShowEntity entity = (HoverEvent.ShowEntity) event.value();
            return action + ':' + entity.type() + ':' + entity.id() + ':'
                + (entity.name() == null ? "-" : signature(entity.name()));
        }
        return action + ':' + event.value();
    }
}
