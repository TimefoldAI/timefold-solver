package ai.timefold.jpyinterpreter.opcodes.variable;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.VariableImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class LoadFastOpcode extends AbstractOpcode {

    public LoadFastOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.push(stackMetadata.getLocalVariableValueSource(instruction.arg()));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        VariableImplementor.loadLocalVariable(functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
