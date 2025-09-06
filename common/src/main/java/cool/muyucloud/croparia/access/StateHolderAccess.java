package cool.muyucloud.croparia.access;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.component.BlockProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface StateHolderAccess<S> {
    static BlockState apply(BlockState state, BlockProperties properties) {
        if (properties.isEmpty()) return state;
        for (var entry : properties) {
            StateHolderAccess<?> access = (StateHolderAccess<?>) state;
            Property<?> property = access.cif$getProperty(entry.getKey());
            if (property == null) {
                CropariaIf.LOGGER.error("Cannot find property '{}' in block '{}'", entry.getKey(), state.getBlock().arch$registryName());
                continue;
            }
            Comparable<?> value = property.getValue(entry.getValue()).orElse(null);
            if (value == null) {
                CropariaIf.LOGGER.error("Cannot apply value '{}' to property '{}' in block '{}'", entry.getValue(), entry.getKey(), state.getBlock().arch$registryName());
                continue;
            }
            state = (BlockState) access.cif$setValue(entry.getKey(), entry.getValue());
        }
        return state;
    }

    Property<?> cif$getProperty(String key);

    @Nullable
    String cif$getValue(String key);

    @SuppressWarnings("unused")
    <P extends Comparable<P>> S cif$setValue(String key, String value);

    Map<String, String> cif$getProperties();
}
