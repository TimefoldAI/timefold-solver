package ai.timefold.jpyinterpreter.opcodes.dunder;

import java.util.Optional;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonFunctionSignature;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class BinaryDunderOpcode extends AbstractOpcode {

    final PythonBinaryOperator operator;

    public BinaryDunderOpcode(PythonBytecodeInstruction instruction, PythonBinaryOperator operator) {
        super(instruction);
        this.operator = operator;
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        PythonLikeType leftOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(1)).orElse(BuiltinTypes.BASE_TYPE);
        PythonLikeType rightOperand =
                Optional.ofNullable(stackMetadata.getTypeAtStackIndex(0)).orElse(BuiltinTypes.BASE_TYPE);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = leftOperand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters(rightOperand);
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                return stackMetadata.pop().pop().push(ValueSourceInfo.of(this, functionSignature.getReturnType(),
                        stackMetadata.getValueSourcesUpToStackIndex(2)));
            }
        }

        // TODO: Right dunder method
        return stackMetadata.pop().pop()
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.binaryOperator(functionMetadata.methodVisitor, stackMetadata, operator);
    }
}
