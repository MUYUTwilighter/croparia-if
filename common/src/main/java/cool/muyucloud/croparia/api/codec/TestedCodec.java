package cool.muyucloud.croparia.api.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Constructs a codec that performs encoding and decoding only if the specified tests pass.
 * <p>
 * This class wraps a base {@link Codec} and allows custom tests to be applied before encoding or decoding.
 * If a test fails, the operation returns an error with the provided message.
 * </p>
 *
 * @param <T> The type of object to encode or decode.
 */
public class TestedCodec<T> implements Codec<T> {
    public static final TestResult SUCCESS = new TestResult(true, () -> null);
    public static final TestResult FAIL = new TestResult(false, () -> "Test not passed");

    /**
     * Returns a TestResult instance indicating the test has passed.
     *
     * @return a successful TestResult
     */
    public static TestResult success() {
        return SUCCESS;
    }

    /**
     * Returns a failed TestResult with the specified error message supplier.
     *
     * @param msg Supplier for the error message
     * @return a failed TestResult
     */
    public static TestResult fail(Supplier<String> msg) {
        return new TestResult(false, msg);
    }


    /**
     * Returns a failed TestResult with a default error message.
     *
     * @return a failed TestResult with default message
     */
    public static TestResult fail() {
        return FAIL;
    }

    private final Codec<T> codec;
    private final EncodeTest<T> encodeTest;
    private final DecodeTest<?> decodeTest;

    /**
     * Constructs a TestedCodec with the given base codec, encode test, and decode test.
     *
     * @param codec      The base codec to wrap.
     * @param encodeTest The test to apply before encoding.
     * @param decodeTest The test to apply before decoding.
     */
    public TestedCodec(Codec<T> codec, EncodeTest<T> encodeTest, DecodeTest<?> decodeTest) {
        this.codec = codec;
        this.encodeTest = encodeTest;
        this.decodeTest = decodeTest;
    }

    public Codec<T> getCodec() {
        return codec;
    }

    public TestResult canEncode(T object) {
        return this.encodeTest.test(object);
    }

    public <I> TestResult canDecode(DynamicOps<I> ops, I input) {
        DecodeTest<I> tester = this.decodeTest.adapt();
        return tester.test(ops, input);
    }

    @Override
    public <I> DataResult<Pair<T, I>> decode(DynamicOps<I> ops, I input) {
        TestResult testResult = this.canDecode(ops, input);
        if (testResult.success()) {
            return this.codec.decode(ops, input);
        } else {
            return DataResult.error(testResult.msg());
        }
    }

    @Override
    public <O> DataResult<O> encode(T object, DynamicOps<O> ops, O prefix) {
        TestResult testResult = this.canEncode(object);
        if (testResult.success()) {
            return this.codec.encode(object, ops, prefix);
        } else {
            return DataResult.error(testResult.msg());
        }
    }

    @Override
    public TestedFieldCodec<T> fieldOf(String name) {
        return new TestedFieldCodec<>(name, this);
    }

    @SuppressWarnings("unchecked")
    public <T2> TestedCodec<T2> adapt() {
        return (TestedCodec<T2>) this;
    }

    /**
     * Interface for testing objects before encoding.
     * Implement this interface or use functional expressions to define custom pre-encoding validation logic.
     *
     * @param <T> Type of the object to be encoded
     */
    public interface EncodeTest<T> {
        TestResult test(T toEncode);

        @SuppressWarnings("unchecked")
        default <I2> EncodeTest<I2> adapt() {
            return (EncodeTest<I2>) this;
        }
    }

    public interface DecodeTest<I> {
        TestResult test(DynamicOps<I> ops, I toDecode);

        @SuppressWarnings("unchecked")
        default <I2> DecodeTest<I2> adapt() {
            return (DecodeTest<I2>) this;
        }
    }

    public record TestResult(boolean success, @NotNull Supplier<String> msg) {
    }
}
