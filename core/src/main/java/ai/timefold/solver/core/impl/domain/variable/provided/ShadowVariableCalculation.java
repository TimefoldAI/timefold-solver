package ai.timefold.solver.core.impl.domain.variable.provided;

import java.lang.reflect.Method;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ShadowVariableCalculation<Solution_, Entity_, Value_> {
    final DefaultShadowVariableFactory<Solution_> shadowVariableFactory;
    @Nullable
    private final ShadowVariableCalculation<Solution_, Entity_, Value_> fallback;
    private final AbstractVariableReference<Solution_, Entity_, ?>[] inputs;
    private final Method calculatorMethod;
    private final Object calculator;

    public ShadowVariableCalculation(DefaultShadowVariableFactory<Solution_> shadowVariableFactory,
            AbstractVariableReference<Solution_, Entity_, ?>[] inputs, Method calculatorMethod,
            Object calculator) {
        this.shadowVariableFactory = shadowVariableFactory;
        this.fallback = null;
        this.inputs = inputs;
        this.calculatorMethod = calculatorMethod;
        this.calculator = calculator;
        for (var input : inputs) {
            input.addReferences(shadowVariableFactory);
        }
    }

    public ShadowVariableCalculation(@NonNull ShadowVariableCalculation<Solution_, Entity_, Value_> fallback,
            AbstractVariableReference<Solution_, Entity_, ?>[] inputs, Method calculatorMethod, Object calculator) {
        this.shadowVariableFactory = fallback.shadowVariableFactory;
        this.fallback = fallback;
        this.inputs = inputs;
        this.calculatorMethod = calculatorMethod;
        this.calculator = calculator;
    }

    public ShadowVariableCalculation<Solution_, Entity_, Value_>
            withFallback(ShadowVariableCalculation<Solution_, Entity_, Value_> fallback) {
        return new ShadowVariableCalculation<>(fallback, inputs, calculatorMethod, calculator);
    }

    public Value_ calculate(Entity_ entity) {
        try {
            var inputValues = new Object[inputs.length + 1];
            inputValues[0] = entity;
            for (int i = 1; i < inputs.length + 1; i++) {
                inputValues[i] = inputs[i - 1].getValue(entity);
                if (inputValues[i] == null) {
                    if (fallback != null) {
                        return fallback.calculate(entity);
                    } else {
                        return null;
                    }
                }
            }
            return (Value_) calculatorMethod.invoke(calculator, inputValues);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void visitGraph(VariableReferenceGraph<Solution_> graph) {
        for (var input : inputs) {
            input.processVariableReference(graph);
        }
        if (fallback != null) {
            fallback.visitGraph(graph);
        }
    }

    public void visitEntity(ShadowVariableReference<Solution_, Entity_, Value_> shadowVariable,
            VariableReferenceGraph<Solution_> graph,
            Object entity) {
        var shadowVariableId = shadowVariable.getVariableId();
        if (!shadowVariableId.entityClass().isInstance(entity)) {
            return;
        }
        for (var input : inputs) {
            input.processObject(graph, entity);
            var inputVariableId = input.getVariableId();
            graph.addFixedEdge(graph.addVariableReferenceEntity(inputVariableId, entity, input),
                    graph.addVariableReferenceEntity(shadowVariableId, entity, shadowVariable));
        }
        if (fallback != null) {
            fallback.visitEntity(shadowVariable, graph, entity);
        } else {
            graph.addFixedEdge(graph.addVariableReferenceEntity(VariableId.entity(shadowVariableId.entityClass()), entity,
                    DefaultSingleVariableReference.entity(shadowVariableFactory,
                            shadowVariable.solutionDescriptor,
                            shadowVariable.supplyManager,
                            shadowVariable.getVariableId().entityClass())),
                    graph.addVariableReferenceEntity(shadowVariableId, entity, shadowVariable));
        }
    }
}
