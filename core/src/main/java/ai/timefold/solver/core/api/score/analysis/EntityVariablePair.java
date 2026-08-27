package ai.timefold.solver.core.api.score.analysis;

import org.jspecify.annotations.NullMarked;

/**
 * A pair of an entity and a variable on it.
 * 
 * @param entity The entity.
 * @param variableName The variable on the entity.
 */
@NullMarked
public record EntityVariablePair(Object entity, String variableName) {
    @Override
    public String toString() {
        return "%s.%s".formatted(entity, variableName);
    }
}
