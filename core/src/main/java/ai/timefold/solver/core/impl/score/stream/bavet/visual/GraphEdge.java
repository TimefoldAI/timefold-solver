package ai.timefold.solver.core.impl.score.stream.bavet.visual;

import java.util.Objects;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractNode;

record GraphEdge(AbstractNode from, AbstractNode to) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GraphEdge graphEdge))
            return false;
        return Objects.equals(to.getId(), graphEdge.to.getId()) && Objects.equals(from.getId(), graphEdge.from.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(from.getId(), to.getId());
    }
}
