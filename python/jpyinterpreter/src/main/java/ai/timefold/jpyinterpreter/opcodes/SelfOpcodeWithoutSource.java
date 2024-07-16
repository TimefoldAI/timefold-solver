package ai.timefold.jpyinterpreter.opcodes;

import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.StackMetadata;

public class SelfOpcodeWithoutSource implements Opcode {

    @Override
    public int getBytecodeIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isJumpTarget() {
        throw new UnsupportedOperationException();
    }
}
