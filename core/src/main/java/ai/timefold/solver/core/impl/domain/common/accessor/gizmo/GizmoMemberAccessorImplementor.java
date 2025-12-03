package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.timefold.solver.core.impl.domain.common.accessor.ExtendedMemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.util.MutableReference;

import org.jspecify.annotations.NonNull;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Generates the bytecode for the MemberAccessor of a particular Member
 */
public final class GizmoMemberAccessorImplementor {
    /**
     * Generates the constructor and implementations of {@link AbstractGizmoMemberAccessor} methods for the given
     * {@link Member}.
     *
     * @param className never null
     * @param classOutput never null, defines how to write the bytecode
     * @param memberInfo never null, member to generate MemberAccessor methods implementation for
     */
    public static void defineAccessorFor(String className, ClassOutput classOutput, GizmoMemberInfo memberInfo) {
        Class<? extends AbstractGizmoMemberAccessor> superClass = getCorrectSuperclass(memberInfo);
        var gizmo = Gizmo.create(classOutput);
        gizmo.class_(className, classCreator -> {
            classCreator.final_();
            classCreator.extends_(superClass);

            var genericType = classCreator.field("genericType", fieldCreator -> {
                fieldCreator.final_();
                fieldCreator.setType(Type.class);
            });

            // We only add the field if the read method accepts a parameter
            FieldDesc methodParameterType = null;
            if (memberInfo.readMethodWithParameter()) {
                methodParameterType = classCreator.field("readMethodParameterType", fieldCreator -> {
                    fieldCreator.final_();
                    fieldCreator.setType(Type.class);
                    fieldCreator.setInitial((Class<?>) memberInfo.descriptor().getMethodParameterType());
                });
            }

            var annotatedElement = classCreator.field("annotatedElement", fieldCreator -> {
                fieldCreator.final_();
                fieldCreator.setType(AnnotatedElement.class);
            });

            var generatedClassInfo =
                    new GeneratedClassInfo(classCreator, genericType, methodParameterType, annotatedElement, memberInfo);

            // ************************************************************************
            // MemberAccessor methods
            // ************************************************************************
            createConstructor(generatedClassInfo);
            createGetName(generatedClassInfo);
            createGetDeclaringClass(generatedClassInfo);
            createGetType(generatedClassInfo);
            createGetGenericType(generatedClassInfo);
            if (ExtendedMemberAccessor.class.isAssignableFrom(superClass)) {
                // The read method with a parameter requires a different getter implementation
                // and another method to return the parameter type.
                createGetGetterMethodParameterType(generatedClassInfo);
                createExecuteGetterWithParameter(generatedClassInfo);
            } else {
                createExecuteGetter(generatedClassInfo);
            }
            if (superClass == AbstractReadWriteGizmoMemberAccessor.class
                    || superClass == AbstractReadWriteExtendedGizmoMemberAccessor.class) {
                createExecuteSetter(generatedClassInfo);
            }
            createGetAnnotation(generatedClassInfo);
            createDeclaredAnnotationsByType(generatedClassInfo);
        });
    }

    private static Class<? extends AbstractGizmoMemberAccessor> getCorrectSuperclass(GizmoMemberInfo memberInfo) {
        AtomicBoolean supportsSetter = new AtomicBoolean();
        AtomicBoolean methodWithParameter = new AtomicBoolean();
        memberInfo.descriptor().whenIsMethod(method -> {
            supportsSetter.set(memberInfo.descriptor().getSetter().isPresent());
            methodWithParameter.set(memberInfo.readMethodWithParameter());
        });
        memberInfo.descriptor().whenIsField(field -> {
            supportsSetter.set(true);
            methodWithParameter.set(false);
        });
        if (supportsSetter.get()) {
            return methodWithParameter.get() ? AbstractReadWriteExtendedGizmoMemberAccessor.class
                    : AbstractReadWriteGizmoMemberAccessor.class;
        } else {
            return methodWithParameter.get() ? AbstractReadOnlyExtendedGizmoMemberAccessor.class
                    : AbstractReadOnlyGizmoMemberAccessor.class;
        }
    }

    /**
     * Creates a MemberAccessor for a given member, generating
     * the MemberAccessor bytecode if required
     *
     * @param member The member to generate a MemberAccessor for
     * @param annotationClass The annotation it was annotated with (used for
     *        error reporting)
     * @param accessorInfo additional information of the accessor
     * @param gizmoClassLoader never null
     * @return A new MemberAccessor that uses Gizmo generated bytecode.
     *         Will generate the bytecode the first type it is called
     *         for a member, unless a classloader has been set,
     *         in which case no Gizmo code will be generated.
     */
    static MemberAccessor createAccessorFor(Member member, Class<? extends Annotation> annotationClass,
            AccessorInfo accessorInfo, GizmoClassLoader gizmoClassLoader) {
        String className = GizmoMemberAccessorFactory.getGeneratedClassName(member);
        if (gizmoClassLoader.hasBytecodeFor(className)) {
            return createInstance(className, gizmoClassLoader);
        }
        final MutableReference<byte @NonNull []> classBytecodeHolder = new MutableReference<>(null);
        ClassOutput classOutput = (path, byteCode) -> classBytecodeHolder.setValue(byteCode);
        var descriptor = new GizmoMemberDescriptor(member, accessorInfo.readMethodWithParameter());
        GizmoMemberInfo memberInfo = new GizmoMemberInfo(descriptor, accessorInfo.returnTypeRequired(),
                descriptor.getMethodParameterType() != null, annotationClass);
        defineAccessorFor(className, classOutput, memberInfo);
        byte[] classBytecode = classBytecodeHolder.getValue();

        gizmoClassLoader.storeBytecode(className, classBytecode);
        return createInstance(className, gizmoClassLoader);
    }

    private static MemberAccessor createInstance(String className, GizmoClassLoader gizmoClassLoader) {
        try {
            return (MemberAccessor) gizmoClassLoader.loadClass(className)
                    .getConstructor().newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException
                | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    // ************************************************************************
    // MemberAccessor methods
    // ************************************************************************
    private static void createConstructor(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;

        classCreator.constructor(constructorCreator -> constructorCreator.body(blockCreator -> {
            var thisObj = constructorCreator.this_();

            // Invoke Object's constructor
            blockCreator.invokeSpecial(ConstructorDesc.of(classCreator.superClass()), thisObj);

            var declaringClass = blockCreator.localVar("declaringClass", Const.of(ClassDesc.of(
                    memberInfo.descriptor().getDeclaringClassName())));
            memberInfo.descriptor().whenMetadataIsOnField(md -> {
                var name = Const.of(md.name());
                var field = blockCreator.localVar("declaredField", blockCreator.invokeVirtual(
                        MethodDesc.of(Class.class, "getDeclaredField",
                                Field.class, String.class),
                        declaringClass, name));
                var type =
                        blockCreator.invokeVirtual(
                                MethodDesc.of(Field.class, "getGenericType", Type.class),
                                field);
                blockCreator.set(thisObj.field(generatedClassInfo.genericTypeField), type);
                blockCreator.set(thisObj.field(generatedClassInfo.annotatedElementField), field);
            });

            memberInfo.descriptor().whenMetadataIsOnMethod(md -> {
                var name = Const.of(md.name());
                // If the read method takes a parameter,
                // we must specify the type of that parameter to locate the declared method
                var parameterArray = generatedClassInfo.readMethodParameterTypeField() != null
                        ? blockCreator.newArray(Class.class,
                                Const.of((Class<?>) generatedClassInfo.memberInfo.descriptor().getMethodParameterType()))
                        : blockCreator.newEmptyArray(Class.class, 0);
                var method = blockCreator.localVar("method", blockCreator.invokeVirtual(
                        MethodDesc.of(Class.class, "getDeclaredMethod",
                                Method.class, String.class, Class[].class),
                        declaringClass, name, parameterArray));
                if (memberInfo.returnTypeRequired()) {
                    // We create a field to store the result, only if the called method has a return type.
                    // Otherwise, we will only execute it
                    var type =
                            blockCreator.invokeVirtual(MethodDesc.of(Method.class, "getGenericReturnType", Type.class), method);
                    blockCreator.set(thisObj.field(generatedClassInfo.genericTypeField), type);
                }
                blockCreator.set(thisObj.field(generatedClassInfo.annotatedElementField), method);
            });
            blockCreator.return_();
        }));
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Class getDeclaringClass() {
     *     return ClassThatDeclaredMember.class;
     * }
     * </pre>
     */
    private static void createGetDeclaringClass(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;

        classCreator.method("getDeclaringClass", builder -> {
            builder.public_();
            builder.returning(Class.class);
            builder.body(blockCreator -> blockCreator.return_(
                    Const.of(ClassDesc.of(
                            memberInfo.descriptor().getDeclaringClassName()))));
        });
    }

    /**
     * Asserts method is a getter or read method
     *
     * @param method Method to assert is getter or read
     * @param returnTypeRequired Flag used to check method return type
     * @param readMethodWithParameter Flag used to enable the method to accept an argument
     */
    private static void assertIsGoodMethod(MethodDesc method, boolean returnTypeRequired, boolean readMethodWithParameter) {
        // V = void return type
        // Z = primitive boolean return type
        String methodName = method.name();
        if (!readMethodWithParameter && method.parameterCount() != 0) {
            // not read or getter method
            throw new IllegalStateException("The getterMethod (%s) must not have any parameters, but has parameters (%s)."
                    .formatted(methodName, Arrays.toString(method.parameterTypes().toArray())));
        }
        if (methodName.startsWith("get")) {
            if (method.returnType().equals(ConstantDescs.CD_void)) {
                throw new IllegalStateException("The getterMethod (%s) must have a non-void return type."
                        .formatted(methodName));
            }
        } else if (methodName.startsWith("is")) {
            if (!method.returnType().equals(ConstantDescs.CD_boolean)) {
                throw new IllegalStateException("""
                        The getterMethod (%s) must have a primitive boolean return type but returns (%s).
                        Maybe rename the method (get%s)?"""
                        .formatted(methodName, method.returnType(), methodName.substring(2)));
            }
        } else {
            // must be a read method
            if (returnTypeRequired && method.returnType().equals(ConstantDescs.CD_void)) {
                throw new IllegalStateException("The readMethod (%s) must have a non-void return type."
                        .formatted(methodName));
            }
        }
    }

    /**
     * Asserts method is a getter or read method
     *
     * @param method Method to assert is getter or read
     * @param returnTypeRequired Flag used to check method return type
     * @param readMethodWithParameter Flag used to enable the method to accept an argument
     * @param annotationClass Used in exception message
     */
    private static void assertIsGoodMethod(MethodDesc method, boolean returnTypeRequired, boolean readMethodWithParameter,
            Class<? extends Annotation> annotationClass) {
        // V = void return type
        // Z = primitive boolean return type
        String methodName = method.name();
        if (!readMethodWithParameter && method.parameterCount() != 0) {
            // not read or getter method
            throw new IllegalStateException(
                    "The getterMethod (%s) with a %s annotation must not have any parameters, but has parameters (%s)."
                            .formatted(methodName, annotationClass.getSimpleName(),
                                    method.parameterTypes().stream().map(ClassDesc::descriptorString).toList()));
        }
        if (methodName.startsWith("get")) {
            if (method.returnType().equals(ConstantDescs.CD_void)) {
                throw new IllegalStateException("The getterMethod (%s) with a %s annotation must have a non-void return type."
                        .formatted(methodName, annotationClass.getSimpleName()));
            }
        } else if (methodName.startsWith("is")) {
            if (!method.returnType().equals(ConstantDescs.CD_boolean)) {
                throw new IllegalStateException("""
                        The getterMethod (%s) with a %s annotation must have a primitive boolean return type but returns (%s).
                        Maybe rename the method (get%s)?"""
                        .formatted(methodName, annotationClass.getSimpleName(), method.returnType().descriptorString(),
                                methodName.substring(2)));
            }
        } else {
            // must be a read method and return a result only if returnTypeRequired is true
            if (returnTypeRequired && method.returnType().equals(ConstantDescs.CD_void)) {
                throw new IllegalStateException("The readMethod (%s) with a %s annotation must have a non-void return type."
                        .formatted(methodName, annotationClass.getSimpleName()));
            }
        }
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * String getName() {
     *     return "fieldOrMethodName";
     * }
     * </pre>
     *
     * If it is a getter method, "get" is removed and the first
     * letter become lowercase
     */
    private static void createGetName(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;

        classCreator.method("getName", builder -> {
            builder.public_();
            builder.returning(String.class);
            builder.body(blockCreator -> {
                // If it is a method, assert that it has the required
                // properties
                memberInfo.descriptor().whenIsMethod(method -> {
                    var annotationClass = memberInfo.annotationClass();
                    if (annotationClass == null) {
                        assertIsGoodMethod(method, memberInfo.returnTypeRequired(), memberInfo.readMethodWithParameter());
                    } else {
                        assertIsGoodMethod(method, memberInfo.returnTypeRequired(), memberInfo.readMethodWithParameter(),
                                annotationClass);
                    }
                });

                String fieldName = memberInfo.descriptor().getName();
                blockCreator.return_(Const.of(fieldName));
            });
        });

    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Class getType() {
     *     return FieldTypeOrMethodReturnType.class;
     * }
     * </pre>
     */
    private static void createGetType(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;

        classCreator.method("getType", builder -> {
            builder.public_();
            builder.returning(Class.class);
            builder.body(blockCreator -> {
                if (memberInfo.descriptor().getType() instanceof Class<?> clazz) {
                    blockCreator.return_(Const.of(clazz));
                } else {
                    blockCreator.return_(
                            Const.of(ClassDesc.of(memberInfo.descriptor().getTypeName())));
                }
            });
        });
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Type getGenericType() {
     *     return GizmoMemberAccessorImplementor.getGenericTypeFor(this.getClass().getName());
     * }
     * </pre>
     *
     * We are unable to load a non-primitive object constant, so we need to store it
     * in the implementor, which then can return us the Type when needed. The type
     * is stored in gizmoMemberAccessorNameToGenericType when this method is called.
     */
    private static void createGetGenericType(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        classCreator.method("getGenericType", builder -> {
            builder.public_();
            builder.returning(Type.class);
            builder.body(blockCreator -> blockCreator.return_(classCreator.this_().field(generatedClassInfo.genericTypeField)));
        });
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Type getGetterMethodParameterType() {
     *     return GizmoMemberAccessorImplementor.getGenericTypeFor(this.readMethodParameterType);
     * }
     * </pre>
     */
    private static void createGetGetterMethodParameterType(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        classCreator.method("getGetterMethodParameterType", builder -> {
            builder.public_();
            builder.returning(Type.class);
            builder.body(blockCreator -> blockCreator
                    .return_(classCreator.this_().field(generatedClassInfo.readMethodParameterTypeField)));
        });
    }

    /**
     * Generates the following code:
     * <p>
     * For a field
     *
     * <pre>
     * Object executeGetter(Object bean) {
     *     return ((DeclaringClass) bean).field;
     * }
     * </pre>
     *
     * For a method with returning type
     *
     * <pre>
     * Object executeGetter(Object bean) {
     *     return ((DeclaringClass) bean).method();
     * }
     * </pre>
     *
     * For a method without returning type
     *
     * <pre>
     * Object executeGetter(Object bean) {
     *     ((DeclaringClass) bean).method();
     *     return null;
     * }
     * </pre>
     *
     * The member MUST be public if not called in Quarkus
     * (i.e. we don't delegate to the field getter/setter).
     * In Quarkus, we generate simple getter/setter for the
     * member if it is private (which get passed to the MemberDescriptor).
     */
    private static void createExecuteGetter(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;
        classCreator.method("executeGetter", builder -> {
            builder.public_();
            builder.returning(Object.class);
            var bean = builder.parameter("bean", Object.class);
            builder.body(blockCreator -> {
                var castedBean =
                        blockCreator.localVar("castedBean", ClassDesc.of(memberInfo.descriptor().getDeclaringClassName()),
                                bean);
                if (memberInfo.returnTypeRequired()) {
                    blockCreator.return_(memberInfo.descriptor().readMemberValue(blockCreator, castedBean));
                } else {
                    memberInfo.descriptor().readMemberValue(blockCreator, castedBean);
                    // Returns null as the called method has no return type
                    blockCreator.returnNull();
                }
            });
        });
    }

    /**
     * Generates the following code:
     * <p>
     *
     * For a method with returning type
     *
     * <pre>
     * Object executeGetter(Object bean, Object value) {
     *     return ((DeclaringClass) bean).method((ParameterType) value);
     * }
     * </pre>
     *
     * For a method without returning type
     *
     * <pre>
     * Object executeGetter(Object bean, Object value) {
     *     ((DeclaringClass) bean).method((ParameterType) value);
     *     return null;
     * }
     * </pre>
     *
     * The member MUST be public if not called in Quarkus
     * (i.e. we don't delegate to the field getter/setter).
     * In Quarkus, we generate simple getter/setter for the
     * member if it is private (which get passed to the MemberDescriptor).
     */
    private static void createExecuteGetterWithParameter(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;
        classCreator.method("executeGetter", builder -> {
            builder.public_();
            builder.returning(Object.class);
            var bean = builder.parameter("bean", Object.class);
            var value = builder.parameter("value", Object.class);
            builder.body(blockCreator -> {
                var castedBean =
                        blockCreator.localVar("castedBean", ClassDesc.of(memberInfo.descriptor().getDeclaringClassName()),
                                bean);
                var castedValue =
                        blockCreator.localVar("castedValue",
                                ClassDesc.of(memberInfo.descriptor().getMethodParameterType().getTypeName()),
                                value);
                if (memberInfo.returnTypeRequired()) {
                    blockCreator.return_(memberInfo.descriptor().readMemberValue(blockCreator, castedBean, castedValue));
                } else {
                    memberInfo.descriptor().readMemberValue(blockCreator, castedBean);
                    // Returns null as the called method has no return type
                    blockCreator.returnNull();
                }
            });
        });
    }

    /**
     * Generates the following code:
     * <p>
     * For a field
     *
     * <pre>
     * void executeSetter(Object bean, Object value) {
     *     return ((DeclaringClass) bean).field = value;
     * }
     * </pre>
     *
     * For a getter method with a corresponding setter
     *
     * <pre>
     * void executeSetter(Object bean, Object value) {
     *     return ((DeclaringClass) bean).setValue(value);
     * }
     * </pre>
     *
     * For a read method or a getter method without a setter
     *
     * <pre>
     * void executeSetter(Object bean, Object value) {
     *     throw new UnsupportedOperationException("Setter not supported");
     * }
     * </pre>
     */
    private static void createExecuteSetter(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        var memberInfo = generatedClassInfo.memberInfo;
        classCreator.method("executeSetter", builder -> {
            builder.public_();
            builder.returning(void.class);
            var bean = builder.parameter("bean", Object.class);
            var value = builder.parameter("value", Object.class);
            builder.body(blockCreator -> {
                var castedBean =
                        blockCreator.localVar("castedBean", ClassDesc.of(memberInfo.descriptor().getDeclaringClassName()),
                                bean);
                if (memberInfo.descriptor().writeMemberValue(blockCreator, castedBean, value)) {
                    // we are here only if writing was successful
                    blockCreator.return_();
                } else {
                    blockCreator.throw_(blockCreator.new_(UnsupportedOperationException.class,
                            Const.of("Setter not supported")));
                }
            });
        });
    }

    private static MethodDesc getAnnotationMethod(Class<?> returnType, String methodName, Class<?>... parameters) {
        return MethodDesc.of(AnnotatedElement.class, methodName, returnType, parameters);
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Object getAnnotation(Class annotationClass) {
     *     return annotatedElement.getAnnotation(annotationClass);
     * }
     * </pre>
     */
    private static void createGetAnnotation(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        classCreator.method("getAnnotation", builder -> {
            builder.public_();
            builder.returning(Annotation.class);
            var query = builder.parameter("query", Class.class);
            builder.body(blockCreator -> {
                var annotatedElement = classCreator.this_().field(generatedClassInfo.annotatedElementField);
                blockCreator.return_(
                        blockCreator.invokeInterface(getAnnotationMethod(Annotation.class, "getAnnotation", Class.class),
                                annotatedElement, query));
            });
        });
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Annotation[] getDeclaredAnnotationsByType(Class annotationClass) {
     *     return annotatedElement.getDeclaredAnnotationsByType(annotationClass);
     * }
     * </pre>
     */
    private static void createDeclaredAnnotationsByType(GeneratedClassInfo generatedClassInfo) {
        var classCreator = generatedClassInfo.classCreator;
        classCreator.method("getDeclaredAnnotationsByType", builder -> {
            builder.public_();
            builder.returning(Annotation[].class);
            var query = builder.parameter("query", Class.class);
            builder.body(blockCreator -> {
                var annotatedElement = classCreator.this_().field(generatedClassInfo.annotatedElementField);
                blockCreator.return_(blockCreator.invokeInterface(
                        getAnnotationMethod(Annotation[].class, "getDeclaredAnnotationsByType", Class.class),
                        annotatedElement, query));
            });
        });
    }

    private GizmoMemberAccessorImplementor() {

    }

    private record GeneratedClassInfo(ClassCreator classCreator, FieldDesc genericTypeField,
            FieldDesc readMethodParameterTypeField, FieldDesc annotatedElementField, GizmoMemberInfo memberInfo) {
    }
}
