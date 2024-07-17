package ai.timefold.jpyinterpreter.opcodes.dunder;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.implementors.PythonBuiltinOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class NotOpcode extends AbstractOpcode {

    public NotOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.pop().push(ValueSourceInfo.of(this, BuiltinTypes.BOOLEAN_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.unaryOperator(functionMetadata.methodVisitor, stackMetadata,
                PythonUnaryOperator.AS_BOOLEAN);
        PythonBuiltinOperatorImplementor.performNotOnTOS(functionMetadata.methodVisitor);
    }
}
