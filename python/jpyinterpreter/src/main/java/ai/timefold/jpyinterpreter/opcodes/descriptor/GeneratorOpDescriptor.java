package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.generator.GeneratorStartOpcode;
import ai.timefold.jpyinterpreter.opcodes.generator.GetYieldFromIterOpcode;
import ai.timefold.jpyinterpreter.opcodes.generator.ResumeOpcode;
import ai.timefold.jpyinterpreter.opcodes.generator.SendOpcode;
import ai.timefold.jpyinterpreter.opcodes.generator.YieldFromOpcode;
import ai.timefold.jpyinterpreter.opcodes.generator.YieldValueOpcode;
import ai.timefold.jpyinterpreter.opcodes.meta.NopOpcode;
import ai.timefold.jpyinterpreter.opcodes.meta.ReturnGeneratorOpcode;
import ai.timefold.jpyinterpreter.util.JumpUtils;

public enum GeneratorOpDescriptor implements OpcodeDescriptor {
    /**
     * Another do nothing code. Performs internal tracing, debugging and optimization checks in CPython
     */
    RESUME(ResumeOpcode::new),

    /**
     * Pops TOS and yields it from a generator.
     */
    YIELD_VALUE(YieldValueOpcode::new),

    /**
     * Pops TOS and delegates to it as a subiterator from a generator.
     */
    YIELD_FROM(YieldFromOpcode::new),

    /**
     * If TOS is a generator iterator or coroutine object it is left as is.
     * Otherwise, implements TOS = iter(TOS).
     */
    GET_YIELD_FROM_ITER(GetYieldFromIterOpcode::new),

    /**
     * Pops TOS. The kind operand corresponds to the type of generator or coroutine.
     * The legal kinds are 0 for generator, 1 for coroutine, and 2 for async generator.
     */
    GEN_START(GeneratorStartOpcode::new),

    /**
     * TOS1 is a subgenerator, TOS is a value. Calls TOS1.send(TOS) if self.thrownValue is null.
     * Otherwise, set self.thrownValue to null and call TOS1.throwValue(TOS) instead. TOS is replaced by the subgenerator
     * yielded value; TOS1 remains. When the subgenerator is exhausted, jump forward by its argument.
     */
    SEND(SendOpcode::new, JumpUtils::getRelativeTarget),

    END_SEND(NopOpcode::new),

    /**
     * Create a generator, coroutine, or async generator from the current frame.
     * Clear the current frame and return the newly created generator. A no-op for us, since we detect if
     * the code represent a generator (and if so, generate a wrapper function for it that act like
     * RETURN_GENERATOR) before interpreting it
     */
    RETURN_GENERATOR(ReturnGeneratorOpcode::new);

    final VersionMapping versionLookup;

    GeneratorOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    GeneratorOpDescriptor(BiFunction<PythonBytecodeInstruction, Integer, Opcode> opcodeConstructor,
            ToIntBiFunction<PythonBytecodeInstruction, PythonVersion> labelFunction) {
        this.versionLookup = VersionMapping.constantMapping(
                (instruction, pythonVersion) -> opcodeConstructor.apply(
                        instruction,
                        labelFunction.applyAsInt(instruction, pythonVersion)));
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
