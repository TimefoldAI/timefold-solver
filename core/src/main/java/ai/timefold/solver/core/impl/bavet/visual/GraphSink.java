package ai.timefold.solver.core.impl.bavet.visual;

import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;

record GraphSink<Solution_>(AbstractNode node, BavetConstraint<Solution_> constraint) {
}
