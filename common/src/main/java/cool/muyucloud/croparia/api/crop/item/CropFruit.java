package cool.muyucloud.croparia.api.crop.item;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.AbstractFruit;
import cool.muyucloud.croparia.api.crop.Crop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.crop.util.CropRegistryAccess;
import cool.muyucloud.croparia.api.crop.util.CropStackProvider;
import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.registry.DgRegistries;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CropFruit extends Item implements CropRegistryAccess<Crop>, CropStackProvider, AbstractFruit<Crop> {
    public CropFruit(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide() || !CropariaIf.CONFIG.getFruitUse()) return super.use(level, player, hand);
        ItemStack stack = player.getItemInHand(hand);
        Identifier cropId = stack.get(CropariaComponents.CROP_ID.get());
        if (cropId == null) {
            return super.use(level, player, hand);
        }
        Optional<? extends AbstractCrop<?>> mayCrop = this.getRegistry().getById(cropId);
        if (mayCrop.isEmpty()) return super.use(level, player, hand);
        AbstractCrop<?> crop = mayCrop.get();
        CifUtil.exportItem(level, player.getOnPos(), crop.getMaterial().asItem(), player);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
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
