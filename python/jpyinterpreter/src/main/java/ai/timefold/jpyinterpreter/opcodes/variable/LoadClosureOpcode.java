package ai.timefold.jpyinterpreter.opcodes.variable;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.VariableImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.PythonCell;

public class LoadClosureOpcode extends AbstractOpcode {

    public LoadClosureOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(ValueSourceInfo.of(this, PythonCell.CELL_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.loadCell(functionMetadata, stackMetadata,
                VariableImplementor.getCellIndex(functionMetadata, instruction.arg()));
    }
}
