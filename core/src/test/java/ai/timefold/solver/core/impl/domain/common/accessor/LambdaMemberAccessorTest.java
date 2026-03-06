package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LambdaMemberAccessorTest {

    @Test
    void getterAndSetter() {
        var accessor = new LambdaMemberAccessor(
                "value", TestBean.class, String.class, String.class,
                (TestBean b) -> b.value, (TestBean b, Object v) -> b.value = (String) v);

        var bean = new TestBean("hello");
        assertThat(accessor.executeGetter(bean)).isEqualTo("hello");

        accessor.executeSetter(bean, "world");
        assertThat(bean.value).isEqualTo("world");
    }

    @Test
    void metadata() {
        var accessor = new LambdaMemberAccessor(
                "myField", TestBean.class, String.class, String.class,
                (TestBean b) -> b.value, null);

        assertThat(accessor.getName()).isEqualTo("myField");
        assertThat(accessor.getDeclaringClass()).isEqualTo(TestBean.class);
        assertThat(accessor.getType()).isEqualTo(String.class);
        assertThat(accessor.getGenericType()).isEqualTo(String.class);
        assertThat(accessor.getSpeedNote()).isEqualTo("lambda");
    }

    @Test
    void annotationsReturnNull() {
        var accessor = new LambdaMemberAccessor(
                "value", TestBean.class, String.class, String.class,
                (TestBean b) -> b.value, null);

        assertThat(accessor.getAnnotation(Override.class)).isNull();
        assertThat(accessor.getDeclaredAnnotationsByType(Override.class)).isNull();
    }

    @Test
    void noSetterThrows() {
        var accessor = new LambdaMemberAccessor(
                "value", TestBean.class, String.class, String.class,
                (TestBean b) -> b.value, null);

        assertThat(accessor.supportSetter()).isFalse();
        assertThatThrownBy(() -> accessor.executeSetter(new TestBean("x"), "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void genericTypeFallsBackToType() {
        var accessor = new LambdaMemberAccessor(
                "value", TestBean.class, String.class, null,
                (TestBean b) -> b.value, null);

        assertThat(accessor.getGenericType()).isEqualTo(String.class);
    }

    @Test
    void toStringFormat() {
        var accessor = new LambdaMemberAccessor(
                "value", TestBean.class, String.class, String.class,
                (TestBean b) -> b.value, null);

        assertThat(accessor.toString()).isEqualTo("lambda:TestBean.value");
    }

    private static class TestBean {
        String value;

        TestBean(String value) {
            this.value = value;
        }
    }
}
