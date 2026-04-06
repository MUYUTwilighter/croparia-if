package cool.muyucloud.croparia.api.core.component;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.access.StateHolderAccess;
import cool.muyucloud.croparia.util.text.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class BlockProperties implements Iterable<Map.Entry<String, String>> {
    public static final Codec<BlockProperties> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(BlockProperties::new, BlockProperties::getProperties);
    public static final BlockProperties EMPTY = new BlockProperties(Map.of());
    public static final Component TITLE = Texts.translatable("tooltip.croparia.block_properties");

    public static BlockProperties extract(@NotNull BlockState state) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        var defaults = ((StateHolderAccess<?>) state.getBlock().defaultBlockState()).cif$getProperties();
        var access = (StateHolderAccess<?>) state;
        for (var entry : access.cif$getProperties().entrySet()) {
            if (!entry.getValue().equals(defaults.get(entry.getKey()))) {
                builder.put(entry);
            }
        }
        return of(builder.build());
    }

    public static BlockProperties of(@NotNull BlockState state) {
        return of(((StateHolderAccess<?>) state).cif$getProperties());
    }

    @NotNull
    public static BlockProperties of(@NotNull Map<String, String> properties) {
        return properties.isEmpty() ? EMPTY : new BlockProperties(properties);
    }

    private final Map<String, String> properties;

    protected BlockProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isEmpty() {
        return this.getProperties().isEmpty();
    }

    public void addToTooltip(ItemStack stack) {
        if (this.getProperties().isEmpty()) return;
        Texts.tooltip(stack, Texts.literal("").append(TITLE));
        this.getProperties().forEach((key, value) -> Texts.tooltip(stack, Texts.literal("%s=%s".formatted(key, value))));
    }

    public boolean isSubsetOf(BlockState state) {
        var access = (StateHolderAccess<?>) state;
        for (Map.Entry<String, String> entry : this.getProperties().entrySet()) {
            @NotNull String key = entry.getKey();
            @Nullable String value = entry.getValue();
            @Nullable String blockVal = access.cif$getValue(key);
            if (value == null && blockVal != null) {
                continue;
            }
            if (!Objects.equals(blockVal, value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockProperties that)) return false;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return this.getProperties().entrySet().iterator();
    }
}
