package cool.muyucloud.croparia.api.element;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

public interface ElementAccess {
    Codec<Element> CODEC = Codec.STRING.xmap(name -> Element.valueOf(name.toUpperCase()), Element::getSerializedName);

    @NotNull
    Element getElement();

    default Element assertEmpty(@NotNull Element element) {
        if (element != Element.EMPTY) return element;
        throw new IllegalArgumentException("Element cannot be empty");
    }
}
