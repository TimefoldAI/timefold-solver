package ai.timefold.jpyinterpreter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ExceptionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.MetaOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Builds except and finally blocks
 */
public class ExceptBuilder {

    /**
     * The {@link PythonFunctionBuilder} that created this {@link ExceptBuilder}.
     */
    final PythonFunctionBuilder delegate;

    /**
     * The {@link ControlOpDescriptor#JUMP_ABSOLUTE} instruction at the end
     * of the try block, which is where the try should go if it completed without error
     * (finally block if it was specified, else catch block).
     */
    PythonBytecodeInstruction tryEndGoto;

    /**
     * The {@link ExceptionOpDescriptor#SETUP_FINALLY} instruction before the try block that
     * handles the case where the exception is not caught
     */
    PythonBytecodeInstruction exceptFinallyInstruction;

    final List<PythonBytecodeInstruction> exceptEndJumpList = new ArrayList<>();

    boolean hasFinally = false;

    boolean allExceptsExitEarly = true;

    public ExceptBuilder(PythonFunctionBuilder delegate, PythonBytecodeInstruction tryEndGoto,
            PythonBytecodeInstruction exceptFinallyInstruction) {
        this.delegate = delegate;
        this.tryEndGoto = tryEndGoto;
        this.exceptFinallyInstruction = exceptFinallyInstruction;
    }

    /**
     * Add an except block for the {@code type} argument.
     *
     * @param type The exception type handled by the except block
     * @param exceptBuilder The code in the except block
     */
    public ExceptBuilder except(PythonLikeType type, Consumer<PythonFunctionBuilder> exceptBuilder,
            boolean exitEarly) {

        PythonBytecodeInstruction exceptBlockStartInstruction = delegate.instruction(StackOpDescriptor.DUP_TOP)
                .markAsJumpTarget();
        delegate.instructionList.add(exceptBlockStartInstruction);
        delegate.loadConstant(type);
        PythonBytecodeInstruction instruction = delegate.instruction(ControlOpDescriptor.JUMP_IF_NOT_EXC_MATCH);
        delegate.instructionList.add(instruction);

        delegate.op(StackOpDescriptor.POP_TOP);
        delegate.op(StackOpDescriptor.POP_TOP);
        delegate.op(StackOpDescriptor.POP_TOP);
        delegate.op(ExceptionOpDescriptor.POP_EXCEPT);
        exceptBuilder.accept(delegate);

        if (!exitEarly) {
            allExceptsExitEarly = false;
            PythonBytecodeInstruction exceptEndJumpInstruction = delegate.instruction(ControlOpDescriptor.JUMP_ABSOLUTE);
            delegate.instructionList.add(exceptEndJumpInstruction);
            exceptEndJumpList.add(exceptEndJumpInstruction);
        }

        delegate.update(instruction.withArg(delegate.instructionList.size()));
        return this;
    }

    /**
     * Creates a finally block.
     *
     * @param finallyBuilder The code in the finally block
     */
    public ExceptBuilder andFinally(Consumer<PythonFunctionBuilder> finallyBuilder, boolean exitEarly) {
        hasFinally = true;

        if (!exitEarly) {
            allExceptsExitEarly = false;
        }

        PythonBytecodeInstruction exceptGotoTarget = delegate.instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        delegate.instructionList.add(exceptGotoTarget);

        delegate.op(ExceptionOpDescriptor.RERAISE, 0);

        if (tryEndGoto != null) {
            delegate.update(tryEndGoto = tryEndGoto.withArg(delegate.instructionList.size()));
        }

        for (int i = 0; i < exceptEndJumpList.size(); i++) {
            var instruction = exceptEndJumpList.get(i).withArg(delegate.instructionList.size());
            exceptEndJumpList.set(i, instruction);
            delegate.update(instruction);
        }

        PythonBytecodeInstruction finallyFromTryStartInstruction = delegate.instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        delegate.instructionList.add(finallyFromTryStartInstruction);

        finallyBuilder.accept(delegate); // finally from try

        PythonBytecodeInstruction finallyEndInstruction = delegate.instruction(ControlOpDescriptor.JUMP_ABSOLUTE);
        delegate.instructionList.add(finallyEndInstruction);

        delegate.update(exceptFinallyInstruction =
                exceptFinallyInstruction.withArg(delegate.instructionList.size() - exceptFinallyInstruction.offset() - 1));

        PythonBytecodeInstruction finallyFromUncaughtStartInstruction = delegate.instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        delegate.instructionList.add(finallyFromUncaughtStartInstruction);

        finallyBuilder.accept(delegate);

        delegate.op(ExceptionOpDescriptor.RERAISE);

        delegate.update(finallyEndInstruction.withArg(delegate.instructionList.size()));

        PythonBytecodeInstruction tryCatchBlockEnd = delegate.instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        delegate.instructionList.add(tryCatchBlockEnd);
        return this;
    }

    /**
     * Ends the except/finally blocks, returning the {@link PythonFunctionBuilder} that created this
     * {@link ExceptBuilder}.
     *
     * @return the {@link PythonFunctionBuilder} that created this {@link ExceptBuilder}.
     */
    public PythonFunctionBuilder tryEnd() {
        if (!hasFinally) {
            PythonBytecodeInstruction exceptGotoTarget = delegate.instruction(MetaOpDescriptor.NOP)
                    .markAsJumpTarget();
            delegate.instructionList.add(exceptGotoTarget);

            delegate.op(ExceptionOpDescriptor.RERAISE, 0);
        }

        if (tryEndGoto == null || tryEndGoto.arg() == 0) {
            if (!hasFinally) {
                delegate.update(exceptFinallyInstruction = exceptFinallyInstruction
                        .withArg(delegate.instructionList.size() - exceptFinallyInstruction.offset() - 1));
                PythonBytecodeInstruction reraiseInstruction =
                        delegate.instruction(ExceptionOpDescriptor.RERAISE)
                                .withArg(0)
                                .markAsJumpTarget();
                delegate.instructionList.add(reraiseInstruction);
            }

            if (!allExceptsExitEarly) {
                if (tryEndGoto != null) {
                    delegate.update(tryEndGoto = tryEndGoto.withArg(delegate.instructionList.size()));
                }
                PythonBytecodeInstruction noopInstruction = delegate.instruction(MetaOpDescriptor.NOP)
                        .markAsJumpTarget();
                delegate.instructionList.add(noopInstruction);

                if (!hasFinally) {
                    for (int i = 0; i < exceptEndJumpList.size(); i++) {
                        var instruction = exceptEndJumpList.get(i).withArg(delegate.instructionList.size());
                        exceptEndJumpList.set(i, instruction);
                        delegate.update(instruction);
                    }
                }
            }
        }
        return delegate;
    }
}