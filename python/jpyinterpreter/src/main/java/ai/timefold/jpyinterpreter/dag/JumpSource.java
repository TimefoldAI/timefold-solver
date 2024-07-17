package ai.timefold.jpyinterpreter.dag;

import ai.timefold.jpyinterpreter.StackMetadata;

public class JumpSource {
    BasicBlock fromBasicBlock;
    StackMetadata stackMetadata;

    public JumpSource(BasicBlock fromBasicBlock) {
        this.fromBasicBlock = fromBasicBlock;
    }

    public StackMetadata getStackMetadata() {
        return stackMetadata;
    }

    public void setStackMetadata(StackMetadata stackMetadata) {
        this.stackMetadata = stackMetadata;
    }

    @Override
    public String toString() {
        return "JumpSource{" +
                "fromBasicBlock=" + fromBasicBlock +
                ", stackMetadata=" + stackMetadata +
                '}';
    }
}
