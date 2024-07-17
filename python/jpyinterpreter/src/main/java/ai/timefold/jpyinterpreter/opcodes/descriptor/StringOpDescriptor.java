package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.string.BuildStringOpcode;
import ai.timefold.jpyinterpreter.opcodes.string.FormatValueOpcode;
import ai.timefold.jpyinterpreter.opcodes.string.PrintExprOpcode;

public enum StringOpDescriptor implements OpcodeDescriptor {
    /**
     * Implements the expression statement for the interactive mode. TOS is removed from the stack and printed.
     * In non-interactive mode, an expression statement is terminated with POP_TOP.
     */
    PRINT_EXPR(PrintExprOpcode::new),
    FORMAT_VALUE(FormatValueOpcode::new),
    BUILD_STRING(BuildStringOpcode::new);

    final VersionMapping versionLookup;

    StringOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
