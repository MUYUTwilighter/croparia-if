package cool.muyucloud.croparia.api.crop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import cool.muyucloud.croparia.api.codec.CodecUtil;
import cool.muyucloud.croparia.api.crop.util.Material;
import cool.muyucloud.croparia.api.generator.util.DgElement;
import cool.muyucloud.croparia.api.generator.util.Placeholder;
import cool.muyucloud.croparia.api.generator.util.TranslatableElement;
import cool.muyucloud.croparia.util.supplier.LazySupplier;
import cool.muyucloud.croparia.util.supplier.OnLoadSupplier;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class AbstractCrop implements TranslatableElement {
    public static final Placeholder<AbstractCrop> MATERIAL = Placeholder.of("\\{material}", crop -> CodecUtil.encodeJson(crop.getMaterial(), Material.CODEC).getOrThrow().toString());
    public static final Placeholder<AbstractCrop> MATERIAL_ID = Placeholder.of("\\{material_id}", crop -> crop.getMaterial().getId().toString());
    public static final Placeholder<AbstractCrop> MATERIAL_COUNT = Placeholder.of("\\{material_count}", crop -> String.valueOf(crop.getMaterial().getCount()));
    public static final Placeholder<AbstractCrop> MATERIAL_PATH = Placeholder.of("\\{material_path}", crop -> crop.getMaterial().getId().getPath());
    public static final Placeholder<AbstractCrop> MATERIAL_TYPE = Placeholder.of("\\{material_type}", crop -> crop.getMaterial().isTag() ? "tag" : "item");
    public static final Placeholder<AbstractCrop> MATERIAL_TAGGABLE = Placeholder.of("\\{material_taggable}", crop -> crop.getMaterial().getName());
    public static final Placeholder<AbstractCrop> MATERIAL_COMPONENTS = Placeholder.of("\\{material_components}", crop -> CodecUtil.encodeJson(crop.getMaterial().getComponents(), DataComponentPatch.CODEC).getOrThrow().toString());
    public static final Placeholder<AbstractCrop> RESULT = Placeholder.of("\\{result}", crop -> CodecUtil.encodeJson(crop.getResult(), ItemStack.CODEC).getOrThrow().toString());
    public static final Placeholder<AbstractCrop> RESULT_ID = Placeholder.of("\\{result_id}", crop -> Objects.requireNonNull(crop.getResult().getItem().arch$registryName()).toString());
    public static final Placeholder<AbstractCrop> RESULT_NAMESPACE = Placeholder.of("\\{result_namespace}", crop -> Objects.requireNonNull(crop.getResult().getItem().arch$registryName()).getNamespace());
    public static final Placeholder<AbstractCrop> RESULT_PATH = Placeholder.of("\\{result_path}", crop -> Objects.requireNonNull(crop.getResult().getItem().arch$registryName()).getPath());
    public static final Placeholder<AbstractCrop> RESULT_COUNT = Placeholder.of("\\{result_count}", (matcher, crop) -> String.valueOf(crop.getResult().getCount()));

    private final LazySupplier<Collection<Placeholder<? extends DgElement>>> placeholders = LazySupplier.of(() -> {
        ArrayList<Placeholder<? extends DgElement>> list = new ArrayList<>();
        this.buildPlaceholders(list);
        return ImmutableSet.copyOf(list);
    });
    protected transient OnLoadSupplier<List<ItemStack>> results = OnLoadSupplier.of(() -> {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : this.getMaterial().getItems()) {
            items.add(item.getDefaultInstance().copyWithCount(this.getMaterial().getCount()));
        }
        if (items.isEmpty()) items.add(ItemStack.EMPTY);
        return ImmutableList.copyOf(items);
    });

    public ItemStack getResult() {
        return this.getResults().getFirst();
    }

    public List<ItemStack> getResults() {
        return this.results.get();
    }

    public abstract @NotNull Material getMaterial();

    public ItemStack getMaterialStack() {
        return this.getMaterial().getStack();
    }

    public String getMaterialName() {
        return this.getMaterial().getName();
    }

    public abstract void onRegister();

    @Override
    public Collection<Placeholder<? extends DgElement>> placeholders() {
        return placeholders.get();
    }

    @Override
    public void buildPlaceholders(Collection<Placeholder<? extends DgElement>> list) {
        TranslatableElement.super.buildPlaceholders(list);
        list.add(MATERIAL);
        list.add(MATERIAL_ID);
        list.add(MATERIAL_COUNT);
        list.add(MATERIAL_PATH);
        list.add(MATERIAL_TYPE);
        list.add(MATERIAL_TAGGABLE);
        list.add(MATERIAL_COMPONENTS);
        list.add(RESULT);
        list.add(RESULT_ID);
        list.add(RESULT_NAMESPACE);
        list.add(RESULT_PATH);
        list.add(RESULT_COUNT);
    }
}
