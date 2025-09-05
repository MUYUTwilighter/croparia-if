package cool.muyucloud.croparia.access;

import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface StateHolderAccess<S> {
    Property<?> cif$getProperty(String key);

    @Nullable
    String cif$getValue(String key);

    S cif$setValue(String key, String value);

    Map<String, String> cif$getProperties();
}
