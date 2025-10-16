package ai.timefold.solver.core.impl.bavet.common;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface BavetRootNode<A> {
    void insert(@Nullable A a);

    void update(@Nullable A a);

    void retract(@Nullable A a);

    void settle();

    boolean allowsInstancesOf(Class<?> clazz);

    Class<?>[] getSourceClasses();

    /**
     * Determines if this node supports the given lifecycle operation.
     * Unsupported nodes will not be called during that lifecycle operation.
     *
     * @param lifecycleOperation the lifecycle operation to check
     * @return {@code true} if the given lifecycle operation is supported; otherwise, {@code false}.
     */
    boolean supports(BavetRootNode.LifecycleOperation lifecycleOperation);

    /**
     * Represents the various lifecycle operations that can be performed
     * on tuples within a node in Bavet.
     */
    enum LifecycleOperation {
        /**
         * Represents the operation of inserting a new tuple into the node.
         * This operation is typically performed when a new fact is added to the working solution
         * and needs to be propagated through the node network.
         */
        INSERT,
        /**
         * Represents the operation of updating an existing tuple within the node.
         * This operation is typically triggered when a fact in the working solution
         * is modified, requiring the corresponding tuple to be updated and its changes
         * propagated through the node network.
         */
        UPDATE,
        /**
         * Represents the operation of retracting or removing an existing tuple from the node.
         * This operation is typically used when a fact is removed from the working solution
         * and its corresponding tuple needs to be removed from the node network.
         */
        RETRACT,
        /**
         * Represents the operation of recalculating the score, just prior to all queued operations being propagated.
         */
        SETTLE
    }
}
