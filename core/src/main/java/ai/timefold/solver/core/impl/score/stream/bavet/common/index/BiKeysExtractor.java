package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;

@FunctionalInterface
public interface BiKeysExtractor<A, B> extends KeysExtractor<BiTuple<A, B>> {

}
