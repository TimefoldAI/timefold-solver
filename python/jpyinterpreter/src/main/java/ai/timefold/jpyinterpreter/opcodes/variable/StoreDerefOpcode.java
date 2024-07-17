package ai.timefold.jpyinterpreter.opcodes.variable;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.VariableImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class StoreDerefOpcode extends AbstractOpcode {

    public StoreDerefOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop().setCellVariableValueSource(
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg()), stackMetadata.getTOSValueSource());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.storeInCellVariable(functionMetadata, stackMetadata,
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg()));
    }
}
