package ai.timefold.jpyinterpreter.implementors;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.opcodes.descriptor.VariableOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonCell;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implementations of local variable manipulation opcodes.
 * See https://tenthousandmeters.com/blog/python-behind-the-scenes-5-how-variables-are-implemented-in-cpython/
 * for a detailed explanation of the differences between LOAD_FAST, LOAD_GLOBAL, LOAD_DEREF, etc.
 */
public class VariableImplementor {

    /**
     * Loads the local variable or parameter indicated by the {@code instruction} argument onto the stack.
     */
    public static void loadLocalVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper) {
        localVariableHelper.readLocal(methodVisitor, instruction.arg());
    }

    /**
     * Stores TOS into the local variable or parameter indicated by the {@code instruction} argument.
     */
    public static void storeInLocalVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper) {
        localVariableHelper.writeLocal(methodVisitor, instruction.arg());
    }

    /**
     * Deletes the global variable or parameter indicated by the {@code instruction} argument.
     */
    public static void deleteGlobalVariable(MethodVisitor methodVisitor, String className,
            PythonCompiledFunction pythonCompiledFunction,
            PythonBytecodeInstruction instruction) {
        String globalName = pythonCompiledFunction.co_names.get(instruction.arg());

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
                "deleteGlobal", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class),
                        Type.getType(String.class)),
                true);
    }

    /**
     * Loads the global variable or parameter indicated by the {@code instruction} argument onto the stack.
     */
    public static void loadGlobalVariable(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int globalIndex,
            PythonLikeType globalType) {
        PythonCompiledFunction pythonCompiledFunction = functionMetadata.pythonCompiledFunction;
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        String className = functionMetadata.className;

        String globalName = pythonCompiledFunction.co_names.get(globalIndex);

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
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, globalType.getJavaTypeInternalName());
    }

    /**
     * Stores TOS into the global variable or parameter indicated by the {@code instruction} argument.
     */
    public static void storeInGlobalVariable(MethodVisitor methodVisitor, String className,
            PythonCompiledFunction pythonCompiledFunction,
            PythonBytecodeInstruction instruction) {
        String globalName = pythonCompiledFunction.co_names.get(instruction.arg());

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, className);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className,
                PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonInterpreter.class));
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className,
                PythonBytecodeToJavaBytecodeTranslator.GLOBALS_MAP_STATIC_FIELD_NAME,
                Type.getDescriptor(Map.class));
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitLdcInsn(globalName);
        StackManipulationImplementor.swap(methodVisitor);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonInterpreter.class),
                "setGlobal", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class),
                        Type.getType(String.class),
                        Type.getType(PythonLikeObject.class)),
                true);
    }

    /**
     * Deletes the local variable or parameter indicated by the {@code instruction} argument.
     */
    public static void deleteLocalVariable(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction,
            LocalVariableHelper localVariableHelper) {
        // Deleting is implemented as setting the value to null
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        localVariableHelper.writeLocal(methodVisitor, instruction.arg());
    }

    public static int getCellIndex(FunctionMetadata functionMetadata, int instructionArg) {
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
            // free variables are offset by co_varnames.size(), bound variables are not
            if (instructionArg >= functionMetadata.pythonCompiledFunction.co_cellvars.size()) {
                // it a free variable
                return instructionArg - functionMetadata.pythonCompiledFunction.co_varnames.size();
            }
            return instructionArg; // it a bound variable
        } else {
            return instructionArg; // Python 3.10 and below, we don't need to do anything
        }
    }

    /**
     * Loads the cell indicated by the {@code instruction} argument onto the stack.
     * This is used by {@link VariableOpDescriptor#LOAD_CLOSURE} when creating a closure
     * for a dependent function.
     */
    public static void createCell(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper, int cellIndex) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonCell.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonCell.class), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        methodVisitor.visitInsn(Opcodes.DUP);
        localVariableHelper.readCellInitialValue(methodVisitor, cellIndex);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeObject.class));
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                Type.getDescriptor(PythonLikeObject.class));
        localVariableHelper.writeCell(methodVisitor, cellIndex);
    }

    /**
     * Moves the {@code cellIndex} free variable (stored in the
     * {@link PythonBytecodeToJavaBytecodeTranslator#CELLS_INSTANCE_FIELD_NAME} field
     * to its corresponding local variable.
     */
    public static void setupFreeVariableCell(MethodVisitor methodVisitor, String internalClassName,
            LocalVariableHelper localVariableHelper, int cellIndex) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, internalClassName,
                PythonBytecodeToJavaBytecodeTranslator.CELLS_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonLikeTuple.class));
        methodVisitor.visitLdcInsn(cellIndex);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "get",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(int.class)),
                true);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonCell.class));
        localVariableHelper.writeFreeCell(methodVisitor, cellIndex);
    }

    /**
     * Loads the cell indicated by the {@code instruction} argument onto the stack.
     * This is used by {@link VariableOpDescriptor#LOAD_CLOSURE} when creating a closure
     * for a dependent function.
     */
    public static void loadCell(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int cellIndex) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;
        LocalVariableHelper localVariableHelper = stackMetadata.localVariableHelper;

        localVariableHelper.readCell(methodVisitor, cellIndex);
    }

    /**
     * Loads the cell variable/free variable indicated by the {@code instruction} argument onto the stack.
     * (which is an {@link PythonCell}, so it can see changes from the parent function).
     */
    public static void loadCellVariable(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int cellIndex) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        loadCell(functionMetadata, stackMetadata, cellIndex);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                Type.getDescriptor(PythonLikeObject.class));
    }

    /**
     * Stores TOS into the cell variable or parameter indicated by the {@code instruction} argument
     * (which is an {@link PythonCell}, so changes in the parent function affect the variable in dependent functions).
     */
    public static void storeInCellVariable(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int cellIndex) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        loadCell(functionMetadata, stackMetadata, cellIndex);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                Type.getDescriptor(PythonLikeObject.class));
    }

    /**
     * Deletes the cell variable or parameter indicated by the {@code instruction} argument
     * (which is an {@link PythonCell}, so changes in the parent function affect the variable in dependent functions).
     */
    public static void deleteCellVariable(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int cellIndex) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Deleting is implemented as setting the value to null
        loadCell(functionMetadata, stackMetadata, cellIndex);
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PythonCell.class), "cellValue",
                Type.getDescriptor(PythonLikeObject.class));
    }
}
