package ai.timefold.jpyinterpreter;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import ai.timefold.jpyinterpreter.types.PythonCell;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class GeneratorLocalVariableHelper extends LocalVariableHelper {

    final ClassWriter classWriter;

    final String classInternalName;

    final int cellStart;

    final int freeStart;

    int maxTemps;

    Map<Integer, String> slotToLocalName;

    Map<Integer, String> slotToLocalTypeDescriptor;

    public GeneratorLocalVariableHelper(ClassWriter classWriter, String classInternalName,
            Type[] parameters, PythonCompiledFunction compiledFunction) {
        super(parameters, compiledFunction);
        this.classWriter = classWriter;
        this.classInternalName = classInternalName;
        cellStart =
                compiledFunction.co_varnames.size();
        freeStart = compiledFunction.co_varnames.size() + compiledFunction.co_cellvars.size();
        slotToLocalName = new HashMap<>();
        slotToLocalTypeDescriptor = new HashMap<>();

        for (int i = 0; i < compiledFunction.co_varnames.size(); i++) {
            slotToLocalName.put(i, compiledFunction.co_varnames.get(i));
        }

        for (int i = 0; i < compiledFunction.co_cellvars.size(); i++) {
            slotToLocalName.put(i + compiledFunction.co_varnames.size(), compiledFunction.co_cellvars.get(i));
        }

        for (int i = 0; i < compiledFunction.co_freevars.size(); i++) {
            slotToLocalName.put(i + compiledFunction.co_varnames.size() + compiledFunction.co_cellvars.size(),
                    compiledFunction.co_freevars.get(i));
        }

        // Cannot use parameter types as the type descriptor, since the variables assigned to the
        // Python parameter can change types in the middle of code
        for (int i = 0; i < compiledFunction.co_varnames.size(); i++) {
            slotToLocalTypeDescriptor.put(i, Type.getDescriptor(PythonLikeObject.class));
        }

        for (int i = 0; i < compiledFunction.co_cellvars.size(); i++) {
            slotToLocalTypeDescriptor.put(i + compiledFunction.co_varnames.size(), Type.getDescriptor(PythonCell.class));
        }

        for (int i = 0; i < compiledFunction.co_freevars.size(); i++) {
            slotToLocalTypeDescriptor.put(i + compiledFunction.co_varnames.size() + compiledFunction.co_cellvars.size(),
                    Type.getDescriptor(PythonCell.class));
        }
    }

    GeneratorLocalVariableHelper(Type[] parameters, int argcount, int parameterSlotsEnd, int pythonCellVariablesStart,
            int pythonFreeVariablesStart, int pythonLocalVariablesSlotEnd,
            int pythonBoundVariables, int pythonFreeVariables, Map<Integer, Integer> boundCellIndexToVariableIndex,
            int currentExceptionVariableSlot, int callKeywordsSlot, Map<Integer, Integer> exceptionTableTargetToSavedStackMap,
            ClassWriter classWriter, String classInternalName, int maxTemps,
            int cellStart, int freeStart, Map<Integer, String> slotToLocalName,
            Map<Integer, String> slotToLocalTypeDescriptor) {
        super(parameters, argcount, parameterSlotsEnd, pythonCellVariablesStart,
                pythonFreeVariablesStart, pythonLocalVariablesSlotEnd,
                pythonBoundVariables, pythonFreeVariables, boundCellIndexToVariableIndex,
                currentExceptionVariableSlot, callKeywordsSlot, exceptionTableTargetToSavedStackMap);
        this.classWriter = classWriter;
        this.classInternalName = classInternalName;
        this.maxTemps = maxTemps;
        this.cellStart = cellStart;
        this.freeStart = freeStart;
        this.slotToLocalName = new HashMap<>(slotToLocalName);
        this.slotToLocalTypeDescriptor = new HashMap<>(slotToLocalTypeDescriptor);
    }

    public GeneratorLocalVariableHelper copy() {
        GeneratorLocalVariableHelper out = new GeneratorLocalVariableHelper(parameters, argcount, parameterSlotsEnd,
                pythonCellVariablesStart,
                pythonFreeVariablesStart, pythonLocalVariablesSlotEnd, pythonBoundVariables, pythonFreeVariables,
                boundCellIndexToVariableIndex, currentExceptionVariableSlot, callKeywordsSlot,
                exceptionTableTargetToSavedStackMap,
                classWriter, classInternalName, maxTemps, cellStart, freeStart, slotToLocalName, slotToLocalTypeDescriptor);
        out.usedLocals = usedLocals;
        return out;
    }

    private String slotToFieldName(int slot) {
        return "$temp" + slot;
    }

    @Override
    public int newLocal() {
        int slot = pythonLocalVariablesSlotEnd + usedLocals;
        usedLocals++;
        if (usedLocals > maxTemps) {
            maxTemps = usedLocals;
            classWriter.visitField(Modifier.PRIVATE, slotToFieldName(slot), Type.getDescriptor(Object.class), null, null);
        }
        return slot;
    }

    @Override
    public void freeLocal() {
        usedLocals--;
    }

    @Override
    public int getUsedLocals() {
        return usedLocals;
    }

    @Override
    public void readLocal(MethodVisitor methodVisitor, int local) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, slotToLocalName.get(local),
                slotToLocalTypeDescriptor.get(local));
    }

    @Override
    public void writeLocal(MethodVisitor methodVisitor, int local) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, slotToLocalName.get(local),
                slotToLocalTypeDescriptor.get(local));
    }

    @Override
    public void readCell(MethodVisitor methodVisitor, int cell) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, slotToLocalName.get(cellStart + cell),
                slotToLocalTypeDescriptor.get(cellStart + cell));
    }

    @Override
    public void writeCell(MethodVisitor methodVisitor, int cell) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, slotToLocalName.get(cellStart + cell),
                slotToLocalTypeDescriptor.get(cellStart + cell));
    }

    @Override
    public void writeFreeCell(MethodVisitor methodVisitor, int cell) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, slotToLocalName.get(freeStart + cell),
                slotToLocalTypeDescriptor.get(freeStart + cell));
    }

    @Override
    public void readCurrentException(MethodVisitor methodVisitor) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, PythonGeneratorTranslator.CURRENT_EXCEPTION,
                Type.getDescriptor(Throwable.class));
    }

    @Override
    public void writeCurrentException(MethodVisitor methodVisitor) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, PythonGeneratorTranslator.CURRENT_EXCEPTION,
                Type.getDescriptor(Throwable.class));
    }

    @Override
    public void readExceptionTableTargetStack(MethodVisitor methodVisitor, int target) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName,
                PythonGeneratorTranslator.exceptionHandlerTargetStackLocal(target),
                Type.getDescriptor(PythonLikeObject[].class));
    }

    @Override
    public void writeExceptionTableTargetStack(MethodVisitor methodVisitor, int target) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName,
                PythonGeneratorTranslator.exceptionHandlerTargetStackLocal(target),
                Type.getDescriptor(PythonLikeObject[].class));
    }

    @Override
    public void readTemp(MethodVisitor methodVisitor, Type type, int temp) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classInternalName, slotToFieldName(temp),
                Type.getDescriptor(Object.class));

        switch (type.getSort()) {
            case Type.OBJECT:
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                return;

            case Type.INT: {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Integer.class));
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue",
                        Type.getMethodDescriptor(Type.INT_TYPE), false);
            }

            default: {
                throw new IllegalArgumentException("Unsupported sort: " + type.getSort());
            }
        }
    }

    @Override
    public void writeTemp(MethodVisitor methodVisitor, Type type, int temp) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitInsn(Opcodes.SWAP);

        switch (type.getSort()) {
            case Type.OBJECT:
                break;

            case Type.INT: {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
                        Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE), false);
            }

            default: {
                throw new IllegalArgumentException("Unsupported sort: " + type.getSort());
            }
        }
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, slotToFieldName(temp),
                Type.getDescriptor(Object.class));
    }

    @Override
    public void incrementTemp(MethodVisitor methodVisitor, int temp) {
        readTemp(methodVisitor, Type.INT_TYPE, temp);
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.IADD);
        writeTemp(methodVisitor, Type.INT_TYPE, temp);
    }
}
