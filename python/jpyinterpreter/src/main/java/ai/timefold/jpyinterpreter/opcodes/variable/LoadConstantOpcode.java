package ai.timefold.jpyinterpreter.opcodes.variable;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.PythonConstantsImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

import org.objectweb.asm.Opcodes;

public class LoadConstantOpcode extends AbstractOpcode {

    public LoadConstantOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg());
        PythonLikeType constantType = constant.$getGenericType();
        return stackMetadata.push(ValueSourceInfo.of(this, constantType));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = functionMetadata.pythonCompiledFunction.co_constants.get(instruction.arg());
        PythonLikeType constantType = constant.$getGenericType();

        PythonConstantsImplementor.loadConstant(functionMetadata.methodVisitor, functionMetadata.className,
                instruction.arg());
        functionMetadata.methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, constantType.getJavaTypeInternalName());
    }
}
