package ai.timefold.jpyinterpreter.implementors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StringOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class StringImplementor {

    /**
     * Constructs a string from the top {@code itemCount} on the stack.
     * Basically generate the following code:
     *
     * <pre>
     *     StringBuilder builder = new StringBuilder();
     *     builder.insert(0, TOS);
     *     builder.insert(0, TOS1);
     *     ...
     *     builder.insert(0, TOS(itemCount - 1));
     *     TOS' = PythonString.valueOf(builder.toString())
     * </pre>
     *
     * @param itemCount The number of items to put into collection from the stack
     */
    public static void buildString(MethodVisitor methodVisitor,
            int itemCount) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);

        for (int i = 0; i < itemCount; i++) {
            methodVisitor.visitInsn(Opcodes.SWAP);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.SWAP);

            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class),
                    "insert",
                    Type.getMethodDescriptor(Type.getType(StringBuilder.class),
                            Type.INT_TYPE,
                            Type.getType(Object.class)),
                    false);
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                "toString",
                Type.getMethodDescriptor(Type.getType(String.class)), false);

        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonString.class),
                "valueOf",
                Type.getMethodDescriptor(Type.getType(PythonString.class),
                        Type.getType(String.class)),
                false);
    }

    /**
     * TOS1 is a value and TOS is an optional format string (either PythonNone or PythonString).
     * Depending on {@code instruction.arg}, does one of several things to TOS1 before formatting it
     * (as specified by {@link StringOpDescriptor#FORMAT_VALUE}:
     *
     * arg &amp; 3 == 0: Do nothing
     * arg &amp; 3 == 1: Call str() on value before formatting it
     * arg &amp; 3 == 2: Call repr() on value before formatting it
     * arg &amp; 3 == 3: Call ascii() on value before formatting it
     *
     * if arg &amp; 4 == 0, TOS is the value to format, so push PythonNone before calling format
     * if arg &amp; 4 == 4, TOS is a format string, use it in the call
     */
    public static void formatValue(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        if ((instruction.arg() & 4) == 0) {
            // No format string on stack; push None
            PythonConstantsImplementor.loadNone(methodVisitor);
        }

        switch (instruction.arg() & 3) {
            case 0 -> {
                // Do Nothing
            }
            case 1 -> {
                // Call str()
                StackManipulationImplementor.swap(methodVisitor);
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.AS_STRING);
                StackManipulationImplementor.swap(methodVisitor);
            }
            case 2 -> {
                // Call repr()
                StackManipulationImplementor.swap(methodVisitor);
                DunderOperatorImplementor.unaryOperator(methodVisitor, PythonUnaryOperator.REPRESENTATION);
                StackManipulationImplementor.swap(methodVisitor);
            }
            case 3 -> {
                // Call ascii()
                StackManipulationImplementor.swap(methodVisitor);
                // TODO: Create method that calls repr and convert non-ascii character to ascii and call it
                StackManipulationImplementor.swap(methodVisitor);
            }
            default -> throw new IllegalStateException("Invalid flag: %d; & did not produce a value in range 0-3: %d"
                    .formatted(instruction.arg(), instruction.arg() & 3));
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$method$__format__",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    /**
     * TOS is an PythonLikeObject to be printed. Pop TOS off the stack and print it.
     */
    public static void print(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        String className = functionMetadata.className;
        String globalName = "print";
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, className);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonInterpreter.class));
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className,
                PythonBytecodeToJavaBytecodeTranslator.GLOBALS_MAP_STATIC_FIELD_NAME,
                Type.getDescriptor(Map.class));
        methodVisitor.visitLdcInsn(globalName);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonInterpreter.class),
                "getGlobal", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(Map.class),
                        Type.getType(String.class)),
                true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeFunction.class));
        methodVisitor.visitInsn(Opcodes.SWAP);
        CollectionImplementor.buildCollection(PythonLikeTuple.class, methodVisitor, 1);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class), "emptyMap",
                Type.getMethodDescriptor(Type.getType(Map.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeFunction.class), "$call",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(List.class),
                        Type.getType(Map.class), Type.getType(PythonLikeObject.class)),
                true);
    }
}
