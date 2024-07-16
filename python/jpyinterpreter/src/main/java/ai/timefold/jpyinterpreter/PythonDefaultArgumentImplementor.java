package ai.timefold.jpyinterpreter;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.implementors.CollectionImplementor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implement classes that hold static constants used for default arguments when calling
 */
public class PythonDefaultArgumentImplementor {
    public static final String ARGUMENT_PREFIX = "argument_";
    public static final String CONSTANT_PREFIX = "DEFAULT_VALUE_";

    public static final String ARGUMENT_SPEC_STATIC_FIELD_NAME = "argumentSpec";

    public static final String KEY_TUPLE_FIELD_NAME = "keyword_args";

    public static final String REMAINING_KEY_ARGUMENTS_FIELD_NAME = "remaining_keys";

    public static final String POSITIONAL_INDEX = "positional_index";

    public static String getArgumentName(int argumentIndex) {
        return ARGUMENT_PREFIX + argumentIndex;
    }

    public static String getConstantName(int defaultIndex) {
        return CONSTANT_PREFIX + defaultIndex;
    }

    public static String createDefaultArgumentFor(MethodDescriptor methodDescriptor,
            List<PythonLikeObject> defaultArgumentList,
            Map<String, Integer> argumentNameToIndexMap, Optional<Integer> extraPositionalArgumentsVariableIndex,
            Optional<Integer> extraKeywordArgumentsVariableIndex,
            ArgumentSpec<?> argumentSpec) {
        String maybeClassName = PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE +
                methodDescriptor.getDeclaringClassInternalName().replace('/', '.') +
                "."
                + methodDescriptor.getMethodName() + "$$Defaults";
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                Type.getInternalName(Object.class), new String[] {
                        Type.getInternalName(PythonLikeFunction.class)
                });

        // static constants
        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, ARGUMENT_SPEC_STATIC_FIELD_NAME,
                Type.getDescriptor(ArgumentSpec.class), null, null);

        final int defaultStart = methodDescriptor.getParameterTypes().length - defaultArgumentList.size();
        for (int i = 0; i < defaultArgumentList.size(); i++) {
            String fieldName = getConstantName(i);
            classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, fieldName,
                    methodDescriptor.getParameterTypes()[defaultStart + i].getDescriptor(),
                    null,
                    null);
        }

        // instance fields (representing actual arguments)
        classWriter.visitField(Modifier.PRIVATE | Modifier.FINAL, KEY_TUPLE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class), null, null);
        classWriter.visitField(Modifier.PRIVATE, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class), null, null);
        classWriter.visitField(Modifier.PRIVATE, POSITIONAL_INDEX,
                Type.getDescriptor(int.class), null, null);
        for (int i = 0; i < methodDescriptor.getParameterTypes().length; i++) {
            String fieldName = getArgumentName(i);

            if (extraPositionalArgumentsVariableIndex.isPresent() && extraPositionalArgumentsVariableIndex.get() == i) {
                classWriter.visitField(Modifier.PUBLIC, fieldName,
                        Type.getDescriptor(PythonLikeTuple.class),
                        null,
                        null);
            } else if (extraKeywordArgumentsVariableIndex.isPresent() && extraKeywordArgumentsVariableIndex.get() == i) {
                classWriter.visitField(Modifier.PUBLIC, fieldName,
                        Type.getDescriptor(PythonLikeDict.class),
                        null,
                        null);
            } else {
                classWriter.visitField(Modifier.PUBLIC, fieldName,
                        methodDescriptor.getParameterTypes()[i].getDescriptor(),
                        null,
                        null);
            }
        }

        // public constructor; an instance is created for keyword function calls, since we need consistent stack frames
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeTuple.class), Type.INT_TYPE),
                        null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, KEY_TUPLE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class));
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                Type.getMethodDescriptor(Type.INT_TYPE), true);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, POSITIONAL_INDEX, Type.getDescriptor(int.class));

        for (int i = 0; i < defaultArgumentList.size(); i++) {
            int argumentIndex = i + (methodDescriptor.getParameterTypes().length - defaultArgumentList.size());
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName,
                    getConstantName(i),
                    methodDescriptor.getParameterTypes()[defaultStart + i].getDescriptor());
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName,
                    getArgumentName(argumentIndex),
                    methodDescriptor.getParameterTypes()[argumentIndex].getDescriptor());
        }

        if (extraPositionalArgumentsVariableIndex.isPresent()) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName,
                    getArgumentName(extraPositionalArgumentsVariableIndex.get()),
                    Type.getDescriptor(PythonLikeTuple.class));
        }

        if (extraKeywordArgumentsVariableIndex.isPresent()) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName,
                    getArgumentName(extraKeywordArgumentsVariableIndex.get()),
                    Type.getDescriptor(PythonLikeDict.class));
        }
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        createAddArgumentMethod(classWriter, internalClassName, methodDescriptor, argumentNameToIndexMap,
                extraPositionalArgumentsVariableIndex, extraKeywordArgumentsVariableIndex, argumentSpec);

        // clinit to set ArgumentSpec, as class cannot be loaded if it contains
        // yet to be compiled forward references
        methodVisitor = classWriter.visitMethod(Modifier.PUBLIC | Modifier.STATIC, "<clinit>",
                Type.getMethodDescriptor(Type.VOID_TYPE), null, null);
        methodVisitor.visitCode();

        argumentSpec.loadArgumentSpec(methodVisitor);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, internalClassName,
                ARGUMENT_SPEC_STATIC_FIELD_NAME, Type.getDescriptor(ArgumentSpec.class));

        for (int i = 0; i < defaultArgumentList.size(); i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(defaultStart + i);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ArgumentSpec.class),
                    "getDefaultValue", Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                    false);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                    methodDescriptor.getParameterTypes()[defaultStart + i].getInternalName());
            String fieldName = getConstantName(i);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, internalClassName,
                    fieldName, methodDescriptor.getParameterTypes()[defaultStart + i].getDescriptor());
        }

        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        classWriter.visitEnd();
        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        return internalClassName;
    }

    /**
     * Create code that look like this:
     *
     * <pre>
     * void addArgument(PythonLikeObject argument) {
     *     if (remainingKeywords > 0) {
     *         String keyword = keywordTuple.get(remainingKeywords - 1).getValue();
     *         switch (keyword) {
     *             case "key1":
     *                 argument_0 = (Argument0Type) argument;
     *                 break;
     *             case "key2":
     *                 argument_1 = (Argument1Type) argument;
     *                 break;
     *             ...
     *             default:
     *                 #ifdef EXTRA_KEYWORD_VAR
     *                 EXTRA_KEYWORD_VAR.put(keyword, argument);
     *                 #endif
     *
     *                 #ifndef EXTRA_KEYWORD_VAR
     *                 throw new TypeError();
     *                 #endif
     *         }
     *         remainingKeywords--;
     *         return;
     *     } else {
     *         switch (positionalIndex) {
     *             case 0:
     *                 argument_0 = (Argument0Type) argument;
     *                 break;
     *             case 1:
     *                 argument_1 = (Argument1Type) argument;
     *                 break;
     *             ...
     *             default:
     *                 #ifdef EXTRA_POSITIONAL_VAR
     *                 EXTRA_POSITIONAL_VAR.add(0, argument);
     *                 #endif
     *
     *                 #ifndef EXTRA_POSITIONAL_VAR
     *                 throw new TypeError();
     *                 #endif
     *         }
     *         positionalIndex--;
     *         return;
     *     }
     * }
     * </pre>
     *
     * @param classVisitor
     * @param classInternalName
     * @param methodDescriptor
     * @param argumentNameToIndexMap
     */
    private static void createAddArgumentMethod(ClassVisitor classVisitor, String classInternalName,
            MethodDescriptor methodDescriptor,
            Map<String, Integer> argumentNameToIndexMap, Optional<Integer> extraPositionalArgumentsVariableIndex,
            Optional<Integer> extraKeywordArgumentsVariableIndex,
            ArgumentSpec<?> argumentSpec) {
        MethodVisitor methodVisitor = classVisitor.visitMethod(Modifier.PUBLIC, "addArgument",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeObject.class)),
                null, null);

        methodVisitor.visitParameter("argument", 0);

        methodVisitor.visitCode();

        Label noMoreKeywordArguments = new Label();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, noMoreKeywordArguments);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, KEY_TUPLE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class));

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));

        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                true);

        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonString.class), "getValue",
                Type.getMethodDescriptor(Type.getType(String.class)),
                false);

        BytecodeSwitchImplementor.createStringSwitch(methodVisitor, argumentNameToIndexMap.keySet(),
                2, key -> {
                    int index = argumentNameToIndexMap.get(key);
                    Type parameterType = methodDescriptor.getParameterTypes()[index];
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getInternalName());
                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, getArgumentName(index),
                            parameterType.getDescriptor());
                },
                () -> {
                    if (extraKeywordArgumentsVariableIndex.isPresent()) {
                        // Extra keys dict
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName,
                                getArgumentName(extraKeywordArgumentsVariableIndex.get()),
                                Type.getDescriptor(PythonLikeDict.class));

                        // Key
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, KEY_TUPLE_FIELD_NAME,
                                Type.getDescriptor(PythonLikeTuple.class));

                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                                Type.getDescriptor(int.class));

                        methodVisitor.visitInsn(Opcodes.ICONST_1);
                        methodVisitor.visitInsn(Opcodes.ISUB);

                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                                true);

                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));

                        // Value
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeDict.class),
                                "put", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(PythonLikeObject.class),
                                        Type.getType(PythonLikeObject.class)),
                                false);
                        methodVisitor.visitInsn(Opcodes.POP);
                    } else {
                        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(TypeError.class));
                        methodVisitor.visitInsn(Opcodes.DUP);

                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, KEY_TUPLE_FIELD_NAME,
                                Type.getDescriptor(PythonLikeTuple.class));

                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                                Type.getDescriptor(int.class));

                        methodVisitor.visitInsn(Opcodes.ICONST_1);
                        methodVisitor.visitInsn(Opcodes.ISUB);

                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                                Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                                true);

                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));

                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonString.class),
                                "getValue",
                                Type.getMethodDescriptor(Type.getType(String.class)),
                                false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                                Type.getInternalName(PythonDefaultArgumentImplementor.class),
                                "getUnknownKeyArgument", Type.getMethodDescriptor(Type.getType(String.class),
                                        Type.getType(String.class)),
                                false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TypeError.class),
                                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
                        methodVisitor.visitInsn(Opcodes.ATHROW);
                    }
                },
                false);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, REMAINING_KEY_ARGUMENTS_FIELD_NAME,
                Type.getDescriptor(int.class));
        methodVisitor.visitInsn(Opcodes.RETURN);

        // No more keyword arguments
        methodVisitor.visitLabel(noMoreKeywordArguments);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, POSITIONAL_INDEX, Type.getDescriptor(int.class));

        BytecodeSwitchImplementor.createIntSwitch(methodVisitor,
                IntStream.range(0, argumentSpec.getAllowPositionalArgumentCount())
                        .boxed().collect(Collectors.toList()),
                index -> {
                    Type parameterType = methodDescriptor.getParameterTypes()[index];
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getInternalName());
                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, getArgumentName(index),
                            parameterType.getDescriptor());
                },
                () -> {
                    if (extraPositionalArgumentsVariableIndex.isPresent()) {
                        // Extra argument tuple
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName,
                                getArgumentName(extraPositionalArgumentsVariableIndex.get()),
                                Type.getDescriptor(PythonLikeTuple.class));

                        // Index (need to insert in front of list since positional arguments are read in reverse)
                        methodVisitor.visitInsn(Opcodes.ICONST_0);

                        // Item
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

                        // Insert at front of list (since positional arguments are read in reverse)
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeTuple.class), "add",
                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(PythonLikeObject.class)),
                                false);

                    } else {
                        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(TypeError.class));
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, POSITIONAL_INDEX,
                                Type.getDescriptor(int.class));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                                Type.getInternalName(PythonDefaultArgumentImplementor.class),
                                "getTooManyPositionalArguments", Type.getMethodDescriptor(Type.getType(String.class),
                                        Type.INT_TYPE),
                                false);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TypeError.class),
                                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
                        methodVisitor.visitInsn(Opcodes.ATHROW);
                    }
                },
                false);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, POSITIONAL_INDEX, Type.getDescriptor(int.class));
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.ISUB);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, POSITIONAL_INDEX, Type.getDescriptor(int.class));

        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();
    }

    public static String getUnknownKeyArgument(String keyArgument) {
        return "got an unexpected keyword argument '" + keyArgument + "'";
    }

    public static String getTooManyPositionalArguments(int numOfArguments) {
        return "Got too many positional arguments (" + numOfArguments + ")";
    }
}
