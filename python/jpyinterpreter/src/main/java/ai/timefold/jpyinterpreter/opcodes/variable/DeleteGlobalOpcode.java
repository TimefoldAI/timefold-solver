package ai.timefold.jpyinterpreter.opcodes.variable;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.VariableImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class DeleteGlobalOpcode extends AbstractOpcode {

    public DeleteGlobalOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.copy();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.deleteGlobalVariable(functionMetadata.methodVisitor, functionMetadata.className,
                functionMetadata.pythonCompiledFunction, instruction);
    }
}
