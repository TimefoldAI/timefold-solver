package ai.timefold.jpyinterpreter.implementors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.types.PythonSlice;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.StopIteration;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implementations of opcodes related to collections (list, tuple, set, dict).
 */
public class CollectionImplementor {

    /**
     * TOS is an iterator; perform TOS' = next(TOS).
     * If TOS is exhausted (which is indicated when it raises a {@link StopIteration} exception),
     * Jump relatively by the instruction argument and pop TOS. Otherwise,
     * leave TOS below TOS' and go to the next instruction.
     *
     * Note: {@link StopIteration} does not fill its stack trace, which make it much more efficient than
     * normal exceptions.
     */
    public static void iterateIterator(MethodVisitor methodVisitor, int jumpTarget,
            StackMetadata stackMetadata,
            FunctionMetadata functionMetadata) {
        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label catchStartLabel = new Label();
        Label catchEndLabel = new Label();
        Label loopEndLabel =
                functionMetadata.bytecodeCounterToLabelMap.computeIfAbsent(jumpTarget,
                        key -> new Label());

        int[] storedStack = StackManipulationImplementor.storeStack(methodVisitor, stackMetadata);

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, catchStartLabel,
                Type.getInternalName(StopIteration.class));

        methodVisitor.visitLabel(tryStartLabel);

        methodVisitor.visitInsn(Opcodes.DUP);
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
        methodVisitor.visitLabel(tryEndLabel);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, catchEndLabel);

        methodVisitor.visitLabel(catchStartLabel);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, loopEndLabel);
        methodVisitor.visitLabel(catchEndLabel);

        functionMetadata.bytecodeCounterToCodeArgumenterList
                .computeIfAbsent(jumpTarget, key -> new ArrayList<>())
                .add(() -> {
                    StackManipulationImplementor.restoreStack(methodVisitor, stackMetadata, storedStack);
                    methodVisitor.visitInsn(Opcodes.POP);
                });
    }

    /**
     * TOS is an iterable; push {@code toUnpack} elements from it to the stack
     * (with first item of the iterable as the new TOS). Raise an exception if it does not
     * have exactly {@code toUnpack} elements.
     */
    public static void unpackSequence(MethodVisitor methodVisitor, int toUnpack, LocalVariableHelper localVariableHelper) {
        // Initialize size and unpacked elements local variables
        int sizeLocal = localVariableHelper.newLocal();

        methodVisitor.visitInsn(Opcodes.ICONST_0);
        localVariableHelper.writeTemp(methodVisitor, Type.INT_TYPE, sizeLocal);

        int[] unpackedLocals = new int[toUnpack];

        for (int i = 0; i < toUnpack; i++) {
            unpackedLocals[i] = localVariableHelper.newLocal();
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(Object.class), unpackedLocals[i]);
        }

        // Get the iterator for the iterable
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.ITERATOR);

        // Surround the unpacking code in a try...finally block
        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label finallyStartLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, finallyStartLabel, null);

        methodVisitor.visitLabel(tryStartLabel);

        for (int i = 0; i < toUnpack + 1; i++) {
            // Call the next method of the iterator
            methodVisitor.visitInsn(Opcodes.DUP);
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
            if (i < toUnpack) { // Store the unpacked value in a local
                localVariableHelper.writeTemp(methodVisitor, Type.getType(Object.class), unpackedLocals[i]);
            } else { // Try to get more elements to see if the iterable contain exactly enough
                methodVisitor.visitInsn(Opcodes.POP);
            }
            // increment size
            localVariableHelper.incrementTemp(methodVisitor, sizeLocal);
        }
        methodVisitor.visitLabel(tryEndLabel);

        methodVisitor.visitLabel(finallyStartLabel);

        Label toFewElements = new Label();
        Label toManyElements = new Label();
        Label exactNumberOfElements = new Label();

        // Pop off the iterator
        methodVisitor.visitInsn(Opcodes.POP);

        // Check if too few
        localVariableHelper.readTemp(methodVisitor, Type.INT_TYPE, sizeLocal);
        methodVisitor.visitLdcInsn(toUnpack);
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, toFewElements);

        // Check if too many
        localVariableHelper.readTemp(methodVisitor, Type.INT_TYPE, sizeLocal);
        methodVisitor.visitLdcInsn(toUnpack);
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, toManyElements);

        // Must have exactly enough
        methodVisitor.visitJumpInsn(Opcodes.GOTO, exactNumberOfElements);

        // TODO: Throw ValueError instead
        methodVisitor.visitLabel(toFewElements);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);

        // Build error message string
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("not enough values to unpack (expected " + toUnpack + ", got ");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);

        localVariableHelper.readTemp(methodVisitor, Type.INT_TYPE, sizeLocal);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.INT_TYPE),
                false);

        methodVisitor.visitLdcInsn(")");
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.getType(String.class)),
                false);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                "toString", Type.getMethodDescriptor(Type.getType(String.class)),
                false);

        // Call the constructor of the Error
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        // And throw it
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(toManyElements);
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("too many values to unpack (expected " + toUnpack + ")");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(exactNumberOfElements);
        for (int i = toUnpack - 1; i >= 0; i--) {
            // Unlike all other collection operators, UNPACK_SEQUENCE unpacks the result in reverse order
            localVariableHelper.readTemp(methodVisitor, Type.getType(Object.class), unpackedLocals[i]);
        }

        localVariableHelper.freeLocal();
        for (int i = 0; i < toUnpack; i++) {
            localVariableHelper.freeLocal();
        }
    }

    /**
     * TOS is an iterable; push {@code toUnpack} elements from it to the stack
     * (with first item of the iterable as the new TOS). Below the elements in the stack
     * is a list containing the remaining elements in the iterable (empty if there are none).
     * Raise an exception if it has less than {@code toUnpack} elements.
     */
    public static void unpackSequenceWithTail(MethodVisitor methodVisitor, int toUnpack,
            LocalVariableHelper localVariableHelper) {
        // TODO: Correctly handle when high byte is set
        // Initialize size, unpacked elements and tail local variables
        int sizeLocal = localVariableHelper.newLocal();

        methodVisitor.visitInsn(Opcodes.ICONST_0);
        localVariableHelper.writeTemp(methodVisitor, Type.INT_TYPE, sizeLocal);

        int[] unpackedLocals = new int[toUnpack];

        for (int i = 0; i < toUnpack; i++) {
            unpackedLocals[i] = localVariableHelper.newLocal();
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            localVariableHelper.writeTemp(methodVisitor, Type.getType(Object.class), unpackedLocals[i]);
        }

        int tailLocal = localVariableHelper.newLocal();
        CollectionImplementor.buildCollection(PythonLikeList.class, methodVisitor, 0);
        localVariableHelper.writeTemp(methodVisitor, Type.getType(Object.class), tailLocal);

        // Get the iterator for the iterable
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.ITERATOR);

        // Surround the unpacking code in a try...finally block
        Label tryStartLabel = new Label();
        Label tryEndLabel = new Label();
        Label finallyStartLabel = new Label();

        methodVisitor.visitTryCatchBlock(tryStartLabel, tryEndLabel, finallyStartLabel, null);

        methodVisitor.visitLabel(tryStartLabel);

        for (int i = 0; i < toUnpack; i++) {
            // Call the next method of the iterator
            methodVisitor.visitInsn(Opcodes.DUP);
            DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
            // Store the unpacked value in a local
            localVariableHelper.writeTemp(methodVisitor, Type.getType(Object.class), unpackedLocals[i]);
            // increment size
            localVariableHelper.incrementTemp(methodVisitor, sizeLocal);
        }

        // Keep iterating through the iterable until StopIteration is raised to get all of its elements
        Label tailLoopStart = new Label();
        methodVisitor.visitLabel(tailLoopStart);

        methodVisitor.visitInsn(Opcodes.DUP);
        DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.NEXT);
        localVariableHelper.readTemp(methodVisitor, Type.getType(Object.class), tailLocal);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "add",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        methodVisitor.visitInsn(Opcodes.POP); // Pop the return value of add
        methodVisitor.visitJumpInsn(Opcodes.GOTO, tailLoopStart);

        methodVisitor.visitLabel(tryEndLabel);

        methodVisitor.visitLabel(finallyStartLabel);

        Label exactNumberOfElements = new Label();

        // Pop off the iterator
        methodVisitor.visitInsn(Opcodes.POP);

        // Check if too few
        localVariableHelper.readTemp(methodVisitor, Type.INT_TYPE, sizeLocal);
        methodVisitor.visitLdcInsn(toUnpack);
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, exactNumberOfElements);

        // TODO: Throw ValueError instead
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);

        // Build error message string
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("not enough values to unpack (expected " + toUnpack + ", got ");
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);

        localVariableHelper.readTemp(methodVisitor, Type.INT_TYPE, sizeLocal);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.INT_TYPE),
                false);

        methodVisitor.visitLdcInsn(")");
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                "append", Type.getMethodDescriptor(Type.getType(StringBuilder.class), Type.getType(String.class)),
                false);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                "toString", Type.getMethodDescriptor(Type.getType(String.class)),
                false);

        // Call the constructor of the Error
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        // And throw it
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(exactNumberOfElements);

        // Unlike all other collection operators, UNPACK_SEQUENCE unpacks the result in reverse order
        localVariableHelper.readTemp(methodVisitor, Type.getType(Object.class), tailLocal);
        for (int i = toUnpack - 1; i >= 0; i--) {
            localVariableHelper.readTemp(methodVisitor, Type.getType(Object.class), unpackedLocals[i]);
        }

        localVariableHelper.freeLocal();
        for (int i = 0; i < toUnpack; i++) {
            localVariableHelper.freeLocal();
        }
        localVariableHelper.freeLocal();
    }

    /**
     * Constructs a collection from the top {@code itemCount} on the stack.
     * {@code collectionType} MUST implement PythonLikeObject and define
     * a reverseAdd(PythonLikeObject) method. Basically generate the following code:
     *
     * <pre>
     *     CollectionType collection = new CollectionType(itemCount);
     *     collection.reverseAdd(TOS);
     *     collection.reverseAdd(TOS1);
     *     ...
     *     collection.reverseAdd(TOS(itemCount - 1));
     * </pre>
     *
     * @param collectionType The type of collection to create
     * @param itemCount The number of items to put into collection from the stack
     */
    public static void buildCollection(Class<?> collectionType, MethodVisitor methodVisitor,
            int itemCount) {
        String typeInternalName = Type.getInternalName(collectionType);
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn(itemCount);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInternalName, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

        for (int i = 0; i < itemCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X1);
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typeInternalName,
                    "reverseAdd",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeObject.class)),
                    false);
        }
    }

    /**
     * Convert TOS from a List to a tuple. Basically generates this code
     *
     * <pre>
     *     TOS' = PythonLikeTuple.fromList(TOS);
     * </pre>
     */
    public static void convertListToTuple(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonLikeTuple.class),
                "fromList",
                Type.getMethodDescriptor(Type.getType(PythonLikeTuple.class), Type.getType(List.class)),
                false);
    }

    /**
     * Constructs a map from the top {@code 2 * itemCount} on the stack.
     * {@code mapType} MUST implement PythonLikeObject. Basically generate the following code:
     *
     * <pre>
     *     MapType collection = new MapType(itemCount);
     *     collection.put(TOS1, TOS);
     *     collection.put(TOS3, TOS2);
     *     ...
     *     collection.put(TTOS(2*itemCount - 1), TOS(2*itemCount - 2));
     * </pre>
     *
     * @param mapType The type of map to create
     * @param itemCount The number of key value pairs to put into map from the stack
     */
    public static void buildMap(Class<? extends Map> mapType, MethodVisitor methodVisitor,
            int itemCount) {
        String typeInternalName = Type.getInternalName(mapType);
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInternalName, "<init>", "()V", false);

        for (int i = 0; i < itemCount; i++) {
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            StackManipulationImplementor.rotateThree(methodVisitor);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                    "put",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class),
                            Type.getType(Object.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.POP); // pop return value of "put"
        }
    }

    /**
     * Constructs a map from the top {@code itemCount + 1} on the stack.
     * TOS is a tuple containing keys; TOS1-TOS(itemCount) are the values
     * {@code mapType} MUST implement PythonLikeObject. Basically generate the following code:
     *
     * <pre>
     *     MapType collection = new MapType();
     *     collection.put(TOS[0], TOS1);
     *     collection.put(TOS[1], TOS2);
     *     ...
     *     collection.put(TOS[itemCount-1], TOS(itemCount));
     * </pre>
     *
     * @param mapType The type of map to create
     * @param itemCount The number of key value pairs to put into map from the stack
     */
    public static void buildConstKeysMap(Class<? extends Map> mapType, MethodVisitor methodVisitor,
            int itemCount) {
        String typeInternalName = Type.getInternalName(mapType);
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeInternalName);
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInternalName, "<init>", "()V", false);

        for (int i = 0; i < itemCount; i++) {
            // Stack is value, keyTuple, Map
            methodVisitor.visitInsn(Opcodes.DUP_X2);
            StackManipulationImplementor.rotateThree(methodVisitor);

            // Stack is Map, Map, value, keyTuple
            methodVisitor.visitInsn(Opcodes.DUP_X2);

            //Stack is Map, keyTuple, Map, value, keyTuple
            methodVisitor.visitLdcInsn(itemCount - i - 1); // We are adding in reverse order
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                    "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(int.class)),
                    true);

            // Stack is Map, keyTuple, Map, value, key
            methodVisitor.visitInsn(Opcodes.SWAP);

            // Stack is Map, keyTuple, Map, key, value
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                    "put",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class),
                            Type.getType(Object.class)),
                    true);
            methodVisitor.visitInsn(Opcodes.POP); // pop return value of "put"

            // Stack is Map, keyTuple
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
        // Stack is keyTuple, Map
        // Pop the keyTuple off the stack
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.POP);
    }

    /**
     * Implements TOS1 in TOS. TOS must be a collection/object that implement the "__contains__" dunder method.
     */
    public static void containsOperator(MethodVisitor methodVisitor, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        StackManipulationImplementor.swap(methodVisitor);
        DunderOperatorImplementor.binaryOperator(methodVisitor, stackMetadata
                .pop(2)
                .push(stackMetadata.getTOSValueSource())
                .push(stackMetadata.getValueSourceForStackIndex(1)), PythonBinaryOperator.CONTAINS);
        // TODO: implement fallback on __iter__ if __contains__ does not exist
        if (instruction.arg() == 1) {
            PythonBuiltinOperatorImplementor.performNotOnTOS(methodVisitor);
        }
    }

    /**
     * Implements TOS1[TOS] = TOS2. TOS1 must be a collection/object that implement the "__setitem__" dunder method.
     */
    public static void setItem(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Stack is TOS2, TOS1, TOS
        StackManipulationImplementor.rotateThree(methodVisitor);
        StackManipulationImplementor.rotateThree(methodVisitor);
        // Stack is TOS1, TOS, TOS2
        DunderOperatorImplementor.ternaryOperator(functionMetadata, stackMetadata.pop(3)
                .push(stackMetadata.getValueSourceForStackIndex(1))
                .push(stackMetadata.getValueSourceForStackIndex(0))
                .push(stackMetadata.getValueSourceForStackIndex(2)), PythonTernaryOperator.SET_ITEM);
        StackManipulationImplementor.popTOS(methodVisitor);
    }

    /**
     * Calls collection.add(TOS[i], TOS). TOS[i] remains on stack; TOS is popped. Used to implement list/set comprehensions.
     */
    public static void collectionAdd(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // instruction.arg is distance from TOS
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg());
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "add",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)),
                true);
        StackManipulationImplementor.popTOS(methodVisitor); // pop Collection.add return value
    }

    /**
     * Calls collection.addAll(TOS[i], TOS). TOS[i] remains on stack; TOS is popped. Used to build lists/maps.
     */
    public static void collectionAddAll(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // instruction.arg is distance from TOS
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg());
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Collection.class),
                "addAll",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Collection.class)),
                true);
        StackManipulationImplementor.popTOS(methodVisitor); // pop Collection.add return value
    }

    /**
     * Calls map.put(TOS1[i], TOS1, TOS). TOS1[i] remains on stack; TOS and TOS1 are popped. Used to implement map
     * comprehensions.
     */
    public static void mapPut(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // instruction.arg is distance from TOS1, so add 1 to get distance from TOS
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg() + 1);

        StackManipulationImplementor.rotateThree(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "put",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                true);
        StackManipulationImplementor.popTOS(methodVisitor); // pop Map.put return value
    }

    /**
     * Calls map.putAll(TOS[i], TOS). TOS[i] remains on stack; TOS is popped. Used to build maps
     */
    public static void mapPutAll(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        // instruction.arg is distance from TOS
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg());
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "putAll",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class)),
                true);
    }

    /**
     * Calls map.putAll(TOS1[i], TOS) if TOS does not share any common keys with TOS[i].
     * If TOS shares common keys with TOS[i], an exception is thrown.
     * TOS1[i] remains on stack; TOS is popped. Used to build maps
     */
    public static void mapPutAllOnlyIfAllNewElseThrow(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // instruction.arg is distance from TOS
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg());
        StackManipulationImplementor.swap(methodVisitor);

        // Duplicate both maps so we can get their key sets
        StackManipulationImplementor.duplicateTOSAndTOS1(methodVisitor);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "keySet",
                Type.getMethodDescriptor(Type.getType(Set.class)),
                true);

        StackManipulationImplementor.swap(methodVisitor);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "keySet",
                Type.getMethodDescriptor(Type.getType(Set.class)),
                true);

        // Check if the two key sets are disjoints
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class),
                "disjoint",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Collection.class), Type.getType(Collection.class)),
                false);

        Label performPutAllLabel = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFNE, performPutAllLabel); // if result == 1 (i.e. true), do the putAll operation

        // else, throw a new exception
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalArgumentException.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IllegalArgumentException.class),
                "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE),
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(performPutAllLabel);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class),
                "putAll",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class)),
                true);
    }

    public static void buildSlice(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int argCount) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        if (argCount == 2) {
            // Push 1 as the third argument (step)
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(PythonInteger.class), "ONE",
                    Type.getDescriptor(PythonInteger.class));
        } else if (argCount == 3) {
            // do nothing; third argument already on stack
        } else {
            throw new IllegalArgumentException("arg for BUILD_SLICE must be 2 or 3");
        }

        // Store step in temp variable (need to move slice down 2)
        int stepTemp = localVariableHelper.newLocal();
        localVariableHelper.writeTemp(methodVisitor, Type.getType(PythonLikeObject.class), stepTemp);

        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonSlice.class));
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);

        // Restore step
        localVariableHelper.readTemp(methodVisitor, Type.getType(PythonLikeObject.class), stepTemp);
        localVariableHelper.freeLocal();

        // Create the slice
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonSlice.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE,
                        Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class)),
                false);
    }
}
