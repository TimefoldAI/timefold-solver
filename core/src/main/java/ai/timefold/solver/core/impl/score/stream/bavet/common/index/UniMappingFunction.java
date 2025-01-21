package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.common.AbstractJoiner;
import ai.timefold.solver.core.impl.score.stream.common.bi.DefaultBiJoiner;

@FunctionalInterface
interface UniMappingFunction<A> extends Function<A, Object> {

    static <A> UniMappingFunction<A> of(AbstractJoiner<?> joiner, int index) {
        var castJoiner = (DefaultBiJoiner<A, ?>) joiner;
        var mapping = castJoiner.getLeftMapping(index);
        return mapping::apply;
    }

}
