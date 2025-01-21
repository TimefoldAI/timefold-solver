package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

@FunctionalInterface
public interface UniKeysExtractor<A> extends KeysExtractor<UniTuple<A>> {

}
