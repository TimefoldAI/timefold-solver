package ai.timefold.solver.core.impl.heuristic.selector.common.nearby;

/**
 * A {@link NearbyDistanceMeter} with several distance matrices,
 * for example one for each combination of transport type and time of day.
 * Each property that changes travel has a few values,
 * and one matrix exists for each combination of values:
 * 3 transport types × 3 time slots give 9 matrices,
 * numbered for example {@code type * 3 + slot}.
 * Keep the number of matrices as small as possible.
 * Selection time grows with the number of matrices,
 * and the work and memory to build the lists of swap candidates grow with its square.
 * Add a property only when it changes which items are near each other,
 * not only how long the trips take.
 * <p>
 * Unlike a plain {@link NearbyDistanceMeter}, this meter may hold state,
 * which {@link #initializeDistanceMatrices(Object)} creates.
 * One instance serves one solver, across its solves and restarts;
 * in partitioned search, each partition has its own instance.
 *
 * @param <Solution_> the solution type, the class with the {@code PlanningSolution} annotation
 * @param <Moved_> the type of the item that a move changes
 * @param <Neighbor_> the type of the item that the moved item can be placed near
 */
public interface MatrixAwareNearbyDistanceMeter<Solution_, Moved_, Neighbor_> extends NearbyDistanceMeter<Moved_, Neighbor_> {

    /**
     * The entry point of this meter.
     * The solver calls it once each time solving starts, before any other method,
     * so again after each problem change.
     * It may read the solution and create state, for example distance tables;
     * it must rebuild that state, not add to it.
     * <p>
     * The solution may be uninitialized, because solving starts before the construction heuristic.
     * Treat it as read-only, and do not expect its data to stay constant after the call.
     *
     * @param workingSolution never null
     * @return the number of distance matrices, from 1 to 16 inclusive, also for an empty dataset;
     *         any other value fails fast
     */
    int initializeDistanceMatrices(Solution_ workingSolution);

    /**
     * The matrix that the item is on now, in the working solution,
     * for example the transport type of the vehicle that a visit is assigned to.
     * The solver asks for neighbors, and in swaps also for moved items,
     * because both swapped items have the same type there.
     * It also asks for unassigned items; return a default matrix for them, such as 0.
     * <p>
     * The solver calls this method many times on every selection.
     * Read only the given item, and keep it to a few field reads.
     *
     * @param neighbor never null
     * @return from 0 to the count that {@link #initializeDistanceMatrices(Object)} returned, minus 1;
     *         any other value fails fast
     */
    int getDistanceMatrixIndex(Neighbor_ neighbor);

    /**
     * Measures the distance from the moved item to the neighbor in the given matrix.
     * Use the same unit in every matrix, for example seconds,
     * because the solver compares distances across matrices.
     * <p>
     * The solver calls only this method, never {@link #getNearbyDistance(Object, Object)}.
     * Keep it cheap, such as a table lookup:
     * the solver may compute a distance again when it needs it.
     * The distance must not depend on the working solution.
     *
     * @param moved never null
     * @param neighbor never null
     * @param matrix from 0 to the count that {@link #initializeDistanceMatrices(Object)} returned, minus 1
     * @return Preferably always {@code >= 0.0}. If moved == neighbor, it usually returns 0.0.
     */
    double getNearbyDistance(Moved_ moved, Neighbor_ neighbor, int matrix);

    /**
     * The distance in matrix 0.
     */
    @Override
    default double getNearbyDistance(Moved_ moved, Neighbor_ neighbor) {
        return getNearbyDistance(moved, neighbor, 0);
    }

}
