package ai.timefold.solver.core.impl.move.streams.generic.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.InnerVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractMove<Solution_> implements Move<Solution_> {

    private static final char OPENING_PARENTHESES = '(';
    private static final char CLOSING_PARENTHESES = ')';

    @Override
    public final String describe() {
        var metaModels = getVariableMetaModels();
        var substring = switch (metaModels.size()) {
            case 0 -> "";
            case 1 -> OPENING_PARENTHESES + getVariableDescriptor(metaModels.get(0)).getSimpleEntityAndVariableName()
                    + CLOSING_PARENTHESES;
            default -> {
                var stringBuilder = new StringBuilder()
                        .append(OPENING_PARENTHESES);
                var first = true;
                for (var variableMetaModel : getVariableMetaModels()) {
                    if (first) {
                        first = false;
                    } else {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(getVariableDescriptor(variableMetaModel).getSimpleEntityAndVariableName());
                }
                stringBuilder.append(CLOSING_PARENTHESES);
                yield stringBuilder.toString();
            }
        };
        return getClass().getSimpleName() + substring;
    }

    public abstract String toString();

    protected List<VariableMetaModel<Solution_, ?, ?>> getVariableMetaModels() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    protected static <Solution_> VariableDescriptor<Solution_>
            getVariableDescriptor(VariableMetaModel<Solution_, ?, ?> variableMetaModel) {
        return ((InnerVariableMetaModel<Solution_>) variableMetaModel).variableDescriptor();
    }

    protected static <Solution_> GenuineVariableDescriptor<Solution_>
            getVariableDescriptor(PlanningVariableMetaModel<Solution_, ?, ?> variableMetaModel) {
        return ((DefaultPlanningVariableMetaModel<Solution_, ?, ?>) variableMetaModel).variableDescriptor();
    }

    protected static <Solution_> ListVariableDescriptor<Solution_>
            getVariableDescriptor(PlanningListVariableMetaModel<Solution_, ?, ?> variableMetaModel) {
        return ((DefaultPlanningListVariableMetaModel<Solution_, ?, ?>) variableMetaModel).variableDescriptor();
    }

    public static <E> List<E> rebaseList(List<E> externalObjectList, ScoreDirector<?> destinationScoreDirector) {
        var rebasedObjectList = new ArrayList<E>(externalObjectList.size());
        for (var entity : externalObjectList) {
            rebasedObjectList.add(destinationScoreDirector.lookUpWorkingObject(entity));
        }
        return rebasedObjectList;
    }

    public static <E> Set<E> rebaseSet(Set<E> externalObjectSet, ScoreDirector<?> destinationScoreDirector) {
        var rebasedObjectSet = new LinkedHashSet<E>(externalObjectSet.size());
        for (var entity : externalObjectSet) {
            rebasedObjectSet.add(destinationScoreDirector.lookUpWorkingObject(entity));
        }
        return rebasedObjectSet;
    }

}
