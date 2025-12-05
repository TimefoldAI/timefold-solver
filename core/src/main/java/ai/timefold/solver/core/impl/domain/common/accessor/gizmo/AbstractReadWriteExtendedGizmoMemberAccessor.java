package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.util.function.Function;

public abstract class AbstractReadWriteExtendedGizmoMemberAccessor extends AbstractReadWriteGizmoMemberAccessor {

    @Override
    public Object executeGetter(Object bean) {
        throw new UnsupportedOperationException(
                "Impossible state: the method executeGetter(Object) without parameter is not supported.");
    }

    @Override
    public <Fact_, Result_> Function<Fact_, Result_> getGetterFunction() {
        throw new UnsupportedOperationException("Impossible state: the method getGetterFunction() is not supported.");
    }

}
