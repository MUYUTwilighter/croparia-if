package cool.muyucloud.croparia.config;

import java.util.List;

public record RawConfig(String filePath, String recipeWizard, Boolean override, Boolean infusor, Boolean ritual,
                        Boolean fruitUse, Integer autoReload, Integer soakAttempts,
                        List<String> blacklist) {
}
