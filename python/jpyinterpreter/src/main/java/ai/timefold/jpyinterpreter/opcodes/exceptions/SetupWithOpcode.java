package ai.timefold.jpyinterpreter.opcodes.exceptions;

import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;
import ai.timefold.jpyinterpreter.types.errors.PythonTraceback;

public class SetupWithOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public SetupWithOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                jumpTarget);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(
                stackMetadata
                        .pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(), stackMetadata.getTOSValueSource()))
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getTOSValueSource())),
                stackMetadata
                        .pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(), stackMetadata.getTOSValueSource()))
                        .pushTemp(BuiltinTypes.NONE_TYPE)
                        .pushTemp(BuiltinTypes.INT_TYPE)
                        .pushTemp(BuiltinTypes.NONE_TYPE)
                        .pushTemp(PythonTraceback.TRACEBACK_TYPE)
                        .pushTemp(PythonBaseException.BASE_EXCEPTION_TYPE)
                        .pushTemp(BuiltinTypes.TYPE_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.setupWith(jumpTarget, functionMetadata, stackMetadata);
    }
}
