package cool.muyucloud.croparia.api.repo;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cool.muyucloud.croparia.api.resource.TypeToken;
import cool.muyucloud.croparia.api.resource.TypedResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TypeTokenAndRepoUnitTest {
    private static final class DummyResource implements TypedResource<String> {
        private static final MapCodec<DummyResource> CODEC = Codec.STRING.fieldOf("resource")
            .xmap(DummyResource::new, DummyResource::getResource);
        private static TypeToken<DummyResource> TOKEN;

        private final String value;

        private DummyResource(String value) {
            this.value = value;
        }

        @Override
        public MapCodec<? extends TypedResource<String>> getCodec() {
            return CODEC;
        }

        @Override
        public String getResource() {
            return value;
        }

        @Override
        public TypeToken<? extends TypedResource<String>> getType() {
            return TOKEN;
        }
    }

    private static TypeToken<DummyResource> registerDummyToken(String pathSuffix) {
        Identifier id = Identifier.fromNamespaceAndPath("croparia_test", "dummy_" + pathSuffix + "_" + UUID.randomUUID());
        TypeToken<DummyResource> token = TypeToken.registerOrThrow(id, new DummyResource(""), DummyResource.CODEC);
        DummyResource.TOKEN = token;
        return token;
    }

    @Test
    void typeTokenRegistryAndCodecBehaveAsExpected() {
        TypeToken<DummyResource> token = registerDummyToken("codec");
        assertTrue(TypeToken.get(token.id()).isPresent());

        var idJson = Identifier.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, token.id()).getOrThrow();
        TypeToken<?> decoded = TypeToken.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, idJson).getOrThrow();
        assertEquals(token.id(), decoded.id());

        Identifier unknown = Identifier.fromNamespaceAndPath("croparia_test", "unknown_" + UUID.randomUUID());
        var unknownJson = Identifier.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, unknown).getOrThrow();
        assertTrue(TypeToken.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, unknownJson).isError());
    }

    @Test
    void repoUnitAcceptConsumeAndSimulationRespectFlags() {
        TypeToken<DummyResource> token = registerDummyToken("repo");
        RepoUnit<DummyResource> unit = new RepoUnit<>(token, resource -> resource.getResource().startsWith("ok"), 10);
        DummyResource ok = new DummyResource("ok_a");
        DummyResource bad = new DummyResource("bad");

        unit.setAcceptable(true);
        unit.setConsumable(true);

        assertEquals(5, unit.simAccept(0, ok, 5));
        assertEquals(0, unit.simAccept(0, bad, 5));
        assertEquals(5, unit.accept(0, ok, 5));
        assertEquals(5, unit.amountFor(0, ok));
        assertEquals(10, unit.capacityFor(0, ok));

        assertEquals(3, unit.simConsume(0, ok, 3));
        assertEquals(3, unit.consume(0, ok, 3));
        assertEquals(2, unit.amountFor(0, ok));
        assertEquals(0, unit.consume(0, bad, 2));
    }

    @Test
    void repoUnitLockAndFilterAffectValidity() {
        TypeToken<DummyResource> token = registerDummyToken("valid");
        RepoUnit<DummyResource> unit = new RepoUnit<>(token, resource -> resource.getResource().startsWith("ok"), 10);
        DummyResource ok = new DummyResource("ok_a");
        DummyResource bad = new DummyResource("bad");

        assertTrue(unit.isFluidValid(ok));
        assertFalse(unit.isFluidValid(bad));

        unit.setLocked(true);
        assertFalse(unit.isFluidValid(ok));

        unit.setLocked(false);
        unit.setResource(ok);
        unit.setAmount(1);
        assertTrue(unit.isFluidValid(ok));
        assertFalse(unit.isFluidValid(new DummyResource("ok_b")));
    }

    @Test
    void repoUnitSaveAndLoadJsonAndNbtRoundTrip() {
        TypeToken<DummyResource> token = registerDummyToken("serialize");
        RepoUnit<DummyResource> unit = new RepoUnit<>(token, resource -> true, 20);
        DummyResource resource = new DummyResource("ok_serialized");
        unit.setResource(resource);
        unit.setAmount(9);
        unit.setCapacity(20);
        unit.setAcceptable(true);
        unit.setConsumable(true);
        unit.setLocked(true);

        JsonObject json = new JsonObject();
        unit.save(json);
        RepoUnit<DummyResource> fromJson = new RepoUnit<>(token, r -> true, 1);
        fromJson.load(json);
        assertEquals("ok_serialized", fromJson.getResource().getResource());
        assertEquals(9, fromJson.getAmount());
        assertTrue(fromJson.isAcceptable());
        assertTrue(fromJson.isConsumable());
        assertTrue(fromJson.isLocked());

        CompoundTag nbt = new CompoundTag();
        unit.save(nbt);
        RepoUnit<DummyResource> fromNbt = new RepoUnit<>(token, r -> true, 1);
        fromNbt.load(nbt);
        assertEquals("ok_serialized", fromNbt.getResource().getResource());
        assertEquals(9, fromNbt.getAmount());
        assertTrue(fromNbt.isAcceptable());
        assertTrue(fromNbt.isConsumable());
        assertTrue(fromNbt.isLocked());
    }

    @Test
    void registerOrThrowRejectsDuplicateId() {
        Identifier id = Identifier.fromNamespaceAndPath("croparia_test", "dup_" + UUID.randomUUID());
        TypeToken<DummyResource> first = TypeToken.registerOrThrow(id, new DummyResource(""), DummyResource.CODEC);
        DummyResource.TOKEN = first;
        assertThrows(IllegalArgumentException.class, () -> TypeToken.registerOrThrow(id, new DummyResource(""), DummyResource.CODEC));
    }

    @Test
    void repoBatchAggregatesUnitsAndDelegatesOperations() {
        TypeToken<DummyResource> token = registerDummyToken("batch");
        RepoUnit<DummyResource> u1 = new RepoUnit<>(token, r -> true, 10);
        RepoUnit<DummyResource> u2 = new RepoUnit<>(token, r -> true, 10);
        DummyResource a = new DummyResource("a");
        DummyResource b = new DummyResource("b");

        u1.setAcceptable(true);
        u2.setAcceptable(true);
        u1.accept(0, a, 4);
        u2.accept(0, b, 3);
        u1.setConsumable(true);
        u2.setConsumable(true);

        RepoBatch<DummyResource> batch = RepoBatch.of(token, u1, u2);
        assertEquals(2, batch.size());
        assertEquals(4, batch.amountFor(a));
        assertEquals(3, batch.amountFor(b));
        assertTrue(batch.isChanged());

        assertEquals(5, batch.simAccept(a, 5));
        assertEquals(2, batch.consume(a, 2));
        assertEquals(2, batch.amountFor(a));

        RepoUnit<DummyResource> removed = batch.remove(1);
        assertEquals("b", removed.getResource().getResource());
        assertEquals(1, batch.size());
        batch.clear();
        assertEquals(0, batch.size());
    }

    @Test
    void repoBatchSaveLoadAndSizeMismatchBehaveSafely() {
        TypeToken<DummyResource> token = registerDummyToken("batch_io");
        RepoUnit<DummyResource> u1 = new RepoUnit<>(token, r -> true, 10);
        RepoUnit<DummyResource> u2 = new RepoUnit<>(token, r -> true, 10);
        u1.setAcceptable(true);
        u2.setAcceptable(true);
        u1.accept(0, new DummyResource("x"), 6);
        u2.accept(0, new DummyResource("y"), 2);

        RepoBatch<DummyResource> src = RepoBatch.of(token, u1, u2);
        com.google.gson.JsonArray json = new com.google.gson.JsonArray();
        net.minecraft.nbt.ListTag nbt = new net.minecraft.nbt.ListTag();
        src.save(json);
        src.save(nbt);

        RepoBatch<DummyResource> dst = RepoBatch.of(token,
            new RepoUnit<>(token, r -> true, 10),
            new RepoUnit<>(token, r -> true, 10));
        dst.load(json);
        assertEquals(6, dst.amountFor(0));
        assertEquals(2, dst.amountFor(1));

        RepoBatch<DummyResource> dstNbt = RepoBatch.of(token,
            new RepoUnit<>(token, r -> true, 10),
            new RepoUnit<>(token, r -> true, 10));
        dstNbt.load(nbt);
        assertEquals(6, dstNbt.amountFor(0));
        assertEquals(2, dstNbt.amountFor(1));

        com.google.gson.JsonArray wrongJson = new com.google.gson.JsonArray();
        wrongJson.add(new com.google.gson.JsonObject());
        long before = dst.amountFor(0);
        dst.load(wrongJson);
        assertEquals(before, dst.amountFor(0));

        net.minecraft.nbt.ListTag wrongNbt = new net.minecraft.nbt.ListTag();
        wrongNbt.add(new CompoundTag());
        long beforeNbt = dstNbt.amountFor(0);
        dstNbt.load(wrongNbt);
        assertEquals(beforeNbt, dstNbt.amountFor(0));
    }
}
