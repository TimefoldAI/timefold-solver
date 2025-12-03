package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.common.accessor.ExtendedMemberAccessor;

public abstract class AbstractReadWriteExtendedGizmoMemberAccessor extends AbstractReadWriteGizmoMemberAccessor
        implements ExtendedMemberAccessor {

    @Override
    public Object executeGetter(Object bean) {
        throw new UnsupportedOperationException(
                "The method executeGetter(Object) without parameter is not supported. Maybe call executeGetter(Object, Object) from ExtendedMemberAccessor instead.");
    }

    @Override
    public <Fact_, Result_> Function<Fact_, Result_> getGetterFunction() {
        throw new UnsupportedOperationException("The method getGetterFunction() is not supported.");
    }

}
