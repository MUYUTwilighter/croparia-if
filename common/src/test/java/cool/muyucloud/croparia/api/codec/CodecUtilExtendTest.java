package cool.muyucloud.croparia.api.codec;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;

import static cool.muyucloud.croparia.TestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodecUtilExtendTest {
    static class Base {
        private final int a;

        Base(int a) {
            this.a = a;
        }

        int getA() {
            return a;
        }
    }

    static class Child extends Base {
        private final String b;

        Child(int a, String b) {
            super(a);
            this.b = b;
        }

        String getB() {
            return b;
        }
    }

    static class Octo extends Base {
        private final String b;
        private final int c;
        private final int d;
        private final int e;
        private final int f;
        private final int g;
        private final int h;
        private final int i;

        Octo(int a, String b, int c, int d, int e, int f, int g, int h, int i) {
            super(a);
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
            this.g = g;
            this.h = h;
            this.i = i;
        }

        String getB() {
            return b;
        }

        int getC() {
            return c;
        }

        int getD() {
            return d;
        }

        int getE() {
            return e;
        }

        int getF() {
            return f;
        }

        int getG() {
            return g;
        }

        int getH() {
            return h;
        }

        int getI() {
            return i;
        }
    }

    private static final MapCodec<Base> BASE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("a").forGetter(Base::getA)
    ).apply(instance, Base::new));

    private static final RecordCodecBuilder<Octo, String> B_FIELD = Codec.STRING.fieldOf("b").forGetter(Octo::getB);
    private static final RecordCodecBuilder<Octo, Integer> C_FIELD = Codec.INT.fieldOf("c").forGetter(Octo::getC);
    private static final RecordCodecBuilder<Octo, Integer> D_FIELD = Codec.INT.fieldOf("d").forGetter(Octo::getD);
    private static final RecordCodecBuilder<Octo, Integer> E_FIELD = Codec.INT.fieldOf("e").forGetter(Octo::getE);
    private static final RecordCodecBuilder<Octo, Integer> F_FIELD = Codec.INT.fieldOf("f").forGetter(Octo::getF);
    private static final RecordCodecBuilder<Octo, Integer> G_FIELD = Codec.INT.fieldOf("g").forGetter(Octo::getG);
    private static final RecordCodecBuilder<Octo, Integer> H_FIELD = Codec.INT.fieldOf("h").forGetter(Octo::getH);
    private static final RecordCodecBuilder<Octo, Integer> I_FIELD = Codec.INT.fieldOf("i").forGetter(Octo::getI);

    @Test
    void extendBuildsCodecWithExtraField() {
        MapCodec<Child> childCodec = CodecUtil.extend(
            BASE_CODEC,
            Codec.STRING.fieldOf("b").forGetter(Child::getB),
            (base, b) -> new Child(base.getA(), b)
        );

        JsonObject json = new JsonObject();
        json.addProperty("a", 7);
        json.addProperty("b", "ok");
        Child decoded = getOrThrow(childCodec.codec().parse(JsonOps.INSTANCE, json));
        assertEquals(7, decoded.getA());
        assertEquals("ok", decoded.getB());

        JsonObject encoded = getOrThrow(childCodec.codec().encodeStart(JsonOps.INSTANCE, new Child(9, "x"))).getAsJsonObject();
        assertEquals(9, encoded.get("a").getAsInt());
        assertEquals("x", encoded.get("b").getAsString());
    }

    @Test
    void rawExtendVarargsMapperWorksAndPropagatesDecodeErrors() {
        var bField = Codec.STRING.fieldOf("b").forGetter(Child::getB);
        MapCodec<Child> childCodec = CodecUtil.extend(
            BASE_CODEC,
            (base, fields) -> new Child(base.getA(), (String) first(fields)),
            bField
        );

        JsonObject ok = new JsonObject();
        ok.addProperty("a", 1);
        ok.addProperty("b", "v");
        Child decoded = getOrThrow(childCodec.codec().parse(JsonOps.INSTANCE, ok));
        assertEquals(1, decoded.getA());
        assertEquals("v", decoded.getB());

        JsonObject missing = new JsonObject();
        missing.addProperty("a", 1);
        assertTrue(isError(childCodec.codec().parse(JsonOps.INSTANCE, missing)));
    }

    @Test
    void extendOverloadsForTwoToEightFieldsDecode() {
        JsonObject json = new JsonObject();
        json.addProperty("a", 1);
        json.addProperty("b", "bb");
        json.addProperty("c", 2);
        json.addProperty("d", 3);
        json.addProperty("e", 4);
        json.addProperty("f", 5);
        json.addProperty("g", 6);
        json.addProperty("h", 7);
        json.addProperty("i", 8);

        MapCodec<Octo> two = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD,
            (base, b, c) -> new Octo(base.getA(), b, c, 0, 0, 0, 0, 0, 0));
        assertEquals(2, getOrThrow(two.codec().parse(JsonOps.INSTANCE, json)).getC());

        MapCodec<Octo> three = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD, D_FIELD,
            (base, b, c, d) -> new Octo(base.getA(), b, c, d, 0, 0, 0, 0, 0));
        assertEquals(3, getOrThrow(three.codec().parse(JsonOps.INSTANCE, json)).getD());

        MapCodec<Octo> four = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD, D_FIELD, E_FIELD,
            (base, b, c, d, e) -> new Octo(base.getA(), b, c, d, e, 0, 0, 0, 0));
        assertEquals(4, getOrThrow(four.codec().parse(JsonOps.INSTANCE, json)).getE());

        MapCodec<Octo> five = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD, D_FIELD, E_FIELD, F_FIELD,
            (base, b, c, d, e, f) -> new Octo(base.getA(), b, c, d, e, f, 0, 0, 0));
        assertEquals(5, getOrThrow(five.codec().parse(JsonOps.INSTANCE, json)).getF());

        MapCodec<Octo> six = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD, D_FIELD, E_FIELD, F_FIELD, G_FIELD,
            (base, b, c, d, e, f, g) -> new Octo(base.getA(), b, c, d, e, f, g, 0, 0));
        assertEquals(6, getOrThrow(six.codec().parse(JsonOps.INSTANCE, json)).getG());

        MapCodec<Octo> seven = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD, D_FIELD, E_FIELD, F_FIELD, G_FIELD, H_FIELD,
            (base, b, c, d, e, f, g, h) -> new Octo(base.getA(), b, c, d, e, f, g, h, 0));
        assertEquals(7, getOrThrow(seven.codec().parse(JsonOps.INSTANCE, json)).getH());

        MapCodec<Octo> eight = CodecUtil.extend(BASE_CODEC, B_FIELD, C_FIELD, D_FIELD, E_FIELD, F_FIELD, G_FIELD, H_FIELD, I_FIELD,
            (base, b, c, d, e, f, g, h, i) -> new Octo(base.getA(), b, c, d, e, f, g, h, i));
        assertEquals(8, getOrThrow(eight.codec().parse(JsonOps.INSTANCE, json)).getI());
    }
}
