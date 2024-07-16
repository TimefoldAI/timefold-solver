package ai.timefold.jpyinterpreter.opcodes;

import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.StackMetadata;

public class OpcodeWithoutSource implements Opcode {

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

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        return other.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
