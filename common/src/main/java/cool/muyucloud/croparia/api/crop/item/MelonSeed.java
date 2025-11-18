package cool.muyucloud.croparia.api.crop.item;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.CropAccess;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.registry.Tabs;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MelonSeed extends BlockItem implements CropAccess<Melon> {
    private final Melon melon;

    @SuppressWarnings("UnstableApiUsage")
    public MelonSeed(Melon melon) {
        super(
            melon.getStem().get(),
            new Properties().setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), melon.getSeed().getId())).arch$tab(Tabs.MELONS)
        );
        this.melon = melon;
    }

    @Override
    public Melon getCrop() {
        return this.melon;
    }

    @Override
    public @NotNull Component getName(ItemStack itemStack) {
        MutableComponent cropName = Texts.translatable(this.getCrop().getTranslationKey());
        return Texts.translatable("item." + CropariaIf.MOD_ID + ".melon.seed", cropName);
    }
}
