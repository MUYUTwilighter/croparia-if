package cool.muyucloud.croparia.registry;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.generator.pack.DataPackHandler;
import cool.muyucloud.croparia.api.generator.pack.ResourcePackHandler;
import cool.muyucloud.croparia.api.json.JsonBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

@SuppressWarnings("unused")
public class PackHandlers {
    public static final DataPackHandler DATAPACK = DataPackHandler.register(
        CropariaIf.of("datapack"),
        CropariaIf.CONFIG.getFilePath().resolve("datapack"),
        packMeta(PackType.SERVER_DATA, "Croparia mandatory datapack, please do not modify"),
        CropariaIf.CONFIG::getOverride
    );
    public static final ResourcePackHandler RESOURCEPACK = ResourcePackHandler.register(
        CropariaIf.of("resourcepack"),
        CropariaIf.CONFIG.getFilePath().resolve("resourcepack"),
        packMeta(PackType.CLIENT_RESOURCES, "Croparia mandatory resourcepack, please do not modify"),
        CropariaIf.CONFIG::getOverride
    );

    private static com.google.gson.JsonObject packMeta(PackType packType, String description) {
        int format = SharedConstants.getCurrentVersion().packVersion(packType).major();
        var pack = JsonBuilder.map(
            "pack_format", format,
            "min_format", format,
            "max_format", format,
            "description", description
        );
        if (format < 82) {
            pack.add("supported_formats", JsonBuilder.map(
                "min_inclusive", format,
                "max_inclusive", format
            ));
        }
        return JsonBuilder.map("pack", pack);
    }

    public static void register() {
        CropariaIf.LOGGER.debug("Registering pack handlers");
    }
}
