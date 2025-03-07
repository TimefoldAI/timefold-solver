package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowCalculationBuilderFactory;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableFactory;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.SingleVariableReference;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultShadowVariableFactory<Solution_> implements ShadowVariableFactory {
    static final String IDENTITY = "#id";
    static final String FACT = "#fact";
    static final String NEXT = "#next";
    static final String PREVIOUS = "#previous";
    static final String INVERSE = "#inverse";
    static final String INTERMEDIATE_PREFIX = "#intermediate";

    final SolutionDescriptor<Solution_> solutionDescriptor;
    final SupplyManager supplyManager;
    final List<AbstractShadowVariableReference<Solution_, ?, ?>> shadowVariableReferenceList;
    final List<DefaultGroupVariableReference<Solution_, ?, ?, ?>> groupVariableReferenceList;
    final Map<String, List<InnerVariableReference<Solution_, ?, ?>>> shadowVariableToReferencesMap;
    final Map<String, IdentityHashMap<Object, Object>> intermediateNameToValueMap;
    final AtomicInteger groupCounter;

    public DefaultShadowVariableFactory(SolutionDescriptor<Solution_> solutionDescriptor, SupplyManager supplyManager) {
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.shadowVariableReferenceList = new ArrayList<>();
        this.groupVariableReferenceList = new ArrayList<>();
        this.shadowVariableToReferencesMap = new HashMap<>();
        this.intermediateNameToValueMap = new HashMap<>();
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

    void addShadowVariable(AbstractShadowVariableReference<Solution_, ?, ?> shadowVariableReference) {
        shadowVariableReferenceList.add(shadowVariableReference);
    }

    List<AbstractShadowVariableReference<Solution_, ?, ?>> getShadowVariableReferenceList() {
        return shadowVariableReferenceList;
    }

    public void addShadowVariableReference(String variableName, InnerVariableReference<Solution_, ?, ?> reference) {
        shadowVariableToReferencesMap.computeIfAbsent(variableName, (ignored) -> new ArrayList<>())
                .add(reference);
    }

    public void addGroupVariableReference(DefaultGroupVariableReference<Solution_, ?, ?, ?> reference) {
        groupVariableReferenceList.add(reference);
    }

    public List<InnerVariableReference<Solution_, ?, ?>> getShadowVariableReferences(String variableName) {
        return shadowVariableToReferencesMap.computeIfAbsent(variableName, (ignored) -> new ArrayList<>());
    }

    public List<InnerVariableReference<Solution_, ?, ?>> getIntermediateShadowVariableReferences(String variableName) {
        return shadowVariableToReferencesMap.computeIfAbsent(getIntermediateVariableName(variableName),
                (ignored) -> new ArrayList<>());
    }

    public List<DefaultGroupVariableReference<Solution_, ?, ?, ?>> getGroupVariableReferenceList() {
        return groupVariableReferenceList;
    }

    public int nextGroupId() {
        return groupCounter.getAndIncrement();
    }

    @SuppressWarnings("unchecked")
    public <Entity_, Value_> IdentityHashMap<Entity_, Value_> getIntermediateValueMap(String intermediateName) {
        return (IdentityHashMap<Entity_, Value_>) intermediateNameToValueMap.computeIfAbsent(intermediateName,
                ignored -> new IdentityHashMap<>());
    }

    public static String getIntermediateVariableName(String intermediateName) {
        return INTERMEDIATE_PREFIX + "_" + intermediateName;
    }
}