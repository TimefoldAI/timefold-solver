package ai.timefold.jpyinterpreter.opcodes.string;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.StringImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class FormatValueOpcode extends AbstractOpcode {

    public FormatValueOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        if ((instruction.arg() & 4) == 4) {
            // There is a format argument above the value
            return stackMetadata.pop(2)
                    .push(ValueSourceInfo.of(this, BuiltinTypes.STRING_TYPE,
                            stackMetadata.getValueSourcesUpToStackIndex(2)));
        } else {
            // There is no format argument above the value
            return stackMetadata.pop()
                    .push(ValueSourceInfo.of(this, BuiltinTypes.STRING_TYPE,
                            stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.formatValue(functionMetadata.methodVisitor, instruction);
    }
}
