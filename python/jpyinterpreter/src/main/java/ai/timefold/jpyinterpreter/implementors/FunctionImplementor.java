package ai.timefold.jpyinterpreter.implementors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonCode;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeGenericType;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implements opcodes related to functions
 */
public class FunctionImplementor {

    public static void callBinaryMethod(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            MethodVisitor methodVisitor, String methodName) {
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(methodName);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.SWAP);

        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 2);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        getCallerInstance(functionMetadata, stackMetadata);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    public static void callBinaryMethod(MethodVisitor methodVisitor, String methodName) {
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(methodName);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.SWAP);

        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 2);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    /**
     * Loads a method named co_names[namei] from the TOS object. TOS is popped. This bytecode distinguishes two cases:
     * if TOS has a method with the correct name, the bytecode pushes the unbound method and TOS.
     * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
     * Otherwise, NULL and the object return by the attribute lookup are pushed.
     */
    public static void loadMethod(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int nameIndex) {
        var methodVisitor = functionMetadata.methodVisitor;
        var function = functionMetadata.pythonCompiledFunction;
        var className = functionMetadata.className;
        PythonLikeType stackTosType = stackMetadata.getTOSType();
        PythonLikeType tosType;
        boolean isTosType;
        if (stackTosType instanceof PythonLikeGenericType) {
            tosType = ((PythonLikeGenericType) stackTosType).getOrigin();
            isTosType = true;
        } else {
            tosType = stackTosType;
            isTosType = false;
        }
        tosType.getMethodType(functionMetadata.pythonCompiledFunction.co_names.get(nameIndex)).ifPresentOrElse(
                knownFunctionType -> {
                    if (isTosType && knownFunctionType.isStaticMethod()) {
                        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);

                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
                            // Need to move NULL behind method
                            methodVisitor.visitInsn(Opcodes.DUP_X1);
                            methodVisitor.visitInsn(Opcodes.POP);
                        }
                    } else if (!isTosType && knownFunctionType.isStaticMethod()) {
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                true);
                        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
                            // Need to move NULL behind method
                            methodVisitor.visitInsn(Opcodes.DUP_X1);
                            methodVisitor.visitInsn(Opcodes.POP);
                        }
                    } else if (isTosType && knownFunctionType.isClassMethod()) {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                    } else if (!isTosType && knownFunctionType.isClassMethod()) {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                true);
                        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                    } else if (isTosType) {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                    } else {
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                true);
                        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
                        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                        Type.getType(String.class)),
                                true);
                        methodVisitor.visitInsn(Opcodes.SWAP);
                    }
                },
                () -> loadGenericMethod(functionMetadata, methodVisitor, className, function, stackMetadata,
                        nameIndex));
    }

    /**
     * Loads a method named co_names[namei] from the TOS object. TOS is popped. This bytecode distinguishes two cases:
     * if TOS has a method with the correct name, the bytecode pushes the unbound method and TOS.
     * TOS will be used as the first argument (self) by CALL_METHOD when calling the unbound method.
     * Otherwise, NULL and the object return by the attribute lookup are pushed.
     */
    private static void loadGenericMethod(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            PythonCompiledFunction function,
            StackMetadata stackMetadata, int nameIndex) {

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(function.co_names.get(nameIndex));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeType.class),
                "loadMethod", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                false);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        Label blockEnd = new Label();

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, blockEnd);

        // TOS is null; type does not have attribute; do normal attribute lookup
        // Stack is object, null
        methodVisitor.visitInsn(Opcodes.POP);
        ObjectImplementor.getAttribute(functionMetadata, stackMetadata, nameIndex);

        // Stack is method
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isBefore(PythonVersion.PYTHON_3_11)) {
            // Python 3.11+ swap these
            methodVisitor.visitInsn(Opcodes.SWAP);
        }

        methodVisitor.visitLabel(blockEnd);

        // Stack is either:
        // object, method if it was in type
        // null, method if it was not in type (Or method, null if Python 3.11+)
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now:
        // method, object if it was in type
        // method, null if it was not in type (and prior to Python 3.11+)
        // null, method if it was not in type (if Python 3.11+)
    }

    public static void setCallKeywordNameTuple(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            int constantIndex) {
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;
        PythonConstantsImplementor.loadConstant(functionMetadata.methodVisitor, functionMetadata.className, constantIndex);
        localVariableHelper.writeCallKeywords(functionMetadata.methodVisitor);
    }

    /**
     * Calls a function. argc is the number of positional arguments. Keyword arguments are stored in a local variable.
     * Keyword arguments (if any) are at the top of the stack, followed by, positional arguments.
     * Below them either self and an unbound method object or NULL and an arbitrary callable).
     * All of them are popped and the return value is pushed.
     */
    public static void call(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argumentCount) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(argumentCount + 1);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            List<String> keywordArgumentNameList = stackMetadata.getCallKeywordNameList();
            List<PythonLikeType> callStackParameterTypes = stackMetadata.getValueSourcesUpToStackIndex(argumentCount)
                    .stream().map(ValueSourceInfo::getValueType).collect(Collectors.toList());

            knownFunctionType
                    .getFunctionForParameters(argumentCount - keywordArgumentNameList.size(), keywordArgumentNameList,
                            callStackParameterTypes)
                    .ifPresentOrElse(functionSignature -> {
                        KnownCallImplementor.callPython311andAbove(functionSignature, functionMetadata, stackMetadata,
                                argumentCount,
                                stackMetadata.getCallKeywordNameList());
                    }, () -> callGeneric(functionMetadata, stackMetadata, argumentCount));
        } else {
            functionType = stackMetadata.getTypeAtStackIndex(argumentCount);
            if (functionType instanceof PythonLikeGenericType) {
                functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
            }
            if (functionType instanceof PythonKnownFunctionType) {
                PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
                List<String> keywordArgumentNameList = stackMetadata.getCallKeywordNameList();
                List<PythonLikeType> callStackParameterTypes = stackMetadata.getValueSourcesUpToStackIndex(argumentCount)
                        .stream().map(ValueSourceInfo::getValueType).collect(Collectors.toList());

                knownFunctionType
                        .getFunctionForParameters(argumentCount - keywordArgumentNameList.size(), keywordArgumentNameList,
                                callStackParameterTypes)
                        .ifPresentOrElse(functionSignature -> {
                            KnownCallImplementor.callPython311andAbove(functionSignature, functionMetadata, stackMetadata,
                                    argumentCount,
                                    stackMetadata.getCallKeywordNameList());
                        }, () -> callGeneric(functionMetadata, stackMetadata, argumentCount));
            } else {
                callGeneric(functionMetadata, stackMetadata, argumentCount);
            }
        }
    }

    private static void callGeneric(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            int argumentCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        int keywordArgs = localVariableHelper.newLocal();
        int positionalArgs = localVariableHelper.newLocal();

        localVariableHelper.readCallKeywords(methodVisitor);
        CollectionImplementor.buildCollection(TupleMapPair.class, methodVisitor, argumentCount + 1);

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "tuple",
                Type.getDescriptor(PythonLikeTuple.class));
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeTuple.class), positionalArgs);

        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "map",
                Type.getDescriptor(PythonLikeDict.class));
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeDict.class), keywordArgs);

        // Stack is (null or method), (obj or method)
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is (obj or method) (null or method)
        Label ifNullStart = new Label();
        Label blockEnd = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifNullStart);

        // Stack is obj, method
        StackManipulationImplementor.swap(methodVisitor);

        // Stack is method, obj

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class), positionalArgs);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.ICONST_0);

        // Stack is method, argList, obj, index
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, argList, index, obj
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "add",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(Object.class)),
                true);

        // Stack is method
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class), positionalArgs);

        // Stack is method, positionalArgs
        methodVisitor.visitJumpInsn(Opcodes.GOTO, blockEnd);

        methodVisitor.visitLabel(ifNullStart);
        // Stack is method, null
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is method
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeTuple.class), positionalArgs);

        // Stack is method, positionalArgs
        methodVisitor.visitLabel(blockEnd);

        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeDict.class), keywordArgs);

        // Stack is method, positionalArgs, keywordArgs
        getCallerInstance(functionMetadata, stackMetadata);

        // Stack is callable, positionalArgs, keywordArgs, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);

        localVariableHelper.resetCallKeywords(methodVisitor);
        localVariableHelper.freeLocal();
        localVariableHelper.freeLocal();
    }

    /**
     * Calls a method. argc is the number of positional arguments. Keyword arguments are not supported.
     * This opcode is designed to be used with LOAD_METHOD. Positional arguments are on top of the stack.
     * Below them, the two items described in LOAD_METHOD are on the stack
     * (either self and an unbound method object or NULL and an arbitrary callable).
     * All of them are popped and the return value is pushed.
     */
    public static void callMethod(FunctionMetadata functionMetadata, StackMetadata stackMetadata, MethodVisitor methodVisitor,
            PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg() + 1);
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            PythonLikeType[] parameterTypes = new PythonLikeType[instruction.arg()];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterTypes[parameterTypes.length - i - 1] = stackMetadata.getTypeAtStackIndex(i);
            }
            knownFunctionType.getFunctionForParameters(parameterTypes)
                    .ifPresentOrElse(functionSignature -> {
                        KnownCallImplementor.callMethod(functionSignature, methodVisitor, localVariableHelper,
                                instruction.arg());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitInsn(Opcodes.POP);
                        if (knownFunctionType.isStaticMethod()) {
                            methodVisitor.visitInsn(Opcodes.SWAP);
                            methodVisitor.visitInsn(Opcodes.POP);
                        }
                    }, () -> callGenericMethod(functionMetadata, stackMetadata, methodVisitor, instruction,
                            localVariableHelper));
        } else {
            callGenericMethod(functionMetadata, stackMetadata, methodVisitor, instruction, localVariableHelper);
        }
    }

    private static void callGenericMethod(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            MethodVisitor methodVisitor,
            PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper) {
        // Stack is method, (obj or null), arg0, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, instruction.arg());
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, (obj or null)
        Label ifNullStart = new Label();
        Label blockEnd = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifNullStart);

        // Stack is method, argList, obj
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, 1);
        StackManipulationImplementor.swap(methodVisitor);

        // Stack is method, argList, argList, obj
        methodVisitor.visitInsn(Opcodes.ICONST_0);

        // Stack is method, argList, argList, obj, index
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is method, argList, argList, index, obj
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "add",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.getType(Object.class)),
                true);

        // Stack is method, argList
        methodVisitor.visitJumpInsn(Opcodes.GOTO, blockEnd);

        methodVisitor.visitLabel(ifNullStart);
        // Stack is method, argList, null
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is method, argList
        methodVisitor.visitLabel(blockEnd);

        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);

        // Stack is method, argList
        getCallerInstance(functionMetadata, stackMetadata);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    /**
     * Calls a function. TOS...TOS[argc - 1] are the arguments to the function.
     * TOS[argc] is the function to call. TOS...TOS[argc] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callFunction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg());
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            knownFunctionType.getDefaultFunctionSignature()
                    .ifPresentOrElse(functionSignature -> {
                        KnownCallImplementor.callWithoutKeywords(functionSignature, functionMetadata, stackMetadata,
                                instruction.arg());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitInsn(Opcodes.POP);
                    }, () -> callGenericFunction(functionMetadata, stackMetadata, methodVisitor, instruction));
        } else {
            callGenericFunction(functionMetadata, stackMetadata, methodVisitor, instruction);
        }
    }

    public static void callGenericFunction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        callGenericFunction(functionMetadata, stackMetadata, methodVisitor, instruction.arg());
    }

    public static void callGenericFunction(MethodVisitor methodVisitor, int argCount) {
        // stack is callable, arg0, arg1, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, argCount);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);

        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    public static void callGenericFunction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            MethodVisitor methodVisitor, int argCount) {
        // stack is callable, arg0, arg1, ..., arg(argc - 1)
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, argCount);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        getCallerInstance(functionMetadata, stackMetadata);

        // Stack is callable, argument_list, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    /**
     * Calls a function. TOS is a tuple containing keyword names.
     * TOS[1]...TOS[len(TOS)] are the keyword arguments to the function (TOS[1] is (TOS)[0], TOS[2] is (TOS)[1], ...).
     * TOS[len(TOS) + 1]...TOS[argc + 1] are the positional arguments (rightmost first).
     * TOS[argc + 2] is the function to call. TOS...TOS[argc + 2] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callFunctionWithKeywords(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg() + 1);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            knownFunctionType.getDefaultFunctionSignature()
                    .ifPresentOrElse(functionSignature -> {
                        KnownCallImplementor.callWithKeywordsAndUnwrapSelf(functionSignature, functionMetadata, stackMetadata,
                                instruction.arg());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitInsn(Opcodes.POP);
                    }, () -> callGenericFunction(functionMetadata, stackMetadata, methodVisitor, instruction));
        } else {
            callGenericFunctionWithKeywords(functionMetadata, stackMetadata, methodVisitor, instruction);
        }
    }

    /**
     * Calls a function. TOS is a tuple containing keyword names.
     * TOS[1]...TOS[len(TOS)] are the keyword arguments to the function (TOS[1] is (TOS)[0], TOS[2] is (TOS)[1], ...).
     * TOS[len(TOS) + 1]...TOS[argc + 1] are the positional arguments (rightmost first).
     * TOS[argc + 2] is the function to call. TOS...TOS[argc + 2] are all popped and
     * the result is pushed onto the stack.
     */
    public static void callGenericFunctionWithKeywords(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        // stack is callable, arg0, arg1, ..., arg(argc - len(keys)), ..., arg(argc - 1), keys
        // We know the total number of arguments, but not the number of individual positional/keyword arguments
        // Since Java Bytecode require consistent stack frames  (i.e. the body of a loop must start with
        // the same number of elements in the stack), we need to add the tuple/map in the same object
        // which will delegate it to either the tuple or the map depending on position and the first item size
        CollectionImplementor.buildCollection(TupleMapPair.class, methodVisitor, instruction.arg() + 1);

        // stack is callable, tupleMapPair
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "tuple",
                Type.getDescriptor(PythonLikeTuple.class));

        // stack is callable, tupleMapPair, positionalArgs
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(TupleMapPair.class), "map",
                Type.getDescriptor(PythonLikeDict.class));

        getCallerInstance(functionMetadata, stackMetadata);

        // Stack is callable, positionalArgs, keywordArgs
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    /**
     * Calls a function. If the lowest bit of instruction.arg is set, TOS is a mapping object containing keyword
     * arguments, TOS[1] is an iterable containing positional arguments and TOS[2] is callable. Otherwise,
     * TOS is an iterable containing positional arguments and TOS[1] is callable.
     */
    public static void callFunctionUnpack(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        if ((instruction.arg() & 1) == 1) {
            callFunctionUnpackMapAndIterable(functionMetadata, stackMetadata, functionMetadata.methodVisitor);
        } else {
            callFunctionUnpackIterable(functionMetadata, stackMetadata, functionMetadata.methodVisitor);
        }
    }

    public static void callFunctionUnpackMapAndIterable(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            MethodVisitor methodVisitor) {
        getCallerInstance(functionMetadata, stackMetadata);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.POP);
        }
    }

    public static void callFunctionUnpackIterable(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        getCallerInstance(functionMetadata, stackMetadata);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class),
                "$call", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(List.class),
                        Type.getType(Map.class),
                        Type.getType(PythonLikeObject.class)),
                true);
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.POP);
        }
    }

    private static void getCallerInstance(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        if (functionMetadata.pythonCompiledFunction.totalArgCount() > 0) {
            // Use null as the key for the current instance, used by super()
            stackMetadata.localVariableHelper.readLocal(methodVisitor, 0);
        } else {
            // Put the current instance as null
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        }
    }

    /**
     * Creates a function. The stack depends on {@code instruction.arg}:
     *
     * - If (arg &amp; 1) == 1, a tuple of default values for positional-only and positional-or-keyword parameters in positional
     * order
     * - If (arg &amp; 2) == 2, a dictionary of keyword-only parameters’ default values
     * - If (arg &amp; 4) == 4, an annotation dictionary
     * - If (arg &amp; 8) == 8, a tuple containing cells for free variables
     *
     * The stack will contain the following items, in the given order:
     *
     * TOP
     * [Mandatory] Function Name
     * [Mandatory] Class of the PythonLikeFunction to create
     * [Optional, flag = 0x8] A tuple containing the cells for free variables
     * [Optional, flag = 0x4] A tuple containing key,value pairs for the annotation directory
     * [Optional, flag = 0x2] A dictionary of keyword-only parameters’ default values
     * [Optional, flag = 0x1] A tuple of default values for positional-only and positional-or-keyword parameters in positional
     * order
     * BOTTOM
     *
     * All arguments are popped. A new instance of Class is created with the arguments and pushed to the stack.
     */
    public static void createFunction(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        String className = functionMetadata.className;

        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
            // Python 3.11 and above removed qualified name, so we need to get it from the code object's class
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonCode.class));
            methodVisitor.visitInsn(Opcodes.DUP);
            // TODO: maybe create qualifiedName field in PythonCode?
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(PythonCode.class), "functionClass",
                    Type.getDescriptor(Class.class));
            // TODO: get qualified name from static field?
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class), "getName",
                    Type.getMethodDescriptor(Type.getType(String.class)), false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonString.class), "valueOf",
                    Type.getMethodDescriptor(Type.getType(PythonString.class), Type.getType(String.class)), false);
            stackMetadata = stackMetadata.pushTemp(BuiltinTypes.STRING_TYPE);
        }
        int providedOptionalArgs = Integer.bitCount(instruction.arg());

        // If the argument present, decrement providedOptionalArgs to keep argument shifting logic the same
        // Ex: present, missing, present, present -> need to shift default for missing down by 4 = 2 + (3 - 1)
        // Ex: present, present, missing, present -> need to shift default for missing down by 3 = 2 + (3 - 2)
        // Ex: present, missing1, missing2, present -> need to shift default for missing1 down by 3 = 2 + (2 - 1),
        //                                             need to shift default for missing2 down by 3 = 2 + (2 - 1)
        StackMetadata tempStackmetadata = stackMetadata;

        if ((instruction.arg() & 1) != 1) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);

            tempStackmetadata = tempStackmetadata.pushTemp(BuiltinTypes.TUPLE_TYPE);
            tempStackmetadata =
                    StackManipulationImplementor.shiftTOSDownBy(functionMetadata, tempStackmetadata, 2 + providedOptionalArgs);
        } else {
            providedOptionalArgs--;
        }

        if ((instruction.arg() & 2) != 2) {
            CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);

            tempStackmetadata = tempStackmetadata.pushTemp(BuiltinTypes.DICT_TYPE);
            tempStackmetadata =
                    StackManipulationImplementor.shiftTOSDownBy(functionMetadata, tempStackmetadata, 2 + providedOptionalArgs);
        } else {
            providedOptionalArgs--;
        }

        if ((instruction.arg() & 4) != 4) {
            // In Python 3.10 and above, it a tuple of string; in 3.9 and below, a dict
            if (functionMetadata.pythonCompiledFunction.pythonVersion.isBefore(PythonVersion.PYTHON_3_10)) {
                CollectionImplementor.buildMap(PythonLikeDict.class, methodVisitor, 0);

                tempStackmetadata = tempStackmetadata.pushTemp(BuiltinTypes.DICT_TYPE);
                tempStackmetadata = StackManipulationImplementor.shiftTOSDownBy(functionMetadata, tempStackmetadata,
                        2 + providedOptionalArgs);

            } else {
                CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);

                tempStackmetadata = tempStackmetadata.pushTemp(BuiltinTypes.TUPLE_TYPE);
                tempStackmetadata = StackManipulationImplementor.shiftTOSDownBy(functionMetadata, tempStackmetadata,
                        2 + providedOptionalArgs);
            }
        } else {
            providedOptionalArgs--;
        }

        if ((instruction.arg() & 8) != 8) {
            CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 0);

            tempStackmetadata = tempStackmetadata.pushTemp(BuiltinTypes.TUPLE_TYPE);
            tempStackmetadata =
                    StackManipulationImplementor.shiftTOSDownBy(functionMetadata, tempStackmetadata, 2 + providedOptionalArgs);
        }

        // Stack is now:
        // default positional args, default keyword args, annotation directory tuple, cell tuple, function class, function name

        // Do type casts for name string and code object
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonCode.class));

        // Pass the current function's interpreter to the new function instance
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonInterpreter.class));

        // Need to change constructor depending on Python version
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isBefore(PythonVersion.PYTHON_3_10)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(FunctionImplementor.class),
                    "createInstance", Type.getMethodDescriptor(Type.getType(PythonLikeFunction.class),
                            Type.getType(PythonLikeTuple.class),
                            Type.getType(PythonLikeDict.class),
                            Type.getType(PythonLikeDict.class),
                            Type.getType(PythonLikeTuple.class),
                            Type.getType(PythonString.class),
                            Type.getType(PythonCode.class),
                            Type.getType(PythonInterpreter.class)),
                    false);
        } else {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(FunctionImplementor.class),
                    "createInstance", Type.getMethodDescriptor(Type.getType(PythonLikeFunction.class),
                            Type.getType(PythonLikeTuple.class),
                            Type.getType(PythonLikeDict.class),
                            Type.getType(PythonLikeTuple.class),
                            Type.getType(PythonLikeTuple.class),
                            Type.getType(PythonString.class),
                            Type.getType(PythonCode.class),
                            Type.getType(PythonInterpreter.class)),
                    false);
        }
    }

    // For Python 3.9 and below
    @SuppressWarnings("unused")
    public static PythonLikeFunction createInstance(PythonLikeTuple defaultPositionalArgs,
            PythonLikeDict defaultKeywordArgs,
            PythonLikeDict annotationDict,
            PythonLikeTuple closure,
            PythonString functionName,
            PythonCode code,
            PythonInterpreter pythonInterpreter) {
        return createInstance(defaultPositionalArgs, defaultKeywordArgs, annotationDict.toFlattenKeyValueTuple(),
                closure, functionName, code.functionClass, pythonInterpreter);
    }

    // For Python 3.10 and above
    @SuppressWarnings("unused")
    public static PythonLikeFunction createInstance(PythonLikeTuple defaultPositionalArgs,
            PythonLikeDict defaultKeywordArgs,
            PythonLikeTuple annotationTuple,
            PythonLikeTuple closure,
            PythonString functionName,
            PythonCode code,
            PythonInterpreter pythonInterpreter) {
        return createInstance(defaultPositionalArgs, defaultKeywordArgs, annotationTuple, closure, functionName,
                code.functionClass, pythonInterpreter);
    }

    public static <T> T createInstance(PythonLikeTuple defaultPositionalArgs,
            PythonLikeDict defaultKeywordArgs,
            PythonLikeTuple annotationTuple,
            PythonLikeTuple closure,
            PythonString functionName,
            Class<T> functionClass,
            PythonInterpreter pythonInterpreter) {
        PythonLikeDict annotationDirectory = new PythonLikeDict();
        for (int i = 0; i < (annotationTuple.size() >> 1); i++) {
            annotationDirectory.put(annotationTuple.get(i * 2), annotationTuple.get(i * 2 + 1));
        }

        try {
            Constructor<T> constructor = functionClass.getConstructor(PythonLikeTuple.class,
                    PythonLikeDict.class,
                    PythonLikeDict.class,
                    PythonLikeTuple.class,
                    PythonString.class,
                    PythonInterpreter.class);
            return (T) constructor.newInstance(defaultPositionalArgs, defaultKeywordArgs, annotationDirectory, closure,
                    functionName, pythonInterpreter);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TupleMapPair {
        public PythonLikeTuple tuple;
        public PythonLikeDict map;

        List<PythonLikeObject> mapKeyTuple;

        final int totalNumberOfPositionalAndKeywordArguments;

        public TupleMapPair(int itemsToPop) {
            tuple = null; // Tuple is created when we know how many items are in it
            mapKeyTuple = null; // mapKeyTuple is the first item reverseAdded
            map = new PythonLikeDict();
            this.totalNumberOfPositionalAndKeywordArguments = itemsToPop - 1;
        }

        public void reverseAdd(PythonLikeObject object) {
            if (mapKeyTuple == null) {
                mapKeyTuple = (List<PythonLikeObject>) object;
                tuple = new PythonLikeTuple(totalNumberOfPositionalAndKeywordArguments - mapKeyTuple.size());
                return;
            }

            if (map.size() < mapKeyTuple.size()) {
                map.put(mapKeyTuple.get(mapKeyTuple.size() - map.size() - 1), object);
            } else {
                tuple.reverseAdd(object);
            }
        }
    }

}
