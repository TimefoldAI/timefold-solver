package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.common.AbstractJoiner;
import ai.timefold.solver.core.impl.score.stream.common.quad.DefaultQuadJoiner;

@FunctionalInterface
interface TriMappingFunction<A, B, C> extends TriFunction<A, B, C, Object> {

    static <A, B, C> TriMappingFunction<A, B, C> of(AbstractJoiner<?> joiner, int index) {
        var castJoiner = (DefaultQuadJoiner<A, B, C, ?>) joiner;
        var mapping = castJoiner.getLeftMapping(index);
        return mapping::apply;
    }

}
