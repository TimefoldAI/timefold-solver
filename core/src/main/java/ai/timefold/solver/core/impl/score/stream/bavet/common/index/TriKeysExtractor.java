package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;

@FunctionalInterface
public interface TriKeysExtractor<A, B, C> extends KeysExtractor<TriTuple<A, B, C>> {

}
