package cool.muyucloud.croparia.api.crop.item;

import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.crop.Melon;
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

public class MelonSeed extends BlockItem implements CropRegistryAccess<Melon> , CropStackProvider {
    public MelonSeed(Supplier<? extends Block> melon, Properties properties) {
        super(melon.get(), properties);
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
        Optional<Melon> mayCrop = DgRegistries.MELONS.getById(id);
        MutableComponent cropName = mayCrop.map(crop ->
            Texts.mayTranslatable(crop.getTranslationKey()).orElseGet(() -> Texts.literal(crop.translate()))
        ).orElseGet(() -> Texts.translatable("melon.croparia.unknown"));
        return Texts.translatable(this.getDescriptionId(), cropName);
    }

    @Override
    public CropRegistry<Melon> getRegistry() {
        return DgRegistries.MELONS;
    }
}
