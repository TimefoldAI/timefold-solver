package ai.timefold.jpyinterpreter.opcodes.dunder;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class CompareOpcode extends AbstractOpcode {

    public CompareOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BuiltinTypes.BOOLEAN_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.compareValues(functionMetadata.methodVisitor, stackMetadata,
                CompareOp.getOp(instruction.argRepr()));
    }
}
