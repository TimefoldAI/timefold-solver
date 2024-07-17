package ai.timefold.jpyinterpreter.implementors;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.jpyinterpreter.BytecodeSwitchImplementor;
import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonGeneratorTranslator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.types.PythonGenerator;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.StopIteration;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class GeneratorImplementor {

    public static void restoreGeneratorState(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STACK,
                Type.getDescriptor(List.class));

        for (int i = stackMetadata.getStackSize() - 1; i >= 0; i--) {
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, stackMetadata.getTypeAtStackIndex(i).getJavaTypeInternalName());
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        methodVisitor.visitInsn(Opcodes.POP);
    }

    private static void saveGeneratorState(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Store stack in generatorStack
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(ArrayList.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(stackMetadata.getStackSize());
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STACK,
                Type.getDescriptor(List.class));

        for (int i = 0; i < stackMetadata.getStackSize() - 1; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "add",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), true);
            methodVisitor.visitInsn(Opcodes.POP); // Do not use return value of add
        }
        methodVisitor.visitInsn(Opcodes.POP);

        // Set the generator state
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitLdcInsn(instruction.offset() + 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STATE,
                Type.INT_TYPE.getDescriptor());
    }

    public static void yieldValue(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // First, store TOS in yieldedValue
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));

        // Next, save stack and generator position
        saveGeneratorState(instruction, functionMetadata, stackMetadata);

        // return control to the caller
        methodVisitor.visitInsn(Opcodes.RETURN);
    }

    public static void yieldFrom(PythonBytecodeInstruction instruction, FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // TODO: Find out what TOS, which is usually None, is used for

        // Pop TOS (Unknown what is used for)
        methodVisitor.visitInsn(Opcodes.POP);

        // Store the subiterator into yieldFromIterator
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className,
                PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                Type.getDescriptor(PythonLikeObject.class));

        // Save stack and position
        // Store stack in both locals and fields, just in case the iterator stops iteration immediately
        int[] storedStack = StackManipulationImplementor.storeStack(methodVisitor, stackMetadata.pop(2));
        saveGeneratorState(instruction, functionMetadata, stackMetadata.pop(2));

        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label catchStartLabel = new Label();
        Label catchEndLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchStartLabel,
                Type.getInternalName(StopIteration.class));

        methodVisitor.visitLabel(tryStartLabel);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, functionMetadata.className,
                PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                Type.getDescriptor(PythonLikeObject.class));
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));
        methodVisitor.visitInsn(Opcodes.RETURN); // subiterator yielded something; return control to caller

        methodVisitor.visitLabel(tryEndLabel);

        methodVisitor.visitLabel(catchStartLabel);
        methodVisitor.visitInsn(Opcodes.POP); // pop the StopIteration exception
        methodVisitor.visitLabel(catchEndLabel);

        // Set yieldFromIterator to null since it is finished
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className,
                PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                Type.getDescriptor(PythonLikeObject.class));

        // Restore the stack, since subiterator was empty, and resume execution
        StackManipulationImplementor.restoreStack(methodVisitor, stackMetadata.pop(2), storedStack);

        // Since the subiterator was empty, push None to TOS
        PythonConstantsImplementor.loadNone(methodVisitor);
    }

    public static void progressSubgenerator(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int jumpTarget) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        int[] stackVariables = StackManipulationImplementor.storeStack(methodVisitor, stackMetadata);

        Label wasNotSentValue = new Label();
        Label wasNotThrownValue = new Label();
        Label iterateSubiterator = new Label();

        // Stack is subgenerator, sentValue
        // Duplicate subgenerator
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Duplicate sent value
        methodVisitor.visitInsn(Opcodes.DUP);

        // Check if sent a value
        PythonConstantsImplementor.loadNone(methodVisitor);

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, wasNotSentValue);

        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, iterateSubiterator);

        methodVisitor.visitLabel(wasNotSentValue);

        // Check if thrown a value
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(PythonGenerator.class), "thrownValue",
                Type.getDescriptor(Throwable.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, wasNotThrownValue);

        methodVisitor.visitLdcInsn(2);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, iterateSubiterator);

        methodVisitor.visitLabel(wasNotThrownValue);

        // Else, should call next so it will also works with iterables
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitLdcInsn(0);

        methodVisitor.visitLabel(iterateSubiterator);
        // Stack is subgenerator, sent/thrownValue, switchCaseLabel

        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label catchStartLabel = new Label();
        Label catchEndLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchStartLabel,
                Type.getInternalName(StopIteration.class));

        methodVisitor.visitLabel(tryStartLabel);
        BytecodeSwitchImplementor.createIntSwitch(methodVisitor, List.of(0, 1, 2),
                key -> {
                    Label generatorOperationDone = new Label();
                    switch (key) {
                        case 0: { // next
                            methodVisitor.visitLdcInsn("next");
                            methodVisitor.visitInsn(Opcodes.POP);

                            methodVisitor.visitInsn(Opcodes.POP);
                            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
                            break;
                        }
                        case 1: { // send
                            methodVisitor.visitLdcInsn("send");
                            methodVisitor.visitInsn(Opcodes.POP);

                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                            PythonConstantsImplementor.loadNone(methodVisitor);
                            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonGenerator.class),
                                    "sentValue",
                                    Type.getDescriptor(PythonLikeObject.class));
                            FunctionImplementor.callBinaryMethod(methodVisitor,
                                    PythonBinaryOperator.SEND.dunderMethod);
                            break;
                        }
                        case 2: { // throw
                            methodVisitor.visitLdcInsn("throw");
                            methodVisitor.visitInsn(Opcodes.POP);

                            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Throwable.class));
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonGenerator.class),
                                    "thrownValue",
                                    Type.getDescriptor(Throwable.class));

                            methodVisitor.visitInsn(Opcodes.SWAP);
                            // Stack is now Throwable, Generator

                            // Check if the subgenerator has a "throw" method
                            methodVisitor.visitInsn(Opcodes.DUP);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    Type.getInternalName(PythonLikeObject.class),
                                    "$getType", Type.getMethodDescriptor(Type.getType(PythonLikeType.class)),
                                    true);
                            methodVisitor.visitLdcInsn("throw");
                            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    Type.getInternalName(PythonLikeObject.class),
                                    "$getAttributeOrNull",
                                    Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                                            Type.getType(String.class)),
                                    true);

                            // Stack is now Throwable, Generator, maybeMethod
                            Label ifThrowMethodPresent = new Label();
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                            methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifThrowMethodPresent);

                            // does not have a throw method
                            // Set yieldFromIterator to null since it is finished
                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className,
                                    PythonGeneratorTranslator.YIELD_FROM_ITERATOR,
                                    Type.getDescriptor(PythonLikeObject.class));

                            methodVisitor.visitInsn(Opcodes.POP);
                            methodVisitor.visitInsn(Opcodes.ATHROW);

                            methodVisitor.visitLabel(ifThrowMethodPresent);

                            // Swap so it Generator, Throwable instead of Throwable, Generator
                            methodVisitor.visitInsn(Opcodes.SWAP);
                            FunctionImplementor.callBinaryMethod(methodVisitor,
                                    PythonBinaryOperator.THROW.dunderMethod);
                            break;
                        }
                    }
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
                }, () -> {
                    methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalStateException.class));
                    methodVisitor.visitInsn(Opcodes.DUP);
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                            Type.getInternalName(IllegalStateException.class),
                            "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
                    methodVisitor.visitInsn(Opcodes.ATHROW);
                }, false);

        methodVisitor.visitLabel(tryEndLabel);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, catchEndLabel);

        methodVisitor.visitLabel(catchStartLabel);

        methodVisitor.visitInsn(Opcodes.POP); // pop the StopIteration exception
        StackManipulationImplementor.restoreStack(methodVisitor, stackMetadata, stackVariables);
        JumpImplementor.jumpAbsolute(functionMetadata, stackMetadata, jumpTarget);

        methodVisitor.visitLabel(catchEndLabel);
    }

    public static void getYieldFromIter(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        Label isGeneratorOrCoroutine = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(PythonGenerator.class));
        methodVisitor.visitJumpInsn(Opcodes.IFNE, isGeneratorOrCoroutine);

        // not a generator/coroutine
        DunderOperatorImplementor.unaryOperator(methodVisitor, stackMetadata, PythonUnaryOperator.ITERATOR);

        methodVisitor.visitLabel(isGeneratorOrCoroutine);
    }

    public static void endGenerator(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // First, store TOS in yieldedValue
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.YIELDED_VALUE,
                Type.getDescriptor(PythonLikeObject.class));

        // Next, set generatorStack to null
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STACK,
                Type.getDescriptor(List.class));

        // Set the generator state
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitLdcInsn(-1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, functionMetadata.className, PythonGeneratorTranslator.GENERATOR_STATE,
                Type.INT_TYPE.getDescriptor());

        methodVisitor.visitInsn(Opcodes.RETURN);
    }

    public static void generatorStart(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitInsn(Opcodes.POP); // Despite stackMetadata says it empty, the stack actually has
                                              // one item: the first sent item, which MUST BE None
    }
}
