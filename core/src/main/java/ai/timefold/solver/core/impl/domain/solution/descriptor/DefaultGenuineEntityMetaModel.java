package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultGenuineEntityMetaModel<Solution_, Entity_>
        implements GenuineEntityMetaModel<Solution_, Entity_>, InnerPlanningEntityMetaModel<Solution_, Entity_> {

    private final EntityDescriptor<Solution_> entityDescriptor;
    private final PlanningSolutionMetaModel<Solution_> solution;
    private final Class<Entity_> type;
    private final List<VariableMetaModel<Solution_, Entity_, ?>> variables = new ArrayList<>();

    @SuppressWarnings("unchecked")
    DefaultGenuineEntityMetaModel(PlanningSolutionMetaModel<Solution_> solution,
            EntityDescriptor<Solution_> entityDescriptor) {
        this.solution = Objects.requireNonNull(solution);
        this.entityDescriptor = Objects.requireNonNull(entityDescriptor);
        this.type = (Class<Entity_>) entityDescriptor.getEntityClass();
    }

    public EntityDescriptor<Solution_> entityDescriptor() {
        return entityDescriptor;
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> solution() {
        return solution;
    }

    @Override
    public Class<Entity_> type() {
        return type;
    }

    @Override
    public List<VariableMetaModel<Solution_, Entity_, ?>> variables() {
        return Collections.unmodifiableList(variables);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable() {
        var genuineVariables = genuineVariables();
        return switch (genuineVariables.size()) {
            case 0 -> throw new IllegalStateException("The entity class (%s) has no genuine variables."
                    .formatted(type().getCanonicalName()));
            case 1 -> (GenuineVariableMetaModel<Solution_, Entity_, Value_>) genuineVariables.get(0);
            default -> throw new IllegalStateException("The entity class (%s) has multiple genuine variables (%s)."
                    .formatted(type().getCanonicalName(), genuineVariables));
        };
    }

    @Override
    public <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable(String variableName) {
        return castOrFail(variable(variableName), variableName);
    }

    @SuppressWarnings("unchecked")
    private <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_>
            castOrFail(VariableMetaModel<Solution_, Entity_, ?> variable, String variableName) {
        if (!(variable instanceof GenuineVariableMetaModel<Solution_, Entity_, ?> genuineVariable)) {
            throw new IllegalArgumentException(
                    "The variableName (%s) exists among variables (%s) but is not genuine.".formatted(variableName,
                            genuineVariables()));
        }
        return (GenuineVariableMetaModel<Solution_, Entity_, Value_>) genuineVariable;
    }

    @Override
    public <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable(String variableName,
            Class<Value_> variableClass) {
        return castOrFail(variable(variableName, variableClass), variableName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable() {
        var variable = genuineVariable();
        if (variable instanceof PlanningVariableMetaModel<Solution_, Entity_, ?> basicVariableMetaModel) {
            return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) basicVariableMetaModel;
        }
        throw new IllegalArgumentException("The single genuine variable exists but is a list variable, not a basic variable.");
    }

    @Override
    public <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName) {
        return castOrFailBasicVariable(genuineVariable(variableName), variableName);
    }

    @SuppressWarnings("unchecked")
    private <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_>
            castOrFailBasicVariable(GenuineVariableMetaModel<Solution_, Entity_, ?> variable, String variableName) {
        if (!(variable instanceof PlanningVariableMetaModel<Solution_, Entity_, ?> genuineVariable)) {
            throw new IllegalArgumentException(
                    "The variableName (%s) exists among variables (%s) but is a list variable, not a basic variable."
                            .formatted(variableName, genuineVariables()));
        }
        return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) genuineVariable;
    }

    @Override
    public <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName,
            Class<Value_> variableClass) {
        return castOrFailBasicVariable(genuineVariable(variableName, variableClass), variableName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable() {
        var variable = genuineVariable();
        if (variable instanceof PlanningListVariableMetaModel<Solution_, Entity_, ?> listVariableMetaModel) {
            return (PlanningListVariableMetaModel<Solution_, Entity_, Value_>) listVariableMetaModel;
        }
        throw new IllegalArgumentException("The single genuine variable exists but is a basic variable, not a list variable.");
    }

    @Override
    public <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName) {
        return castOrFailListVariable(genuineVariable(variableName), variableName);
    }

    @SuppressWarnings("unchecked")
    private <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_>
            castOrFailListVariable(GenuineVariableMetaModel<Solution_, Entity_, ?> variable, String variableName) {
        if (!(variable instanceof PlanningListVariableMetaModel<Solution_, Entity_, ?> genuineVariable)) {
            throw new IllegalArgumentException(
                    "The variableName (%s) exists among variables (%s) but is a basic variable, not a list variable."
                            .formatted(variableName, genuineVariables()));
        }
        return (PlanningListVariableMetaModel<Solution_, Entity_, Value_>) genuineVariable;
    }

    @Override
    public <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName,
            Class<Value_> variableClass) {
        return castOrFailListVariable(genuineVariable(variableName, variableClass), variableName);
    }

    @Override
    public <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName) {
        return castOrFailListShadow(variable(variableName), variableName);
    }

    @SuppressWarnings("unchecked")
    private <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_>
            castOrFailListShadow(VariableMetaModel<Solution_, Entity_, ?> variable, String variableName) {
        if (!(variable instanceof ShadowVariableMetaModel<Solution_, Entity_, ?> shadowVariableMetaModel)) {
            throw new IllegalArgumentException(
                    "The variableName (%s) exists among variables (%s) but it is a genuine variable, not a shadow."
                            .formatted(variableName, variables()));
        }
        return (ShadowVariableMetaModel<Solution_, Entity_, Value_>) shadowVariableMetaModel;
    }

    @Override
    public <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName,
            Class<Value_> variableClass) {
        return castOrFailListShadow(variable(variableName, variableClass), variableName);
    }

    @Override
    public void addVariable(VariableMetaModel<Solution_, Entity_, ?> variable) {
        if (variable.entity() != this) {
            throw new IllegalArgumentException("The variable (%s) is not part of this entity (%s)."
                    .formatted(variable.name(), type.getSimpleName()));
        }
        variables.add(variable);
    }

    @Override
    public String toString() {
        return "Genuine entity (%s) with variables (%s)"
                .formatted(type.getCanonicalName(),
                        variables().stream()
                                .map(VariableMetaModel::name)
                                .toList());
    }

}
