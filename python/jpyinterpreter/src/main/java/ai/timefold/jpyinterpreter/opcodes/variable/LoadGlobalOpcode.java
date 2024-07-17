package ai.timefold.jpyinterpreter.opcodes.variable;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.VariableImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.wrappers.CPythonType;
import ai.timefold.jpyinterpreter.types.wrappers.PythonObjectWrapper;

import org.objectweb.asm.Opcodes;

public class LoadGlobalOpcode extends AbstractOpcode {

    public LoadGlobalOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    private int getGlobalIndex(FunctionMetadata functionMetadata) {
        return (functionMetadata.pythonCompiledFunction.pythonVersion.compareTo(PythonVersion.PYTHON_3_11) >= 0)
                ? instruction.arg() >> 1
                : instruction.arg();
    }

    private boolean pushNullBeforeGlobal(FunctionMetadata functionMetadata) {
        return functionMetadata.pythonCompiledFunction.pythonVersion.compareTo(PythonVersion.PYTHON_3_11) >= 0
                && ((instruction.arg() & 1) == 1);
    }

    private PythonLikeObject getGlobal(FunctionMetadata functionMetadata) {
        return functionMetadata.pythonCompiledFunction.globalsMap
                .get(functionMetadata.pythonCompiledFunction.co_names.get(getGlobalIndex(functionMetadata)));
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        boolean pushNull = pushNullBeforeGlobal(functionMetadata);
        PythonLikeObject global = getGlobal(functionMetadata);

        if (pushNull) {
            if (global != null) {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, BuiltinTypes.NULL_TYPE))
                        .push(ValueSourceInfo.of(this, global.$getGenericType()));
            } else {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, BuiltinTypes.NULL_TYPE))
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
            }
        } else {
            if (global != null) {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, global.$getGenericType()));
            } else {
                return stackMetadata
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
            }
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        int globalIndex = getGlobalIndex(functionMetadata);
        boolean pushNull = pushNullBeforeGlobal(functionMetadata);

        PythonLikeObject global = getGlobal(functionMetadata);

        if (global instanceof CPythonType || global instanceof PythonObjectWrapper) {
            // TODO: note native objects are used somewhere
        }

        if (pushNull) {
            functionMetadata.methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        }
        VariableImplementor.loadGlobalVariable(functionMetadata, stackMetadata, globalIndex,
                (global != null) ? global.$getGenericType() : BuiltinTypes.BASE_TYPE);
    }
}
