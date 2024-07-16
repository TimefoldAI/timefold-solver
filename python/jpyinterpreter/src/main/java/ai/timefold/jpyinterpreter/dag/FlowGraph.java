package ai.timefold.jpyinterpreter.dag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.ExceptionBlock;
import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;

public class FlowGraph {
    BasicBlock initialBlock;
    List<BasicBlock> basicBlockList;
    Map<BasicBlock, List<BasicBlock>> basicBlockToSourcesMap;

    Map<BasicBlock, List<JumpSource>> basicBlockToJumpSourcesMap;

    Map<IndexBranchPair, JumpSource> opcodeIndexToJumpSourceMap;

    List<StackMetadata> stackMetadataForOperations;

    private FlowGraph(BasicBlock initialBlock, List<BasicBlock> basicBlockList,
            Map<BasicBlock, List<BasicBlock>> basicBlockToSourcesMap,
            Map<BasicBlock, List<JumpSource>> basicBlockToJumpSourcesMap,
            Map<IndexBranchPair, JumpSource> opcodeIndexToJumpSourceMap) {
        this.initialBlock = initialBlock;
        this.basicBlockList = basicBlockList;
        this.basicBlockToSourcesMap = basicBlockToSourcesMap;
        this.basicBlockToJumpSourcesMap = basicBlockToJumpSourcesMap;
        this.opcodeIndexToJumpSourceMap = opcodeIndexToJumpSourceMap;
    }

    public List<StackMetadata> getStackMetadataForOperations() {
        return stackMetadataForOperations;
    }

    @SuppressWarnings("unchecked")
    public <T extends Opcode> void visitOperations(Class<T> opcodeClass, BiConsumer<? super T, StackMetadata> visitor) {
        for (BasicBlock basicBlock : basicBlockList) {
            for (Opcode opcode : basicBlock.getBlockOpcodeList()) {
                if (opcodeClass.isAssignableFrom(opcode.getClass())) {
                    StackMetadata stackMetadata = stackMetadataForOperations.get(opcode.getBytecodeIndex());

                    if (!stackMetadata.isDeadCode()) {
                        visitor.accept((T) opcode, stackMetadata);
                    }
                }
            }
        }
    }

    public static FlowGraph createFlowGraph(FunctionMetadata functionMetadata,
            StackMetadata initialStackMetadata,
            List<Opcode> opcodeList) {
        List<Integer> leaderIndexList = new ArrayList<>();
        boolean wasPreviousInstructionGoto = true; // True so first instruction get added as a leader
        for (int i = 0; i < opcodeList.size(); i++) {
            if (wasPreviousInstructionGoto || opcodeList.get(i).isJumpTarget() ||
                    functionMetadata.pythonCompiledFunction.co_exceptiontable.containsJumpTarget(i)) {
                leaderIndexList.add(i);
            }
            wasPreviousInstructionGoto = opcodeList.get(i).isForcedJump();
        }

        List<BasicBlock> basicBlockList = new ArrayList<>(leaderIndexList.size());
        Map<Integer, BasicBlock> jumpTargetToBasicBlock = new HashMap<>();

        for (int i = 0; i < leaderIndexList.size() - 1; i++) {
            basicBlockList.add(new BasicBlock(leaderIndexList.get(i), opcodeList.subList(leaderIndexList.get(i),
                    leaderIndexList.get(i + 1))));
            jumpTargetToBasicBlock.put(leaderIndexList.get(i), basicBlockList.get(i));
        }

        basicBlockList.add(new BasicBlock(leaderIndexList.get(leaderIndexList.size() - 1),
                opcodeList.subList(leaderIndexList.get(leaderIndexList.size() - 1),
                        opcodeList.size())));
        jumpTargetToBasicBlock.put(leaderIndexList.get(leaderIndexList.size() - 1),
                basicBlockList.get(leaderIndexList.size() - 1));

        BasicBlock initialBlock = basicBlockList.get(0);
        Map<BasicBlock, List<BasicBlock>> basicBlockToSourcesMap = new HashMap<>();
        Map<BasicBlock, List<JumpSource>> basicBlockToJumpSourcesMap = new HashMap<>();
        Map<IndexBranchPair, JumpSource> opcodeIndexToJumpSourceMap = new HashMap<>();

        for (BasicBlock basicBlock : basicBlockList) {
            basicBlockToSourcesMap.put(basicBlock, new ArrayList<>());
        }

        for (BasicBlock basicBlock : basicBlockList) {
            for (Opcode opcode : basicBlock.getBlockOpcodeList()) {
                for (int branch = 0; branch < opcode.getPossibleNextBytecodeIndexList().size(); branch++) {
                    int jumpTarget = opcode.getPossibleNextBytecodeIndexList().get(branch);
                    if (!basicBlock.containsIndex(jumpTarget) || jumpTarget <= opcode.getBytecodeIndex()) {
                        BasicBlock jumpTargetBlock = jumpTargetToBasicBlock.get(jumpTarget);
                        JumpSource jumpSource = new JumpSource(basicBlock);
                        basicBlockToSourcesMap.computeIfAbsent(jumpTargetBlock, key -> new ArrayList<>()).add(basicBlock);
                        basicBlockToJumpSourcesMap.computeIfAbsent(jumpTargetBlock, key -> new ArrayList<>()).add(jumpSource);
                        opcodeIndexToJumpSourceMap.put(new IndexBranchPair(opcode.getBytecodeIndex(), branch), jumpSource);
                    }
                }
            }
        }

        FlowGraph out = new FlowGraph(initialBlock, basicBlockList, basicBlockToSourcesMap, basicBlockToJumpSourcesMap,
                opcodeIndexToJumpSourceMap);
        out.computeStackMetadataForOperations(functionMetadata, initialStackMetadata);
        return out;
    }

    private static StackMetadata getExceptionStackMetadata(ExceptionBlock exceptionBlock,
            FunctionMetadata functionMetadata, StackMetadata initialStackMetadata,
            StackMetadata previousStackMetadata) {
        if (previousStackMetadata == StackMetadata.DEAD_CODE) {
            previousStackMetadata = initialStackMetadata;
        }

        while (previousStackMetadata.getStackSize() < exceptionBlock.getStackDepth()) {
            previousStackMetadata = previousStackMetadata.pushTemp(BuiltinTypes.NONE_TYPE);
        }

        while (previousStackMetadata.getStackSize() > exceptionBlock.getStackDepth()) {
            previousStackMetadata = previousStackMetadata.pop();
        }

        if (exceptionBlock.isPushLastIndex()) {
            return previousStackMetadata.pushTemp(BuiltinTypes.INT_TYPE)
                    .pushTemp(PythonBaseException.BASE_EXCEPTION_TYPE);
        } else {
            return previousStackMetadata.pushTemp(PythonBaseException.BASE_EXCEPTION_TYPE);
        }
    }

    private void computeStackMetadataForOperations(FunctionMetadata functionMetadata,
            StackMetadata initialStackMetadata) {
        Map<Integer, StackMetadata> opcodeIndexToStackMetadata = new HashMap<>();
        opcodeIndexToStackMetadata.put(0, initialStackMetadata);

        for (BasicBlock basicBlock : basicBlockList) {
            for (Opcode opcode : basicBlock.getBlockOpcodeList()) {
                StackMetadata currentStackMetadata =
                        opcodeIndexToStackMetadata.computeIfAbsent(opcode.getBytecodeIndex(), k -> StackMetadata.DEAD_CODE);

                List<Integer> branchList = opcode.getPossibleNextBytecodeIndexList();
                List<StackMetadata> nextStackMetadataList;

                try {
                    nextStackMetadataList = currentStackMetadata.isDeadCode()
                            ? branchList.stream().map(i -> StackMetadata.DEAD_CODE).collect(Collectors.toList())
                            : opcode.getStackMetadataAfterInstructionForBranches(functionMetadata, currentStackMetadata);
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to calculate successor stack metadata for opcode (" + opcode
                            + ") with prior stack metadata ("
                            + currentStackMetadata + ").", t);
                }

                for (int i = 0; i < branchList.size(); i++) {
                    IndexBranchPair indexBranchPair = new IndexBranchPair(opcode.getBytecodeIndex(), i);
                    int nextBytecodeIndex = branchList.get(i);
                    StackMetadata nextStackMetadata = nextStackMetadataList.get(i);
                    if (opcodeIndexToJumpSourceMap.containsKey(indexBranchPair)) {
                        opcodeIndexToJumpSourceMap.get(indexBranchPair).setStackMetadata(nextStackMetadata);
                    }
                    try {
                        opcodeIndexToStackMetadata.merge(nextBytecodeIndex, nextStackMetadata, StackMetadata::unifyWith);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException(
                                "Cannot unify block starting at " + nextBytecodeIndex + ": different stack sizes;\n"
                                        + PythonBytecodeToJavaBytecodeTranslator
                                                .getPythonBytecodeListing(functionMetadata.pythonCompiledFunction),
                                e);
                    }

                }
            }
        }
        for (ExceptionBlock exceptionBlock : functionMetadata.pythonCompiledFunction.co_exceptiontable
                .getEntries()) {
            try {
                opcodeIndexToStackMetadata.merge(exceptionBlock.getTargetInstruction(),
                        getExceptionStackMetadata(exceptionBlock,
                                functionMetadata,
                                initialStackMetadata,
                                opcodeIndexToStackMetadata.getOrDefault(exceptionBlock.getBlockStartInstructionInclusive(),
                                        StackMetadata.DEAD_CODE)),
                        StackMetadata::unifyWith);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Cannot unify block starting at " + exceptionBlock.getTargetInstruction() + ": different stack sizes;\n"
                                + PythonBytecodeToJavaBytecodeTranslator
                                        .getPythonBytecodeListing(functionMetadata.pythonCompiledFunction),
                        e);
            }
        }

        boolean hasChanged;
        do { // Keep unifying until no changes are detected
            hasChanged = false;
            for (BasicBlock basicBlock : basicBlockList) {
                StackMetadata originalMetadata = opcodeIndexToStackMetadata.get(basicBlock.startAtIndex);
                StackMetadata newMetadata = originalMetadata;
                for (JumpSource jumpSource : basicBlockToJumpSourcesMap.getOrDefault(basicBlock, Collections.emptyList())) {
                    newMetadata = newMetadata.unifyWith(jumpSource.getStackMetadata());
                }
                hasChanged |= !newMetadata.equals(originalMetadata);
                opcodeIndexToStackMetadata.put(basicBlock.startAtIndex, newMetadata);
                for (Opcode opcode : basicBlock.getBlockOpcodeList()) {
                    StackMetadata currentStackMetadata = opcodeIndexToStackMetadata.get(opcode.getBytecodeIndex());
                    List<Integer> branchList = opcode.getPossibleNextBytecodeIndexList();
                    List<StackMetadata> nextStackMetadataList;

                    try {
                        nextStackMetadataList = currentStackMetadata.isDeadCode()
                                ? branchList.stream().map(i -> StackMetadata.DEAD_CODE).collect(Collectors.toList())
                                : opcode.getStackMetadataAfterInstructionForBranches(functionMetadata, currentStackMetadata);
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to calculate successor stack metadata for opcode (" + opcode
                                + ") with prior stack metadata ("
                                + currentStackMetadata + ").", t);
                    }
                    for (int i = 0; i < branchList.size(); i++) {
                        IndexBranchPair indexBranchPair = new IndexBranchPair(opcode.getBytecodeIndex(), i);
                        int nextBytecodeIndex = branchList.get(i);
                        StackMetadata nextStackMetadata = nextStackMetadataList.get(i);
                        if (opcodeIndexToJumpSourceMap.containsKey(indexBranchPair)) {
                            opcodeIndexToJumpSourceMap.get(indexBranchPair).setStackMetadata(nextStackMetadata);
                        }
                        try {
                            StackMetadata originalOpcodeMetadata = opcodeIndexToStackMetadata.get(nextBytecodeIndex);
                            StackMetadata newOpcodeMetadata =
                                    opcodeIndexToStackMetadata.merge(nextBytecodeIndex, nextStackMetadata,
                                            StackMetadata::unifyWith);
                            hasChanged |= !newOpcodeMetadata.equals(originalOpcodeMetadata);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalStateException(
                                    "Cannot unify branch (" + indexBranchPair.branch + ": to index " + indexBranchPair.index
                                            + ") stack metadata (" +
                                            nextStackMetadata + ") for source opcode (" + opcode + ") with" +
                                            "prior stack metadata (" + opcodeIndexToStackMetadata.get(nextBytecodeIndex)
                                            + "): different stack sizes;\n"
                                            + PythonBytecodeToJavaBytecodeTranslator
                                                    .getPythonBytecodeListing(functionMetadata.pythonCompiledFunction),
                                    e);
                        }
                    }
                }
            }
            for (ExceptionBlock exceptionBlock : functionMetadata.pythonCompiledFunction.co_exceptiontable
                    .getEntries()) {
                try {
                    StackMetadata originalOpcodeMetadata =
                            opcodeIndexToStackMetadata.get(exceptionBlock.getTargetInstruction());
                    StackMetadata newOpcodeMetadata = opcodeIndexToStackMetadata.merge(exceptionBlock.getTargetInstruction(),
                            getExceptionStackMetadata(exceptionBlock,
                                    functionMetadata,
                                    initialStackMetadata,
                                    opcodeIndexToStackMetadata.getOrDefault(exceptionBlock.getBlockStartInstructionInclusive(),
                                            StackMetadata.DEAD_CODE)),
                            StackMetadata::unifyWith);
                    hasChanged |= !newOpcodeMetadata.equals(originalOpcodeMetadata);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Cannot unify block starting at " + exceptionBlock.getTargetInstruction()
                                    + ": different stack sizes;\n"
                                    + PythonBytecodeToJavaBytecodeTranslator
                                            .getPythonBytecodeListing(functionMetadata.pythonCompiledFunction),
                            e);
                }
            }
        } while (hasChanged);

        stackMetadataForOperations = opcodeIndexToStackMetadata
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private static class IndexBranchPair {
        final Integer index;
        final Integer branch;

        public IndexBranchPair(Integer index, Integer branch) {
            this.index = index;
            this.branch = branch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IndexBranchPair that = (IndexBranchPair) o;
            return index.equals(that.index) && branch.equals(that.branch);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, branch);
        }

        @Override
        public String toString() {
            return "IndexBranchPair{" +
                    "index=" + index +
                    ", branch=" + branch +
                    '}';
        }
    }
}
