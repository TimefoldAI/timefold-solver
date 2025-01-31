package ai.timefold.solver.core.impl.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The goal of this class is to register problem changes and best solutions in a thread-safe way.
 * Problem changes are {@link #addProblemChange(Solver, ProblemChange) put in a queue}
 * and later associated with the best solution which contains them.
 * The best solution is associated with a version number
 * that is incremented each time a {@link #set new best solution is set}.
 * The best solution is {@link #take() taken} together with all problem changes
 * that were registered before the best solution was set.
 * 
 * <p>
 * This class needs to be thread-safe.
 * Due to complicated interactions between the solver, solver manager and problem changes,
 * it is best if we avoid explicit locking here,
 * reducing cognitive complexity of the whole system.
 * The core idea being to never modify the same data structure from multiple threads;
 * instead, we replace the data structure with a new one atomically.
 * The code contains comments throughout the class that explain the reasoning behind the design.
 * 
 * @param <Solution_>
 */
@NullMarked
final class BestSolutionHolder<Solution_> {

    private final AtomicReference<@Nullable VersionedBestSolution<Solution_>> versionedBestSolutionRef =
            new AtomicReference<>();
    private final AtomicReference<SortedMap<BigInteger, List<CompletableFuture<Void>>>> problemChangesPerVersionRef =
            new AtomicReference<>(createNewProblemChangesMap());
    // The version is BigInteger to avoid long overflow.
    // The solver can run potentially forever, so long overflow is a (remote) possibility.
    private final AtomicReference<BigInteger> currentVersion = new AtomicReference<>(BigInteger.ZERO);
    private final AtomicReference<BigInteger> lastProcessedVersion = new AtomicReference<>(BigInteger.valueOf(-1));

    private static SortedMap<BigInteger, List<CompletableFuture<Void>>> createNewProblemChangesMap() {
        return createNewProblemChangesMap(Collections.emptySortedMap());
    }

    private static SortedMap<BigInteger, List<CompletableFuture<Void>>>
            createNewProblemChangesMap(SortedMap<BigInteger, List<CompletableFuture<Void>>> map) {
        return new TreeMap<>(map);
    }

    boolean isEmpty() {
        return versionedBestSolutionRef.get() == null;
    }

    /**
     * @return the last best solution together with problem changes the solution contains.
     *         If there is no new best solution, returns null.
     */
    @Nullable
    BestSolutionContainingProblemChanges<Solution_> take() {
        var versionedBestSolution = versionedBestSolutionRef.getAndSet(null);
        if (versionedBestSolution == null) {
            return null;
        }

        var bestSolutionVersion = versionedBestSolution.version();
        var latestProcessedVersion = this.lastProcessedVersion.getAndUpdate(bestSolutionVersion::max);
        if (latestProcessedVersion.compareTo(bestSolutionVersion) > 0) {
            // Corner case: The best solution has already been taken,
            // because a later take() was scheduled to run before an earlier take().
            // This causes the later take() to return the latest best solution and all the problem changes,
            // and the earlier best solution to be skipped entirely.
            return null;
        }
        // The map is replaced by a map containing only the problem changes that are not contained in the best solution.
        // This is done atomically, so no other thread can access the old map anymore.
        // The old map can then be processed by the current thread without synchronization.
        // The copying of maps is possibly expensive, but due to the nature of problem changes,
        // we do not expect the map to ever get too big.
        // It is not practical to submit a problem change every second, as that gives the solver no time to react.
        // This limits the size of the map on input.
        // The solver also finds new best solutions, which regularly trims the size of the map as well.
        var boundaryVersion = bestSolutionVersion.add(BigInteger.ONE);
        var oldProblemChangesPerVersion =
                problemChangesPerVersionRef.getAndUpdate(map -> createNewProblemChangesMap(map.tailMap(boundaryVersion)));
        // At this point, the old map is not accessible to any other thread.
        // We also do not need to clear it, because this being the only reference, 
        // garbage collector will do it for us.
        var containedProblemChanges = oldProblemChangesPerVersion.headMap(boundaryVersion)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
        return new BestSolutionContainingProblemChanges<>(versionedBestSolution.bestSolution(), containedProblemChanges);
    }

    /**
     * Sets the new best solution if all known problem changes have been processed and thus are contained in this
     * best solution.
     *
     * @param bestSolution the new best solution that replaces the previous one if there is any
     * @param isEveryProblemChangeProcessed a supplier that tells if all problem changes have been processed
     */
    void set(Solution_ bestSolution, BooleanSupplier isEveryProblemChangeProcessed) {
        /*
         * The new best solution can be accepted only if there are no pending problem changes
         * nor any additional changes may come during this operation.
         * Otherwise, a race condition might occur
         * that leads to associating problem changes with a solution that was created later,
         * but does not contain them yet.
         * As a result, CompletableFutures representing these changes would be completed too early.
         */
        if (isEveryProblemChangeProcessed.getAsBoolean()) {
            // This field is atomic, so we can safely set the new best solution without synchronization.
            versionedBestSolutionRef.set(
                    new VersionedBestSolution<>(bestSolution, currentVersion.getAndUpdate(old -> old.add(BigInteger.ONE))));
        }
    }

    /**
     * Adds a new problem change to a solver and registers the problem change
     * to be later retrieved together with a relevant best solution by the {@link #take()} method.
     *
     * @return CompletableFuture that will be completed after the best solution containing this change is passed to
     *         a user-defined Consumer.
     */
    @NonNull
    CompletableFuture<Void> addProblemChange(Solver<Solution_> solver, ProblemChange<Solution_> problemChange) {
        var futureProblemChange = new CompletableFuture<Void>();
        synchronized (this) {
            // This actually needs to be synchronized, 
            // as we want the new problem change and its version to be linked.  
            var futureProblemChangeList =
                    problemChangesPerVersionRef.get().computeIfAbsent(currentVersion.get(), version -> new ArrayList<>());
            futureProblemChangeList.add(futureProblemChange);
            solver.addProblemChange(problemChange);
        }
        return futureProblemChange;
    }

    void cancelPendingChanges() {
        // The map is an atomic reference. 
        // We first replace the reference with a new map atomically, avoiding synchronization issues.
        // Then we process the old map, which is safe because no one can access it anymore.
        // We do not need to clear it, because this being the only reference,
        // the garbage collector will do it for us.
        problemChangesPerVersionRef.getAndSet(createNewProblemChangesMap())
                .values()
                .stream()
                .flatMap(Collection::stream)
                .forEach(pendingProblemChange -> pendingProblemChange.cancel(false));
    }

    private record VersionedBestSolution<Solution_>(Solution_ bestSolution, BigInteger version) {
    }

}
