package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.api.placeholder.MapReader;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public interface TranslatableEntry extends DgEntry {
    Placeholder<TranslatableEntry> PLACEHOLDER = Placeholder.build(node -> node.then(
        Pattern.compile("^translation_key$"), TypeMapper.of(TranslatableEntry::getTranslationKey), Placeholder.STRING
    ).thenMap(
        Pattern.compile("^translations$"), TypeMapper.of(translatable -> MapReader.map(translatable.getTranslations())), Placeholder.STRING
    ).concat(DgEntry.PLACEHOLDER, TypeMapper.of(translatableEntry -> translatableEntry)));

    Collection<String> getLangs();

    String getTranslationKey();

    @Nullable String translate(String lang);

    Map<String, String> getTranslations();

    @Override
    default Placeholder<? extends TranslatableEntry> placeholder() {
        return PLACEHOLDER;
    }
}
