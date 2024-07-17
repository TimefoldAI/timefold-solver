package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.module.ImportFromOpcode;
import ai.timefold.jpyinterpreter.opcodes.module.ImportNameOpcode;

public enum ModuleOpDescriptor implements OpcodeDescriptor {
    IMPORT_NAME(ImportNameOpcode::new),
    IMPORT_FROM(ImportFromOpcode::new),

    /**
     * Loads all symbols not starting with '_' directly from the module TOS to the local namespace. The module is popped
     * after
     * loading all names. This opcode implements from module import *.
     */
    IMPORT_STAR(instruction -> {
        // From https://docs.python.org/3/reference/simple_stmts.html#the-import-statement ,
        // Import * is only allowed at the module level and as such WILL never appear
        // in functions.
        throw new UnsupportedOperationException(
                "Impossible state/invalid bytecode: import * only allowed at module level");
    });

    final VersionMapping versionLookup;

    ModuleOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
