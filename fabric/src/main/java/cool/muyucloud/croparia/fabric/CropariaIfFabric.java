package cool.muyucloud.croparia.fabric;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.json.JsonTransformer;
import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.serial.gson.GsonTomlSerializer;
import net.fabricmc.api.ModInitializer;

public class CropariaIfFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        JsonTransformer.TRANSFORMERS.put("toml", raw -> GsonTomlSerializer.instance().serialize(
            JToml.jToml().readFromString(raw)
        ));
        CropariaIf.init();
    }
}
