package cool.muyucloud.croparia.api.element.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.core.block.ArchitecturyLiquidBlock;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class ElementalLiquidBlock extends ArchitecturyLiquidBlock implements ElementAccess {
    @NotNull
    private final Element element;

    public ElementalLiquidBlock(@NotNull Element element, Properties properties) {
        super(element.getFluidFlowing(), properties);
        this.element = element;
    }

    @Override
    public @NotNull Element getElement() {
        return this.element;
    }

    @Override
    public @NotNull Item asItem() {
        return this.getElement().getBucket().get();
    }

    @Override
    public @NotNull MutableComponent getName() {
        MutableComponent elemName = Texts.translatable(this.getElement().getTranslationKey());
        return Texts.translatable("block." + CropariaIf.MOD_ID + ".element.liquid_block", elemName);
    }
}
