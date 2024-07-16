package ai.timefold.jpyinterpreter.opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.opcodes.descriptor.OpcodeDescriptor;

public abstract class AbstractOpcode implements Opcode {
    protected PythonBytecodeInstruction instruction;
    private static final Map<String, OpcodeDescriptor> opcodeNameToInstructionMap = getOpcodeNameToInstructionMap();

    private static Map<String, OpcodeDescriptor> getOpcodeNameToInstructionMap() {
        Map<String, OpcodeDescriptor> out = new HashMap<>();
        for (Class<?> subclass : OpcodeDescriptor.class.getPermittedSubclasses()) {
            if (!subclass.isEnum()) {
                throw new IllegalStateException("%s is a subclass of %s and is not an enum."
                        .formatted(subclass, OpcodeDescriptor.class.getSimpleName()));
            }
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumSubclass = (Class<? extends Enum<?>>) subclass;
            for (Enum<?> constant : enumSubclass.getEnumConstants()) {
                String name = constant.name();
                if (out.containsKey(name)) {
                    throw new IllegalStateException("Duplicate identifier %s present in both %s and %s."
                            .formatted(name, out.get(name).getClass(), enumSubclass));
                }
                out.put(name, (OpcodeDescriptor) constant);
            }
        }
        return out;
    }

    public AbstractOpcode(PythonBytecodeInstruction instruction) {
        this.instruction = instruction;
    }

    public PythonBytecodeInstruction getInstruction() {
        return instruction;
    }

    @Override
    public int getBytecodeIndex() {
        return instruction.offset();
    }

    @Override
    public boolean isJumpTarget() {
        return instruction.isJumpTarget();
    }

    @Override
    public List<StackMetadata> getStackMetadataAfterInstructionForBranches(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return List.of(getStackMetadataAfterInstruction(functionMetadata, stackMetadata));
    }

    protected abstract StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata);

    public static OpcodeDescriptor lookupInstruction(String name) {
        OpcodeDescriptor out = opcodeNameToInstructionMap.get(name);
        if (out == null) {
            throw new IllegalArgumentException("Invalid opcode identifier %s.".formatted(name));
        }
        return out;
    }

    @Override
    public String toString() {
        return instruction.toString();
    }
}
