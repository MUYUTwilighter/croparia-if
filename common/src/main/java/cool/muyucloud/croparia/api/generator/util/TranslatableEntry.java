package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.RegexParser;
import cool.muyucloud.croparia.util.MapReader;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public interface TranslatableEntry extends DgEntry {
    Placeholder<TranslatableEntry> PLACEHOLDER = Placeholder.build(node -> node.then(
        Pattern.compile("^translation_key$"), RegexParser.of(TranslatableEntry::getTranslationKey)
    ).thenMap(
        Pattern.compile("^translations$"), translatable -> MapReader.map(translatable.getTranslations()), Placeholder.STRING
    ).concat(DgEntry.PLACEHOLDER, translatableEntry -> translatableEntry));

    Collection<String> getLangs();

    String getTranslationKey();

    @Nullable String translate(String lang);

    Map<String, String> getTranslations();

    @Override
    default Placeholder<? extends TranslatableEntry> placeholder() {
        return PLACEHOLDER;
    }
}
