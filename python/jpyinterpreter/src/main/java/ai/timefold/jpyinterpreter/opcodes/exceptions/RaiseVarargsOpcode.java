package ai.timefold.jpyinterpreter.opcodes.exceptions;

import java.util.Collections;
import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;

public class RaiseVarargsOpcode extends AbstractControlFlowOpcode {

    public RaiseVarargsOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return Collections.emptyList();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return Collections.emptyList();
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.raiseWithOptionalExceptionAndCause(functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
