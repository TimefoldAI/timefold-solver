package ai.timefold.jpyinterpreter.opcodes.controlflow;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;

public abstract class AbstractControlFlowOpcode implements Opcode {
    protected PythonBytecodeInstruction instruction;

    public AbstractControlFlowOpcode(PythonBytecodeInstruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public boolean isJumpTarget() {
        return instruction.isJumpTarget();
    }

    @Override
    public int getBytecodeIndex() {
        return instruction.offset();
    }

    @Override
    public String toString() {
        return instruction.toString();
    }
}
