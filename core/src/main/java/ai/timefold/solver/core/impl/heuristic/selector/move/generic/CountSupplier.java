package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

/**
 * Used for converting selector size to some other value.
 * Selector size is always long, for legacy reasons.
 * The rest of the code uses ints though, so this functional interface implies a conversion to int,
 * so that the rest of the code doesn't have to worry about it anymore.
 */
@FunctionalInterface
public interface CountSupplier {

    int applyAsInt(long valueCount);

}
