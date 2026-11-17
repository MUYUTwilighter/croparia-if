package cool.muyucloud.croparia.access;

import cool.muyucloud.croparia.api.core.component.BlockProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

public interface StateHolderAccess<S> {
    static BlockState apply(BlockState state, BlockProperties properties) {
        if (properties.isEmpty()) return state;
        for (var entry : properties) {
            @SuppressWarnings("unchecked")
            StateHolderAccess<BlockState> access = (StateHolderAccess<BlockState>) state;
            state = access.cif$setValue(entry.getKey(), entry.getValue());
        }
        return state;
    }

    Optional<Property<?>> cif$getProperty(String key);

    Optional<String> cif$getValue(String key);

    @SuppressWarnings("unused")
    <P extends Comparable<P>> S cif$setValue(String key, String value);

    <T extends Comparable<T>> Map<String, String> cif$getProperties();
}
