package cool.muyucloud.croparia.api.generator.util;

import com.google.gson.JsonParseException;

public class CdgFormatException extends JsonParseException {
    public CdgFormatException(String message) {
        super(message);
    }
}
