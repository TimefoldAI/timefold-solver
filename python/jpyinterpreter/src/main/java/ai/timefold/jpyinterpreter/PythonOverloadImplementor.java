package ai.timefold.jpyinterpreter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.implementors.KnownCallImplementor;
import ai.timefold.jpyinterpreter.types.BoundPythonLikeFunction;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.util.MethodVisitorAdapters;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class PythonOverloadImplementor {
    public static Comparator<PythonLikeType> TYPE_DEPTH_COMPARATOR = Comparator.comparingInt(PythonLikeType::getDepth)
            .thenComparing(PythonLikeType::getTypeName)
            .thenComparing(PythonLikeType::getJavaTypeInternalName)
            .reversed();

    private final static List<DeferredRunner> deferredRunnerList = new ArrayList<>();

    public interface DeferredRunner {
        PythonLikeType run() throws NoSuchMethodException;
    }

    public static void deferDispatchesFor(DeferredRunner runner) {
        try {
            createDispatchesFor(runner.run());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        // deferredRunnerList.add(runner);
    }

    public static void createDeferredDispatches() {
        while (!deferredRunnerList.isEmpty()) {
            List<DeferredRunner> deferredRunnables = new ArrayList<>(deferredRunnerList);

            List<PythonLikeType> deferredTypes = new ArrayList<>(deferredRunnables.size());
            deferredRunnables.forEach(runner -> {
                try {
                    deferredTypes.add(runner.run());
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            });
            deferredTypes.forEach(PythonOverloadImplementor::createDispatchesFor);
            deferredRunnerList.subList(0, deferredRunnables.size()).clear();
        }
    }

    public static void createDispatchesFor(PythonLikeType pythonLikeType) {
        for (String methodName : pythonLikeType.getKnownMethodsDefinedByClass()) {
            PythonLikeFunction overloadDispatch =
                    createDispatchForMethod(pythonLikeType, methodName, pythonLikeType.getMethodType(methodName).orElseThrow(),
                            pythonLikeType.getMethodKind(methodName)
                                    .orElse(PythonClassTranslator.PythonMethodKind.VIRTUAL_METHOD));
            pythonLikeType.$setAttribute(methodName, overloadDispatch);
        }

        if (pythonLikeType.getConstructorType().isPresent()) {
            PythonLikeFunction overloadDispatch =
                    createDispatchForMethod(pythonLikeType, "__init__", pythonLikeType.getConstructorType().orElseThrow(),
                            PythonClassTranslator.PythonMethodKind.VIRTUAL_METHOD);
            pythonLikeType.setConstructor(overloadDispatch);
            pythonLikeType.$setAttribute("__init__", overloadDispatch);
        }
    }

    private static PythonLikeFunction createDispatchForMethod(PythonLikeType pythonLikeType,
            String methodName,
            PythonKnownFunctionType knownFunctionType,
            PythonClassTranslator.PythonMethodKind methodKind) {
        String maybeClassName = PythonBytecodeToJavaBytecodeTranslator.GENERATED_PACKAGE_BASE
                + pythonLikeType.getJavaTypeInternalName().replace('/', '.') + "."
                + methodName + "$$Dispatcher";
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

        // No args constructor for creating instance of this class
        MethodVisitor methodVisitor =
                classWriter.visitMethod(Modifier.PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE),
                        null, null);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitInsn(Opcodes.RETURN);

        methodVisitor.visitMaxs(-1, -1);
        methodVisitor.visitEnd();

        createDispatchFunction(pythonLikeType, knownFunctionType, classWriter);
        createGetTypeFunction(methodKind, classWriter);

        classWriter.visitEnd();
        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        try {
            Class<? extends PythonLikeFunction> generatedClass =
                    (Class<? extends PythonLikeFunction>) BuiltinTypes.asmClassLoader.loadClass(className);
            return generatedClass.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Impossible State: Unable to load generated class (" +
                    className + ") despite it being just generated.", e);
        } catch (InvocationTargetException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Impossible State: Unable to invoke constructor for generated class (" +
                    className + ").", e);
        }
    }

    private static void createGetTypeFunction(PythonClassTranslator.PythonMethodKind kind, ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "$getType",
                Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                null,
                null);

        methodVisitor.visitCode();

        switch (kind) {
            case VIRTUAL_METHOD:
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(BuiltinTypes.class),
                        "FUNCTION_TYPE", Type.getDescriptor(PythonLikeType.class));
                break;
            case STATIC_METHOD:
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(BuiltinTypes.class),
                        "STATIC_FUNCTION_TYPE", Type.getDescriptor(PythonLikeType.class));
                break;
            case CLASS_METHOD:
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(BuiltinTypes.class),
                        "CLASS_FUNCTION_TYPE", Type.getDescriptor(PythonLikeType.class));
                break;
            default:
                throw new IllegalStateException("Unhandled case: " + kind);
        }
        methodVisitor.visitInsn(Opcodes.ARETURN);

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();

    }

    private static void createDispatchFunction(PythonLikeType type, PythonKnownFunctionType knownFunctionType,
            ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "$call",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                null,
                null);

        methodVisitor = MethodVisitorAdapters.adapt(methodVisitor, "$call",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)));

        methodVisitor.visitParameter("positionalArguments", 0);
        methodVisitor.visitParameter("namedArguments", 0);
        methodVisitor.visitParameter("callerInstance", 0);
        methodVisitor.visitCode();

        List<PythonFunctionSignature> overloadList = knownFunctionType.getOverloadFunctionSignatureList();

        Map<Integer, List<PythonFunctionSignature>> pythonFunctionSignatureByArgumentLength = overloadList.stream()
                .filter(sig -> sig.getExtraPositionalArgumentsVariableIndex().isEmpty()
                        && sig.getExtraKeywordArgumentsVariableIndex().isEmpty())
                .collect(Collectors.groupingBy(sig -> sig.getParameterTypes().length));

        Optional<PythonFunctionSignature> maybeGenericFunctionSignature = overloadList.stream()
                .findAny();

        if (overloadList.get(0).isFromArgumentSpec()
                || pythonFunctionSignatureByArgumentLength.isEmpty()) { // only generic overload
            // No error message since we MUST have a generic overload
            createGenericDispatch(methodVisitor, type, maybeGenericFunctionSignature, "");
        } else {
            int[] argCounts = pythonFunctionSignatureByArgumentLength.keySet().stream().sorted().mapToInt(i -> i).toArray();
            Label[] argCountLabel = new Label[argCounts.length];
            Label defaultCase = new Label();

            for (int i = 0; i < argCountLabel.length; i++) {
                argCountLabel[i] = new Label();
            }

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                    Type.getMethodDescriptor(Type.INT_TYPE), true);
            if (!overloadList.get(0).getMethodDescriptor().getMethodType().isStatic()) {
                methodVisitor.visitInsn(Opcodes.ICONST_M1);
                methodVisitor.visitInsn(Opcodes.IADD);
            }
            methodVisitor.visitLookupSwitchInsn(defaultCase, argCounts, argCountLabel);

            for (int i = 0; i < argCounts.length; i++) {
                methodVisitor.visitLabel(argCountLabel[i]);
                createDispatchForArgCount(methodVisitor, argCounts[i], type,
                        pythonFunctionSignatureByArgumentLength.get(argCounts[i]),
                        maybeGenericFunctionSignature);
            }
            methodVisitor.visitLabel(defaultCase);

            createGenericDispatch(methodVisitor, type, maybeGenericFunctionSignature,
                    "No overload has the given argcount. Possible overload(s) are: " +
                            knownFunctionType.getOverloadFunctionSignatureList().stream().map(PythonFunctionSignature::toString)
                                    .collect(Collectors.joining(",\n")));
        }

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private static void createDispatchForArgCount(MethodVisitor methodVisitor, int argCount,
            PythonLikeType type, List<PythonFunctionSignature> functionSignatureList,
            Optional<PythonFunctionSignature> maybeGenericDispatch) {
        final int MATCHING_OVERLOAD_SET_VARIABLE_INDEX = 3; // 0 = this; 1 = posArguments; 2 = namedArguments
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(HashSet.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(functionSignatureList.size());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(HashSet.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
        for (int i = 0; i < functionSignatureList.size(); i++) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
                    Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE), false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "add",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.POP);
        }
        methodVisitor.visitVarInsn(Opcodes.ASTORE, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);

        int startIndex = 0;
        if (!functionSignatureList.get(0).getMethodDescriptor().getMethodType().isStatic()) {
            startIndex = 1;
        }

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

        // At the start of each iteration, stack = arg_1, arg_2, ..., arg_(i-1), pos_args_list
        for (int i = 0; i < argCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP);

            // Get the ith positional argument
            methodVisitor.visitLdcInsn(i + startIndex);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);

            SortedMap<PythonLikeType, List<PythonFunctionSignature>> typeToPossibleSignatures =
                    getTypeForParameter(functionSignatureList, i);

            Label endOfInstanceOfIfs = new Label();
            for (PythonLikeType pythonLikeType : typeToPossibleSignatures.keySet()) {
                Label nextIf = new Label();
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, pythonLikeType.getJavaTypeInternalName());
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, nextIf);

                // pythonLikeType matches argument type
                List<PythonFunctionSignature> matchingOverloadList = typeToPossibleSignatures.get(pythonLikeType);

                if (matchingOverloadList.size() != functionSignatureList.size()) {
                    // Remove overloads that do not match from the matching overload set
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);
                    for (int sigIndex = 0; sigIndex < functionSignatureList.size(); sigIndex++) {
                        if (!matchingOverloadList.contains(functionSignatureList.get(sigIndex))) {
                            methodVisitor.visitInsn(Opcodes.DUP);
                            methodVisitor.visitLdcInsn(sigIndex);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
                                    Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE), false);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                                    "remove",
                                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                                    true);
                            methodVisitor.visitInsn(Opcodes.POP);
                        }
                    }
                    methodVisitor.visitInsn(Opcodes.POP);
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfInstanceOfIfs);
                } else {
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfInstanceOfIfs);
                }
                methodVisitor.visitLabel(nextIf);
            }
            // This is an else at the end of the instanceof if's, which clear the set as no overloads match
            methodVisitor.visitVarInsn(Opcodes.ALOAD, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "clear",
                    Type.getMethodDescriptor(Type.VOID_TYPE), true);

            // end of instance of ifs
            methodVisitor.visitLabel(endOfInstanceOfIfs);
            methodVisitor.visitInsn(Opcodes.POP); // remove argument (need to typecast it later)
        }
        methodVisitor.visitInsn(Opcodes.POP); // Remove list

        // Stack is arg_1, arg_2, ..., arg_(argCount)
        methodVisitor.visitVarInsn(Opcodes.ALOAD, MATCHING_OVERLOAD_SET_VARIABLE_INDEX);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class), "size",
                Type.getMethodDescriptor(Type.INT_TYPE), true);

        Label setIsNotEmpty = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFNE, setIsNotEmpty);

        createGenericDispatch(methodVisitor, type, maybeGenericDispatch,
                "No overload match the given arguments. Possible overload(s) for " + argCount
                        + " arguments are: " +
                        functionSignatureList.stream().map(PythonFunctionSignature::toString)
                                .collect(Collectors.joining(",\n")));

        methodVisitor.visitLabel(setIsNotEmpty);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterable.class), "iterator",
                Type.getMethodDescriptor(Type.getType(Iterator.class)), true);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Iterator.class), "next",
                Type.getMethodDescriptor(Type.getType(Object.class)), true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Integer.class));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue",
                Type.getMethodDescriptor(Type.INT_TYPE), false);

        Label defaultHandler = new Label();
        Label[] signatureIndexToDispatch = new Label[functionSignatureList.size()];
        for (int i = 0; i < functionSignatureList.size(); i++) {
            signatureIndexToDispatch[i] = new Label();
        }

        methodVisitor.visitTableSwitchInsn(0, functionSignatureList.size() - 1, defaultHandler, signatureIndexToDispatch);

        for (int i = 0; i < functionSignatureList.size(); i++) {
            methodVisitor.visitLabel(signatureIndexToDispatch[i]);
            PythonFunctionSignature matchingSignature = functionSignatureList.get(i);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);

            if (startIndex != 0) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);

                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                        matchingSignature.getMethodDescriptor().getDeclaringClassInternalName());
                methodVisitor.visitInsn(Opcodes.SWAP);
            }

            for (int argIndex = 0; argIndex < argCount; argIndex++) {
                PythonLikeType parameterType = matchingSignature.getParameterTypes()[argIndex];
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(argIndex + startIndex);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getJavaTypeInternalName());
                methodVisitor.visitInsn(Opcodes.SWAP);
            }
            methodVisitor.visitInsn(Opcodes.POP);
            matchingSignature.getMethodDescriptor().callMethod(methodVisitor);
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }

        methodVisitor.visitLabel(defaultHandler);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalStateException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("Return signature index is out of bounds");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalStateException.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);
    }

    private static void createGenericDispatch(MethodVisitor methodVisitor,
            PythonLikeType type, Optional<PythonFunctionSignature> maybeGenericDispatch, String errorMessage) {
        if (maybeGenericDispatch.isEmpty()) {
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(TypeError.class));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class),
                    "<init>", Type.getMethodDescriptor(Type.VOID_TYPE),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonOverloadImplementor.class),
                    "getCallErrorInfo", Type.getMethodDescriptor(Type.getType(String.class),
                            Type.getType(List.class),
                            Type.getType(Map.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.getType(String.class)),
                    false);
            methodVisitor.visitLdcInsn(errorMessage);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.getType(String.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "toString", Type.getMethodDescriptor(Type.getType(String.class)),
                    false);

            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TypeError.class),
                    "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.ATHROW);
        } else {
            PythonFunctionSignature functionSignature = maybeGenericDispatch.get();
            if (functionSignature.getMethodDescriptor().getMethodType() != MethodDescriptor.MethodType.STATIC) {
                // It a class/virtual method, so need to load instance/type from argument list
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitLdcInsn(0);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                        "get", Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE),
                        true);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                if (functionSignature.getMethodDescriptor().getMethodType() == MethodDescriptor.MethodType.CLASS) {
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(BoundPythonLikeFunction.class),
                            "boundToTypeOfObject",
                            Type.getMethodDescriptor(Type.getType(BoundPythonLikeFunction.class),
                                    Type.getType(PythonLikeObject.class),
                                    Type.getType(PythonLikeFunction.class)),
                            false);
                } else {
                    methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(BoundPythonLikeFunction.class));
                    methodVisitor.visitInsn(Opcodes.DUP_X2);
                    methodVisitor.visitInsn(Opcodes.DUP_X2);
                    methodVisitor.visitInsn(Opcodes.POP);
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(BoundPythonLikeFunction.class),
                            "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class),
                                    Type.getType(PythonLikeFunction.class)),
                            false);
                }

                // Load the sublist without the self/type argument
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                        "size", Type.getMethodDescriptor(Type.INT_TYPE), true);
                methodVisitor.visitLdcInsn(1);
                methodVisitor.visitInsn(Opcodes.SWAP);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                        "subList", Type.getMethodDescriptor(Type.getType(List.class), Type.INT_TYPE, Type.INT_TYPE),
                        true);
            } else {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            KnownCallImplementor.callUnpackListAndMap(functionSignature.getDefaultArgumentHolderClassInternalName(),
                    functionSignature.getMethodDescriptor(), methodVisitor);
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }
    }

    public static String getCallErrorInfo(List<PythonLikeObject> positionalArgs,
            Map<PythonString, PythonLikeObject> namedArgs) {
        return "Could not find an overload that accept " + positionalArgs.stream()
                .map(arg -> arg.$getType().getTypeName()).collect(Collectors.joining(", ", "(", ") argument types. "));
    }

    private static SortedMap<PythonLikeType, List<PythonFunctionSignature>>
            getTypeForParameter(List<PythonFunctionSignature> functionSignatureList, int parameter) {
        SortedMap<PythonLikeType, List<PythonFunctionSignature>> out = new TreeMap<>(TYPE_DEPTH_COMPARATOR);
        for (PythonFunctionSignature functionSignature : functionSignatureList) {
            out.computeIfAbsent(functionSignature.getParameterTypes()[parameter], type -> new ArrayList<>())
                    .add(functionSignature);
        }
        return out;
    }

}
