package ai.timefold.jpyinterpreter.opcodes.string;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.StringImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class PrintExprOpcode extends AbstractOpcode {

    public PrintExprOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StringImplementor.print(functionMetadata, stackMetadata);
    }
}
