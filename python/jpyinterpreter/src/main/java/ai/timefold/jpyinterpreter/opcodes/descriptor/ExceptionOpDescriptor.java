package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.BeforeWithOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.CheckExcMatchOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.CleanupThrowOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.LoadAssertionErrorOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.PopBlockOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.PopExceptOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.PushExcInfoOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.RaiseVarargsOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.ReraiseOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.SetupFinallyOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.SetupWithOpcode;
import ai.timefold.jpyinterpreter.opcodes.exceptions.WithExceptStartOpcode;
import ai.timefold.jpyinterpreter.util.JumpUtils;

public enum ExceptionOpDescriptor implements OpcodeDescriptor {
    /**
     * Pushes AssertionError onto the stack. Used by the assert statement.
     */
    LOAD_ASSERTION_ERROR(LoadAssertionErrorOpcode::new),
    /**
     * Removes one block from the block stack. Per frame, there is a stack of blocks,
     * denoting try statements, and such.
     */
    POP_BLOCK(PopBlockOpcode::new),

    /**
     * Removes one block from the block stack. The popped block must be an exception handler block, as implicitly created
     * when entering an except handler. In addition to popping extraneous values from the frame stack, the last three
     * popped values are used to restore the exception state.
     */
    POP_EXCEPT(PopExceptOpcode::new),

    /**
     * Re-raises the exception currently on top of the stack.
     */
    RERAISE(ReraiseOpcode::new),

    /**
     * Performs exception matching for except. Tests whether the TOS1 is an exception matching TOS.
     * Pops TOS and pushes the boolean result of the test.
     */
    CHECK_EXC_MATCH(CheckExcMatchOpcode::new),

    /**
     * Pops a value from the stack. Pushes the current exception to the top of the stack.
     * Pushes the value originally popped back to the stack. Used in exception handlers.
     */
    PUSH_EXC_INFO(PushExcInfoOpcode::new),

    RAISE_VARARGS(RaiseVarargsOpcode::new),

    /**
     * Calls the function in position 7 on the stack with the top three items on the stack as arguments. Used to implement
     * the call context_manager.__exit__(*exc_info()) when an exception has occurred in a with statement.
     */
    WITH_EXCEPT_START(WithExceptStartOpcode::new),
    SETUP_FINALLY(SetupFinallyOpcode::new, JumpUtils::getRelativeTarget),

    BEFORE_WITH(BeforeWithOpcode::new),
    SETUP_WITH(SetupWithOpcode::new, JumpUtils::getRelativeTarget),
    CLEANUP_THROW(CleanupThrowOpcode::new);

    final VersionMapping versionLookup;

    ExceptionOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    ExceptionOpDescriptor(BiFunction<PythonBytecodeInstruction, Integer, Opcode> opcodeFunction,
            ToIntBiFunction<PythonBytecodeInstruction, PythonVersion> jumpFunction) {
        this.versionLookup = VersionMapping.constantMapping((instruction, pythonVersion) -> opcodeFunction.apply(instruction,
                jumpFunction.applyAsInt(instruction, pythonVersion)));
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
