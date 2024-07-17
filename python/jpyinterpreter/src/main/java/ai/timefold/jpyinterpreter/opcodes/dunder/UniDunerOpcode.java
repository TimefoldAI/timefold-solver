package ai.timefold.jpyinterpreter.opcodes.dunder;

import java.util.Optional;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonFunctionSignature;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class UniDunerOpcode extends AbstractOpcode {

    final PythonUnaryOperator operator;

    public UniDunerOpcode(PythonBytecodeInstruction instruction, PythonUnaryOperator operator) {
        super(instruction);
        this.operator = operator;
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        PythonLikeType operand =
                Optional.ofNullable(stackMetadata.getTOSType()).orElse(BuiltinTypes.BASE_TYPE);

        Optional<PythonKnownFunctionType> maybeKnownFunctionType = operand.getMethodType(operator.getDunderMethod());
        if (maybeKnownFunctionType.isPresent()) {
            PythonKnownFunctionType knownFunctionType = maybeKnownFunctionType.get();
            Optional<PythonFunctionSignature> maybeFunctionSignature = knownFunctionType.getFunctionForParameters();
            if (maybeFunctionSignature.isPresent()) {
                PythonFunctionSignature functionSignature = maybeFunctionSignature.get();
                return stackMetadata.pop().push(ValueSourceInfo.of(this, functionSignature.getReturnType(),
                        stackMetadata.getValueSourcesUpToStackIndex(1)));
            }
        }

        return stackMetadata.pop()
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourcesUpToStackIndex(1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.unaryOperator(functionMetadata.methodVisitor, stackMetadata, operator);
    }
}
