package cool.muyucloud.croparia.api.crop.util;

import cool.muyucloud.croparia.api.placeholder.PatternKey;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import cool.muyucloud.croparia.util.CifUtil;
import cool.muyucloud.croparia.util.TagUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class Material<T> {
    public static final Placeholder<Material<?>> PLACEHOLDER = Placeholder.build(node -> node
        .then(PatternKey.literal("type"), TypeMapper.of(material -> material.isTag() ? "tag" : "item"), Placeholder.STRING)
        .then(PatternKey.literal("name"), TypeMapper.of(Material::getName), Placeholder.STRING)
        .then(PatternKey.literal("count"), TypeMapper.of(Material::getCount), Placeholder.NUMBER)
        .then(PatternKey.literal("id"), TypeMapper.of(Material::getId), Placeholder.ID));

    protected final boolean tag;
    @NotNull
    protected final ResourceLocation id;
    protected final int count;

    public Material(@NotNull String name, int count) {
        this.count = count;
        if (name.startsWith("#")) {
            this.tag = true;
            this.id = ResourceLocation.parse(name.substring(1));
        } else {
            this.tag = false;
            this.id = ResourceLocation.parse(name);
        }
    }

    public boolean isTag() {
        return tag;
    }

    public @NotNull ResourceLocation getId() {
        return id;
    }

    public String getName() {
        return this.isTag() ? "#" + this.getId() : this.getId().toString();
    }

    public int getCount() {
        return count;
    }

    protected Collection<T> candidates(ResourceLocation registryName) {
        @Nullable
        Registry<T> registry = CifUtil.castUnsafe(BuiltInRegistries.REGISTRY.getValue(registryName));
        if (registry == null) {
            return Collections.emptyList();
        }
        if (this.isTag()) {
            List<T> result = new ArrayList<>();
            for (Holder<T> holder : TagUtil.forEntries(registry.key(), this.getId())) {
                if (holder.isBound()) {
                    result.add(holder.value());
                }
            }
            return result;
        } else {
            T value = registry.getValue(this.getId());
            if (value != null) {
                return Collections.singletonList(value);
            } else {
                return Collections.emptyList();
            }
        }
    }

    public abstract List<T> candidates();
}
