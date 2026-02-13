package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Describe and provide simplified/unified access for {@link Member}.
 */
@NullMarked
public final class GizmoMemberDescriptor {

    /**
     * The name of a member.
     * For a field, it is the field name.
     * For a method,
     * if it is a getter, the method name without "get"/"is" and the first letter lowercase;
     * otherwise, the method name.
     */
    private final String name;

    /**
     * The type of the read method parameter. Null if the method does not accept a parameter.
     */
    @Nullable
    private final Type methodParameterType;

    private final AccessorInfo accessorInfo;

    private final GizmoMemberHandler memberHandler;

    /**
     * Should only be used for metadata (i.e. Generic Type and Annotated Element).
     */
    private final GizmoMemberHandler metadataHandler;

    /**
     * The MethodDescriptor of the corresponding setter. Empty if not present.
     */
    @Nullable
    private final MethodDesc setter;

    public GizmoMemberDescriptor(Member member) {
        this(member, AccessorInfo.withReturnValueAndArguments());
    }

    public GizmoMemberDescriptor(Member member, AccessorInfo accessorInfo) {
        Class<?> declaringClass = member.getDeclaringClass();
        this.accessorInfo = accessorInfo;
        if (member instanceof Field field) {
            var fieldDescriptor = FieldDesc.of(field);
            this.name = member.getName();
            this.memberHandler = GizmoMemberHandler.of(declaringClass, name, fieldDescriptor, false);
            this.setter = null;
            this.methodParameterType = null;
        } else if (member instanceof Method method) {
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("""
                        Member (%s) of class (%s) is not public."""
                        .formatted(method.getName(), declaringClass.getName()));
            }
            var methodDescriptor = MethodDesc.of(method);
            this.name = ReflectionHelper.isGetterMethod(method) ? ReflectionHelper.getGetterPropertyName(member)
                    : member.getName();
            this.methodParameterType = getMethodParameterType(method, accessorInfo.readMethodWithParameter());
            this.memberHandler = GizmoMemberHandler.of(declaringClass, (Class<?>) methodParameterType, methodDescriptor);
            this.setter = lookupSetter(methodDescriptor, declaringClass, name).orElse(null);
        } else {
            throw new IllegalArgumentException("%s is not a Method or a Field.".formatted(member));
        }
        this.metadataHandler = this.memberHandler;
    }

    public GizmoMemberDescriptor(String name, FieldDesc fieldDescriptor, Class<?> declaringClass,
            AccessorInfo accessorInfo) {
        this.name = name;
        this.memberHandler = GizmoMemberHandler.of(declaringClass, name, fieldDescriptor, true);
        this.metadataHandler = this.memberHandler;
        this.setter = null;
        this.methodParameterType = null;
        this.accessorInfo = accessorInfo;
    }

    public GizmoMemberDescriptor(String name, MethodDesc memberDescriptor, MethodDesc metadataDescriptor,
            Type methodParameterType, Class<?> declaringClass, @Nullable MethodDesc setterDescriptor,
            AccessorInfo accessorInfo) {
        this.name = name;
        this.memberHandler = GizmoMemberHandler.of(declaringClass, (Class<?>) methodParameterType, memberDescriptor);
        this.metadataHandler = memberDescriptor == metadataDescriptor ? this.memberHandler
                : GizmoMemberHandler.of(declaringClass, metadataDescriptor);
        this.methodParameterType = methodParameterType;
        this.setter = setterDescriptor;
        this.accessorInfo = accessorInfo;
    }

    public GizmoMemberDescriptor(String name, MethodDesc memberDescriptor, Type methodParameterType, Class<?> declaringClass,
            @Nullable MethodDesc setterDescriptor, AccessorInfo accessorInfo) {
        this.name = name;
        this.memberHandler = GizmoMemberHandler.of(declaringClass, (Class<?>) methodParameterType, memberDescriptor);
        this.metadataHandler = this.memberHandler;
        this.methodParameterType = methodParameterType;
        this.setter = setterDescriptor;
        this.accessorInfo = accessorInfo;
    }

    public GizmoMemberDescriptor(String name, MethodDesc memberDescriptor, FieldDesc metadataDescriptor,
            @Nullable Type methodParameterType, Class<?> declaringClass, @Nullable MethodDesc setterDescriptor) {
        this.name = name;
        this.memberHandler = GizmoMemberHandler.of(declaringClass, (Class<?>) methodParameterType, memberDescriptor);
        this.metadataHandler = GizmoMemberHandler.of(declaringClass, name, metadataDescriptor, true);
        this.methodParameterType = methodParameterType;
        this.setter = setterDescriptor;
        this.accessorInfo = AccessorInfo.of(MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD);
    }

    @Nullable
    public static Type getMethodParameterType(Method method, boolean methodWithParameter) {
        var parameterCount = method.getParameterCount();
        Type methodParameterType = null;
        if (methodWithParameter && parameterCount == 1) {
            methodParameterType = method.getParameterTypes()[0];
        }
        if (methodWithParameter && parameterCount > 1) {
            // Multiple parameters are not allowed
            throw new IllegalStateException("The getterMethod (%s) must have only one parameter (%s)."
                    .formatted(method.getName(), Arrays.toString(method.getParameterTypes())));
        } else if (parameterCount == 1) {
            return methodParameterType;
        } else {
            return null;
        }
    }

    /**
     * If the member accessor is a field, pass the member's field descriptor to the
     * provided consumer. Otherwise, do nothing. Returns self for chaining.
     *
     * @param fieldDescriptorConsumer What to do if the member a field.
     * @return this
     */
    public GizmoMemberDescriptor whenIsField(Consumer<FieldDesc> fieldDescriptorConsumer) {
        memberHandler.whenIsField(fieldDescriptorConsumer);
        return this;
    }

    /**
     * If the member accessor is a method, pass the member's method descriptor to the
     * provided consumer. Otherwise, do nothing. Returns self for chaining.
     *
     * @param methodDescriptorConsumer What to do if the member a method.
     * @return this
     */
    public GizmoMemberDescriptor whenIsMethod(Consumer<MethodDesc> methodDescriptorConsumer) {
        memberHandler.whenIsMethod(methodDescriptorConsumer);
        return this;
    }

    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj) {
        return memberHandler.readMemberValue(bytecodeCreator, thisObj);
    }

    public Expr readMemberValue(BlockCreator bytecodeCreator, Expr thisObj, Expr parameter) {
        return memberHandler.readMemberValue(bytecodeCreator, thisObj, parameter);
    }

    /**
     * Write the bytecode for writing to this member. If there is no setter,
     * it write the bytecode for throwing the exception. Return true if
     * it was able to write the member value.
     *
     * @param bytecodeCreator the bytecode creator to use
     * @param thisObj the bean to write the new value to
     * @param newValue to new value of the member
     * @return True if it was able to write the member value, false otherwise
     */
    public boolean writeMemberValue(BlockCreator bytecodeCreator, Expr thisObj, Expr newValue) {
        return memberHandler.writeMemberValue(setter, bytecodeCreator, thisObj, newValue);
    }

    /**
     * If the member metadata is on a field, pass the member's field descriptor to the
     * provided consumer. Otherwise, do nothing. Returns self for chaining.
     *
     * @param fieldDescriptorConsumer What to do if the member a field.
     * @return this
     */
    public GizmoMemberDescriptor whenMetadataIsOnField(Consumer<FieldDesc> fieldDescriptorConsumer) {
        metadataHandler.whenIsField(fieldDescriptorConsumer);
        return this;
    }

    /**
     * If the member metadata is on a method, pass the member's method descriptor to the
     * provided consumer. Otherwise, do nothing. Returns self for chaining.
     *
     * @param methodDescriptorConsumer What to do if the member a method.
     * @return this
     */
    public GizmoMemberDescriptor whenMetadataIsOnMethod(Consumer<MethodDesc> methodDescriptorConsumer) {
        metadataHandler.whenIsMethod(methodDescriptorConsumer);
        return this;
    }

    /**
     * Returns the declaring class name of the member in descriptor format.
     * For instance, the declaring class name of Object.toString() is "java/lang/Object".
     *
     * @return Returns the declaring class name of the member in descriptor format
     */
    public String getDeclaringClassName() {
        return memberHandler.getDeclaringClassName();
    }

    public Optional<MethodDesc> getSetter() {
        return Optional.ofNullable(setter);
    }

    private static Optional<MethodDesc> lookupSetter(Object memberDescriptor, Class<?> declaringClass, String name) {
        if (memberDescriptor instanceof MethodDesc) {
            return Optional.ofNullable(ReflectionHelper.getSetterMethod(declaringClass, name))
                    .map(MethodDesc::of);
        } else {
            return Optional.empty();
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the member type (for fields) / return type (for methods) name.
     * The name does not include generic information.
     */
    public String getTypeName() {
        return metadataHandler.getTypeName();
    }

    public Type getType() {
        return metadataHandler.getType();
    }

    @Nullable
    public Type getMethodParameterType() {
        return methodParameterType;
    }

    public AccessorInfo getAccessorInfo() {
        return accessorInfo;
    }

    @Override
    public String toString() {
        return memberHandler.toString();
    }
}
