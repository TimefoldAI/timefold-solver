package ai.timefold.solver.core.impl.score.stream.bavet.visual;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractNode;

record GraphSink<Solution_>(AbstractNode node, BavetConstraint<Solution_> constraint) {
}
