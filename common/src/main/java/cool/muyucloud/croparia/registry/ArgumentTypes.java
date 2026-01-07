package cool.muyucloud.croparia.registry;

import com.mojang.brigadier.arguments.ArgumentType;
import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.mixin.ArgumentTypeInfosMixin;
import cool.muyucloud.croparia.util.ResourceLocationArgument;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;

public class ArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> REGISTRY = DeferredRegister.create("croparia", Registries.COMMAND_ARGUMENT_TYPE);

    @SuppressWarnings("unused")
    public static final RegistrySupplier<SingletonArgumentInfo<ResourceLocationArgument>> RESOURCE_LOCATION = register(
        "resource_location", ResourceLocationArgument.class, SingletonArgumentInfo.contextFree(ResourceLocationArgument::id)
    );

    public static <
        A extends ArgumentType<?>,
        T extends ArgumentTypeInfo.Template<A>,
        I extends ArgumentTypeInfo<A, T>
        > RegistrySupplier<I> register(String path, Class<A> clz, I info) {
        ArgumentTypeInfosMixin.getByClassMap().put(clz, info);
        return REGISTRY.register(path, () -> info);
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registering command argument types");
        REGISTRY.register();
    }
}
