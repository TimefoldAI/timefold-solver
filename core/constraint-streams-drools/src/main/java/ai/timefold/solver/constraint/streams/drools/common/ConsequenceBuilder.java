package ai.timefold.solver.constraint.streams.drools.common;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.constraint.streams.drools.DroolsConstraint;

import org.drools.model.Global;
import org.drools.model.consequences.ConsequenceBuilder.ValidBuilder;

@FunctionalInterface
interface ConsequenceBuilder<Solution_>
        extends BiFunction<DroolsConstraint<Solution_>, Global<WeightedScoreImpacter>, ValidBuilder> {
}
