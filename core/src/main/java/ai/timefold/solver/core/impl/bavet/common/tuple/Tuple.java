package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A tuple is an <i>out tuple</i> in exactly one node and an <i>in tuple</i> in one or more nodes.
 *
 * <p>
 * A tuple must not implement equals()/hashCode() to fact equality,
 * because some stream operations ({@link UniConstraintStream#map(Function)}, ...)
 * might create 2 different tuple instances to contain the same facts
 * and because a tuple's origin may replace a tuple's fact.
 *
 * <p>
 * A tuple is modifiable.
 * However, only the origin node of a tuple (the node where the tuple is the out tuple) may modify it.
 */
@NullMarked
public sealed interface Tuple permits BiTuple, QuadTuple, TriTuple, UniTuple {

    TupleState getState();

    void setState(TupleState state);

    <Value_> @Nullable Value_ getStore(int index);

    void setStore(int index, @Nullable Object value);

    <Value_> @Nullable Value_ removeStore(int index);

}
