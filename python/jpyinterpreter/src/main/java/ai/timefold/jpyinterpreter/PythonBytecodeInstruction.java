package ai.timefold.jpyinterpreter;

import java.util.OptionalInt;

import ai.timefold.jpyinterpreter.opcodes.descriptor.OpcodeDescriptor;

public record PythonBytecodeInstruction(String opname, int offset, int arg,
        String argRepr, OptionalInt startsLine,
        boolean isJumpTarget) {
    public static PythonBytecodeInstruction atOffset(String opname, int offset) {
        return new PythonBytecodeInstruction(opname, offset, 0, "", OptionalInt.empty(), false);
    }

    public static PythonBytecodeInstruction atOffset(OpcodeDescriptor instruction, int offset) {
        return atOffset(instruction.name(), offset);
    }

    public PythonBytecodeInstruction withArg(int newArg) {
        return new PythonBytecodeInstruction(opname, offset, newArg, argRepr, startsLine, isJumpTarget);
    }

    public PythonBytecodeInstruction withArgRepr(String newArgRepr) {
        return new PythonBytecodeInstruction(opname, offset, arg, newArgRepr, startsLine, isJumpTarget);
    }

    public PythonBytecodeInstruction startsLine(int lineNumber) {
        return new PythonBytecodeInstruction(opname, offset, arg, argRepr, OptionalInt.of(lineNumber),
                isJumpTarget);
    }

    public PythonBytecodeInstruction withIsJumpTarget(boolean isJumpTarget) {
        return new PythonBytecodeInstruction(opname, offset, arg, argRepr, startsLine, isJumpTarget);
    }

    public PythonBytecodeInstruction markAsJumpTarget() {
        return new PythonBytecodeInstruction(opname, offset, arg, argRepr, startsLine, true);
    }

    @Override
    public String toString() {
        return "[%d] %s (%d) %s"
                .formatted(offset, opname, arg, isJumpTarget ? "{JUMP TARGET}" : "");
    }
}
