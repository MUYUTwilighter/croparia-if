package cool.muyucloud.croparia.api.core.component;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public record Text(List<Component> lines) implements TooltipProvider {
    public Text() {
        this(new ArrayList<>());
    }

    public Text(Component line) {
        this(new ArrayList<>(List.of(line)));
    }

    public Text(List<Component> lines) {
        this.lines = new ArrayList<>(lines);
    }

    public static final Codec<Text> CODEC = Codec.either(ComponentSerialization.CODEC, ComponentSerialization.CODEC.listOf()).xmap(
        encoded -> encoded.map(Text::new, Text::new),
        text -> text.lines.size() == 1 ? Either.left(text.lines.getFirst()) : Either.right(text.lines)
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag, DataComponentGetter componentGetter) {
        lines.forEach(tooltipAdder);
    }

    public void append(Component text) {
        this.lines.add(text);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Text(List<Component> otherLines))) return false;
        return Objects.equals(this.lines, otherLines);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lines);
    }
}
