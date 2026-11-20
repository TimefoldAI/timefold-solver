package ai.timefold.solver.core.impl.heuristic.selector.common.nearby;

/**
 * Measures how near items are to each other, for nearby selection.
 * Nearby selection uses the distances only to rank candidate moves;
 * the score still judges every move.
 * <p>
 * Implementations are expected to be stateless.
 * The solver may choose to reuse instances.
 * <p>
 * A meter can declare several distance matrices,
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
 *
 * @param <Moved_> the type of the item that a move moves
 * @param <Neighbor_> the type of the item that the moved item can be placed near
 */
@FunctionalInterface
public interface NearbyDistanceMeter<Moved_, Neighbor_> {

    /**
     * Measures the distance from the moved item to the neighbor.
     * The distance can be in any unit, such a meters, foot, seconds or milliseconds.
     * For example, vehicle routing often uses driving time in seconds.
     * <p>
     * Distances can be asymmetrical: the distance from an origin to a destination
     * often differs from the distance from that destination to that origin.
     * The solver asks for the other direction itself, where a move needs it.
     * <p>
     * The distance must not depend on the working solution.
     *
     * @param moved never null
     * @param neighbor never null
     * @return Preferably always {@code >= 0.0}. If moved == neighbor, it usually returns 0.0.
     */
    double getNearbyDistance(Moved_ moved, Neighbor_ neighbor);

    /**
     * The number of distance matrices.
     * The limit of 16 is deliberate:
     * selection time grows with the number of matrices,
     * and the work and memory to build the lists of swap candidates grow with its square.
     *
     * <p>
     * The solver calls this method once when solving starts, and again after each problem change.
     * Treat the solution as read-only, and do not expect its data to stay constant after the call.
     *
     * @param workingSolution never null
     * @return from 1 to 16, also for an empty dataset; any other value fails fast when the solver starts
     */
    default int getDistanceMatrixCount(Object workingSolution) {
        return 1;
    }

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
     * @return from 0 to {@link #getDistanceMatrixCount(Object)} - 1; any other value fails fast
     */
    default int getDistanceMatrixIndex(Neighbor_ neighbor) {
        return 0;
    }

    /**
     * Measures the distance from the moved item to the neighbor in the given matrix.
     * Use the same unit in every matrix, for example seconds,
     * because the solver compares distances across matrices.
     * <p>
     * With more than one matrix, the solver calls only this method.
     * Keep it cheap, such as a table lookup:
     * the solver may compute a distance again when it needs it.
     * The distance must not depend on the working solution.
     *
     * @param moved never null
     * @param neighbor never null
     * @param matrix from 0 to {@link #getDistanceMatrixCount(Object)} - 1
     * @return Preferably always {@code >= 0.0}. If moved == neighbor, it usually returns 0.0.
     */
    default double getNearbyDistance(Moved_ moved, Neighbor_ neighbor, int matrix) {
        return getNearbyDistance(moved, neighbor);
    }

}
