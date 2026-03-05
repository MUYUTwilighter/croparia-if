package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonParseException;

import java.util.Collection;

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

    public static PlaceholderException noMatchingKey(String next, String remaining, Collection<PatternKey> availableKeys) {
        return new PlaceholderException(
            "No matching key for segment '%s' (remaining: '%s'). Available keys: %s"
                .formatted(next, remaining, availableKeys)
        );
    }
}
