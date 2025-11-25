package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

final class GizmoMethodHandler implements GizmoMemberHandler {

    private final Class<?> declaringClass;
    private final MethodDesc methodDescriptor;

    GizmoMethodHandler(Class<?> declaringClass, MethodDesc methodDescriptor) {
        this.declaringClass = declaringClass;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public void whenIsField(Consumer<FieldDesc> fieldDescriptorConsumer) {
        // Do nothing,
    }

    @Override
    public void whenIsMethod(Consumer<MethodDesc> methodDescriptorConsumer) {
        methodDescriptorConsumer.accept(methodDescriptor);
    }

    @Override
    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj) {
        return invokeMemberMethod(declaringClass, bytecodeCreator, methodDescriptor, thisObj);
    }

    @Override
    public boolean writeMemberValue(MethodDesc setter, BlockCreator bytecodeCreator, Expr thisObj,
            Expr newValue) {
        if (setter == null) {
            return false;
        } else {
            invokeMemberMethod(declaringClass, bytecodeCreator, setter, thisObj, newValue);
            return true;
        }
    }

    private Expr invokeMemberMethod(Class<?> declaringClass, BlockCreator creator, MethodDesc method,
            Expr bean, Expr... parameters) {
        if (declaringClass.isInterface()) {
            // method might be from the implementation class; we need it from the declaring interface
            var interfaceMethod = InterfaceMethodDesc.of(ClassDesc.ofDescriptor(declaringClass.descriptorString()),
                    method.name(), method.type());
            return creator.invokeInterface(interfaceMethod, bean, parameters);
        } else {
            return creator.invokeVirtual(method, bean, parameters);
        }
    }

    @Override
    public String getDeclaringClassName() {
        return GizmoMemberHandler.getTypeName(methodDescriptor.owner());
    }

    @Override
    public String getTypeName() {
        return GizmoMemberHandler.getTypeName(methodDescriptor.returnType());
    }

    @Override
    public Type getType() {
        try {
            return declaringClass.getDeclaredMethod(methodDescriptor.name()).getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Cannot find method (" + methodDescriptor.name() + ") on class (" + declaringClass + ").",
                    e);
        }
    }

    @Override
    public String toString() {
        return methodDescriptor.toString();
    }

}
