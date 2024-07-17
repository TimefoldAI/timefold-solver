package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.function.LoadMethodOpcode;
import ai.timefold.jpyinterpreter.opcodes.object.DeleteAttrOpcode;
import ai.timefold.jpyinterpreter.opcodes.object.IsOpcode;
import ai.timefold.jpyinterpreter.opcodes.object.LoadAttrOpcode;
import ai.timefold.jpyinterpreter.opcodes.object.LoadSuperAttrOpcode;
import ai.timefold.jpyinterpreter.opcodes.object.StoreAttrOpcode;

public enum ObjectOpDescriptor implements OpcodeDescriptor {
    IS_OP(IsOpcode::new),
    LOAD_ATTR(new VersionMapping()
            .map(PythonVersion.MINIMUM_PYTHON_VERSION, LoadAttrOpcode::new)
            .map(PythonVersion.PYTHON_3_12,
                    instruction -> ((instruction.arg() & 1) == 1)
                            ? new LoadMethodOpcode(instruction.withArg(instruction.arg() >> 1))
                            : new LoadAttrOpcode(instruction.withArg(instruction.arg() >> 1)))),
    LOAD_SUPER_ATTR(LoadSuperAttrOpcode::new),
    STORE_ATTR(StoreAttrOpcode::new),
    DELETE_ATTR(DeleteAttrOpcode::new);

    final VersionMapping versionLookup;

    ObjectOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this(VersionMapping.constantMapping(opcodeFunction));
    }

    ObjectOpDescriptor(VersionMapping versionLookup) {
        this.versionLookup = versionLookup;
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
