package ai.timefold.jpyinterpreter.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.PythonExceptionTable;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.TypeHint;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.CollectionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ExceptionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.FunctionOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.MetaOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ModuleOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.ObjectOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.OpcodeDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.StackOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.VariableOpDescriptor;

/**
 * A builder for Python bytecode.
 */
public class PythonFunctionBuilder {

    /**
     * The list of bytecode instructions
     */
    List<PythonBytecodeInstruction> instructionList = new ArrayList<>();

    /**
     * List of global names used in the bytecode
     */
    List<String> co_names = new ArrayList<>();

    /**
     * List of names of local variables in the bytecode
     */
    List<String> co_varnames = new ArrayList<>();

    /**
     * List of names of shared variables in the bytecode
     */
    List<String> co_cellvars = new ArrayList<>();

    /**
     * List of free variables in the bytecode
     */
    List<String> co_freevars = new ArrayList<>();

    /**
     * Constants used in the bytecode
     */
    List<PythonLikeObject> co_consts = new ArrayList<>();

    Map<String, PythonLikeObject> globalsMap = new HashMap<>();

    Map<String, TypeHint> typeAnnotations = new HashMap<>();

    int co_argcount = 0;
    int co_kwonlyargcount = 0;

    /**
     * Creates a new function builder for a Python function with the given parameters
     *
     * @param parameters The names of the function's parameters
     * @return
     */
    public static PythonFunctionBuilder newFunction(String... parameters) {
        PythonFunctionBuilder out = new PythonFunctionBuilder();
        out.co_varnames.addAll(Arrays.asList(parameters));
        out.co_names.addAll(out.co_varnames);
        out.co_argcount = parameters.length;
        out.co_kwonlyargcount = 0;
        return out;
    }

    /**
     * Creates the bytecode data for the function
     *
     * @return The bytecode data, which can be used by
     *         {@link PythonBytecodeToJavaBytecodeTranslator#translatePythonBytecode(PythonCompiledFunction, Class)} or
     *         {@link PythonBytecodeToJavaBytecodeTranslator#translatePythonBytecodeToClass(PythonCompiledFunction, Class)}.
     */
    public PythonCompiledFunction build() {
        PythonCompiledFunction out = new PythonCompiledFunction();
        out.module = "test";
        out.qualifiedName = "TestFunction";
        out.instructionList = instructionList;
        out.typeAnnotations = typeAnnotations;
        out.globalsMap = globalsMap;
        out.co_exceptiontable = new PythonExceptionTable(); // we use an empty exception table since it for Python 3.10
        // (i.e. use block try...except instead of co_exceptiontable)
        out.co_constants = co_consts;
        out.co_varnames = co_varnames;
        out.co_names = co_names;
        out.co_argcount = co_argcount;
        out.co_kwonlyargcount = co_kwonlyargcount;
        out.co_cellvars = co_cellvars;
        out.co_freevars = co_freevars;
        out.pythonVersion = PythonVersion.PYTHON_3_10;
        return out;
    }

    PythonBytecodeInstruction instruction(OpcodeDescriptor opcode) {
        return PythonBytecodeInstruction.atOffset(opcode, instructionList.size());
    }

    void update(PythonBytecodeInstruction instruction) {
        instructionList.set(instruction.offset(), instruction);
    }

    /**
     * Perform the specified opcode with no argument
     */
    public PythonFunctionBuilder op(OpcodeDescriptor opcode) {
        PythonBytecodeInstruction instruction = instruction(opcode);
        instructionList.add(instruction);
        return this;
    }

    /**
     * Perform the specified opcode with an argument
     */
    public PythonFunctionBuilder op(OpcodeDescriptor opcode, int arg) {
        PythonBytecodeInstruction instruction = instruction(opcode)
                .withArg(arg);
        instructionList.add(instruction);
        return this;
    }

    /**
     * TOS is an iterator. While the iterator is not empty, push the
     * iterator's next element to the top of the stack and run the specified
     * code. Repeat until the iterator is empty, and then pop the iterator off the stack.
     *
     * @param blockBuilder the bytecode to run in the loop
     */
    public PythonFunctionBuilder loop(Consumer<PythonFunctionBuilder> blockBuilder) {
        return loop(blockBuilder, false);
    }

    /**
     * TOS is an iterator. While the iterator is not empty, push the
     * iterator's next element to the top of the stack and run the specified
     * code. Repeat until the iterator is empty, and then pop the iterator off the stack.
     *
     * @param blockBuilder the bytecode to run in the loop
     */
    public PythonFunctionBuilder loop(Consumer<PythonFunctionBuilder> blockBuilder, boolean alwaysExitEarly) {
        PythonBytecodeInstruction instruction = instruction(ControlOpDescriptor.FOR_ITER)
                .markAsJumpTarget();
        instructionList.add(instruction);

        blockBuilder.accept(this);

        if (!alwaysExitEarly) {
            PythonBytecodeInstruction jumpBackInstruction = instruction(ControlOpDescriptor.JUMP_ABSOLUTE)
                    .withArg(instruction.offset());
            instructionList.add(jumpBackInstruction);
        }

        update(instruction.withArg(instructionList.size() - instruction.offset() - 1));

        PythonBytecodeInstruction afterLoopInstruction = instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        instructionList.add(afterLoopInstruction);

        return this;
    }

    /**
     * Declare a try block, and return an except builder for that try block.
     *
     * @param tryBlockBuilder The code to execute inside the try block
     * @return An {@link ExceptBuilder} for the try block
     */
    public ExceptBuilder tryCode(Consumer<PythonFunctionBuilder> tryBlockBuilder, boolean tryExitEarly) {
        PythonBytecodeInstruction notCatchedFinallyBlock = instruction(ExceptionOpDescriptor.SETUP_FINALLY);
        instructionList.add(notCatchedFinallyBlock);

        PythonBytecodeInstruction setupFinallyInstruction = instruction(ExceptionOpDescriptor.SETUP_FINALLY);
        instructionList.add(setupFinallyInstruction);
        int tryStart = instructionList.size();

        tryBlockBuilder.accept(this);

        update(setupFinallyInstruction.withArg(instructionList.size() - tryStart + (tryExitEarly ? 0 : 1)));

        PythonBytecodeInstruction tryJumpToEnd = null;

        if (!tryExitEarly) {
            tryJumpToEnd = instruction(ControlOpDescriptor.JUMP_ABSOLUTE)
                    .withArg(0);
            instructionList.add(tryJumpToEnd);
        }

        return new ExceptBuilder(this, tryJumpToEnd, notCatchedFinallyBlock);
    }

    /**
     * Execute the code generated by the parameter if TOS is True; skip it otherwise.
     * TOS is popped.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifTrue(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = instruction(ControlOpDescriptor.POP_JUMP_IF_FALSE);
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        instructionList.add(afterIfInstruction);

        update(instruction.withArg(afterIfInstruction.offset()));

        return this;
    }

    /**
     * Execute the code generated by the parameter if TOS is False; skip it otherwise.
     * TOS is popped.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifFalse(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = instruction(ControlOpDescriptor.POP_JUMP_IF_TRUE);
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        instructionList.add(afterIfInstruction);

        update(instruction.withArg(afterIfInstruction.offset()));

        return this;
    }

    /**
     * Execute the code generated by the parameter if TOS is True; skip it otherwise.
     * If TOS is True, TOS is popped; otherwise it remains on the stack.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifTruePopTop(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = instruction(ControlOpDescriptor.JUMP_IF_FALSE_OR_POP);
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        instructionList.add(afterIfInstruction);

        update(instruction.withArg(afterIfInstruction.offset()));

        return this;
    }

    /**
     * Execute the code generated by the parameter if TOS is False; skip it otherwise.
     * If TOS is False, TOS is popped; otherwise it remains on the stack.
     *
     * @param blockBuilder The code inside the if statement
     */
    public PythonFunctionBuilder ifFalsePopTop(Consumer<PythonFunctionBuilder> blockBuilder) {
        PythonBytecodeInstruction instruction = instruction(ControlOpDescriptor.JUMP_IF_TRUE_OR_POP);
        instructionList.add(instruction);

        blockBuilder.accept(this);

        PythonBytecodeInstruction afterIfInstruction = instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        instructionList.add(afterIfInstruction);

        update(instruction.withArg(afterIfInstruction.offset()));

        return this;
    }

    /**
     * Use TOS as a context_manager, pushing the result of its __enter__ method to TOS, and calling its
     * __exit__ method on exit of the with block (both normal and exceptional exits)
     *
     * @param blockBuilder The code inside the with block
     */
    public PythonFunctionBuilder with(Consumer<PythonFunctionBuilder> blockBuilder) {
        var instruction = instruction(ExceptionOpDescriptor.SETUP_WITH);
        instructionList.add(instruction);

        blockBuilder.accept(this);

        // Call the exit function
        loadConstant(null);
        loadConstant(null);
        loadConstant(null);
        callFunction(3);
        op(StackOpDescriptor.POP_TOP);

        var skipExceptionHandler = instruction(ControlOpDescriptor.JUMP_ABSOLUTE);
        instructionList.add(skipExceptionHandler);

        var exceptionHandler = instruction(ExceptionOpDescriptor.WITH_EXCEPT_START)
                .markAsJumpTarget();
        instructionList.add(exceptionHandler);

        update(instruction.withArg(exceptionHandler.offset() - instruction.offset() - 1));

        ifFalse(reraiseExceptionBlock -> reraiseExceptionBlock
                .op(ExceptionOpDescriptor.RERAISE));

        op(StackOpDescriptor.POP_TOP);
        op(StackOpDescriptor.POP_TOP);
        op(StackOpDescriptor.POP_TOP);
        op(ExceptionOpDescriptor.POP_EXCEPT);
        op(StackOpDescriptor.POP_TOP);

        update(skipExceptionHandler.withArg(instructionList.size()));

        PythonBytecodeInstruction afterWithInstruction = instruction(MetaOpDescriptor.NOP)
                .markAsJumpTarget();
        instructionList.add(afterWithInstruction);

        return this;
    }

    /**
     * Create a list from the {@code count} top items on the stack. TOS is the last element in the list
     *
     * @param count The number of elements to pop and put into the list.
     */
    public PythonFunctionBuilder list(int count) {
        return op(CollectionOpDescriptor.BUILD_LIST, count);
    }

    /**
     * Create a tuple from the {@code count} top items on the stack. TOS is the last element in the tuple
     *
     * @param count The number of elements to pop and put into the tuple.
     */
    public PythonFunctionBuilder tuple(int count) {
        return op(CollectionOpDescriptor.BUILD_TUPLE, count);
    }

    /**
     * Create a dict from the {@code 2 * count} top items on the stack, which are read as key-value pairs.
     *
     * @param count The number of key-value pairs to pop and put into the dict.
     */
    public PythonFunctionBuilder dict(int count) {
        return op(CollectionOpDescriptor.BUILD_MAP, count);
    }

    /**
     * TOS is a tuple containing keys, and below TOS are {@code count} items representing the keys values.
     * The last item in the tuple maps to TOS1, the second last item to TOS2, etc.
     *
     * @param count The number of values in the dict
     */
    public PythonFunctionBuilder constDict(int count) {
        return op(CollectionOpDescriptor.BUILD_CONST_KEY_MAP, count);
    }

    /**
     * Creates a set from the top {@code count} items in the stack.
     *
     * @param count The number of elements to pop and put into the set.
     */
    public PythonFunctionBuilder set(int count) {
        return op(CollectionOpDescriptor.BUILD_SET, count);
    }

    /**
     * Call a function with {@code argc} parameters. TOS[argc+1] is the function; above it are its arguments.
     *
     * @param argc The number of arguments the function takes
     */
    public PythonFunctionBuilder callFunction(int argc) {
        return op(FunctionOpDescriptor.CALL_FUNCTION, argc);
    }

    /**
     * Call a function with {@code argc} parameters, some of which are keywords.
     * TOS[argc+1] is the function; above it are its arguments; keyword-only parameters are store
     * in a dict at TOS, and positional parameters are stored in the stack.
     *
     * @param argc The number of arguments the function takes
     */
    public PythonFunctionBuilder callFunctionWithKeywords(int argc) {
        return op(FunctionOpDescriptor.CALL_FUNCTION_KW, argc);
    }

    /**
     * Call the function at TOS1 with the parameters specified in the tuple at TOS is {@code hasKeywords} is false,
     * otherwise call the function at TOS2 with the parameters specified in the tuple at TOS1 and the keyword dict at TOS.
     *
     * @param hasKeywords true if keyword-only parameters are being passed
     */
    public PythonFunctionBuilder callFunctionUnpack(boolean hasKeywords) {
        return op(FunctionOpDescriptor.CALL_FUNCTION_EX, hasKeywords ? 1 : 0);
    }

    /**
     * Load the specified method on TOS. If type(TOS) has the method, self, method is pushed; otherwise
     * null, TOS.__getattribute__(method) is pushed.
     *
     * @param methodName the method to load
     */
    public PythonFunctionBuilder loadMethod(String methodName) {
        int methodIndex = co_names.indexOf(methodName);
        if (methodIndex == -1) {
            methodIndex = co_names.size();
            co_names.add(methodName);
        }

        return op(FunctionOpDescriptor.LOAD_METHOD, methodIndex);
    }

    /**
     * Call a method with {@code argc} arguments. Keyword-only arguments are not allowed.
     *
     * @param argc The number of arguments the method accepts
     */
    public PythonFunctionBuilder callMethod(int argc) {
        return op(FunctionOpDescriptor.CALL_METHOD, argc);
    }

    /**
     * Get an attribute on TOS.
     *
     * @param attributeName The attribute to get
     */
    public PythonFunctionBuilder getAttribute(String attributeName) {
        int attributeIndex = co_names.indexOf(attributeName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(attributeName);
        }

        return op(ObjectOpDescriptor.LOAD_ATTR, attributeIndex);
    }

    /**
     * TOS is an object, and TOS1 is a value. Store TOS1 into the {@code attributeName} attribute of TOS.
     * TOS and TOS1 are popped.
     *
     * @param attributeName The attribute to store.
     * @return
     */
    public PythonFunctionBuilder storeAttribute(String attributeName) {
        int attributeIndex = co_names.indexOf(attributeName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(attributeName);
        }

        return op(ObjectOpDescriptor.STORE_ATTR, attributeIndex);
    }

    /**
     * Loads a constant (converting it to the Interpreter equivalent if required).
     *
     * @param constant The Java constant to load
     */
    public PythonFunctionBuilder loadConstant(Object constant) {
        PythonLikeObject wrappedConstant = JavaPythonTypeConversionImplementor.wrapJavaObject(constant);

        int index = co_consts.indexOf(wrappedConstant);
        if (index == -1) {
            index = co_consts.size();
            co_consts.add(JavaPythonTypeConversionImplementor.wrapJavaObject(constant));
        }

        return op(VariableOpDescriptor.LOAD_CONST, index);
    }

    /**
     * Load the specified parameter
     *
     * @param parameterName The parameter to load
     * @throws IllegalArgumentException if the parameter is not in the function's parameter list
     */
    public PythonFunctionBuilder loadParameter(String parameterName) {
        var arg = co_varnames.indexOf(parameterName);

        if (arg == -1) {
            throw new IllegalArgumentException("Parameter (" + parameterName + ") is not in the parameter list (" +
                    co_varnames + ").");
        }

        return op(VariableOpDescriptor.LOAD_FAST, arg);
    }

    /**
     * Loads a variable with the given name (creating an entry in co_varnames if needed).
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadVariable(String variableName) {
        var arg = co_varnames.indexOf(variableName);

        if (arg == -1) {
            co_varnames.add(variableName);
            arg = co_varnames.size() - 1;
        }

        return op(VariableOpDescriptor.LOAD_FAST, arg);
    }

    /**
     * Store TOS into a variable with the given name (creating an entry in co_varnames if needed).
     *
     * @param variableName The variable to store TOS in
     */
    public PythonFunctionBuilder storeVariable(String variableName) {
        var arg = co_varnames.indexOf(variableName);

        if (arg == -1) {
            co_varnames.add(variableName);
            arg = co_varnames.size() - 1;
        }

        return op(VariableOpDescriptor.STORE_FAST, arg);
    }

    /**
     * Loads a variable that is shared with an inner function with the given name (creating an entry in co_cellvars if needed).
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadCellVariable(String variableName) {
        var arg = co_cellvars.indexOf(variableName);

        if (arg == -1) {
            co_cellvars.add(variableName);
            arg = co_cellvars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        return op(VariableOpDescriptor.LOAD_DEREF, arg);
    }

    /**
     * Stores TOS into a variable that is shared with an inner function with the given name (creating an entry in co_cellvars if
     * needed).
     *
     * @param variableName The variable to store TOS in
     */
    public PythonFunctionBuilder storeCellVariable(String variableName) {
        var arg = co_cellvars.indexOf(variableName);

        if (arg == -1) {
            co_cellvars.add(variableName);
            arg = co_cellvars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        return op(VariableOpDescriptor.STORE_DEREF, arg);
    }

    /**
     * Loads a free variable (creating an entry in co_freevars if needed).
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadFreeVariable(String variableName) {
        var arg = co_freevars.indexOf(variableName);

        if (arg == -1) {
            co_freevars.add(variableName);
            arg = co_freevars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        return op(VariableOpDescriptor.LOAD_DEREF, arg);
    }

    /**
     * Stores TOS into a free variable (creating an entry in co_freevars if needed).
     *
     * @param variableName The variable to store TOS in
     */
    public PythonFunctionBuilder storeFreeVariable(String variableName) {
        var arg = co_freevars.indexOf(variableName);

        if (arg == -1) {
            co_freevars.add(variableName);
            arg = co_freevars.size() - 1;

            if (!co_varnames.contains(variableName)) {
                co_varnames.add(variableName);
            }
        }

        return op(VariableOpDescriptor.STORE_DEREF, arg);
    }

    public PythonFunctionBuilder usingGlobalsMap(Map<String, PythonLikeObject> globalsMap) {
        this.globalsMap = globalsMap;
        return this;
    }

    /**
     * Loads a global variable
     *
     * @param variableName The variable to load
     */
    public PythonFunctionBuilder loadGlobalVariable(String variableName) {
        var arg = co_names.indexOf(variableName);

        if (arg == -1) {
            co_names.add(variableName);
            arg = co_names.size() - 1;
        }

        return op(VariableOpDescriptor.LOAD_GLOBAL, arg);
    }

    /**
     * Store TOS into a global variable.
     *
     * @param variableName The global variable to store TOS in
     */
    public PythonFunctionBuilder storeGlobalVariable(String variableName) {
        var arg = co_names.indexOf(variableName);

        if (arg == -1) {
            co_names.add(variableName);
            arg = co_names.size() - 1;
        }

        return op(VariableOpDescriptor.STORE_GLOBAL, arg);
    }

    /**
     * Loads a module using TOS as level and TOS1 as from_list
     *
     * @param moduleName The module to get
     */
    public PythonFunctionBuilder loadModule(String moduleName) {
        int attributeIndex = co_names.indexOf(moduleName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(moduleName);
        }

        return op(ModuleOpDescriptor.IMPORT_NAME, attributeIndex);
    }

    /**
     * Loads an attribute from TOS (which is a module)
     *
     * @param attributeName The attribute to get
     */
    public PythonFunctionBuilder getFromModule(String attributeName) {
        int attributeIndex = co_names.indexOf(attributeName);
        if (attributeIndex == -1) {
            attributeIndex = co_names.size();
            co_names.add(attributeName);
        }

        return op(ModuleOpDescriptor.IMPORT_FROM, attributeIndex);
    }

    /**
     * Perform a comparison on TOS and TOS1, popping TOS, TOS1 and pushing the result.
     *
     * @param compareOp The comparison to perform
     */
    public PythonFunctionBuilder compare(CompareOp compareOp) {
        PythonBytecodeInstruction instruction = instruction(DunderOpDescriptor.COMPARE_OP)
                .withArg(0)
                .withArgRepr(compareOp.id);
        instructionList.add(instruction);
        return this;
    }
}
