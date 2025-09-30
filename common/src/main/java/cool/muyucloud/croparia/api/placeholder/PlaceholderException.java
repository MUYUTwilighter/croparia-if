package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonParseException;

public class PlaceholderException extends JsonParseException {
    public PlaceholderException(String message) {
        super(message);
    }

    public PlaceholderException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlaceholderException(Throwable cause) {
        super(cause);
    }
}
