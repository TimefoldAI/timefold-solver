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
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The goal of this class is to register problem changes and best solutions in a thread-safe way.
 * Problem changes are {@link #addProblemChange(Solver, List) put in a queue}
 * and later associated with the best solution which contains them.
 * The best solution is associated with a version number
 * that is incremented each time a {@link #set new best solution is set}.
 * The best solution is {@link #take() taken} together with all problem changes
 * that were registered before the best solution was set.
 * 
 * <p>
 * This class needs to be thread-safe.
 * 
 * @param <Solution_>
 */
@NullMarked
final class BestSolutionHolder<Solution_> {

    private final AtomicReference<BigInteger> lastProcessedVersion = new AtomicReference<>(BigInteger.valueOf(-1));

    // These references are non-final and being accessed from multiple threads, 
    // therefore they need to be volatile and all access synchronized.
    // Both the map and the best solution are based on the current version,
    // and therefore access to both needs to be guarded by the same lock.
    // The version is BigInteger to avoid long overflow.
    // The solver can run potentially forever, so long overflow is a (remote) possibility.
    private volatile SortedMap<BigInteger, List<CompletableFuture<Void>>> problemChangesPerVersionMap =
            createNewProblemChangesMap();
    private volatile @Nullable VersionedBestSolution<Solution_> versionedBestSolution = null;
    private volatile BigInteger currentVersion = BigInteger.ZERO;

    private static SortedMap<BigInteger, List<CompletableFuture<Void>>> createNewProblemChangesMap() {
        return createNewProblemChangesMap(Collections.emptySortedMap());
    }

    private static SortedMap<BigInteger, List<CompletableFuture<Void>>>
            createNewProblemChangesMap(SortedMap<BigInteger, List<CompletableFuture<Void>>> map) {
        return new TreeMap<>(map);
    }

    synchronized boolean isEmpty() {
        return this.versionedBestSolution == null;
    }

    /**
     * @return the last best solution together with problem changes the solution contains.
     *         If there is no new best solution, returns null.
     */
    @Nullable
    BestSolutionContainingProblemChanges<Solution_> take() {
        var latestVersionedBestSolution = resetVersionedBestSolution();
        if (latestVersionedBestSolution == null) {
            return null;
        }

        var bestSolutionVersion = latestVersionedBestSolution.version();
        var latestProcessedVersion = this.lastProcessedVersion.getAndUpdate(bestSolutionVersion::max);
        if (latestProcessedVersion.compareTo(bestSolutionVersion) > 0) {
            // Corner case: The best solution has already been taken,
            // because a later take() was scheduled to run before an earlier take().
            // This causes the later take() to return the latest best solution and all the problem changes,
            // and the earlier best solution to be skipped entirely.
            return null;
        }
        // The map is replaced by a map containing only the problem changes that are not contained in the best solution.
        // This is fully synchronized, so no other thread can access the old map anymore.
        // The old map can then be processed by the current thread without synchronization.
        // The copying of maps is possibly expensive, but due to the nature of problem changes,
        // we do not expect the map to ever get too big.
        // It is not practical to submit a problem change every second, as that gives the solver no time to react.
        // This limits the size of the map on input.
        // The solver also finds new best solutions, which regularly trims the size of the map as well.
        var boundaryVersion = bestSolutionVersion.add(BigInteger.ONE);
        var oldProblemChangesPerVersion =
                replaceMapSynchronized(map -> createNewProblemChangesMap(map.tailMap(boundaryVersion)));
        // At this point, the old map is not accessible to any other thread.
        // We also do not need to clear it, because this being the only reference, 
        // garbage collector will do it for us.
        var containedProblemChanges = oldProblemChangesPerVersion.headMap(boundaryVersion)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
        return new BestSolutionContainingProblemChanges<>(latestVersionedBestSolution.bestSolution(), containedProblemChanges);
    }

    private synchronized @Nullable VersionedBestSolution<Solution_> resetVersionedBestSolution() {
        var oldVersionedBestSolution = this.versionedBestSolution;
        this.versionedBestSolution = null;
        return oldVersionedBestSolution;
    }

    private synchronized SortedMap<BigInteger, List<CompletableFuture<Void>>> replaceMapSynchronized(
            UnaryOperator<SortedMap<BigInteger, List<CompletableFuture<Void>>>> replaceFunction) {
        var oldMap = problemChangesPerVersionMap;
        problemChangesPerVersionMap = replaceFunction.apply(oldMap);
        return oldMap;
    }

    /**
     * Sets the new best solution if all known problem changes have been processed
     * and thus are contained in this best solution.
     *
     * @param bestSolution the new best solution that replaces the previous one if there is any
     * @param isEveryProblemChangeProcessed a supplier that tells if all problem changes have been processed
     */
    void set(Solution_ bestSolution, BooleanSupplier isEveryProblemChangeProcessed) {
        // The new best solution can be accepted only if there are no pending problem changes
        // nor any additional changes may come during this operation.
        // Otherwise, a race condition might occur
        // that leads to associating problem changes with a solution that was created later,
        // but does not contain them yet.
        // As a result, CompletableFutures representing these changes would be completed too early.
        if (isEveryProblemChangeProcessed.getAsBoolean()) {
            synchronized (this) {
                versionedBestSolution = new VersionedBestSolution<>(bestSolution, currentVersion);
                currentVersion = currentVersion.add(BigInteger.ONE);
            }
        }
    }

    /**
     * Adds a batch of problem changes to a solver and registers them
     * to be later retrieved together with a relevant best solution by the {@link #take()} method.
     *
     * @return CompletableFuture that will be completed after the best solution containing this change is passed to
     *         a user-defined Consumer.
     */
    @NonNull
    CompletableFuture<Void> addProblemChange(Solver<Solution_> solver, List<ProblemChange<Solution_>> problemChangeList) {
        var futureProblemChange = new CompletableFuture<Void>();
        synchronized (this) {
            var futureProblemChangeList = problemChangesPerVersionMap.computeIfAbsent(currentVersion,
                    version -> new ArrayList<>());
            futureProblemChangeList.add(futureProblemChange);
            solver.addProblemChanges(problemChangeList);
        }
        return futureProblemChange;
    }

    void cancelPendingChanges() {
        // We first replace the reference with a new map, fully synchronized.
        // Then we process the old map unsynchronized, which is safe because no one can access it anymore.
        // We do not need to clear it, because this being the only reference,
        // the garbage collector will do it for us.
        replaceMapSynchronized(map -> createNewProblemChangesMap())
                .values()
                .stream()
                .flatMap(Collection::stream)
                .forEach(pendingProblemChange -> pendingProblemChange.cancel(false));
    }

    private record VersionedBestSolution<Solution_>(Solution_ bestSolution, BigInteger version) {
    }

}
