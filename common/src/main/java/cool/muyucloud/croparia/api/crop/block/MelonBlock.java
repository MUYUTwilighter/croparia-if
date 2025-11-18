package cool.muyucloud.croparia.api.crop.block;


import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class MelonBlock extends Block implements CropAccess<Melon> {
    private final Melon melon;

    public MelonBlock(Melon melon) {
        super(Properties.ofFullCopy(Blocks.PUMPKIN).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), melon.getMelon().getId())));
        this.melon = melon;
    }

    @Override
    public Melon getCrop() {
        return this.melon;
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
