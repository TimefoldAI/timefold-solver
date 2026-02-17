package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Consumer;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

interface GizmoMemberHandler {

    /**
     * Creates handler for a {@link Field}.
     *
     * @param declaringClass never null, class that declares the {@link Field} in question
     * @param name never null, name of the field
     * @param fieldDescriptor never null, descriptor of the {@link Field} in question
     * @param ignoreFinalChecks true if Quarkus will make the field non-final for us
     * @return never null
     */
    static GizmoMemberHandler of(Class<?> declaringClass, String name, FieldDesc fieldDescriptor,
            boolean ignoreFinalChecks) {
        try {
            Field field = declaringClass.getField(name);
            return new GizmoFieldHandler(declaringClass, fieldDescriptor,
                    ignoreFinalChecks, ignoreFinalChecks || !Modifier.isFinal(field.getModifiers()));
        } catch (NoSuchFieldException e) { // The field is only used for its metadata and never actually called.
            return new GizmoFieldHandler(declaringClass, fieldDescriptor, ignoreFinalChecks, false);
        }
    }

    static GizmoMemberHandler of(Class<?> declaringClass, MethodDesc methodDescriptor) {
        return of(declaringClass, null, methodDescriptor);
    }

    /**
     * Creates handler for a {@link Method}.
     *
     * @param declaringClass never null, class that declares the {@link Method} in question
     * @param readMethodParameterType the method parameter type. It is null if the method has no parameter.
     * @param methodDescriptor never null, descriptor of the {@link Method} in question
     * @return never null
     */
    static GizmoMemberHandler of(Class<?> declaringClass, Class<?> readMethodParameterType, MethodDesc methodDescriptor) {
        return readMethodParameterType != null
                ? new GizmoMethodExtendedHandler(declaringClass, readMethodParameterType, methodDescriptor)
                : new GizmoMethodHandler(declaringClass, methodDescriptor);
    }

    void whenIsField(Consumer<FieldDesc> fieldDescriptorConsumer);

    void whenIsMethod(Consumer<MethodDesc> methodDescriptorConsumer);

    Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj);

    Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj, Expr parameter);

    boolean writeMemberValue(MethodDesc setter, BlockCreator bytecodeCreator, Expr thisObj,
            Expr newValue);

    String getDeclaringClassName();

    String getTypeName();

    Type getType();

    static String getTypeName(ClassDesc classDesc) {
        if (classDesc.isPrimitive()) {
            // will return "int", "boolean", etc. for primitive types,
            // but the simple name for class types
            return classDesc.displayName();
        }
        if (classDesc.isArray()) {
            return getTypeName(classDesc.componentType()) + "[]";
        }
        // class descriptor format
        // "L" + className + ";"
        var classDescriptor = classDesc.descriptorString();
        return classDescriptor.substring(1, classDescriptor.length() - 1).replace('/', '.');
    }
}
