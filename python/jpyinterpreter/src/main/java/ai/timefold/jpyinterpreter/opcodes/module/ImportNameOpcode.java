package ai.timefold.jpyinterpreter.opcodes.module;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ModuleImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class ImportNameOpcode extends AbstractOpcode {

    public ImportNameOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2)
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ModuleImplementor.importName(functionMetadata, stackMetadata, instruction);
    }
}
