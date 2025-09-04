package cool.muyucloud.croparia.api.core.component;

import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.Objects;
import java.util.function.Consumer;

public record Text(MutableComponent text) implements TooltipProvider {
    public Text() {
        this(Texts.literal(""));
    }

    public static final Codec<Text> CODEC = ComponentSerialization.CODEC.xmap(component -> new Text(Texts.literal("").append(component)), Text::text);

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        tooltipAdder.accept(text);
    }

    public void append(Component text) {
        this.text.append(text);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Text(Component text1))) return false;
        return Objects.equals(this.text, text1);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }
}
