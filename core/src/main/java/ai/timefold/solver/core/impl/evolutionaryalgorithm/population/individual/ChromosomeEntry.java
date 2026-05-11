package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChromosomeEntry(Object value, Object entity, int index) {
}
