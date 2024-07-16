package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.meta.NopOpcode;
import ai.timefold.jpyinterpreter.opcodes.meta.UnaryIntrinsicFunction;

public enum MetaOpDescriptor implements OpcodeDescriptor {
    /**
     * Do nothing code. Used as a placeholder by the bytecode optimizer.
     */
    NOP(NopOpcode::new),

    /**
     * A no-op code used by CPython to hold arbitrary data for JIT.
     */
    CACHE(NopOpcode::new),

    /**
     * Prefixes {@link FunctionOpDescriptor#CALL}.
     * Logically this is a no op.
     * It exists to enable effective specialization of calls. argc is the number of arguments as described in CALL.
     */
    PRECALL(NopOpcode::new),
    MAKE_CELL(NopOpcode::new),
    COPY_FREE_VARS(NopOpcode::new),
    CALL_INTRINSIC_1(UnaryIntrinsicFunction::lookup),

    // TODO
    EXTENDED_ARG(ignored -> {
        throw new UnsupportedOperationException("EXTENDED_ARG");
    }),

    /**
     * Pushes builtins.__build_class__() onto the stack.
     * It is later called by CALL_FUNCTION to construct a class.
     */
    LOAD_BUILD_CLASS(ignored -> {
        throw new UnsupportedOperationException("LOAD_BUILD_CLASS");
    }),

    /**
     * Checks whether __annotations__ is defined in locals(), if not it is set up to an empty dict. This opcode is only
     * emitted if a class or module body contains variable annotations statically.
     * TODO: Properly implement this
     */
    SETUP_ANNOTATIONS(NopOpcode::new);

    private final VersionMapping versionLookup;

    MetaOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
