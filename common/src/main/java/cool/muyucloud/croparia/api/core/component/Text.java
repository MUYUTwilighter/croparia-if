package cool.muyucloud.croparia.api.core.component;

import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;

public record Text(MutableComponent text) {
    public Text() {
        this(Texts.literal(""));
    }

    public static final Codec<Text> CODEC = Codec.STRING.xmap(
        json -> new Text(Texts.literal("").append(Component.Serializer.fromJson(json))),
        text -> Component.Serializer.toJson(text.text())
    );

    public void append(Component text) {
        this.text.append(text);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Text other)) return false;
        return Objects.equals(this.text, other.text());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }
}
