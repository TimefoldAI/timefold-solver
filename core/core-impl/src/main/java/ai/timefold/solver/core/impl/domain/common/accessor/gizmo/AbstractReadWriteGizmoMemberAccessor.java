package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

public abstract class AbstractReadWriteGizmoMemberAccessor extends AbstractGizmoMemberAccessor {

    @Override
    public final boolean supportSetter() {
        return true;
    }

}
