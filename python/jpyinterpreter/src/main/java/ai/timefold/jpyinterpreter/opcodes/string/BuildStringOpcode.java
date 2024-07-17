package ai.timefold.jpyinterpreter.opcodes.string;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.StringImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class BuildStringOpcode extends AbstractOpcode {

    public BuildStringOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg()).push(
                ValueSourceInfo.of(this, BuiltinTypes.STRING_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(instruction.arg())));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.buildString(functionMetadata.methodVisitor, instruction.arg());
    }
}
