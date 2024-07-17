package ai.timefold.jpyinterpreter.util;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;

public final class JumpUtils {

    private JumpUtils() {
    }

    public static int getInstructionIndexForByteOffset(int byteOffset, PythonVersion pythonVersion) {
        return byteOffset >> 1;
    }

    private static int parseArgRepr(PythonBytecodeInstruction instruction) {
        return Integer.parseInt(instruction.argRepr().substring(3)) / 2;
    }

    public static int getAbsoluteTarget(PythonBytecodeInstruction instruction, PythonVersion pythonVersion) {
        if (pythonVersion.isBefore(PythonVersion.PYTHON_3_12)) {
            return instruction.arg();
        } else {
            return parseArgRepr(instruction);
        }
    }

    public static int getRelativeTarget(PythonBytecodeInstruction instruction, PythonVersion pythonVersion) {
        if (pythonVersion.isBefore(PythonVersion.PYTHON_3_12)) {
            return instruction.offset() + instruction.arg() + 1;
        } else {
            return parseArgRepr(instruction);
        }
    }

    public static int getBackwardRelativeTarget(PythonBytecodeInstruction instruction, PythonVersion pythonVersion) {
        if (pythonVersion.isBefore(PythonVersion.PYTHON_3_12)) {
            return instruction.offset() - instruction.arg() + 1;
        } else {
            return parseArgRepr(instruction);
        }
    }
}
