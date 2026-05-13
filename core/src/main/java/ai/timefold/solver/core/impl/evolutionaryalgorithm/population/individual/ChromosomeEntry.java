package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ChromosomeEntry(Object entity, @Nullable Object value, int index) {
}
