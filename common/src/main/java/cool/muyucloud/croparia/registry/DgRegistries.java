package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.Crop;
import cool.muyucloud.croparia.api.crop.CropRegistry;
import cool.muyucloud.croparia.api.crop.Melon;
import cool.muyucloud.croparia.api.element.Element;
import cool.muyucloud.croparia.api.generator.util.DgRegistry;
import cool.muyucloud.croparia.api.generator.util.ItemDgRegistry;

public class DgRegistries {
    public static final CropRegistry<Crop> CROPS = DgRegistry.register(
        CropariaIf.of("crops"), new CropRegistry<>(CropariaIf.CONFIG.getFilePath().resolve("crops"), Crop.CODEC.codec())
    );
    public static final CropRegistry<Melon> MELONS = DgRegistry.register(
        CropariaIf.of("melons"), new CropRegistry<>(CropariaIf.CONFIG.getFilePath().resolve("melons"), Melon.CODEC.codec())
    );
    @SuppressWarnings("unused")
    public static final DgRegistry.EnumRegistry<Element> ELEMENTS = DgRegistry.register(
        CropariaIf.of("elements"), DgRegistry.ofEnum(Element.class)
    );
    @SuppressWarnings("unused")
    public static final ItemDgRegistry ITEMS = DgRegistry.register(
        CropariaIf.of("items"), new ItemDgRegistry()
    );

    public static void register() {
        CropariaIf.LOGGER.debug("Registering data generator iterables");
    }
}
