package ai.timefold.jpyinterpreter.opcodes.controlflow;

import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.JumpImplementor;

public class JumpIfTrueOrPopOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public JumpIfTrueOrPopOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
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
        return List.of(stackMetadata.pop(),
                stackMetadata.copy());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        JumpImplementor.jumpIfTrueElsePop(functionMetadata, stackMetadata, jumpTarget);
    }
}
