package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.reflect.Type;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

final class GizmoMethodExtendedHandler extends GizmoMethodHandler {

    private final Class<?> methodParameterType;

    GizmoMethodExtendedHandler(Class<?> declaringClass, Class<?> methodParameterType, MethodDesc methodDescriptor) {
        super(declaringClass, methodDescriptor);
        this.methodParameterType = methodParameterType;
    }

    @Override
    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj, Expr parameter) {
        return invokeMemberMethod(declaringClass, bytecodeCreator, methodDescriptor, thisObj, parameter);
    }

    @Override
    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj) {
        throw new IllegalStateException("Cannot invoke it without a parameter.");
    }

    @Override
    public Type getType() {
        try {
            return declaringClass
                    .getDeclaredMethod(methodDescriptor.name(), methodParameterType)
                    .getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot find method (%s) on class (%s)."
                    .formatted(methodDescriptor.name(), declaringClass), e);
        }
    }
}
