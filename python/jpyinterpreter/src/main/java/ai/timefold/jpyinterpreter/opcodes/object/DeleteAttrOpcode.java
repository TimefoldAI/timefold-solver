package ai.timefold.jpyinterpreter.opcodes.object;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.ObjectImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class DeleteAttrOpcode extends AbstractOpcode {

    public DeleteAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(1);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.deleteAttribute(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                stackMetadata, instruction);
    }
}
