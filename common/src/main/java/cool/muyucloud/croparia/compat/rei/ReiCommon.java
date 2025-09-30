package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.util.ReiDisplay;
import cool.muyucloud.croparia.compat.rei.util.ReiType;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings({"unused"})
public class ReiCommon implements REICommonPlugin {
    private static final Set<ReiType<?>> PROXIES = new HashSet<>();

    public static final ReiType<InfusorRecipe> INFUSOR = addProxy(InfusorRecipe.TYPED_SERIALIZER);
    public static final ReiType<RitualRecipe> RITUAL = addProxy(RitualRecipe.TYPED_SERIALIZER);
    public static final ReiType<RitualStructure> RITUAL_STRUCTURE = addProxy(RitualStructure.TYPED_SERIALIZER);
    public static final ReiType<SoakRecipe> SOAK = addProxy(SoakRecipe.TYPED_SERIALIZER);

    public static void forEach(Consumer<ReiType<?>> consumer) {
        PROXIES.forEach(consumer);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <R extends DisplayableRecipe<?>> ReiType<R> addProxy(TypedSerializer<R> type) {
        ReiType<R> reiType = new ReiType<>(type);
        PROXIES.add(reiType);
        return reiType;
    }

    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        CropariaIf.LOGGER.debug("Registering rei recipe fillers...");
        forEach(proxy -> registry.beginRecipeFiller(proxy.adapt().getType().getRecipeClass())
            .filterType(proxy.adapt().getType())
            .fill(holder -> new ReiDisplay<>(
                holder, proxy.adapt()
            )));
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        CropariaIf.LOGGER.debug("Registering rei recipe display serializers...");
        forEach(proxy -> registry.register(proxy.getType().getId(), proxy.getSerializer()));
    }
}
