package cool.muyucloud.croparia.config;

import dev.architectury.platform.Platform;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Config {
    public static @NonNull Optional<Path> parsePath(@Nullable String path) {
        if (path == null) {
            return Optional.empty();
        }
        Path p = Path.of(path);
        if (p.isAbsolute()) {
            return Optional.of(p);
        } else {
            return Optional.of(Platform.getGameFolder().resolve(path));
        }
    }

    public static @NonNull String resolvePath(@NonNull Path path) {
        Path normalizedPath = path.normalize();
        try {
            Path basePath = Platform.getGameFolder();
            if (basePath != null) {
                Path normalizedBasePath = basePath.normalize();
                if (normalizedPath.startsWith(normalizedBasePath)) {
                    return normalizedBasePath.relativize(normalizedPath).toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return normalizedPath.toAbsolutePath().toString();
    }

    @NonNull
    private Path filePath;
    @NonNull
    private Path recipeWizard;
    @NonNull
    private Boolean override;
    @NonNull
    private Boolean infusor;
    @NonNull
    private Boolean ritual;
    @NonNull
    private Boolean fruitUse;
    @NonNull
    private Boolean breakEffect;
    @NonNull
    private Integer autoReload;
    @NonNull
    private Integer soakAttempts;
    @NonNull
    private Integer cropYield;
    @NonNull
    private Integer melonYield;
    @NonNull
    private final List<Identifier> cropBlackList;
    private final List<String> modBlackList;

    /**
     * Default config
     */
    public Config() {
        this.filePath = Platform.getGameFolder().resolve("croparia");
        this.recipeWizard = Platform.getGameFolder().resolve("croparia/recipe_wizard/dump");
        this.autoReload = 20;
        this.override = true;
        this.fruitUse = true;
        this.infusor = true;
        this.ritual = true;
        this.breakEffect = true;
        this.soakAttempts = 1;
        this.cropYield = 2;
        this.melonYield = 2;
        this.cropBlackList = new ArrayList<>();
        this.modBlackList = new ArrayList<>();
    }

    /**
     * Deserialize config
     */
    public Config(RawConfig raw) {
        this.filePath = parsePath(raw.filePath()).orElseGet(() -> Platform.getGameFolder().resolve("croparia"));
        this.recipeWizard = parsePath(raw.recipeWizard()).orElseGet(() -> Platform.getGameFolder().resolve("croparia/recipe_wizard/dump"));
        this.autoReload = raw.autoReload() != null ? raw.autoReload() : 20;
        this.override = raw.override() != null ? raw.override() : true;
        this.fruitUse = raw.fruitUse() != null ? raw.fruitUse() : true;
        this.infusor = raw.infusor() != null ? raw.infusor() : true;
        this.ritual = raw.ritual() != null ? raw.ritual() : true;
        this.breakEffect = raw.breakEffect() != null ? raw.breakEffect() : true;
        this.soakAttempts = raw.soakAttempts() != null ? raw.soakAttempts() : 1;
        this.cropYield = raw.cropYield() != null && raw.cropYield() > 0 ? raw.cropYield() : 2;
        this.melonYield = raw.melonYield() != null && raw.melonYield() > 0 ? raw.melonYield() : 2;
        this.melonYield = 2;
        this.cropBlackList = new ArrayList<>();
        this.modBlackList = new ArrayList<>();
        this.setBlackList(raw.blacklist());
    }

    public RawConfig toRaw() {
        return new RawConfig(resolvePath(filePath), resolvePath(recipeWizard), override, infusor, ritual, fruitUse, breakEffect, autoReload, soakAttempts, cropYield, melonYield, this.getBlacklist());
    }

    public @NonNull Integer getCropYield() {
        return cropYield;
    }

    public void setCropYield(@NonNull Integer cropYield) {
        this.cropYield = cropYield;
    }

    public @NonNull Integer getMelonYield() {
        return melonYield;
    }

    public void setMelonYield(@NonNull Integer melonYield) {
        this.melonYield = melonYield;
    }

    public @NonNull Path getFilePath() {
        return filePath;
    }

    public void setFilePath(@NonNull Path filePath) {
        this.filePath = filePath;
    }

    public @NonNull Path getRecipeWizard() {
        return recipeWizard;
    }

    public void setRecipeWizard(@NonNull Path recipeWizard) {
        this.recipeWizard = recipeWizard;
    }

    public @NonNull Integer getAutoReload() {
        return autoReload;
    }

    public void setAutoReload(@NonNull Integer autoReload) {
        this.autoReload = autoReload;
    }

    public @NonNull Boolean getOverride() {
        return override;
    }

    public void setOverride(@NonNull Boolean override) {
        this.override = override;
    }

    public @NonNull Boolean getFruitUse() {
        return fruitUse;
    }

    public void setFruitUse(@NonNull Boolean fruitUse) {
        this.fruitUse = fruitUse;
    }

    public @NonNull Boolean getInfusor() {
        return infusor;
    }

    public void setInfusor(@NonNull Boolean infusor) {
        this.infusor = infusor;
    }

    public @NonNull Integer getSoakAttempts() {
        return soakAttempts;
    }

    public void setSoakAttempts(@NonNull Integer soakAttempts) {
        this.soakAttempts = soakAttempts;
    }

    public @NonNull Boolean getRitual() {
        return ritual;
    }

    public void setRitual(@NonNull Boolean ritual) {
        this.ritual = ritual;
    }

    public @NonNull Boolean getBreakEffect() {
        return breakEffect;
    }

    public void setBreakEffect(@NonNull Boolean breakEffect) {
        this.breakEffect = breakEffect;
    }

    public @NonNull List<Identifier> getCropBlackList() {
        return cropBlackList;
    }

    public @NonNull List<String> getModBlackList() {
        return modBlackList;
    }

    public List<String> getBlacklist() {
        List<String> blacklist = new ArrayList<>(this.getCropBlackList().size() + this.getModBlackList().size());
        for (Identifier id : this.getCropBlackList()) {
            blacklist.add(id.toString());
        }
        for (String token : this.getModBlackList()) {
            blacklist.add("@" + token);
        }
        return blacklist;
    }

    public void setBlackList(@NonNull List<String> blacklist) {
        this.getCropBlackList().clear();
        this.getModBlackList().clear();
        for (String token : blacklist) {
            if (token.startsWith("@")) {
                this.getModBlackList().add(token.substring(1));
            } else {
                Identifier id = Identifier.tryParse(token);
                if (id != null) this.getCropBlackList().add(id);
            }
        }
    }

    public boolean isCropValid(Identifier id) {
        for (Identifier e : this.getCropBlackList()) {
            if (e.equals(id)) {
                return false;
            }
        }
        return true;
    }

    public boolean isModValid(String mod) {
        for (String token : this.getModBlackList()) {
            if (Pattern.compile(token).matcher(mod).matches()) {
                return false;
            }
        }
        return true;
    }
}
