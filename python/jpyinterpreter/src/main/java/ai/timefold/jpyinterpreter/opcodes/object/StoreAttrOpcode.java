package ai.timefold.jpyinterpreter.opcodes.object;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.ObjectImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class StoreAttrOpcode extends AbstractOpcode {

    public StoreAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.setAttribute(functionMetadata, functionMetadata.methodVisitor, functionMetadata.className,
                stackMetadata,
                instruction, stackMetadata.localVariableHelper);
    }
}
