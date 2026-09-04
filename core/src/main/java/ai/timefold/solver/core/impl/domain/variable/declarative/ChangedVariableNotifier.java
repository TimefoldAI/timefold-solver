package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.CollectionInverseVariableState;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.Nullable;

public record ChangedVariableNotifier<Solution_>(BiConsumer<VariableDescriptor<Solution_>, Object> beforeVariableChanged,
        BiConsumer<VariableDescriptor<Solution_>, Object> afterVariableChanged,
        @Nullable InnerScoreDirector<Solution_, ?> innerScoreDirector) {

    private static final ChangedVariableNotifier<?> EMPTY = new ChangedVariableNotifier<>((a, b) -> {
    },
            (a, b) -> {
            },
            null);

    public CollectionInverseVariableState getCollectionInverseVariableSupply(VariableMetaModel<?, ?, ?> variableMetaModel) {
        if (innerScoreDirector == null) {
            return new CollectionInverseVariableState() {
                @Override
                public <Entity_> Collection<Entity_> getInverseCollection(Object planningValue) {
                    return Collections.emptyList();
                }
            };
        } else {
            var solutionDescriptor = innerScoreDirector.getSolutionDescriptor();
            var variableDescriptor = solutionDescriptor.getEntityDescriptorStrict(variableMetaModel.entity().type())
                    .getVariableDescriptor(variableMetaModel.name());
            return innerScoreDirector.getSupplyManager().demand(new BasicVariableStateDemand<>(variableDescriptor));
        }
    }

    public @Nullable Solution_ getWorkingSolution() {
        return innerScoreDirector != null ? innerScoreDirector.getWorkingSolution() : null;
    }

    @SuppressWarnings("unchecked")
    public static <Solution_> ChangedVariableNotifier<Solution_> empty() {
        return (ChangedVariableNotifier<Solution_>) EMPTY;
    }

    public static <Solution_> ChangedVariableNotifier<Solution_> of(InnerScoreDirector<Solution_, ?> scoreDirector) {
        return new ChangedVariableNotifier<>(
                scoreDirector::beforeVariableChanged,
                scoreDirector::afterVariableChanged,
                scoreDirector);
    }

}
