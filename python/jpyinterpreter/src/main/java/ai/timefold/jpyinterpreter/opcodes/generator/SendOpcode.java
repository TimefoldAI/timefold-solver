package ai.timefold.jpyinterpreter.opcodes.generator;

import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.GeneratorImplementor;
import ai.timefold.jpyinterpreter.opcodes.controlflow.AbstractControlFlowOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class SendOpcode extends AbstractControlFlowOpcode {
    int jumpTarget;

    public SendOpcode(PythonBytecodeInstruction instruction, int jumpTarget) {
        super(instruction);
        this.jumpTarget = jumpTarget;
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return List.of(
                getBytecodeIndex() + 1,
                jumpTarget);
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(
                stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(2))),
                stackMetadata.pop());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        GeneratorImplementor.progressSubgenerator(functionMetadata, stackMetadata, jumpTarget);
    }
}
