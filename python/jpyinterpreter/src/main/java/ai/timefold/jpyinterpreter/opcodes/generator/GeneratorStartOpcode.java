package ai.timefold.jpyinterpreter.opcodes.generator;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.GeneratorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class GeneratorStartOpcode extends AbstractOpcode {

    public GeneratorStartOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        GeneratorImplementor.generatorStart(functionMetadata, stackMetadata);
    }
}
