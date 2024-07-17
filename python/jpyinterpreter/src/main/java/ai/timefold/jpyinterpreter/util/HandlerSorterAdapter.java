package ai.timefold.jpyinterpreter.util;

import java.util.Collections;
import java.util.Comparator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

// Taken from https://gist.github.com/sampsyo/281277
/**
 * Sorts the exception handlers in a method innermost-to-outermost. This
 * allows the programmer to add handlers without worrying about ordering them
 * correctly with respect to existing, in-code handlers.
 *
 * Behavior is only defined for properly-nested handlers. If any "try" blocks
 * overlap (something that isn't possible in Java code) then this may not
 * do what you want.
 */
public class HandlerSorterAdapter extends MethodNode {

    private final MethodVisitor mv;

    public HandlerSorterAdapter(
            final MethodVisitor mv,
            final int api,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        super(api, access, name, desc, signature, exceptions);
        this.mv = mv;
    }

    public void visitEnd() {
        TryCatchBlockLengthComparator comp =
                new TryCatchBlockLengthComparator(this);
        Collections.sort(tryCatchBlocks, comp);

        if (mv != null) {
            accept(mv);
        }
    }

    /**
     * Compares TryCatchBlockNodes by the length of their "try" block.
     */
    static class TryCatchBlockLengthComparator implements Comparator {

        private final MethodNode meth;

        public TryCatchBlockLengthComparator(MethodNode meth) {
            this.meth = meth;
        }

        public int compare(Object o1, Object o2) {
            int len1 = blockLength((TryCatchBlockNode) o1);
            int len2 = blockLength((TryCatchBlockNode) o2);
            return len1 - len2;
        }

        private int blockLength(TryCatchBlockNode block) {
            int startidx = meth.instructions.indexOf(block.start);
            int endidx = meth.instructions.indexOf(block.end);
            return endidx - startidx;
        }
    }

}
