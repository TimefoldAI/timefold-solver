package ai.timefold.jpyinterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.util.JumpUtils;

public class PythonExceptionTable {
    private final List<ExceptionBlock> blockList;

    public PythonExceptionTable() {
        this.blockList = new ArrayList<>();
    }

    public void addEntry(PythonVersion pythonVersion, int blockStartInstructionInclusive, int blockEndInstructionInclusive,
            int targetByteOffset, int stackDepth, boolean pushLastIndex) {
        blockList.add(
                new ExceptionBlock(JumpUtils.getInstructionIndexForByteOffset(blockStartInstructionInclusive, pythonVersion),
                        JumpUtils.getInstructionIndexForByteOffset(blockEndInstructionInclusive, pythonVersion) + 1,
                        JumpUtils.getInstructionIndexForByteOffset(targetByteOffset, pythonVersion),
                        stackDepth, pushLastIndex));
    }

    public List<ExceptionBlock> getEntries() {
        return blockList;
    }

    public boolean containsJumpTarget(int target) {
        return blockList.stream().anyMatch(block -> block.targetInstruction == target);
    }

    public Set<Integer> getJumpTargetSet() {
        return blockList.stream().map(ExceptionBlock::getTargetInstruction).collect(Collectors.toSet());
    }

    public Set<Integer> getJumpTargetForStartSet(int start) {
        return blockList.stream()
                .filter(exceptionBlock -> exceptionBlock.getBlockStartInstructionInclusive() == start)
                .map(ExceptionBlock::getTargetInstruction)
                .collect(Collectors.toSet());
    }

    public List<ExceptionBlock> getInnerExceptionBlockList(int start, Set<Integer> possibleJumpTargetSet) {
        return blockList.stream()
                .filter(exceptionBlock -> exceptionBlock.getBlockStartInstructionInclusive() == start ||
                        exceptionBlock.containsAnyTargetInSet(possibleJumpTargetSet))
                .collect(Collectors.toList());
    }

    public Set<Integer> getStartPositionSet() {
        return blockList.stream().map(ExceptionBlock::getBlockStartInstructionInclusive).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return blockList.stream().map(ExceptionBlock::toString)
                .collect(Collectors.joining("\n    ", "ExceptionTable:\n    ", ""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonExceptionTable that = (PythonExceptionTable) o;
        return Objects.equals(blockList, that.blockList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockList);
    }
}
