package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.SolutionExtractor;
import ai.timefold.solver.core.preview.api.move.SolutionView;

public final class ForEachFromSolutionUniNode<Solution_, A>
        extends ForEachIncludingUnassignedUniNode<A> {

    private final SolutionExtractor<Solution_, A> solutionExtractor;

    public ForEachFromSolutionUniNode(Class<A> forEachClass, SolutionExtractor<Solution_, A> solutionExtractor,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.solutionExtractor = Objects.requireNonNull(solutionExtractor);
    }

    public void read(SolutionView<Solution_> solutionView, Solution_ solution) {
        var seenFactSet = Collections.newSetFromMap(new IdentityHashMap<A, Boolean>());
        solutionExtractor.apply(solutionView, solution).forEach(a -> {
            if (seenFactSet.contains(a)) { // Eliminate duplicates in the source data.
                return;
            }
            seenFactSet.add(a);
            var tuple = tupleMap.get(a);
            if (tuple == null) {
                super.insert(a);
            } else {
                updateExisting(a, tuple);
            }
        });
        // Retract all tuples that were not seen in the source data.
        var iterator = tupleMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var fact = entry.getKey();
            if (!seenFactSet.contains(fact)) {
                iterator.remove();
                retractExisting(fact, entry.getValue());
            }
        }
    }

    @Override
    public void insert(A a) {
        throw new IllegalStateException("Impossible state: solution-based node cannot insert.");
    }

    @Override
    public void update(A a) {
        throw new IllegalStateException("Impossible state: solution-based node cannot update.");
    }

    @Override
    public void retract(A a) {
        throw new IllegalStateException("Impossible state: solution-based node cannot retract.");
    }

    @Override
    public boolean supportsIndividualUpdates() {
        return false;
    }
}
