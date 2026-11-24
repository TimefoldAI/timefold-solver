package ai.timefold.solver.core.impl.heuristic.selector.common.nearby;

/**
 * Measures the distance between items for nearby selection.
 * Nearby selection uses the distances only to rank candidate moves;
 * the score still judges every move.
 * <p>
 * Implementations are expected to be stateless.
 * The solver may choose to reuse instances.
 * For more than one meaning of "near", see {@link MatrixAwareNearbyDistanceMeter}.
 *
 * @param <Moved_> the type of the item that a move changes
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

}
