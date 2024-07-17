package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.BinaryDunderOpcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.CompareOpcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.GetSliceOpcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.NotOpcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.StoreSliceOpcode;
import ai.timefold.jpyinterpreter.opcodes.dunder.UniDunerOpcode;

public enum DunderOpDescriptor implements OpcodeDescriptor {
    COMPARE_OP(CompareOpcode::new),

    /**
     * Implements TOS = not TOS.
     */
    UNARY_NOT(NotOpcode::new),

    /**
     * Implements any binary op. Its argument represent the arg to implement, which
     * may or may not be inplace.
     */
    BINARY_OP(PythonBinaryOperator::getBinaryOpcode),
    /**
     * Implements TOS = +TOS.
     */
    UNARY_POSITIVE(PythonUnaryOperator.POSITIVE),

    /**
     * Implements TOS = -TOS.
     */
    UNARY_NEGATIVE(PythonUnaryOperator.NEGATIVE),

    /**
     * Implements TOS = ~TOS.
     */
    UNARY_INVERT(PythonUnaryOperator.INVERT),

    /**
     * Implements TOS = TOS1 ** TOS.
     */
    BINARY_POWER(PythonBinaryOperator.POWER),

    /**
     * Implements TOS = TOS1 * TOS.
     */
    BINARY_MULTIPLY(PythonBinaryOperator.MULTIPLY),

    /**
     * Implements TOS = TOS1 @ TOS.
     */
    BINARY_MATRIX_MULTIPLY(PythonBinaryOperator.MATRIX_MULTIPLY),

    /**
     * Implements TOS = TOS1 // TOS.
     */
    BINARY_FLOOR_DIVIDE(PythonBinaryOperator.FLOOR_DIVIDE),

    /**
     * Implements TOS = TOS1 / TOS.
     */
    BINARY_TRUE_DIVIDE(PythonBinaryOperator.TRUE_DIVIDE),

    /**
     * Implements TOS = TOS1 % TOS.
     */
    BINARY_MODULO(PythonBinaryOperator.MODULO),

    /**
     * Implements TOS = TOS1 + TOS.
     */
    BINARY_ADD(PythonBinaryOperator.ADD),

    /**
     * Implements TOS = TOS1 - TOS.
     */
    BINARY_SUBTRACT(PythonBinaryOperator.SUBTRACT),

    /**
     * Implements TOS = TOS1[TOS].
     */
    BINARY_SUBSCR(PythonBinaryOperator.GET_ITEM),

    /**
     * Implements TOS = TOS2[TOS1:TOS]
     */
    BINARY_SLICE(GetSliceOpcode::new),

    /**
     * Implements TOS2[TOS1:TOS] = TOS3
     */
    STORE_SLICE(StoreSliceOpcode::new),

    /**
     * Implements TOS = TOS1 &lt;&lt; TOS.
     */
    BINARY_LSHIFT(PythonBinaryOperator.LSHIFT),

    /**
     * Implements TOS = TOS1 &gt;&gt; TOS.
     */
    BINARY_RSHIFT(PythonBinaryOperator.RSHIFT),

    /**
     * Implements TOS = TOS1 &amp; TOS.
     */
    BINARY_AND(PythonBinaryOperator.AND),

    /**
     * Implements TOS = TOS1 ^ TOS.
     */
    BINARY_XOR(PythonBinaryOperator.XOR),

    /**
     * Implements TOS = TOS1 | TOS.
     */
    BINARY_OR(PythonBinaryOperator.OR),

    // **************************************************
    // In-place Dunder Operations
    // **************************************************

    /**
     * Implements in-place TOS = TOS1 ** TOS.
     */
    INPLACE_POWER(PythonBinaryOperator.INPLACE_POWER),

    /**
     * Implements in-place TOS = TOS1 * TOS.
     */
    INPLACE_MULTIPLY(PythonBinaryOperator.INPLACE_MULTIPLY),

    /**
     * Implements in-place TOS = TOS1 @ TOS.
     */
    INPLACE_MATRIX_MULTIPLY(PythonBinaryOperator.INPLACE_MATRIX_MULTIPLY),

    /**
     * Implements in-place TOS = TOS1 // TOS.
     */
    INPLACE_FLOOR_DIVIDE(PythonBinaryOperator.INPLACE_FLOOR_DIVIDE),

    /**
     * Implements in-place TOS = TOS1 / TOS.
     */
    INPLACE_TRUE_DIVIDE(PythonBinaryOperator.INPLACE_TRUE_DIVIDE),

    /**
     * Implements in-place TOS = TOS1 % TOS.
     */
    INPLACE_MODULO(PythonBinaryOperator.INPLACE_MODULO),

    /**
     * Implements in-place TOS = TOS1 + TOS.
     */
    INPLACE_ADD(PythonBinaryOperator.INPLACE_ADD),

    /**
     * Implements in-place TOS = TOS1 - TOS.
     */
    INPLACE_SUBTRACT(PythonBinaryOperator.INPLACE_SUBTRACT),

    /**
     * Implements in-place TOS = TOS1 &lt;&lt; TOS.
     */
    INPLACE_LSHIFT(PythonBinaryOperator.INPLACE_LSHIFT),

    /**
     * Implements in-place TOS = TOS1 &gt;&gt; TOS.
     */
    INPLACE_RSHIFT(PythonBinaryOperator.INPLACE_RSHIFT),

    /**
     * Implements in-place TOS = TOS1 &amp; TOS.
     */
    INPLACE_AND(PythonBinaryOperator.INPLACE_AND),

    /**
     * Implements in-place TOS = TOS1 ^ TOS.
     */
    INPLACE_XOR(PythonBinaryOperator.INPLACE_XOR),

    /**
     * Implements in-place TOS = TOS1 | TOS.
     */
    INPLACE_OR(PythonBinaryOperator.INPLACE_OR);

    final VersionMapping versionLookup;

    DunderOpDescriptor(Function<PythonBytecodeInstruction, Opcode> opcodeFunction) {
        this.versionLookup = VersionMapping.constantMapping(opcodeFunction);
    }

    DunderOpDescriptor(PythonUnaryOperator binaryOperator) {
        this((instruction) -> new UniDunerOpcode(instruction, binaryOperator));
    }

    DunderOpDescriptor(PythonBinaryOperator binaryOperator) {
        this((instruction) -> new BinaryDunderOpcode(instruction, binaryOperator));
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
