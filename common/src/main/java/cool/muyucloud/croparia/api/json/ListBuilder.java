package cool.muyucloud.croparia.api.json;

import com.google.gson.JsonArray;
import com.mojang.serialization.Codec;
import cool.muyucloud.croparia.api.codec.CodecUtil;

public class ListBuilder implements JsonBuilder {
    private final JsonArray json = new JsonArray();

    public ListBuilder(Object... list) {
        for (Object e : list) json.add(JsonBuilder.parse(e));
    }

    @Override
    public JsonArray build() {
        return json;
    }

    public ListBuilder add(Object... list) {
        for (Object e : list) json.add(JsonBuilder.parse(e));
        return this;
    }

    @SafeVarargs
    public final <T> ListBuilder add(Codec<T> codec, T... elements) throws IllegalStateException {
        for (T e : elements) CodecUtil.encodeJson(e, codec).mapOrElse(json -> {
            this.json.add(json);
            return null;
        }, err -> {
            err.getOrThrow();
            return null;
        });
        return this;
    }
}
