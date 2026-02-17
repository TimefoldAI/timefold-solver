package ai.timefold.solver.core.impl.solver.change;

import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DefaultProblemChangeDirector<Solution_> implements ProblemChangeDirector {

    private final InnerScoreDirector<Solution_, ?> scoreDirector;

    private boolean problemEntitiesChanged = false;

    public DefaultProblemChangeDirector(InnerScoreDirector<Solution_, ?> scoreDirector) {
        this.scoreDirector = scoreDirector;
    }

    @Override
    public <Entity> void addEntity(Entity entity, Consumer<Entity> entityConsumer) {
        Objects.requireNonNull(entity, () -> "Entity (" + entity + ") cannot be null.");
        Objects.requireNonNull(entityConsumer, () -> "Entity consumer (" + entityConsumer + ") cannot be null.");
        scoreDirector.beforeEntityAdded(entity);
        entityConsumer.accept(entity);
        scoreDirector.afterEntityAdded(entity);
        problemEntitiesChanged = true;
    }

    @Override
    public <Entity> void removeEntity(Entity entity, Consumer<Entity> entityConsumer) {
        Objects.requireNonNull(entity, () -> "Entity (" + entity + ") cannot be null.");
        Objects.requireNonNull(entityConsumer, () -> "Entity consumer (" + entityConsumer + ") cannot be null.");
        var workingEntity = lookUpWorkingObject(entity);
        scoreDirector.beforeEntityRemoved(workingEntity);
        entityConsumer.accept(workingEntity);
        scoreDirector.afterEntityRemoved(workingEntity);
        problemEntitiesChanged = true;
    }

    @Override
    public <Entity> void changeVariable(Entity entity, String variableName, Consumer<Entity> entityConsumer) {
        Objects.requireNonNull(entity, () -> "Entity (" + entity + ") cannot be null.");
        Objects.requireNonNull(variableName, () -> "Planning variable name (" + variableName + ") cannot be null.");
        Objects.requireNonNull(entityConsumer, () -> "Entity consumer (" + entityConsumer + ") cannot be null.");
        var workingEntity = lookUpWorkingObject(entity);
        scoreDirector.beforeVariableChanged(workingEntity, variableName);
        entityConsumer.accept(workingEntity);
        scoreDirector.afterVariableChanged(workingEntity, variableName);
    }

    @Override
    public <ProblemFact> void addProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer) {
        Objects.requireNonNull(problemFact, () -> "Problem fact (" + problemFact + ") cannot be null.");
        Objects.requireNonNull(problemFactConsumer,
                () -> "Problem fact consumer (" + problemFactConsumer + ") cannot be null.");
        scoreDirector.beforeProblemFactAdded(problemFact);
        problemFactConsumer.accept(problemFact);
        scoreDirector.afterProblemFactAdded(problemFact);
    }

    @Override
    public <ProblemFact> void removeProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer) {
        Objects.requireNonNull(problemFact, () -> "Problem fact (" + problemFact + ") cannot be null.");
        Objects.requireNonNull(problemFactConsumer,
                () -> "Problem fact consumer (" + problemFactConsumer + ") cannot be null.");
        var workingProblemFact = lookUpWorkingObject(problemFact);
        scoreDirector.beforeProblemFactRemoved(workingProblemFact);
        problemFactConsumer.accept(workingProblemFact);
        scoreDirector.afterProblemFactRemoved(workingProblemFact);
    }

    @Override
    public <EntityOrProblemFact> void changeProblemProperty(EntityOrProblemFact problemFactOrEntity,
            Consumer<EntityOrProblemFact> problemFactOrEntityConsumer) {
        Objects.requireNonNull(problemFactOrEntity,
                () -> "Problem fact or entity (" + problemFactOrEntity + ") cannot be null.");
        Objects.requireNonNull(problemFactOrEntityConsumer,
                () -> "Problem fact or entity consumer (" + problemFactOrEntityConsumer + ") cannot be null.");
        var workingEntityOrProblemFact = lookUpWorkingObject(problemFactOrEntity);
        scoreDirector.beforeProblemPropertyChanged(workingEntityOrProblemFact);
        problemFactOrEntityConsumer.accept(workingEntityOrProblemFact);
        scoreDirector.afterProblemPropertyChanged(workingEntityOrProblemFact);
    }

    @Override
    public <EntityOrProblemFact> @Nullable EntityOrProblemFact
            lookUpWorkingObject(@Nullable EntityOrProblemFact externalObject) {
        return scoreDirector.lookUpWorkingObject(externalObject);
    }

    @Override
    public void updateShadowVariables() {
        if (problemEntitiesChanged) {
            // Need to rebuild the declarative shadow variable graph
            scoreDirector.setWorkingSolution(scoreDirector.getWorkingSolution());
            problemEntitiesChanged = false;
        } else {
            scoreDirector.triggerVariableListeners();
        }
    }

}
