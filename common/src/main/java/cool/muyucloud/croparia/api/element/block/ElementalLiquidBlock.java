package cool.muyucloud.croparia.api.element.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.element.ElementAccess;
import cool.muyucloud.croparia.util.text.Texts;
import dev.architectury.core.block.ArchitecturyLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, source, orientation, movedByPiston);
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
