package ai.timefold.jpyinterpreter.opcodes.controlflow;

import java.util.Collections;
import java.util.List;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonFunctionType;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.GeneratorImplementor;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.implementors.PythonConstantsImplementor;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class ReturnConstantValueOpcode extends AbstractControlFlowOpcode {

    public ReturnConstantValueOpcode(PythonBytecodeInstruction instruction) {
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

    public PythonLikeObject getConstant(PythonCompiledFunction function) {
        return function.co_constants.get(instruction.arg());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeObject constant = getConstant(functionMetadata.pythonCompiledFunction);
        PythonLikeType constantType = constant.$getGenericType();
        if (functionMetadata.functionType == PythonFunctionType.GENERATOR) {
            PythonConstantsImplementor.loadConstant(functionMetadata.methodVisitor, functionMetadata.className,
                    instruction.arg());
            GeneratorImplementor.endGenerator(functionMetadata, stackMetadata.pushTemp(constantType));
        } else {
            PythonConstantsImplementor.loadConstant(functionMetadata.methodVisitor, functionMetadata.className,
                    instruction.arg());
            JavaPythonTypeConversionImplementor.returnValue(functionMetadata.methodVisitor, functionMetadata.method,
                    stackMetadata.pushTemp(constantType));
        }
    }
}
