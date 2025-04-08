package ai.timefold.solver.core.impl.solver.change;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class DefaultProblemChangeDirector<Solution_> implements ProblemChangeDirector {

    private final InnerScoreDirector<Solution_, ?> scoreDirector;

    public DefaultProblemChangeDirector(InnerScoreDirector<Solution_, ?> scoreDirector) {
        this.scoreDirector = scoreDirector;
    }

    @Override
    public <Entity> void addEntity(@NonNull Entity entity, @NonNull Consumer<Entity> entityConsumer) {
        Objects.requireNonNull(entity, () -> "Entity (" + entity + ") cannot be null.");
        Objects.requireNonNull(entityConsumer, () -> "Entity consumer (" + entityConsumer + ") cannot be null.");
        scoreDirector.beforeEntityAdded(entity);
        entityConsumer.accept(entity);
        scoreDirector.afterEntityAdded(entity);
    }

    @Override
    public <Entity> void removeEntity(@NonNull Entity entity, @NonNull Consumer<Entity> entityConsumer) {
        Objects.requireNonNull(entity, () -> "Entity (" + entity + ") cannot be null.");
        Objects.requireNonNull(entityConsumer, () -> "Entity consumer (" + entityConsumer + ") cannot be null.");
        var workingEntity = lookUpWorkingObjectOrFail(entity);
        scoreDirector.beforeEntityRemoved(workingEntity);
        entityConsumer.accept(workingEntity);
        scoreDirector.afterEntityRemoved(workingEntity);
    }

    @Override
    public <Entity> void changeVariable(@NonNull Entity entity, @NonNull String variableName,
            @NonNull Consumer<Entity> entityConsumer) {
        Objects.requireNonNull(entity, () -> "Entity (" + entity + ") cannot be null.");
        Objects.requireNonNull(variableName, () -> "Planning variable name (" + variableName + ") cannot be null.");
        Objects.requireNonNull(entityConsumer, () -> "Entity consumer (" + entityConsumer + ") cannot be null.");
        var workingEntity = lookUpWorkingObjectOrFail(entity);
        scoreDirector.beforeVariableChanged(workingEntity, variableName);
        entityConsumer.accept(workingEntity);
        scoreDirector.afterVariableChanged(workingEntity, variableName);
    }

    @Override
    public <ProblemFact> void addProblemFact(@NonNull ProblemFact problemFact,
            @NonNull Consumer<ProblemFact> problemFactConsumer) {
        Objects.requireNonNull(problemFact, () -> "Problem fact (" + problemFact + ") cannot be null.");
        Objects.requireNonNull(problemFactConsumer, () -> "Problem fact consumer (" + problemFactConsumer
                + ") cannot be null.");
        scoreDirector.beforeProblemFactAdded(problemFact);
        problemFactConsumer.accept(problemFact);
        scoreDirector.afterProblemFactAdded(problemFact);
    }

    @Override
    public <ProblemFact> void removeProblemFact(@NonNull ProblemFact problemFact,
            @NonNull Consumer<ProblemFact> problemFactConsumer) {
        Objects.requireNonNull(problemFact, () -> "Problem fact (" + problemFact + ") cannot be null.");
        Objects.requireNonNull(problemFactConsumer, () -> "Problem fact consumer (" + problemFactConsumer
                + ") cannot be null.");
        var workingProblemFact = lookUpWorkingObjectOrFail(problemFact);
        scoreDirector.beforeProblemFactRemoved(workingProblemFact);
        problemFactConsumer.accept(workingProblemFact);
        scoreDirector.afterProblemFactRemoved(workingProblemFact);
    }

    @Override
    public <EntityOrProblemFact> void changeProblemProperty(@NonNull EntityOrProblemFact problemFactOrEntity,
            @NonNull Consumer<EntityOrProblemFact> problemFactOrEntityConsumer) {
        Objects.requireNonNull(problemFactOrEntity,
                () -> "Problem fact or entity (" + problemFactOrEntity + ") cannot be null.");
        Objects.requireNonNull(problemFactOrEntityConsumer, () -> "Problem fact or entity consumer ("
                + problemFactOrEntityConsumer + ") cannot be null.");
        var workingEntityOrProblemFact = lookUpWorkingObjectOrFail(problemFactOrEntity);
        scoreDirector.beforeProblemPropertyChanged(workingEntityOrProblemFact);
        problemFactOrEntityConsumer.accept(workingEntityOrProblemFact);
        scoreDirector.afterProblemPropertyChanged(workingEntityOrProblemFact);
    }

    @Override
    public <EntityOrProblemFact> @Nullable EntityOrProblemFact
            lookUpWorkingObjectOrFail(@Nullable EntityOrProblemFact externalObject) {
        return scoreDirector.lookUpWorkingObject(externalObject);
    }

    @Override
    public <EntityOrProblemFact> Optional<EntityOrProblemFact>
            lookUpWorkingObject(EntityOrProblemFact externalObject) {
        return Optional.ofNullable(scoreDirector.lookUpWorkingObjectOrReturnNull(externalObject));
    }

    @Override
    public void updateShadowVariables() {
        scoreDirector.triggerVariableListeners();
    }

}
