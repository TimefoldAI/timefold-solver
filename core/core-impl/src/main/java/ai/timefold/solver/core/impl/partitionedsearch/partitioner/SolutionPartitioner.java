package ai.timefold.solver.core.impl.partitionedsearch.partitioner;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Splits one {@link PlanningSolution solution} into multiple partitions.
 * The partitions are solved and merged based on the {@link PlanningSolution#lookUpStrategyType()}.
 * <p>
 * To add custom properties, configure custom properties and add public setters for them.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface SolutionPartitioner<Solution_> {

    /**
     * Returns a list of partition cloned {@link PlanningSolution solutions}
     * for which each {@link PlanningEntity planning entity}
     * is partition cloned into exactly 1 of those partitions.
     * Problem facts can be multiple partitions (with our without cloning).
     * <p>
     * Any class that is {@link SolutionCloner solution cloned} must also be partitioned cloned.
     * A class can be partition cloned without being solution cloned.
     *
     * @param scoreDirector never null, the {@link ScoreDirector}
     *        which has the {@link ScoreDirector#getWorkingSolution()} that needs to be split up
     * @param runnablePartThreadLimit null if unlimited, never negative
     * @return never null, {@link List#size()} of at least 1.
     */
    List<Solution_> splitWorkingSolution(ScoreDirector<Solution_> scoreDirector, Integer runnablePartThreadLimit);

}
