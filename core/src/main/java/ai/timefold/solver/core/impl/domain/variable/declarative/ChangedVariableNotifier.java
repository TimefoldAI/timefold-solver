package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collections;
import java.util.function.BiConsumer;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.CollectionInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.CollectionInverseVariableSupply;
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

    public CollectionInverseVariableSupply getCollectionInverseVariableSupply(VariableMetaModel<?, ?, ?> variableMetaModel) {
        if (innerScoreDirector == null) {
            return entity -> Collections.emptyList();
        } else {
            var solutionDescriptor = innerScoreDirector.getSolutionDescriptor();
            var variableDescriptor = solutionDescriptor.getEntityDescriptorStrict(variableMetaModel.entity().type())
                    .getVariableDescriptor(variableMetaModel.name());
            return innerScoreDirector.getSupplyManager().demand(new CollectionInverseVariableDemand<>(variableDescriptor));
        }
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
