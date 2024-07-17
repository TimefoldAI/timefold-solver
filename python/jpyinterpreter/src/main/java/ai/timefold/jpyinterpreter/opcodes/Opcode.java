package ai.timefold.jpyinterpreter.opcodes;

import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;

public interface Opcode {

    /**
     * Return the bytecode index of the instruction, which can be used
     * to identify the instruction as the target of a jump.
     *
     * @return The bytecode index of the instruction, which is defined
     *         as the number of instructions before it in the instruction
     *         listing.
     */
    int getBytecodeIndex();

    /**
     * Return the possible next bytecode index after this instruction is executed.
     * The default simply return [getBytecodeIndex() + 1], but is
     * typically overwritten in jump instructions.
     *
     * @return the possible next bytecode index after this instruction is executed
     */
    default List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(getBytecodeIndex() + 1);
    }

    /**
     * Return a list of {@link StackMetadata} corresponding to each branch returned by
     * {@link #getPossibleNextBytecodeIndexList()}.
     *
     * @param functionMetadata Metadata about the function being compiled.
     * @param stackMetadata the StackMetadata just before this instruction is executed.
     * @return a new List, the same size as {@link #getPossibleNextBytecodeIndexList()},
     *         containing the StackMetadata after this instruction is executed for the given branch
     *         in {@link #getPossibleNextBytecodeIndexList()}.
     */
    List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata);

    /**
     * Implements the opcode.
     *
     * @param functionMetadata Metadata about the function being compiled.
     * @param stackMetadata Metadata about the state of the stack when this instruction is executed.
     */
    void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata);

    /**
     * @return true if this opcode the target of a jump
     */
    boolean isJumpTarget();

    /**
     * @return true if this opcode is a forced jump (i.e. goto)
     */
    default boolean isForcedJump() {
        return false;
    }

    static Opcode lookupOpcodeForInstruction(PythonBytecodeInstruction instruction, PythonVersion pythonVersion) {
        return AbstractOpcode.lookupInstruction(instruction.opname())
                .getVersionMapping()
                .getOpcodeForVersion(instruction, pythonVersion);
    }
}
