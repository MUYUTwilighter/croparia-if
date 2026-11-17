package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.placeholder.MapReader;
import cool.muyucloud.croparia.api.placeholder.Placeholder;
import cool.muyucloud.croparia.api.placeholder.TypeMapper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public interface TranslatableEntry extends DgEntry {
    Placeholder<TranslatableEntry> PLACEHOLDER = Placeholder.build(node -> node.then(
        Pattern.compile("^translation_key$"), TypeMapper.of(TranslatableEntry::getTranslationKey), Placeholder.STRING
    ).thenMap(
        Pattern.compile("^translations$"), TypeMapper.of(translatable -> MapReader.map(translatable.getTranslations())), Placeholder.STRING
    ).concat(DgEntry.PLACEHOLDER, TypeMapper.of(translatableEntry -> translatableEntry)));

    Collection<String> getLangs();

    String getTranslationKey();

    default @Nullable String translate(String lang) {
        return this.getTranslations().get(lang);
    }

    default String translate() {
        AtomicReference<String> code = new AtomicReference<>();
        CropariaIf.ifClientOrElse(
            ref -> code.set(ref.get().getLanguageManager().getSelected()),
            _ -> code.set("en_us")
        );
        return this.getTranslations().getOrDefault(code.get(), this.getTranslations().get("en_us"));
    }

    Map<String, String> getTranslations();

    @Override
    default Placeholder<? extends TranslatableEntry> placeholder() {
        return PLACEHOLDER;
    }
}
