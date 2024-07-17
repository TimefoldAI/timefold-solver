package ai.timefold.jpyinterpreter.opcodes.exceptions;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

import org.objectweb.asm.Opcodes;

public class CleanupThrowOpcode extends AbstractOpcode {
    public CleanupThrowOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(3).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getTOSValueSource()));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.getValueFromStopIterationOrReraise(functionMetadata, stackMetadata);
        var methodVisitor = functionMetadata.methodVisitor;
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP2);
    }
}
