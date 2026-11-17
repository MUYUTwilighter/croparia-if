package cool.muyucloud.croparia.api.crop.asset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.AbstractCrop;
import cool.muyucloud.croparia.api.crop.Crop;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CropItemSelectProperty implements SelectItemModelProperty<String> {
    public static final SelectItemModelProperty.Type<CropItemSelectProperty, String> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new CropItemSelectProperty()), Codec.STRING);

    @Override
    public @Nullable String get(@NotNull ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        return AbstractCrop.<Crop>readCrop(itemStack).map(Crop::getType).orElse(CropariaIf.CONFIG.getCropTypes().getFirst());
    }

    @Override
    public @NotNull Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public @NotNull Type<? extends SelectItemModelProperty<String>, String> type() {
        return TYPE;
    }
}
