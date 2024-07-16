package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.stack.CopyOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.DupOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.DupTwoOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.PopOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.RotateFourOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.RotateThreeOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.RotateTwoOpcode;
import ai.timefold.jpyinterpreter.opcodes.stack.SwapOpcode;

public enum StackOpDescriptor implements OpcodeDescriptor {
    /**
     * Removes the top-of-stack (TOS) item.
     */
    POP_TOP(PopOpcode::new),

    /**
     * Swaps the two top-most stack items.
     */
    ROT_TWO(RotateTwoOpcode::new),

    /**
     * Lifts second and third stack item one position up, moves top down to position three.
     */
    ROT_THREE(RotateThreeOpcode::new),

    /**
     * Lifts second, third and fourth stack items one position up, moves top down to position four.
     */
    ROT_FOUR(RotateFourOpcode::new),

    /**
     * Push the i-th item to the top of the stack. The item is not removed from its original location.
     * Uses 1-based indexing (TOS is 1, TOS1 is 2, ...) instead of 0-based indexing.
     */
    COPY(CopyOpcode::new),

    /**
     * Swap TOS with the item at position i. Uses 1-based indexing (TOS is 1, TOS1 is 2, ...) instead of 0-based indexing.
     */
    SWAP(SwapOpcode::new),

    /**
     * Duplicates the reference on top of the stack.
     */
    DUP_TOP(DupOpcode::new),

    /**
     * Duplicates the two references on top of the stack, leaving them in the same order.
     */
    DUP_TOP_TWO(DupTwoOpcode::new);

    final VersionMapping versionLookup;

    StackOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
