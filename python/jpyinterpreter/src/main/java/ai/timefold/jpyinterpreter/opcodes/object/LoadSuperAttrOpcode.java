package ai.timefold.jpyinterpreter.opcodes.object;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ObjectImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class LoadSuperAttrOpcode extends AbstractOpcode {
    final int nameIndex;
    final boolean isLoadMethod;
    final boolean isTwoArgSuper;

    public LoadSuperAttrOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
        nameIndex = instruction.arg() >> 2;
        isLoadMethod = (instruction.arg() & 1) == 1;
        isTwoArgSuper = (instruction.arg() & 2) == 2;
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        if (isLoadMethod) {
            // Pop 3, Push None and Method
            return stackMetadata.pop(3)
                    .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(3)))
                    .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(3)));
        } else {
            // Pop 3, Push Attribute
            return stackMetadata.pop(3)
                    .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(3)));
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ObjectImplementor.getSuperAttribute(functionMetadata, stackMetadata, nameIndex, isLoadMethod);
    }
}
