package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.item.MelonSeed;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class MelonAttach extends AttachedStemBlock implements CropAccess<Melon> {
    private final Melon melon;

    public MelonAttach(Melon melon) {
        super(
            melon.getMelon().get(),
            () -> melon.getSeed().get(),
            Properties.copy(Blocks.ATTACHED_PUMPKIN_STEM)
        );
        this.melon = melon;
    }

    @Override
    public Melon getCrop() {
        return this.melon;
    }

    @Override
    public @NotNull MutableComponent getName() {
        MutableComponent cropName = Texts.translatable(this.getCrop().getTranslationKey());
        return Texts.translatable("block." + CropariaIf.MOD_ID + ".melon.attach", cropName);
    }

    @Override
    public @NotNull MelonSeed asItem() {
        return this.getCrop().getSeed().get();
    }
}
