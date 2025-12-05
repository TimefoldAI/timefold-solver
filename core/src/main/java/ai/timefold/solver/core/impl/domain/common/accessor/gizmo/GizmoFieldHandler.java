package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.reflect.Type;
import java.util.function.Consumer;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

final class GizmoFieldHandler implements GizmoMemberHandler {

    private final Class<?> declaringClass;
    private final FieldDesc fieldDescriptor;
    private final boolean canBeWritten;

    GizmoFieldHandler(Class<?> declaringClass, FieldDesc fieldDescriptor, boolean canBeWritten) {
        this.declaringClass = declaringClass;
        this.fieldDescriptor = fieldDescriptor;
        this.canBeWritten = canBeWritten;
    }

    @Override
    public void whenIsField(Consumer<FieldDesc> fieldDescriptorConsumer) {
        fieldDescriptorConsumer.accept(fieldDescriptor);
    }

    @Override
    public void whenIsMethod(Consumer<MethodDesc> methodDescriptorConsumer) {
        // Do nothing.
    }

    @Override
    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj) {
        return thisObj.field(fieldDescriptor);
    }

    @Override
    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj, Expr parameter) {
        throw new IllegalStateException("Cannot pass a parameter when reading member value.");
    }

    @Override
    public boolean writeMemberValue(MethodDesc setter, BlockCreator bytecodeCreator, Expr thisObj,
            Expr newValue) {
        if (canBeWritten) {
            bytecodeCreator.set(thisObj.field(fieldDescriptor), newValue);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getDeclaringClassName() {
        return GizmoMemberHandler.getTypeName(fieldDescriptor.owner());
    }

    @Override
    public String getTypeName() {
        return GizmoMemberHandler.getTypeName(fieldDescriptor.type());
    }

    @Override
    public Type getType() {
        try {
            return declaringClass.getDeclaredField(fieldDescriptor.name()).getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    "Cannot find field (%s) on class (%s).".formatted(fieldDescriptor.name(), declaringClass), e);
        }
    }

    @Override
    public String toString() {
        return fieldDescriptor.toString();
    }

}
