package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.variable.DeleteDerefOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.DeleteFastOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.DeleteGlobalOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.LoadClosureOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.LoadConstantOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.LoadDerefOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.LoadFastAndClearOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.LoadFastOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.LoadGlobalOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.StoreDerefOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.StoreFastOpcode;
import ai.timefold.jpyinterpreter.opcodes.variable.StoreGlobalOpcode;

public enum VariableOpDescriptor implements OpcodeDescriptor {
    LOAD_CONST(LoadConstantOpcode::new),

    LOAD_NAME(VersionMapping.unimplemented()), //TODO
    STORE_NAME(VersionMapping.unimplemented()), //TODO
    DELETE_NAME(VersionMapping.unimplemented()), //TODO
    LOAD_GLOBAL(LoadGlobalOpcode::new),
    STORE_GLOBAL(StoreGlobalOpcode::new),
    DELETE_GLOBAL(DeleteGlobalOpcode::new),
    // TODO: Implement unbound local variable checks
    LOAD_FAST(LoadFastOpcode::new),

    // This is LOAD_FAST but do an unbound variable check
    LOAD_FAST_CHECK(LoadFastOpcode::new),

    LOAD_FAST_AND_CLEAR(LoadFastAndClearOpcode::new),
    STORE_FAST(StoreFastOpcode::new),
    DELETE_FAST(DeleteFastOpcode::new),
    LOAD_CLOSURE(LoadClosureOpcode::new),
    LOAD_DEREF(LoadDerefOpcode::new),
    STORE_DEREF(StoreDerefOpcode::new),
    DELETE_DEREF(DeleteDerefOpcode::new),
    LOAD_CLASSDEREF(VersionMapping.unimplemented());

    final VersionMapping versionLookup;

    VariableOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this(VersionMapping.constantMapping(opcodeFunction));
    }

    VariableOpDescriptor(VersionMapping lookup) {
        this.versionLookup = lookup;
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
