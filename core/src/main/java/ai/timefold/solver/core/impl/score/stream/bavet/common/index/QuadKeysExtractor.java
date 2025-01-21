package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;

@FunctionalInterface
public interface QuadKeysExtractor<A, B, C, D> extends KeysExtractor<QuadTuple<A, B, C, D>> {

}
