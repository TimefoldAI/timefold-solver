package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.stream.common.ForEachFilteringCriteria;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class ConstraintNodeBuildHelper<Solution_, Score_ extends Score<Score_>>
        extends AbstractNodeBuildHelper<BavetAbstractConstraintStream<Solution_>> {

    private final AbstractScoreInliner<Score_> scoreInliner;
    private final SupplyManager supplyManager;
    private final Map<EntityDescriptor<?>, Map<ForEachFilteringCriteria, Predicate<Object>>> entityDescriptorToForEachCriteriaToPredicateMap;

    public ConstraintNodeBuildHelper(SupplyManager supplyManager,
            Set<BavetAbstractConstraintStream<Solution_>> activeStreamSet,
            AbstractScoreInliner<Score_> scoreInliner) {
        super(activeStreamSet);
        this.supplyManager = supplyManager;
        this.scoreInliner = scoreInliner;
        this.entityDescriptorToForEachCriteriaToPredicateMap = new HashMap<>();
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

    public <A> Predicate<A> getForEachPredicateForEntityDescriptorAndCriteria(EntityDescriptor<?> entityDescriptor,
            ForEachFilteringCriteria criteria) {
        var predicateMap =
                entityDescriptorToForEachCriteriaToPredicateMap.computeIfAbsent(entityDescriptor, ignored -> new HashMap<>());
        return (Predicate<A>) predicateMap.computeIfAbsent(criteria,
                ignored -> criteria.getFilterForEntityDescriptor(supplyManager, entityDescriptor));
    }
}
