package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

@NullMarked
final class GizmoFieldHandler implements GizmoMemberHandler {

    private final Class<?> declaringClass;
    private final FieldDesc fieldDescriptor;
    private final @Nullable MethodDesc getterDescriptor;
    private final @Nullable MethodDesc setterDescriptor;
    private final boolean canBeWritten;

    GizmoFieldHandler(Class<?> declaringClass, FieldDesc fieldDescriptor, boolean canBeWritten) {
        this.declaringClass = declaringClass;
        this.fieldDescriptor = fieldDescriptor;
        var getterMethod = ReflectionHelper.getGetterMethod(declaringClass, fieldDescriptor.name());
        var setterMethod = ReflectionHelper.getSetterMethod(declaringClass, fieldDescriptor.name());

        if (getterMethod == null) {
            getterDescriptor = null;
            setterDescriptor = null;
            this.canBeWritten = canBeWritten;
        } else {
            ReflectionHelper.assertGetterMethod(getterMethod);
            getterDescriptor = MethodDesc.of(getterMethod);

            if (setterMethod != null && Modifier.isPublic(setterMethod.getModifiers())) {
                setterDescriptor = MethodDesc.of(setterMethod);
                this.canBeWritten = true;
            } else {
                setterDescriptor = null;
                this.canBeWritten = false;
            }
        }
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
        if (getterDescriptor != null) {
            return bytecodeCreator.invokeVirtual(getterDescriptor, thisObj);
        }
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
            if (setterDescriptor != null) {
                bytecodeCreator.invokeVirtual(setterDescriptor, thisObj, newValue);
                return true;
            }
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
