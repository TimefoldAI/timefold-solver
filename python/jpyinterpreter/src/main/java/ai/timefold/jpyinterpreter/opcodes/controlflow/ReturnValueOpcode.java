package ai.timefold.jpyinterpreter.opcodes.controlflow;

import java.util.Collections;
import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonFunctionType;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.GeneratorImplementor;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;

public class ReturnValueOpcode extends AbstractControlFlowOpcode {

    public ReturnValueOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public List<Integer> getPossibleNextBytecodeIndexList() {
        return Collections.emptyList();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return Collections.emptyList();
    }

    @Override
    public boolean isForcedJump() {
        return true;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        if (functionMetadata.functionType == PythonFunctionType.GENERATOR) {
            GeneratorImplementor.endGenerator(functionMetadata, stackMetadata);
        } else {
            JavaPythonTypeConversionImplementor.returnValue(functionMetadata.methodVisitor, functionMetadata.method,
                    stackMetadata);
        }
    }
}
