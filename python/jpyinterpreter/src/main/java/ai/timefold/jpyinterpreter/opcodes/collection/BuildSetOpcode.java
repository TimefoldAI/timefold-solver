package ai.timefold.jpyinterpreter.opcodes.collection;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.CollectionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeSet;

public class BuildSetOpcode extends AbstractOpcode {

    public BuildSetOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg()).push(ValueSourceInfo.of(this, BuiltinTypes.SET_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg())));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        // TODO: either modify reverseAdd for PythonLikeSet so it replaces already encountered elements
        //       or store the top count items in local variables and do it in forward order.
        CollectionImplementor.buildCollection(PythonLikeSet.class, functionMetadata.methodVisitor, instruction.arg());
    }
}
