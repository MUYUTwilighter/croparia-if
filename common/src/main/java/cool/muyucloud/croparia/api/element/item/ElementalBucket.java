package cool.muyucloud.croparia.api.element.item;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.core.item.ArchitecturyBucketItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ElementalBucket extends ArchitecturyBucketItem implements ElementAccess {
    private final Element element;

    public ElementalBucket(Element element, Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid, properties);
        this.element = assertEmpty(element);
    }

    @Override
    public @NotNull Element getElement() {
        return element;
    }

    @Override
    public @NotNull Component getName(ItemStack itemStack) {
        MutableComponent elemName = Texts.translatable(this.getElement().getTranslationKey());
        return Texts.translatable("item." + CropariaIf.MOD_ID + ".element.bucket", elemName);
    }
}
