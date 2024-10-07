package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

public class SingletonListInverseVariableListener<Solution_>
        implements ListVariableListener<Solution_, Object, Object>, SingletonInverseVariableSupply {

    protected final InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    protected final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public SingletonListInverseVariableListener(
            InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        if (sourceVariableDescriptor.supportsPinning()) {
            // Required for variable pinning, otherwise pinned values have their inverse set to null.
            var entityDescriptor = sourceVariableDescriptor.getEntityDescriptor();
            entityDescriptor.getSolutionDescriptor()
                    .visitEntitiesByEntityClass(scoreDirector.getWorkingSolution(), entityDescriptor.getEntityClass(),
                            entity -> {
                                beforeEntityAdded(scoreDirector, entity);
                                afterEntityAdded(scoreDirector, entity);
                                return false;
                            });
        }
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        for (var element : sourceVariableDescriptor.getValue(entity)) {
            setInverse((InnerScoreDirector<Solution_, ?>) scoreDirector, element, entity, null);
        }
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        for (var element : sourceVariableDescriptor.getValue(entity)) {
            setInverse(innerScoreDirector, element, null, entity);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, null);
        innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity,
            int fromIndex, int toIndex) {
        // Do nothing
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity, int fromIndex,
            int toIndex) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        var listVariable = sourceVariableDescriptor.getValue(entity);
        for (var i = fromIndex; i < toIndex; i++) {
            var element = listVariable.get(i);
            if (getInverseSingleton(element) != entity) {
                innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
                shadowVariableDescriptor.setValue(element, entity);
                innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
            }
        }
    }

    private void setInverse(InnerScoreDirector<Solution_, ?> scoreDirector,
            Object element, Object inverseEntity, Object expectedOldInverseEntity) {
        var oldInverseEntity = getInverseSingleton(element);
        if (oldInverseEntity == inverseEntity) {
            return;
        }
        if (scoreDirector.expectShadowVariablesInCorrectState() && oldInverseEntity != expectedOldInverseEntity) {
            throw new IllegalStateException("The entity (" + inverseEntity
                    + ") has a list variable (" + sourceVariableDescriptor.getVariableName()
                    + ") and one of its elements (" + element
                    + ") which has a shadow variable (" + shadowVariableDescriptor.getVariableName()
                    + ") has an oldInverseEntity (" + oldInverseEntity + ") which is not that entity.\n"
                    + "Verify the consistency of your input problem for that shadow variable.");
        }
        scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, inverseEntity);
        scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }
}
