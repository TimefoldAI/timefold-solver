package ai.timefold.jpyinterpreter.opcodes.meta;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.collection.ListToTupleOpcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.UniDunerOpcode;
import ai.timefold.jpyinterpreter.opcodes.generator.StopIteratorErrorOpcode;

public enum UnaryIntrinsicFunction {
    INTRINSIC_1_INVALID(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_1_INVALID");
    }),
    INTRINSIC_PRINT(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_PRINT");
    }),
    INTRINSIC_IMPORT_STAR(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_IMPORT_STAR");
    }),
    INTRINSIC_STOPITERATION_ERROR(StopIteratorErrorOpcode::new),
    INTRINSIC_ASYNC_GEN_WRAP(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_ASYNC_GEN_WRAP");
    }),
    INTRINSIC_UNARY_POSITIVE(instruction -> new UniDunerOpcode(instruction, PythonUnaryOperator.POSITIVE)),
    INTRINSIC_LIST_TO_TUPLE(ListToTupleOpcode::new),
    INTRINSIC_TYPEVAR(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_TYPEVAR");
    }),
    INTRINSIC_PARAMSPEC(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_PARAMSPEC");
    }),
    INTRINSIC_TYPEVARTUPLE(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_TYPEVARTUPLE");
    }),
    INTRINSIC_SUBSCRIPT_GENERIC(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_SUBSCRIPT_GENERIC");
    }),
    INTRINSIC_TYPEALIAS(ignored -> {
        throw new UnsupportedOperationException("INTRINSIC_TYPEALIAS");
    });

    final Function<PythonBytecodeInstruction, Opcode> opcodeFunction;

    UnaryIntrinsicFunction(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.opcodeFunction = opcodeFunction;
    }

    public Opcode getOpcode(PythonBytecodeInstruction instruction) {
        return opcodeFunction.apply(instruction);
    }

    public static Opcode lookup(PythonBytecodeInstruction instruction) {
        return UnaryIntrinsicFunction.valueOf(instruction.argRepr()).getOpcode(instruction);
    }
}
