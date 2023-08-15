package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

final class DummyMemberAccessor implements MemberAccessor {

    static final MemberAccessor INSTANCE = new DummyMemberAccessor();

    private DummyMemberAccessor() {

    }

    @Override
    public Class<?> getDeclaringClass() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Class<?> getType() {
        return null;
    }

    @Override
    public Type getGenericType() {
        return null;
    }

    @Override
    public Object executeGetter(Object bean) {
        return null;
    }

    @Override
    public <Fact_, Result_> Function<Fact_, Result_> getGetterFunction() {
        return null;
    }

    @Override
    public boolean supportSetter() {
        return false;
    }

    @Override
    public void executeSetter(Object bean, Object value) {

    }

    @Override
    public String getSpeedNote() {
        return null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return null;
    }
}
