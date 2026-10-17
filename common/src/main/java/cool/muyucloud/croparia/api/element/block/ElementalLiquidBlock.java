package cool.muyucloud.croparia.api.element.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.core.block.ArchitecturyLiquidBlock;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

public class ElementalLiquidBlock extends ArchitecturyLiquidBlock implements ElementAccess {
    @NonNull
    private final Element element;

    public ElementalLiquidBlock(@NonNull Element element, Properties properties) {
        super(element.getFluidFlowing(), properties);
        this.element = element;
    }

    @Override
    public @NonNull Element getElement() {
        return this.element;
    }

    @Override
    public @NonNull Item asItem() {
        return this.getElement().getBucket().get();
    }

    @Override
    public @NonNull MutableComponent getName() {
        MutableComponent elemName = Texts.translatable(this.getElement().getTranslationKey());
        return Texts.translatable("block." + CropariaIf.MOD_ID + ".element.liquid_block", elemName);
    }
}
