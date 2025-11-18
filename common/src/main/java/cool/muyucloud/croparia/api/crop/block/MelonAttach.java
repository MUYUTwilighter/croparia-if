package cool.muyucloud.croparia.api.crop.block;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.crop.item.MelonSeed;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class MelonAttach extends AttachedStemBlock implements CropAccess<Melon> {
    private final Melon melon;

    public MelonAttach(Melon melon) {
        super(
            CifUtil.castUnsafe(melon.getStem().getKey()),
            CifUtil.castUnsafe(melon.getMelon().getKey()),
            CifUtil.castUnsafe(melon.getSeed().getKey()),
            Properties.ofFullCopy(Blocks.ATTACHED_PUMPKIN_STEM).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), melon.getAttach().getId()))
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
