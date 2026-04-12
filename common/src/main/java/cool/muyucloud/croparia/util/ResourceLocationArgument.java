package cool.muyucloud.croparia.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Collection;

public class ResourceLocationArgument implements ArgumentType<Identifier> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

    public static ResourceLocationArgument id() {
        return new ResourceLocationArgument();
    }

    public static <S> Identifier getId(CommandContext<S> commandContext, String string) {
        try {
            return commandContext.getArgument(string, Identifier.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Identifier parse(StringReader stringReader) throws CommandSyntaxException {
        return Identifier.read(stringReader);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
