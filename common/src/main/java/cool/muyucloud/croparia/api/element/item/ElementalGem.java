package cool.muyucloud.croparia.api.element.item;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ElementalGem extends Item implements ElementAccess {
    private static final Map<Element, ElementalGem> ELEMATILIUS_MAP = new HashMap<>();
    @NotNull
    private final Element element;

    public ElementalGem(@NotNull Element element, @NotNull Properties properties) {
        super(properties);
        this.element = this.assertEmpty(element);
        ELEMATILIUS_MAP.put(element, this);
    }

    @Override
    public @NotNull Element getElement() {
        return this.element;
    }

    public static Optional<ElementalGem> getElement(@NotNull Element element) {
        return Optional.ofNullable(ELEMATILIUS_MAP.get(element));
    }

    @Override
    public @NotNull Component getName(ItemStack itemStack) {
        MutableComponent elemName = Texts.translatable(this.getElement().getTranslationKey());
        return Texts.translatable("item." + CropariaIf.MOD_ID + ".element.gem", elemName);
    }
}
