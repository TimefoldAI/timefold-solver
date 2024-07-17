package ai.timefold.jpyinterpreter.implementors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.MethodDescriptor;
import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonFunctionSignature;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.NotImplemented;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonSlice;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.errors.TypeError;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implementations of opcodes that delegate to dunder/magic methods.
 */
public class DunderOperatorImplementor {

    public static void unaryOperator(MethodVisitor methodVisitor, StackMetadata stackMetadata, PythonUnaryOperator operator) {
        PythonLikeType operand = Optional.ofNullable(stackMetadata.getTOSType()).orElse(BuiltinTypes.BASE_TYPE);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = operand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters();
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                MethodDescriptor methodDescriptor = functionSignature.getMethodDescriptor();
                if (methodDescriptor.getParameterTypes().length < 1) {
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
                } else {
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getParameterTypes()[0].getInternalName());
                }
                functionSignature.getMethodDescriptor().callMethod(methodVisitor);
            } else {
                unaryOperator(methodVisitor, operator);
            }
        } else {
            unaryOperator(methodVisitor, operator);
        }
    }

    /**
     * Performs a unary dunder operation on TOS. Generate codes that look like this:
     *
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS.$getType().$getAttributeOrError(operator.getDunderMethod());
     *    List args = new ArrayList(1);
     *    args.set(0) = TOS
     *    pop TOS
     *    TOS' = operand_method.apply(args, null)
     * </pre>
     *
     */
    public static void unaryOperator(MethodVisitor methodVisitor, PythonUnaryOperator operator) {
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);

        // Stack is now TOS, method
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, TOS
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now method, TOS, argList
        pushArgumentIntoList(methodVisitor);

        // Stack is now method, argList
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

    public static void binaryOperator(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBinaryOperator operator) {
        binaryOperator(methodVisitor, stackMetadata, operator, true, true, false);
    }

    private static void binaryOperator(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBinaryOperator operator, boolean isLeft, boolean leftCheckSuccessful,
            boolean forceFallback) {
        PythonLikeType leftOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(1)).orElse(BuiltinTypes.BASE_TYPE);
        PythonLikeType rightOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(0)).orElse(BuiltinTypes.BASE_TYPE);

        PythonBinaryOperator actualOperator = operator;
        if (forceFallback || (!isLeft && operator.getFallbackOperation().isPresent())) {
            actualOperator = operator.getFallbackOperation().get();
        }

        Optional<PythonKnownFunctionType> maybeKnownFunctionType =
                isLeft ? leftOperand.getMethodType(actualOperator.getDunderMethod())
                        : rightOperand.getMethodType(actualOperator.getRightDunderMethod());

        if (maybeKnownFunctionType.isEmpty() && operator.getFallbackOperation().isPresent()) {
            maybeKnownFunctionType =
                    isLeft ? leftOperand.getMethodType(operator.getFallbackOperation().get().getDunderMethod())
                            : rightOperand.getMethodType(operator.getFallbackOperation().get().getRightDunderMethod());
            actualOperator = operator.getFallbackOperation().get();
        }

        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature =
                    isLeft ? knownFunctionType.getFunctionForParameters(rightOperand)
                            : knownFunctionType.getFunctionForParameters(leftOperand);

            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                MethodDescriptor methodDescriptor = functionSignature.getMethodDescriptor();
                boolean needToCheckForNotImplemented =
                        (actualOperator.hasRightDunderMethod() || actualOperator.getFallbackOperation().isPresent())
                                && BuiltinTypes.NOT_IMPLEMENTED_TYPE.isSubclassOf(functionSignature.getReturnType());

                if (isLeft) {
                    if (methodDescriptor.getParameterTypes().length < 2) {
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, methodDescriptor.getDeclaringClassInternalName());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getParameterTypes()[0].getInternalName());
                    } else {
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getParameterTypes()[0].getInternalName());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getParameterTypes()[1].getInternalName());
                    }
                } else {
                    if (methodDescriptor.getParameterTypes().length < 2) {
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getParameterTypes()[0].getInternalName());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getDeclaringClassInternalName());
                    } else {
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getParameterTypes()[1].getInternalName());
                        methodVisitor.visitInsn(Opcodes.SWAP);
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                                methodDescriptor.getParameterTypes()[0].getInternalName());
                    }
                }

                if (needToCheckForNotImplemented) {
                    methodVisitor.visitInsn(Opcodes.DUP2);
                }

                if (!isLeft) {
                    methodVisitor.visitInsn(Opcodes.SWAP);
                }
                functionSignature.getMethodDescriptor().callMethod(methodVisitor);
                if (needToCheckForNotImplemented) {
                    methodVisitor.visitInsn(Opcodes.DUP);
                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(NotImplemented.class),
                            "INSTANCE", Type.getDescriptor(NotImplemented.class));
                    Label ifNotImplemented = new Label();
                    Label done = new Label();

                    methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifNotImplemented);

                    methodVisitor.visitInsn(Opcodes.DUP_X2);
                    methodVisitor.visitInsn(Opcodes.POP);
                    methodVisitor.visitInsn(Opcodes.POP2);
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, done);

                    methodVisitor.visitLabel(ifNotImplemented);
                    if (isLeft) {
                        methodVisitor.visitInsn(Opcodes.POP);
                        if (actualOperator.getFallbackOperation().isPresent()) {
                            binaryOperator(methodVisitor, stackMetadata, operator, true, true, true);
                        } else {
                            binaryOperator(methodVisitor, stackMetadata, operator, false, true, false);
                        }
                    } else {
                        methodVisitor.visitInsn(Opcodes.POP);
                        raiseUnsupportedType(methodVisitor, stackMetadata.localVariableHelper, operator);
                    }
                    methodVisitor.visitLabel(done);
                }
            } else if (isLeft && actualOperator.hasRightDunderMethod()) {
                binaryOperator(methodVisitor, stackMetadata, operator, false, false, false);
            } else if (!isLeft && leftCheckSuccessful) {
                binaryOperatorOnlyRight(methodVisitor, stackMetadata.localVariableHelper, actualOperator);
            } else {
                binaryOperator(methodVisitor, stackMetadata.localVariableHelper, operator);
            }
        } else if (isLeft && actualOperator.hasRightDunderMethod()) {
            binaryOperator(methodVisitor, stackMetadata, operator, false, false, false);
        } else if (!isLeft && leftCheckSuccessful) {
            binaryOperatorOnlyRight(methodVisitor, stackMetadata.localVariableHelper, actualOperator);
        } else {
            binaryOperator(methodVisitor, stackMetadata.localVariableHelper, operator);
        }
    }

    private static void raiseUnsupportedType(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            PythonBinaryOperator operator) {
        int right = localVariableHelper.newLocal();
        int left = localVariableHelper.newLocal();

        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), left);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), right);

        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(TypeError.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        if (!operator.getOperatorSymbol().isEmpty()) {
            methodVisitor.visitLdcInsn("unsupported operand type(s) for " + operator.getOperatorSymbol() + ": '");
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.getType(String.class)),
                    false);
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), left);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeType.class),
                    "getTypeName", Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.getType(String.class)),
                    false);
            methodVisitor.visitLdcInsn("' and '");
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.getType(String.class)),
                    false);
            localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), right);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeType.class),
                    "getTypeName", Type.getMethodDescriptor(Type.getType(String.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.getType(String.class)),
                    false);
            methodVisitor.visitLdcInsn("'");
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.getType(String.class)),
                    false);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "toString", Type.getMethodDescriptor(Type.getType(String.class)),
                    false);

            localVariableHelper.freeLocal();
            localVariableHelper.freeLocal();

            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TypeError.class),
                    "<init>", Type.getMethodDescriptor(Type.VOID_TYPE,
                            Type.getType(String.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.ATHROW);
        } else {
            localVariableHelper.freeLocal();
            localVariableHelper.freeLocal();

            switch (operator) {
                case GET_ITEM: // TODO: Error message
                default:
                    methodVisitor.visitInsn(Opcodes.POP);
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(TypeError.class),
                            "<init>", Type.getMethodDescriptor(Type.VOID_TYPE),
                            false);
                    methodVisitor.visitInsn(Opcodes.ATHROW);
            }
        }
    }

    /**
     * Performs a binary dunder operation on TOS and TOS1. Generate codes that look like this:
     *
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS1.$getType().$getAttributeOrError(operator.getDunderMethod());
     *    List args = new ArrayList(2);
     *    args.set(0) = TOS1
     *    args.set(1) = TOS
     *    pop TOS, TOS1
     *    TOS' = operand_method.apply(args, null)
     * </pre>
     *
     */
    public static void binaryOperator(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            PythonBinaryOperator operator) {
        Label noLeftMethod = new Label();
        methodVisitor.visitInsn(Opcodes.DUP2);
        if (operator.hasRightDunderMethod() || operator.getFallbackOperation().isPresent()) {
            methodVisitor.visitInsn(Opcodes.DUP2);
        }
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Stack is now (TOS1, TOS,)? TOS, TOS1
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, noLeftMethod);

        // Stack is now(TOS1, TOS,)? TOS, TOS1, method
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now (TOS1, TOS,)? method, TOS, TOS1
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now (TOS1, TOS,)? method, TOS, TOS1, argList
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);

        // Stack is now (TOS1, TOS,)? method, argList
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

        // Stack is now (TOS1, TOS,)? method_result
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(NotImplemented.class),
                "INSTANCE", Type.getDescriptor(NotImplemented.class));
        Label ifNotImplemented = new Label();
        Label done = new Label();

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, ifNotImplemented);
        // Stack is TOS1, TOS, method_result
        if (operator.hasRightDunderMethod() || operator.getFallbackOperation().isPresent()) {
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.POP2);
        }
        // Stack is method_result
        methodVisitor.visitJumpInsn(Opcodes.GOTO, done);

        methodVisitor.visitLabel(noLeftMethod);
        methodVisitor.visitInsn(Opcodes.POP2);
        methodVisitor.visitLabel(ifNotImplemented);
        methodVisitor.visitInsn(Opcodes.POP);

        Label raiseError = new Label();
        if (operator.getFallbackOperation().isPresent()) {
            binaryOperator(methodVisitor, localVariableHelper, operator.getFallbackOperation().get());
            methodVisitor.visitJumpInsn(Opcodes.GOTO, done);
        } else if (operator.hasRightDunderMethod()) {
            Label noRightMethod = new Label();
            // Stack is now TOS1, TOS
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                    true);
            methodVisitor.visitLdcInsn(operator.getRightDunderMethod());
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                    "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                            Type.getType(String.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, noRightMethod);

            // Stack is now TOS1, TOS, method
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            methodVisitor.visitInsn(Opcodes.POP);

            // Stack is now method, TOS1, TOS
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE), false);

            // Stack is now method, TOS1, TOS, argList
            pushArgumentIntoList(methodVisitor);
            pushArgumentIntoList(methodVisitor);

            // Stack is now method, argList
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

            // Stack is now method_result
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(NotImplemented.class),
                    "INSTANCE", Type.getDescriptor(NotImplemented.class));

            methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, done);
            // Stack is TOS1, TOS, NotImplemented
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, raiseError);

            methodVisitor.visitLabel(noRightMethod);
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.POP2);
        }
        methodVisitor.visitLabel(raiseError);
        methodVisitor.visitInsn(Opcodes.SWAP);
        raiseUnsupportedType(methodVisitor, localVariableHelper, operator);

        methodVisitor.visitLabel(done);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.POP2);
    }

    public static void binaryOperatorOnlyRight(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            PythonBinaryOperator operator) {
        Label done = new Label();
        Label raiseError = new Label();
        Label noRightMethod = new Label();

        methodVisitor.visitInsn(Opcodes.DUP2);

        // Stack is now TOS1, TOS
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getRightDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, noRightMethod);

        // Stack is now TOS1, TOS, method
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is now method, TOS1, TOS
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now method, TOS1, TOS, argList
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);

        // Stack is now method, argList
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

        // Stack is now method_result
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(NotImplemented.class),
                "INSTANCE", Type.getDescriptor(NotImplemented.class));

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, done);
        // Stack is TOS1, TOS, NotImplemented
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, raiseError);

        methodVisitor.visitLabel(noRightMethod);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.POP2);

        methodVisitor.visitLabel(raiseError);
        methodVisitor.visitInsn(Opcodes.SWAP);
        raiseUnsupportedType(methodVisitor, localVariableHelper, operator);

        methodVisitor.visitLabel(done);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.POP2);
    }

    /**
     * Performs a ternary dunder operation on TOS, TOS1 and TOS2. Generate codes that look like this:
     *
     * <pre>
     *    BiFunction[List, Map, Result] operand_method = TOS2.$getType().$getAttributeOrError(operator.getDunderMethod());
     *    List args = new ArrayList(2);
     *    args.set(0) = TOS2
     *    args.set(1) = TOS1
     *    args.set(2) = TOS
     *    pop TOS, TOS1, TOS2
     *    TOS' = operand_method.apply(args, null)
     * </pre>
     *
     */
    public static void ternaryOperator(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonTernaryOperator operator) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        StackManipulationImplementor.rotateThree(methodVisitor);
        methodVisitor.visitInsn(Opcodes.SWAP);
        // Stack is now TOS, TOS1, TOS2
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrError", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(String.class)),
                true);
        // Stack is now TOS, TOS1, TOS2, method
        StackManipulationImplementor.rotateFour(functionMetadata, stackMetadata.pop(3)
                .push(stackMetadata.getValueSourceForStackIndex(0))
                .push(stackMetadata.getValueSourceForStackIndex(1))
                .push(stackMetadata.getValueSourceForStackIndex(2))
                .pushTemp(BuiltinTypes.FUNCTION_TYPE));

        // Stack is now method, TOS, TOS1, TOS2
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonLikeList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonLikeList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        // Stack is now method, TOS, TOS1, TOS2, argList
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);
        pushArgumentIntoList(methodVisitor);

        // Stack is now method, argList
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
     * TOS is a list and TOS1 is an argument. Pushes TOS1 into TOS, and leave TOS on the stack (pops TOS1).
     */
    private static void pushArgumentIntoList(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                "add",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * Compares TOS and TOS1 via their dunder methods. {@code CompareOp} indicates the operation
     * to perform.
     */
    public static void compareValues(MethodVisitor methodVisitor, StackMetadata stackMetadata, CompareOp op) {
        switch (op) {
            case LESS_THAN:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperator.LESS_THAN);
                break;
            case LESS_THAN_OR_EQUALS:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperator.LESS_THAN_OR_EQUAL);
                break;
            case EQUALS:
            case NOT_EQUALS:
                binaryOpOverridingLeftIfSpecific(methodVisitor, stackMetadata, op);
                break;
            case GREATER_THAN:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperator.GREATER_THAN);
                break;
            case GREATER_THAN_OR_EQUALS:
                binaryOperator(methodVisitor, stackMetadata, PythonBinaryOperator.GREATER_THAN_OR_EQUAL);
                break;
            default:
                throw new IllegalStateException("Unhandled branch: " + op);
        }
    }

    private static void binaryOpOverridingLeftIfSpecific(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            CompareOp op) {
        switch (op) {
            case EQUALS:
            case NOT_EQUALS:
                break;
            default:
                throw new IllegalArgumentException("Should only be called for equals and not equals");
        }

        PythonBinaryOperator operator =
                (op == CompareOp.EQUALS) ? PythonBinaryOperator.EQUAL : PythonBinaryOperator.NOT_EQUAL;

        // If we know TOS1 defines == or !=, we don't need to go here
        if (stackMetadata.getTypeAtStackIndex(1).getDefiningTypeOrNull(operator.getDunderMethod()) != BuiltinTypes.BASE_TYPE) {
            binaryOperator(methodVisitor, stackMetadata, operator);
            return;
        }

        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                true);
        methodVisitor.visitLdcInsn(operator.getDunderMethod());
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonLikeType.class),
                "getDefiningTypeOrNull", Type.getMethodDescriptor(Type.getType(PythonLikeType.class),
                        Type.getType(String.class)),
                false);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(BuiltinTypes.class),
                "BASE_TYPE", Type.getDescriptor(PythonLikeType.class));

        Label ifDefined = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifDefined);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitLabel(ifDefined);
        binaryOperator(methodVisitor, stackMetadata.localVariableHelper, operator);
    }

    public static void getSlice(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        // stack: ..., collection, start, end
        var methodVisitor = functionMetadata.methodVisitor;
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonSlice.class));
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        // stack: ..., collection, <uninit slice>, <uninit slice>, start, end, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonSlice.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class)),
                false);
        // stack: ..., collection, slice
        DunderOperatorImplementor.binaryOperator(methodVisitor, stackMetadata
                .pop(3).pushTemps(stackMetadata.getTypeAtStackIndex(2), BuiltinTypes.SLICE_TYPE),
                PythonBinaryOperator.GET_ITEM);
    }

    public static void storeSlice(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        // stack: ..., values, collection, start, end
        var methodVisitor = functionMetadata.methodVisitor;
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonSlice.class));
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        // stack: ..., values, collection, <uninit slice>, <uninit slice>, start, end, null
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonSlice.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class)),
                false);
        // stack: ..., values, collection, slice
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        // stack: ..., slice, values, collection
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        // stack: ..., collection, slice, values
        DunderOperatorImplementor.ternaryOperator(functionMetadata, stackMetadata
                .pop(4).pushTemps(stackMetadata.getTypeAtStackIndex(2), BuiltinTypes.SLICE_TYPE,
                        stackMetadata.getTypeAtStackIndex(3)),
                PythonTernaryOperator.SET_ITEM);
        methodVisitor.visitInsn(Opcodes.POP);
    }
}
