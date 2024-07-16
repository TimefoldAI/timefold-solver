package ai.timefold.jpyinterpreter.dag;

import java.util.List;
import java.util.Objects;

import ai.timefold.jpyinterpreter.opcodes.Opcode;

public class BasicBlock {
    int startAtIndex;
    List<Opcode> blockOpcodeList;

    public BasicBlock(int startAtIndex, List<Opcode> blockOpcodeList) {
        this.startAtIndex = startAtIndex;
        this.blockOpcodeList = blockOpcodeList;
    }

    public int getStartInclusive() {
        return startAtIndex;
    }

    public int getEndExclusive() {
        return startAtIndex + blockOpcodeList.size();
    }

    public Opcode getLeader() {
        return blockOpcodeList.get(0);
    }

    public Opcode getFinalOpcode() {
        return blockOpcodeList.get(blockOpcodeList.size() - 1);
    }

    public boolean containsIndex(int index) {
        return getStartInclusive() <= index && index < getEndExclusive();
    }

    public List<Opcode> getBlockOpcodeList() {
        return blockOpcodeList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicBlock that = (BasicBlock) o;
        return startAtIndex == that.startAtIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startAtIndex);
    }

    @Override
    public String toString() {
        return "BasicBlock{" +
                "startAtIndex=" + startAtIndex +
                ", blockOpcodeList=" + blockOpcodeList +
                '}';
    }
}
