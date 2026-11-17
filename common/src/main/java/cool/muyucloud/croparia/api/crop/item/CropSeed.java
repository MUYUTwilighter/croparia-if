package cool.muyucloud.croparia.api.crop.item;

import cool.muyucloud.croparia.api.crop.Crop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.crop.util.CropRegistryAccess;
import cool.muyucloud.croparia.api.crop.util.CropStackProvider;
import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.registry.DgRegistries;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public class CropSeed extends BlockItem implements CropRegistryAccess<Crop>, CropStackProvider {
    public CropSeed(Supplier<? extends Block> block, Properties properties) {
        super(block.get(), properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Identifier id = stack.get(CropariaComponents.CROP_ID.get());
        if (id != null) {
            String key = this.getDescriptionId() + "." + id.getPath();
            if (Language.getInstance().has(key)) {
                return Texts.translatable(key);
            }
        }
        Optional<Crop> mayCrop = DgRegistries.CROPS.getById(id);
        MutableComponent cropName = mayCrop.map(crop ->
            Texts.mayTranslatable(crop.getTranslationKey()).orElseGet(() -> Texts.literal(crop.translate()))
        ).orElseGet(() -> Texts.translatable("crop.croparia.unknown"));
        return Texts.translatable(this.getDescriptionId(), cropName);
    }

    @Override
    public CropRegistry<? super Crop> getRegistry() {
        return DgRegistries.CROPS;
    }
}
