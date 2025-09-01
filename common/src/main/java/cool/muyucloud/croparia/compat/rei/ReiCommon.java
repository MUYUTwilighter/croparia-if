package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.core.recipe.InfusorRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualRecipe;
import cool.muyucloud.croparia.api.core.recipe.RitualStructure;
import cool.muyucloud.croparia.api.core.recipe.SoakRecipe;
import cool.muyucloud.croparia.api.recipe.DisplayableRecipe;
import cool.muyucloud.croparia.api.recipe.TypedSerializer;
import cool.muyucloud.croparia.compat.rei.category.*;
import cool.muyucloud.croparia.compat.rei.util.ProxyCategory;
import cool.muyucloud.croparia.compat.rei.util.SimpleDisplay;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReiCommon implements REICommonPlugin {
    private static final Set<ProxyCategory<?>> PROXIES = new HashSet<>();

    public static void forEach(Consumer<ProxyCategory<?>> consumer) {
        PROXIES.forEach(consumer);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <R extends DisplayableRecipe<?>> ProxyCategory<R> addProxy(TypedSerializer<R> type, Function<ProxyCategory<R>, ? extends SimpleCategory<R>> category) {
        ProxyCategory<R> proxy = ProxyCategory.of(type, category);
        PROXIES.add(proxy);
        return proxy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerDisplays(ServerDisplayRegistry registry) {
        CropariaIf.LOGGER.info("Registering rei recipe fillers...");
        forEach(proxy -> registry.beginRecipeFiller(proxy.getType().getRecipeClass())
            .filterType((RecipeType<DisplayableRecipe<?>>) proxy.getType())
            .fill(holder -> new SimpleDisplay<>(
                (RecipeHolder<DisplayableRecipe<?>>) holder, (ProxyCategory<DisplayableRecipe<?>>) proxy
            )));
    }

    @Override
    @SuppressWarnings("Convert2MethodRef")
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        addProxy(InfusorRecipe.TYPED_SERIALIZER, proxy -> new ReiInfusorRecipe(proxy));
        addProxy(RitualRecipe.TYPED_SERIALIZER, proxy -> new ReiRitualRecipe(proxy));
        PROXIES.add(ProxyCategory.of(RitualRecipe.TYPED_SERIALIZER, proxy -> new ReiRitualRecipe(proxy)));
        PROXIES.add(ProxyCategory.of(RitualStructure.TYPED_SERIALIZER, proxy -> new ReiRitualStructure(proxy)));
        PROXIES.add(ProxyCategory.of(SoakRecipe.TYPED_SERIALIZER, proxy -> new ReiSoakRecipe(proxy)));
        CropariaIf.LOGGER.info("Registering rei recipe display serializers...");
        forEach(proxy -> registry.register(proxy.getType().getId().orElseThrow(), proxy.getSerializer()));
    }
}
