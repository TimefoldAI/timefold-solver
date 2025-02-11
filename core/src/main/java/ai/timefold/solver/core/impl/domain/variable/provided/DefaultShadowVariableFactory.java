package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowCalculationBuilderFactory;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableFactory;
import ai.timefold.solver.core.preview.api.variable.provided.SingleVariableReference;

public class DefaultShadowVariableFactory<Solution_> implements ShadowVariableFactory {
    static final String IDENTITY = "#id";
    static final String FACT = "#fact";
    static final String NEXT = "#next";
    static final String PREVIOUS = "#previous";
    static final String INVERSE = "#inverse";

    final SolutionDescriptor<Solution_> solutionDescriptor;
    final SupplyManager supplyManager;
    final List<ShadowVariableReference<Solution_, ?, ?>> shadowVariableReferenceList;
    final List<DefaultGroupVariableReference<Solution_, ?, ?, ?>> groupVariableReferenceList;
    final Map<String, List<AbstractVariableReference<?, ?>>> shadowVariableToReferencesMap;
    final AtomicInteger groupCounter;

    public DefaultShadowVariableFactory(SolutionDescriptor<Solution_> solutionDescriptor, SupplyManager supplyManager) {
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.shadowVariableReferenceList = new ArrayList<>();
        this.groupVariableReferenceList = new ArrayList<>();
        this.shadowVariableToReferencesMap = new HashMap<>();
        this.groupCounter = new AtomicInteger(0);
    }

    @Override
    public <Entity_> SingleVariableReference<Entity_, Entity_> entity(Class<? extends Entity_> entityClass) {
        return DefaultSingleVariableReference.entity(this, solutionDescriptor, supplyManager, entityClass);
    }

    @Override
    public <Entity_> ShadowCalculationBuilderFactory<Entity_> newShadow(Class<? extends Entity_> entityClass) {
        return new DefaultShadowCalculationBuilderFactory<>(this, solutionDescriptor, supplyManager, entityClass);
    }

    void addShadowVariable(ShadowVariableReference<Solution_, ?, ?> shadowVariableReference) {
        shadowVariableReferenceList.add(shadowVariableReference);
    }

    List<ShadowVariableReference<Solution_, ?, ?>> getShadowVariableReferenceList() {
        return shadowVariableReferenceList;
    }

    public void addShadowVariableReference(String variableName, AbstractVariableReference<?, ?> reference) {
        shadowVariableToReferencesMap.computeIfAbsent(variableName, (ignored) -> new ArrayList<>())
                .add(reference);
    }

    public void addGroupVariableReference(DefaultGroupVariableReference<Solution_, ?, ?, ?> reference) {
        groupVariableReferenceList.add(reference);
    }

    public List<AbstractVariableReference<?, ?>> getShadowVariableReferences(String variableName) {
        return shadowVariableToReferencesMap.computeIfAbsent(variableName, (ignored) -> new ArrayList<>());
    }

    public List<DefaultGroupVariableReference<Solution_, ?, ?, ?>> getGroupVariableReferenceList() {
        return groupVariableReferenceList;
    }

    public int nextGroupId() {
        return groupCounter.getAndIncrement();
    }
}