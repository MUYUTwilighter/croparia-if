package cool.muyucloud.croparia.api.element;

import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public interface ElementAccess {
    Codec<Element> CODEC = Codec.STRING.xmap(name -> Element.valueOf(name.toUpperCase()), Element::getSerializedName);
    @SuppressWarnings("unused")
    StreamCodec<RegistryFriendlyByteBuf, Element> STREAM_CODEC = CodecUtil.toStream(CODEC);

    @NonNull
    Element getElement();

    default Element assertEmpty(@NonNull Element element) {
        if (element != Element.EMPTY) return element;
        throw new IllegalArgumentException("Element cannot be empty");
    }
}
