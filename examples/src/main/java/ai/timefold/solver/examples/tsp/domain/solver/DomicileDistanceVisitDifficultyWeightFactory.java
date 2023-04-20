package ai.timefold.solver.examples.tsp.domain.solver;

import static java.util.Comparator.comparingLong;

import java.util.Comparator;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.examples.tsp.domain.Domicile;
import ai.timefold.solver.examples.tsp.domain.TspSolution;
import ai.timefold.solver.examples.tsp.domain.Visit;

public class DomicileDistanceVisitDifficultyWeightFactory implements SelectionSorterWeightFactory<TspSolution, Visit> {

    @Override
    public DomicileDistanceVisitDifficultyWeight createSorterWeight(TspSolution tspSolution, Visit visit) {
        Domicile domicile = tspSolution.getDomicile();
        long domicileRoundTripDistance = domicile.getDistanceTo(visit) + visit.getDistanceTo(domicile);
        return new DomicileDistanceVisitDifficultyWeight(visit, domicileRoundTripDistance);
    }

    public static class DomicileDistanceVisitDifficultyWeight implements Comparable<DomicileDistanceVisitDifficultyWeight> {

        private static final Comparator<DomicileDistanceVisitDifficultyWeight> COMPARATOR =
                // Decreasing: closer to depot is stronger
                comparingLong((DomicileDistanceVisitDifficultyWeight weight) -> -weight.domicileRoundTripDistance)
                        .thenComparingDouble(weight -> weight.visit.getLocation().getLatitude())
                        .thenComparing(weight -> weight.visit, comparingLong(Visit::getId));

        private final Visit visit;
        private final long domicileRoundTripDistance;

        public DomicileDistanceVisitDifficultyWeight(Visit visit, long domicileRoundTripDistance) {
            this.visit = visit;
            this.domicileRoundTripDistance = domicileRoundTripDistance;
        }

        @Override
        public int compareTo(DomicileDistanceVisitDifficultyWeight other) {
            return COMPARATOR.compare(this, other);
        }

    }

}
