package cool.muyucloud.croparia.api.placeholder;

import com.google.gson.JsonParseException;

public class RegexParserException extends JsonParseException {
    public RegexParserException(String message) {
        super(message);
    }

    public RegexParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegexParserException(Throwable cause) {
        super(cause);
    }
}
