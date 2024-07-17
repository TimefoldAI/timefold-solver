package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.function.CallFunctionKeywordOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.CallFunctionOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.CallFunctionUnpackOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.CallMethodOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.CallOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.LoadMethodOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.MakeFunctionOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.PushNullOpcode;
import ai.timefold.jpyinterpreter.opcodes.function.SetCallKeywordNameTupleOpcode;

public enum FunctionOpDescriptor implements OpcodeDescriptor {
    PUSH_NULL(PushNullOpcode::new),
    KW_NAMES(SetCallKeywordNameTupleOpcode::new),
    CALL(CallOpcode::new),
    CALL_FUNCTION(CallFunctionOpcode::new),
    CALL_FUNCTION_KW(CallFunctionKeywordOpcode::new),
    CALL_FUNCTION_EX(CallFunctionUnpackOpcode::new),
    LOAD_METHOD(LoadMethodOpcode::new),
    CALL_METHOD(CallMethodOpcode::new),
    MAKE_FUNCTION(MakeFunctionOpcode::new);

    final VersionMapping versionLookup;

    FunctionOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
