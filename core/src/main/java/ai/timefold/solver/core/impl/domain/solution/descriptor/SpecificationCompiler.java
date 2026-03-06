package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder.DESCENDING;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification;
import ai.timefold.solver.core.api.domain.specification.EntitySpecification;
import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.domain.specification.ShadowSpecification;
import ai.timefold.solver.core.api.domain.specification.ValueRangeSpecification;
import ai.timefold.solver.core.api.domain.specification.VariableSpecification;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.common.LookupStrategyResolver;
import ai.timefold.solver.core.impl.domain.common.accessor.LambdaMemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.OverridesBasedConstraintWeightSupplier;
import ai.timefold.solver.core.impl.domain.solution.cloner.LambdaBasedSolutionCloner;
import ai.timefold.solver.core.impl.domain.variable.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ShadowVariablesInconsistentVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;

/**
 * Converts a {@link PlanningSpecification} into a fully initialized {@link SolutionDescriptor}
 * without calling {@code processAnnotations()}.
 * <p>
 * This class is placed in the {@code SolutionDescriptor}'s package to access package-private members.
 */
public final class SpecificationCompiler {

    private SpecificationCompiler() {
    }

    /**
     * Compile a specification from the programmatic API (no annotations).
     */
    @SuppressWarnings("unchecked")
    public static <Solution_> SolutionDescriptor<Solution_> compile(
            PlanningSpecification<Solution_> spec,
            Set<PreviewFeature> enabledPreviewFeatureSet) {
        return compile(spec, enabledPreviewFeatureSet, DomainAccessType.FORCE_REFLECTION,
                Collections.emptyMap(), false);
    }

    /**
     * Compile a specification, optionally delegating entity-level processing to annotations.
     *
     * @param annotationBasedEntities if true, entity descriptors will use {@code processAnnotations()}
     *        instead of building from specification data. Used when the specification was built from annotations.
     */
    @SuppressWarnings("unchecked")
    public static <Solution_> SolutionDescriptor<Solution_> compile(
            PlanningSpecification<Solution_> spec,
            Set<PreviewFeature> enabledPreviewFeatureSet,
            DomainAccessType domainAccessType,
            Map<String, MemberAccessor> memberAccessorMap,
            boolean annotationBasedEntities) {

        memberAccessorMap = memberAccessorMap != null ? memberAccessorMap : Collections.emptyMap();

        // 1. Create SolutionDescriptor shell
        var solutionDescriptor = new SolutionDescriptor<>(spec.solutionClass(), memberAccessorMap);

        // 2. Create and configure DescriptorPolicy
        var descriptorPolicy = new DescriptorPolicy();
        if (enabledPreviewFeatureSet != null) {
            descriptorPolicy.setEnabledPreviewFeatureSet(enabledPreviewFeatureSet);
        }
        descriptorPolicy.setDomainAccessType(domainAccessType);
        descriptorPolicy.setMemberAccessorFactory(solutionDescriptor.getMemberAccessorFactory());
        solutionDescriptor.setDomainAccessType(domainAccessType);
        solutionDescriptor.setLookUpStrategyResolver(new LookupStrategyResolver(descriptorPolicy));

        // 3. Build score descriptor (with bendable score support)
        var scoreSpec = spec.score();
        var scoreMemberAccessor = new LambdaMemberAccessor(
                "score", spec.solutionClass(),
                (Class<?>) scoreSpec.scoreType(), scoreSpec.scoreType(),
                scoreSpec.getter(), scoreSpec.setter());
        var scoreDescriptor = descriptorPolicy.buildScoreDescriptorFromType(
                scoreMemberAccessor, (Class<? extends Score>) scoreSpec.scoreType(),
                scoreSpec.bendableHardLevelsSize(), scoreSpec.bendableSoftLevelsSize());
        solutionDescriptor.setScoreDescriptor(scoreDescriptor);

        // 4. Register problem fact accessors
        for (var factSpec : spec.facts()) {
            Type factGenericType = factSpec.genericType();
            Class<?> factReturnType;
            if (factSpec.isCollection()) {
                if (factGenericType instanceof ParameterizedType pt) {
                    factReturnType = (Class<?>) pt.getRawType();
                } else {
                    factReturnType = Collection.class;
                    if (factGenericType == null) {
                        factGenericType = new SyntheticParameterizedType(Collection.class,
                                new Type[] { Object.class });
                    }
                }
            } else {
                factReturnType = factGenericType instanceof Class<?> cls ? cls : Object.class;
            }
            var factAccessor = new LambdaMemberAccessor(
                    factSpec.name(), spec.solutionClass(),
                    factReturnType,
                    factGenericType,
                    factSpec.getter(), null);
            if (factSpec.isCollection()) {
                solutionDescriptor.getProblemFactCollectionMemberAccessorMap()
                        .put(factSpec.name(), factAccessor);
            } else {
                solutionDescriptor.getProblemFactMemberAccessorMap()
                        .put(factSpec.name(), factAccessor);
            }
        }

        // 5. Register entity collection/member accessors
        for (var ecSpec : spec.entityCollections()) {
            if (ecSpec.isSingular()) {
                // Singular @PlanningEntityProperty goes into entityMemberAccessorMap
                // The spec getter wraps the entity in List.of(), so unwrap it for the raw accessor
                var collectionGetter = ecSpec.getter();
                java.util.function.Function<Object, Object> rawGetter = solution -> {
                    var collection = collectionGetter.apply((Solution_) solution);
                    return (collection != null && !collection.isEmpty()) ? collection.iterator().next() : null;
                };
                var ecAccessor = new LambdaMemberAccessor(
                        ecSpec.name(), spec.solutionClass(),
                        Object.class, null,
                        rawGetter, null);
                solutionDescriptor.getEntityMemberAccessorMap()
                        .put(ecSpec.name(), ecAccessor);
            } else {
                var ecAccessor = new LambdaMemberAccessor(
                        ecSpec.name(), spec.solutionClass(),
                        Collection.class, null,
                        ecSpec.getter(), null);
                solutionDescriptor.getEntityCollectionMemberAccessorMap()
                        .put(ecSpec.name(), ecAccessor);
            }
        }

        // 6. Register constraint weight overrides
        if (spec.constraintWeights() != null) {
            var cwAccessor = new LambdaMemberAccessor(
                    "constraintWeightOverrides", spec.solutionClass(),
                    ConstraintWeightOverrides.class, null,
                    spec.constraintWeights().getter(), null);
            solutionDescriptor.setConstraintWeightSupplier(
                    OverridesBasedConstraintWeightSupplier.create(solutionDescriptor, cwAccessor));
        }

        // 7. Register value range providers in the policy (solution-level only for annotation path)
        for (var vrSpec : spec.valueRanges()) {
            if (!vrSpec.isEntityScoped()) {
                var vrAccessor = createValueRangeAccessor(vrSpec, spec.solutionClass());
                descriptorPolicy.addFromSolutionValueRangeProvider(vrSpec.id(), vrAccessor);
            }
        }

        if (annotationBasedEntities) {
            // For annotation-based entities, delegate to EntityDescriptor.processAnnotations()
            compileWithAnnotationBasedEntities(spec, solutionDescriptor, descriptorPolicy);
        } else {
            // For programmatic entities, build from specification data
            compileWithSpecificationEntities(spec, solutionDescriptor, descriptorPolicy);
        }

        // Build cloner
        buildAndSetCloner(spec, solutionDescriptor);

        return solutionDescriptor;
    }

    /**
     * Annotation-based entity compilation: entities process their own annotations.
     * Solution-level concerns come from the specification.
     */
    private static <Solution_> void compileWithAnnotationBasedEntities(
            PlanningSpecification<Solution_> spec,
            SolutionDescriptor<Solution_> solutionDescriptor,
            DescriptorPolicy descriptorPolicy) {

        // Build entity class list from the spec
        var entityClassList = new ArrayList<Class<?>>();
        for (var entitySpec : spec.entities()) {
            entityClassList.add(entitySpec.entityClass());
        }

        // Let each entity descriptor process its own annotations
        for (var entityClass : entityClassList) {
            var entityDescriptor = descriptorPolicy.buildEntityDescriptor(solutionDescriptor, entityClass);
            entityDescriptor.processAnnotations(descriptorPolicy);
        }

        // Link descriptors (same as afterAnnotationsProcessed in SolutionDescriptor)
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            entityDescriptor.linkEntityDescriptors(descriptorPolicy);
        }
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            entityDescriptor.linkVariableDescriptors(descriptorPolicy);
        }

        solutionDescriptor.determineGlobalShadowOrder();

        var problemFactOrEntityClassSet = collectProblemFactOrEntityClasses(solutionDescriptor);
        solutionDescriptor.setProblemFactOrEntityClassSet(problemFactOrEntityClassSet);

        var listVarDescriptors = findListVariableDescriptors(solutionDescriptor);
        solutionDescriptor.setListVariableDescriptorList(listVarDescriptors);
        if (listVarDescriptors.size() > 1) {
            throw new UnsupportedOperationException(
                    "Defining multiple list variables (%s) across the model is currently not supported."
                            .formatted(listVarDescriptors));
        }

        // Initialize constraint weight supplier if needed
        if (solutionDescriptor.getConstraintWeightSupplier() != null) {
            solutionDescriptor.getConstraintWeightSupplier().initialize(solutionDescriptor,
                    descriptorPolicy.getMemberAccessorFactory(), descriptorPolicy.getDomainAccessType());
        }
    }

    /**
     * Programmatic entity compilation: entities built from specification data.
     */
    private static <Solution_> void compileWithSpecificationEntities(
            PlanningSpecification<Solution_> spec,
            SolutionDescriptor<Solution_> solutionDescriptor,
            DescriptorPolicy descriptorPolicy) {

        // Register entity-scoped value ranges
        for (var vrSpec : spec.valueRanges()) {
            if (vrSpec.isEntityScoped()) {
                var vrAccessor = createValueRangeAccessor(vrSpec, vrSpec.ownerClass());
                descriptorPolicy.addFromEntityValueRangeProvider(vrSpec.id(), vrAccessor);
            }
        }

        // Create entity descriptors
        for (var entitySpec : spec.entities()) {
            int variableOrdinal = 0;
            var entityDescriptor = descriptorPolicy.buildEntityDescriptor(
                    solutionDescriptor, entitySpec.entityClass());
            entityDescriptor.initializeVariableMaps();

            // Register entity-scoped value range providers
            for (var vrSpec : entitySpec.entityScopedValueRanges()) {
                var vrAccessor = createValueRangeAccessor(vrSpec, entitySpec.entityClass());
                descriptorPolicy.addFromEntityValueRangeProvider(vrSpec.id(), vrAccessor);
            }

            // Create genuine variable descriptors
            for (var varSpec : entitySpec.variables()) {
                variableOrdinal = createGenuineVariable(
                        entityDescriptor, descriptorPolicy, varSpec, variableOrdinal);
            }

            // Create shadow variable descriptors
            var cascadingGroupMap = new java.util.HashMap<String, CascadingUpdateShadowVariableDescriptor<Solution_>>();
            for (var shadowSpec : entitySpec.shadows()) {
                variableOrdinal = createShadowVariable(
                        entityDescriptor, descriptorPolicy, shadowSpec, variableOrdinal, cascadingGroupMap);
            }

            // Set up pinning
            if (entitySpec.pinnedPredicate() != null) {
                var predicate = (java.util.function.Predicate<Object>) entitySpec.pinnedPredicate();
                entityDescriptor.addPinnedPredicate(predicate);
            }

            // Set up pin-to-index
            if (entitySpec.pinToIndexFunction() != null) {
                var pinToIndex = (java.util.function.ToIntFunction<Object>) entitySpec.pinToIndexFunction();
                entityDescriptor.setPlanningPinToIndexReader(pinToIndex::applyAsInt);
            }

            // Set up difficulty sorting
            if (entitySpec.difficultyComparator() != null) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                var sorter = new ComparatorSelectionSorter<Solution_, Object>(
                        (java.util.Comparator) entitySpec.difficultyComparator(), DESCENDING);
                entityDescriptor.setDescendingSorter(sorter);
            } else if (entitySpec.difficultyComparatorFactoryClass() != null) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                var factory = (ComparatorFactory<Solution_, Object>) ConfigUtils.newInstance(
                        () -> entitySpec.entityClass().toString(), "comparatorFactoryClass",
                        (Class) entitySpec.difficultyComparatorFactoryClass());
                entityDescriptor.setDescendingSorter(
                        new ComparatorFactorySelectionSorter<>(factory, DESCENDING));
            }

            // Register planning ID accessor
            if (entitySpec.planningIdGetter() != null) {
                var idAccessor = new LambdaMemberAccessor(
                        "planningId", entitySpec.entityClass(),
                        Comparable.class, null,
                        entitySpec.planningIdGetter(), null);
                solutionDescriptor.getPlanningIdMemberAccessorMap()
                        .put(entitySpec.entityClass(), idAccessor);
            }
        }

        // Link entity descriptors (resolves inheritance)
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            entityDescriptor.linkEntityDescriptors(descriptorPolicy);
        }

        // Process value range refs for each genuine variable
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            for (var variableDescriptor : entityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                var entitySpec = findEntitySpec(spec, entityDescriptor.getEntityClass());
                if (entitySpec != null) {
                    var varSpec = findVariableSpec(entitySpec, variableDescriptor.getVariableName());
                    if (varSpec != null) {
                        // Always call processValueRangeRefsFromSpecification — when refs are empty,
                        // it falls through to anonymous matching by type.
                        variableDescriptor.processValueRangeRefsFromSpecification(
                                descriptorPolicy, varSpec.valueRangeRefs().toArray(new String[0]));
                    }
                }
            }
        }

        // Link shadow variable sources
        linkShadowVariables(solutionDescriptor, spec);

        // For declarative shadows, call linkVariableDescriptors to resolve source paths
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            for (var shadowDescriptor : entityDescriptor.getDeclaredShadowVariableDescriptors()) {
                if (shadowDescriptor instanceof DeclarativeShadowVariableDescriptor<Solution_>) {
                    shadowDescriptor.linkVariableDescriptors(descriptorPolicy);
                } else if (shadowDescriptor instanceof CascadingUpdateShadowVariableDescriptor<Solution_> cascading) {
                    cascading.completeTargetLinking();
                }
            }
        }

        solutionDescriptor.determineGlobalShadowOrder();

        var problemFactOrEntityClassSet = collectProblemFactOrEntityClasses(solutionDescriptor);
        solutionDescriptor.setProblemFactOrEntityClassSet(problemFactOrEntityClassSet);

        var listVarDescriptors = findListVariableDescriptors(solutionDescriptor);
        solutionDescriptor.setListVariableDescriptorList(listVarDescriptors);
        if (listVarDescriptors.size() > 1) {
            throw new UnsupportedOperationException(
                    "Defining multiple list variables (%s) across the model is currently not supported."
                            .formatted(listVarDescriptors));
        }
    }

    private static <Solution_> SequencedSet<Class<?>> collectProblemFactOrEntityClasses(
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var entityClassStream = solutionDescriptor.getEntityDescriptors().stream()
                .map(EntityDescriptor::getEntityClass);
        var factClassStream = solutionDescriptor.getProblemFactMemberAccessorMap().values().stream()
                .map(MemberAccessor::getType);
        var factCollectionClassStream = solutionDescriptor.getProblemFactCollectionMemberAccessorMap().values().stream()
                .map(accessor -> {
                    var genericType = accessor.getGenericType();
                    if (genericType instanceof ParameterizedType paramType) {
                        var typeArgs = paramType.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementType) {
                            return elementType;
                        }
                    }
                    return (Class<?>) Object.class;
                });

        var constraintWeightClassStream = solutionDescriptor.getConstraintWeightSupplier() != null
                ? Stream.of(solutionDescriptor.getConstraintWeightSupplier().getProblemFactClass())
                : Stream.<Class<?>> empty();

        return Stream.of(entityClassStream, factClassStream, factCollectionClassStream, constraintWeightClassStream)
                .flatMap(s -> s)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <Solution_> List<ListVariableDescriptor<Solution_>> findListVariableDescriptors(
            SolutionDescriptor<Solution_> solutionDescriptor) {
        var listVarDescriptors = new ArrayList<ListVariableDescriptor<Solution_>>();
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            for (var varDesc : entityDescriptor.getGenuineVariableDescriptorList()) {
                if (varDesc instanceof ListVariableDescriptor<Solution_> listVarDesc) {
                    listVarDescriptors.add(listVarDesc);
                }
            }
        }
        return listVarDescriptors;
    }

    @SuppressWarnings("unchecked")
    private static <Solution_> void buildAndSetCloner(
            PlanningSpecification<Solution_> spec,
            SolutionDescriptor<Solution_> solutionDescriptor) {
        if (spec.cloning() != null && spec.cloning().customCloner() != null) {
            solutionDescriptor.setSolutionCloner(spec.cloning().customCloner());
        } else if (spec.cloning() != null) {
            // solutionFactory may be null for interface/abstract solution classes;
            // LambdaBasedSolutionCloner handles this by falling back to runtime class instantiation.
            solutionDescriptor.setSolutionCloner(new LambdaBasedSolutionCloner<>(spec.cloning()));
        } else {
            // No cloning configuration — build a minimal one from entity classes.
            // LambdaBasedSolutionCloner will use runtime reflection for all field access.
            Set<Class<?>> entityClasses = spec.entities().stream()
                    .map(e -> (Class<?>) e.entityClass())
                    .collect(Collectors.toSet());
            var minimalCloning = new CloningSpecification<Solution_>(
                    null, List.of(), Map.of(), entityClasses, Set.of(), null);
            solutionDescriptor.setSolutionCloner(new LambdaBasedSolutionCloner<>(minimalCloning));
        }
    }

    private static <Solution_> MemberAccessor createValueRangeAccessor(
            ValueRangeSpecification<Solution_> vrSpec, Class<?> ownerClass) {
        Type genericType = vrSpec.genericReturnType() != null
                ? vrSpec.genericReturnType()
                : new SyntheticParameterizedType(Collection.class, new Type[] { Object.class });
        Class<?> returnType;
        if (genericType instanceof ParameterizedType pt) {
            returnType = (Class<?>) pt.getRawType();
        } else if (genericType instanceof Class<?> cls) {
            returnType = cls;
        } else {
            returnType = Collection.class;
        }
        return new LambdaMemberAccessor(
                vrSpec.id() != null ? vrSpec.id() : "anonymousValueRange",
                ownerClass,
                returnType,
                genericType,
                vrSpec.getter(), null);
    }

    @SuppressWarnings("unchecked")
    private static <Solution_> int createGenuineVariable(
            EntityDescriptor<Solution_> entityDescriptor,
            DescriptorPolicy descriptorPolicy,
            VariableSpecification<Solution_> varSpec,
            int ordinal) {
        Type genericType;
        Class<?> accessorType;
        if (varSpec.isList()) {
            accessorType = List.class;
            genericType = new SyntheticParameterizedType(List.class, new Type[] { varSpec.valueType() });
        } else {
            accessorType = varSpec.valueType();
            genericType = varSpec.valueType();
        }

        var accessor = new LambdaMemberAccessor(
                varSpec.name(), entityDescriptor.getEntityClass(),
                accessorType, genericType,
                varSpec.getter(), varSpec.setter());

        GenuineVariableDescriptor<Solution_> variableDescriptor;
        if (varSpec.isList()) {
            var listDesc = new ListVariableDescriptor<Solution_>(ordinal, entityDescriptor, accessor);
            listDesc.setAllowsUnassignedValues(varSpec.allowsUnassigned());
            variableDescriptor = listDesc;
        } else {
            var basicDesc = new BasicVariableDescriptor<Solution_>(ordinal, entityDescriptor, accessor);
            basicDesc.setAllowsUnassigned(varSpec.allowsUnassigned());
            variableDescriptor = basicDesc;
        }
        if (varSpec.strengthComparator() != null && varSpec.strengthComparatorFactoryClass() != null) {
            throw new IllegalStateException(
                    "The entityClass (%s) property (%s) cannot have a comparatorClass (%s) and a comparatorFactoryClass (%s) at the same time."
                            .formatted(entityDescriptor.getEntityClass(), varSpec.name(),
                                    varSpec.strengthComparator().getClass().getName(),
                                    varSpec.strengthComparatorFactoryClass().getName()));
        }
        if (varSpec.strengthComparator() != null) {
            variableDescriptor.setStrengthSorting(varSpec.strengthComparator());
        } else if (varSpec.strengthComparatorFactoryClass() != null) {
            variableDescriptor.setStrengthSortingFromFactory(varSpec.strengthComparatorFactoryClass());
        }
        entityDescriptor.addGenuineVariableDescriptor(variableDescriptor);
        return ordinal + 1;
    }

    @SuppressWarnings("unchecked")
    private static <Solution_> int createShadowVariable(
            EntityDescriptor<Solution_> entityDescriptor,
            DescriptorPolicy descriptorPolicy,
            ShadowSpecification<Solution_> shadowSpec,
            int ordinal,
            Map<String, CascadingUpdateShadowVariableDescriptor<Solution_>> cascadingGroupMap) {

        var accessor = new LambdaMemberAccessor(
                shadowSpec.name(), entityDescriptor.getEntityClass(),
                shadowSpec.type(), shadowSpec.type(),
                shadowSpec.getter(), shadowSpec.setter());

        ShadowVariableDescriptor<Solution_> shadowDescriptor;

        switch (shadowSpec) {
            case ShadowSpecification.InverseRelation<Solution_> inv -> {
                shadowDescriptor = new InverseRelationShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
            }
            case ShadowSpecification.Index<Solution_> idx -> {
                shadowDescriptor = new IndexShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
            }
            case ShadowSpecification.PreviousElement<Solution_> prev -> {
                shadowDescriptor = new PreviousElementShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
            }
            case ShadowSpecification.NextElement<Solution_> next -> {
                shadowDescriptor = new NextElementShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
            }
            case ShadowSpecification.Declarative<Solution_> decl -> {
                var declDesc = new DeclarativeShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
                MemberAccessor calculatorAccessor;
                if (decl.supplierMethod() != null) {
                    // Annotation-based path: use the actual method to support both 0-param and 1-param suppliers
                    calculatorAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(
                            decl.supplierMethod(),
                            ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                            null, descriptorPolicy.getDomainAccessType());
                } else {
                    // Programmatic path: use the lambda directly
                    calculatorAccessor = new LambdaMemberAccessor(
                            shadowSpec.name() + "Calculator", entityDescriptor.getEntityClass(),
                            shadowSpec.type(), shadowSpec.type(),
                            decl.supplier(), null);
                }
                declDesc.setSpecificationData(
                        calculatorAccessor,
                        decl.sourcePaths().toArray(new String[0]),
                        decl.alignmentKey());
                shadowDescriptor = declDesc;
            }
            case ShadowSpecification.CascadingUpdate<Solution_> casc -> {
                // Check if a CascadingUpdate descriptor with the same targetMethodName already exists
                if (casc.targetMethodName() != null
                        && cascadingGroupMap != null
                        && cascadingGroupMap.containsKey(casc.targetMethodName())) {
                    var existingCascading = cascadingGroupMap.get(casc.targetMethodName());
                    existingCascading.addTargetVariable(entityDescriptor, accessor);
                    // Register in shadow map only (not cascading map) — secondary target
                    var cascDesc = new CascadingUpdateShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
                    entityDescriptor.addShadowVariableDescriptor(cascDesc, false);
                    return ordinal + 1;
                }
                var cascDesc = new CascadingUpdateShadowVariableDescriptor<>(ordinal, entityDescriptor, accessor);
                if (casc.updateMethod() != null) {
                    var updateAccessor = new LambdaMemberAccessor(
                            shadowSpec.name() + "Update", entityDescriptor.getEntityClass(),
                            void.class, void.class,
                            entity -> {
                                ((java.util.function.Consumer<Object>) casc.updateMethod()).accept(entity);
                                return null;
                            }, null);
                    cascDesc.setTargetMethod(updateAccessor);
                }
                if (casc.targetMethodName() != null && cascadingGroupMap != null) {
                    cascadingGroupMap.put(casc.targetMethodName(), cascDesc);
                }
                shadowDescriptor = cascDesc;
            }
            case ShadowSpecification.Inconsistent<Solution_> inc -> {
                shadowDescriptor = new ShadowVariablesInconsistentVariableDescriptor<>(ordinal, entityDescriptor, accessor);
            }
        }

        entityDescriptor.addShadowVariableDescriptor(shadowDescriptor);
        return ordinal + 1;
    }

    private static <Solution_> void linkShadowVariables(
            SolutionDescriptor<Solution_> solutionDescriptor,
            PlanningSpecification<Solution_> spec) {

        for (var entitySpec : spec.entities()) {
            var entityDescriptor = solutionDescriptor.findEntityDescriptor(entitySpec.entityClass());
            if (entityDescriptor == null) {
                continue;
            }

            for (var shadowSpec : entitySpec.shadows()) {
                var shadowDescriptor = entityDescriptor.getShadowVariableDescriptor(shadowSpec.name());
                if (shadowDescriptor == null) {
                    continue;
                }

                switch (shadowSpec) {
                    case ShadowSpecification.InverseRelation<Solution_> inv -> {
                        var sourceVar = findVariableDescriptorByName(
                                solutionDescriptor, inv.sourceVariableName());
                        if (sourceVar != null) {
                            ((InverseRelationShadowVariableDescriptor<Solution_>) shadowDescriptor)
                                    .linkSourceVariable(sourceVar);
                        }
                    }
                    case ShadowSpecification.Index<Solution_> idx -> {
                        var sourceVar = findListVariableDescriptorByName(
                                solutionDescriptor, idx.sourceVariableName());
                        if (sourceVar != null) {
                            ((IndexShadowVariableDescriptor<Solution_>) shadowDescriptor)
                                    .linkSourceVariable(sourceVar);
                        }
                    }
                    case ShadowSpecification.PreviousElement<Solution_> prev -> {
                        var sourceVar = findListVariableDescriptorByName(
                                solutionDescriptor, prev.sourceVariableName());
                        if (sourceVar != null) {
                            ((PreviousElementShadowVariableDescriptor<Solution_>) shadowDescriptor)
                                    .linkSourceVariable(sourceVar);
                        }
                    }
                    case ShadowSpecification.NextElement<Solution_> next -> {
                        var sourceVar = findListVariableDescriptorByName(
                                solutionDescriptor, next.sourceVariableName());
                        if (sourceVar != null) {
                            ((NextElementShadowVariableDescriptor<Solution_>) shadowDescriptor)
                                    .linkSourceVariable(sourceVar);
                        }
                    }
                    case ShadowSpecification.Declarative<Solution_> decl -> {
                        // Declarative shadows are linked via linkVariableDescriptors() later
                    }
                    case ShadowSpecification.CascadingUpdate<Solution_> casc -> {
                        // Cascading updates are linked via completeTargetLinking() later
                    }
                    case ShadowSpecification.Inconsistent<Solution_> inc -> {
                        // No linking needed
                    }
                }
            }
        }
    }

    private static <Solution_> VariableDescriptor<Solution_> findVariableDescriptorByName(
            SolutionDescriptor<Solution_> solutionDescriptor, String variableName) {
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            var varDesc = entityDescriptor.getVariableDescriptor(variableName);
            if (varDesc != null) {
                return varDesc;
            }
        }
        return null;
    }

    private static <Solution_> ListVariableDescriptor<Solution_> findListVariableDescriptorByName(
            SolutionDescriptor<Solution_> solutionDescriptor, String variableName) {
        var varDesc = findVariableDescriptorByName(solutionDescriptor, variableName);
        if (varDesc instanceof ListVariableDescriptor<Solution_> listVarDesc) {
            return listVarDesc;
        }
        return null;
    }

    private static <Solution_> EntitySpecification<Solution_> findEntitySpec(
            PlanningSpecification<Solution_> spec, Class<?> entityClass) {
        for (var entitySpec : spec.entities()) {
            if (entitySpec.entityClass().equals(entityClass)) {
                return entitySpec;
            }
        }
        return null;
    }

    private static <Solution_> VariableSpecification<Solution_> findVariableSpec(
            EntitySpecification<Solution_> entitySpec, String variableName) {
        for (var varSpec : entitySpec.variables()) {
            if (varSpec.name().equals(variableName)) {
                return varSpec;
            }
        }
        return null;
    }

    /**
     * A synthetic ParameterizedType used for list variables (e.g., List&lt;ElementType&gt;).
     */
    record SyntheticParameterizedType(Class<?> rawType, Type[] typeArgs) implements ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return typeArgs;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}
