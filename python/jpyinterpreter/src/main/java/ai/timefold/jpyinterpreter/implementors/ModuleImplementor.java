package ai.timefold.jpyinterpreter.implementors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ModuleImplementor {

    /**
     * TOS is from_list (list or None); TOS1 is level. Imports co_names[instruction.arg] using __import__ with
     * the given from_list and level (using the function globals and locals). TOS and TOS1 are popped,
     * and the imported module is pushed.
     *
     * @see PythonInterpreter#importModule(PythonInteger, List, Map, Map, String)
     */
    public static void importName(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Stack is level, from_list
        // We need it to be interpreter, from_list, level, globals, locals, name
        // (to call interpreter.importModule)

        // check if from_list is None
        Label fromListSet = new Label();

        methodVisitor.visitInsn(Opcodes.DUP);
        PythonConstantsImplementor.loadNone(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, fromListSet);
        // Is None; change it to a list
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class),
                "emptyList", Type.getMethodDescriptor(Type.getType(List.class)),
                false);

        // typecast from_list to List
        methodVisitor.visitLabel(fromListSet);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(List.class));
        methodVisitor.visitInsn(Opcodes.SWAP);

        // typecast level to PythonInteger
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonInteger.class));
        methodVisitor.visitInsn(Opcodes.SWAP);

        // Get the current function's interpreter
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, functionMetadata.className,
                PythonBytecodeToJavaBytecodeTranslator.INTERPRETER_INSTANCE_FIELD_NAME,
                Type.getDescriptor(PythonInterpreter.class));

        // Stack is level, from_list, interpreter
        // Duplicate interpreter BEFORE from_list and level
        methodVisitor.visitInsn(Opcodes.DUP_X2);

        // Stack is interpreter, level, from_list, interpreter

        // Remove the interpreter from TOS
        methodVisitor.visitInsn(Opcodes.POP);

        // Stack is interpreter, level, from_list

        // Get the globals and the locals from the function
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, functionMetadata.className,
                PythonBytecodeToJavaBytecodeTranslator.GLOBALS_MAP_STATIC_FIELD_NAME,
                Type.getDescriptor(Map.class));

        // TODO: Create Map of local variables which is stored in a constant slot?
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class),
                "emptyMap", Type.getMethodDescriptor(Type.getType(Map.class)),
                false);

        // Stack is interpreter, level, from_list, globals_map, locals_map

        // Finally, push the name of the module to load
        methodVisitor.visitLdcInsn(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg()));

        // Now call the interpreter's importModule function
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonInterpreter.class),
                "importModule", Type.getMethodDescriptor(Type.getType(PythonLikeObject.class),
                        Type.getType(PythonInteger.class), Type.getType(List.class),
                        Type.getType(Map.class), Type.getType(Map.class), Type.getType(String.class)),
                true);
    }

    /**
     * TOS is a module; Push the attribute co_names[instruction.arg] from module onto the stack. TOS is NOT popped.
     * (i.e. after this instruction, stack is module, attribute)
     *
     * @see PythonInterpreter#importModule(PythonInteger, List, Map, Map, String)
     */
    public static void importFrom(FunctionMetadata functionMetadata, StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        MethodVisitor methodVisitor = functionMetadata.methodVisitor;

        // Stack is module

        // Duplicate module
        methodVisitor.visitInsn(Opcodes.DUP);

        // Stack is module, module

        // Push the attribute name to load
        methodVisitor.visitLdcInsn(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg()));

        // Stack is module, module, attribute_name

        // Get the attribute
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                "$getAttributeOrError",
                Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(String.class)),
                true);

        // Stack is module, attribute
    }
}
