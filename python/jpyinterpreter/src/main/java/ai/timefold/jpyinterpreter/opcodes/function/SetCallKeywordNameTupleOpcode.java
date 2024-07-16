package ai.timefold.jpyinterpreter.opcodes.function;

import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.FunctionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.PythonString;

public class SetCallKeywordNameTupleOpcode extends AbstractOpcode {

    public SetCallKeywordNameTupleOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.setCallKeywordNameList(
                ((List<PythonString>) functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg()))
                        .stream().map(PythonString::getValue).collect(Collectors.toList()));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.setCallKeywordNameTuple(functionMetadata, stackMetadata, instruction.arg());
    }
}
