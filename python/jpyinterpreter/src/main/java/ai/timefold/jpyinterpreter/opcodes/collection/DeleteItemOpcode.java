package ai.timefold.jpyinterpreter.opcodes.collection;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

import org.objectweb.asm.Opcodes;

public class DeleteItemOpcode extends AbstractOpcode {

    public DeleteItemOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(2);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.binaryOperator(functionMetadata.methodVisitor, stackMetadata,
                PythonBinaryOperator.DELETE_ITEM);
        functionMetadata.methodVisitor.visitInsn(Opcodes.POP); // DELETE_ITEM ignore results of delete function
    }
}
