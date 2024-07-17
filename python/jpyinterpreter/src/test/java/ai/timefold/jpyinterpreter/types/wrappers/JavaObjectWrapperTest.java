package ai.timefold.jpyinterpreter.types.wrappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.wrappers.inaccessible.PublicInterface;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaObjectWrapperTest {
    public record TestObject(String name) {
        public String getName() {
            return name;
        }
    }

    @Test
    void testEquals() {
        assertThat(new TestObject("a")).isEqualTo(new TestObject("a"));
        assertThat(new TestObject("a")).isNotEqualTo(new TestObject("b"));
    }

    @Test
    void testHashCode() {
        assertThat(new TestObject("a").hashCode()).isEqualTo(new TestObject("a").hashCode());
    }

    @Test
    void testCallingMethod() {
        TestObject object = new TestObject("My name");
        JavaObjectWrapper wrapper = new JavaObjectWrapper(object);
        PythonLikeObject wrappedFunction = wrapper.$method$__getattribute__(PythonString.valueOf("getName"));
        assertThat(wrappedFunction).isInstanceOf(PythonLikeFunction.class);
        PythonLikeFunction function = (PythonLikeFunction) wrappedFunction;
        assertThat(function.$call(List.of(), Map.of(), null)).isEqualTo(PythonString.valueOf("My name"));
    }

    @Test
    void testCallingMethodOnInaccessibleClass() {
        PublicInterface object = PublicInterface.getInstance();
        JavaObjectWrapper wrapper = new JavaObjectWrapper(object);
        PythonLikeObject wrappedFunction = wrapper.$method$__getattribute__(PythonString.valueOf("interfaceMethod"));
        assertThat(wrappedFunction).isInstanceOf(PythonLikeFunction.class);
        PythonLikeFunction function = (PythonLikeFunction) wrappedFunction;
        assertThat(function.$call(List.of(), Map.of(), null)).isEqualTo(PythonString.valueOf("PrivateObject"));
    }

    @Test
    void testCallingRecordGetter() {
        TestObject object = new TestObject("My name");
        JavaObjectWrapper wrapper = new JavaObjectWrapper(object);
        PythonLikeObject result = wrapper.$method$__getattribute__(PythonString.valueOf("name"));
        if (result instanceof PythonLikeFunction getter) {
            assertThat(getter.$call(List.of(), Map.of(), null))
                    .isEqualTo(PythonString.valueOf("My name"));
        } else {
            Assertions.fail("Not an instance of a function");
        }

    }

    @Test
    void testComparable() {
        JavaObjectWrapper v1 = new JavaObjectWrapper(1);
        JavaObjectWrapper v2 = new JavaObjectWrapper(2);
        JavaObjectWrapper v3 = new JavaObjectWrapper(3);
        assertThat(v1.compareTo(v1)).isZero();
        assertThat(v1.compareTo(v2)).isNegative();
        assertThat(v1.compareTo(v3)).isNegative();

        assertThat(v2.compareTo(v1)).isPositive();
        assertThat(v2.compareTo(v2)).isZero();
        assertThat(v2.compareTo(v3)).isNegative();

        assertThat(v3.compareTo(v1)).isPositive();
        assertThat(v3.compareTo(v2)).isPositive();
        assertThat(v3.compareTo(v3)).isZero();
    }

    @Test
    void testIterable() {
        JavaObjectWrapper iterable = new JavaObjectWrapper(List.of(1, 2, 3));
        assertThat((Iterable<JavaObjectWrapper>) iterable)
                .containsExactly(
                        new JavaObjectWrapper(1),
                        new JavaObjectWrapper(2),
                        new JavaObjectWrapper(3));
    }

    @Test
    void testLength() {
        JavaObjectWrapper iterable = new JavaObjectWrapper(List.of(1, 2, 3));
        PythonLikeObject object = iterable.$method$__getattribute__(
                PythonString.valueOf(PythonUnaryOperator.LENGTH.getDunderMethod()));
        assertThat(object).isInstanceOf(PythonLikeFunction.class);
        PythonLikeFunction function = (PythonLikeFunction) object;
        assertThat(function.$call(List.of(), Map.of(), null)).isEqualTo(PythonInteger.valueOf(3));
    }

    @Test
    void testGetList() {
        JavaObjectWrapper iterable = new JavaObjectWrapper(List.of(1, 2, 3));
        PythonLikeObject object = iterable.$method$__getattribute__(
                PythonString.valueOf(PythonBinaryOperator.GET_ITEM.getDunderMethod()));
        assertThat(object).isInstanceOf(PythonLikeFunction.class);
        PythonLikeFunction function = (PythonLikeFunction) object;
        assertThat(function.$call(List.of(PythonInteger.valueOf(0)), Map.of(), null)).isEqualTo(PythonInteger.valueOf(1));
        assertThat(function.$call(List.of(PythonInteger.valueOf(1)), Map.of(), null)).isEqualTo(PythonInteger.valueOf(2));
        assertThat(function.$call(List.of(PythonInteger.valueOf(2)), Map.of(), null)).isEqualTo(PythonInteger.valueOf(3));
    }

    @Test
    void testGetMap() {
        JavaObjectWrapper iterable = new JavaObjectWrapper(Map.of("a", 1, "b", 2));
        PythonLikeObject object = iterable.$method$__getattribute__(
                PythonString.valueOf(PythonBinaryOperator.GET_ITEM.getDunderMethod()));
        assertThat(object).isInstanceOf(PythonLikeFunction.class);
        PythonLikeFunction function = (PythonLikeFunction) object;
        assertThat(function.$call(List.of(PythonString.valueOf("a")), Map.of(), null)).isEqualTo(PythonInteger.valueOf(1));
        assertThat(function.$call(List.of(PythonString.valueOf("b")), Map.of(), null)).isEqualTo(PythonInteger.valueOf(2));
    }

    @Test
    void testContains() {
        JavaObjectWrapper iterable = new JavaObjectWrapper(List.of(1, 2, 3));
        PythonLikeObject object = iterable.$method$__getattribute__(
                PythonString.valueOf(PythonBinaryOperator.CONTAINS.getDunderMethod()));
        assertThat(object).isInstanceOf(PythonLikeFunction.class);
        PythonLikeFunction function = (PythonLikeFunction) object;
        assertThat(function.$call(List.of(PythonInteger.valueOf(0)), Map.of(), null)).isEqualTo(PythonBoolean.FALSE);
        assertThat(function.$call(List.of(PythonInteger.valueOf(2)), Map.of(), null)).isEqualTo(PythonBoolean.TRUE);
    }
}
