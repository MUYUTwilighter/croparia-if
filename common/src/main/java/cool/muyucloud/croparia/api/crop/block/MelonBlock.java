package cool.muyucloud.croparia.api.crop.block;


import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import org.jetbrains.annotations.NotNull;

public class MelonBlock extends StemGrownBlock implements CropAccess<Melon> {
    private final Melon melon;

    public MelonBlock(Melon melon) {
        super(Properties.copy(Blocks.PUMPKIN));
        this.melon = melon;
    }

    @Override
    public Melon getCrop() {
        return this.melon;
    }

    @Override
    public @NotNull StemBlock getStem() {
        return this.getCrop().getStem().get();
    }

    @Override
    public @NotNull AttachedStemBlock getAttachedStem() {
        return this.getCrop().getAttach().get();
    }

    @Override
    public @NotNull MutableComponent getName() {
        MutableComponent cropName = Texts.translatable(this.getCrop().getTranslationKey());
        return Texts.translatable("block." + CropariaIf.MOD_ID + ".melon.block", cropName);
    }

    @Override
    public @NotNull Item asItem() {
        return this.getCrop().getMelonItem().get();
    }
}
