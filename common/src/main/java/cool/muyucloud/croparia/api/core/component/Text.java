package cool.muyucloud.croparia.api.core.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public record Text(List<Component> text) implements TooltipProvider {
    public static final Codec<Text> CODEC = CodecUtil.listOf(ComponentSerialization.CODEC).xmap(
        components -> new Text(new ArrayList<>(components)), Text::text
    );

    public Text() {
        this(new ArrayList<>());
    }

    public Text(Component... components) {
        this(Lists.newArrayList(components));
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        this.text.forEach(tooltipAdder);
    }

    public void append(Component text) {
        this.text.add(text);
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
