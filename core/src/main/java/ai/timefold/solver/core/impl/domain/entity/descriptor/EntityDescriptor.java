package ai.timefold.solver.core.impl.domain.entity.descriptor;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD;
import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER;
import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.entity.PinningFilter;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.CustomShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.descriptor.ProblemScaleTracker;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.anchor.AnchorShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.custom.CustomShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.custom.LegacyCustomShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.custom.PiggybackShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.WeightFactorySelectionSorter;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class EntityDescriptor<Solution_> {

    private static final Class[] VARIABLE_ANNOTATION_CLASSES = {
            PlanningVariable.class,
            PlanningListVariable.class,
            InverseRelationShadowVariable.class,
            AnchorShadowVariable.class,
            IndexShadowVariable.class,
            PreviousElementShadowVariable.class,
            NextElementShadowVariable.class,
            ShadowVariable.class,
            ShadowVariable.List.class,
            PiggybackShadowVariable.class,
            CustomShadowVariable.class,
            CascadingUpdateShadowVariable.class };

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDescriptor.class);

    private final int ordinal;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Class<?> entityClass;
    private final Predicate<Object> isInitializedPredicate;
    private final List<MemberAccessor> declaredPlanningPinIndexMemberAccessorList = new ArrayList<>();

    private Predicate<Object> hasNoNullVariablesBasicVar;
    private Predicate<Object> hasNoNullVariablesListVar;
    // Only declared movable filter, excludes inherited and descending movable filters
    private MovableFilter<Solution_> declaredMovableEntityFilter;
    private SelectionSorter<Solution_, Object> decreasingDifficultySorter;

    // Only declared variable descriptors, excludes inherited variable descriptors
    private Map<String, GenuineVariableDescriptor<Solution_>> declaredGenuineVariableDescriptorMap;
    private Map<String, ShadowVariableDescriptor<Solution_>> declaredShadowVariableDescriptorMap;
    private Map<String, CascadingUpdateShadowVariableDescriptor<Solution_>> declaredCascadingUpdateShadowVariableDecriptorMap;

    private List<MovableFilter<Solution_>> declaredPinEntityFilterList;
    private List<EntityDescriptor<Solution_>> inheritedEntityDescriptorList;

    // Caches the inherited, declared and descending movable filters (including @PlanningPin filters) as a composite filter
    private MovableFilter<Solution_> effectiveMovableEntityFilter;
    private PlanningPinToIndexReader<Solution_> effectivePlanningPinToIndexReader;

    // Caches the inherited and declared variable descriptors
    private Map<String, GenuineVariableDescriptor<Solution_>> effectiveGenuineVariableDescriptorMap;
    private Map<String, ShadowVariableDescriptor<Solution_>> effectiveShadowVariableDescriptorMap;
    private Map<String, VariableDescriptor<Solution_>> effectiveVariableDescriptorMap;
    // Duplicate of effectiveGenuineVariableDescriptorMap.values() for faster iteration on the hot path.
    private List<GenuineVariableDescriptor<Solution_>> effectiveGenuineVariableDescriptorList;
    private List<ListVariableDescriptor<Solution_>> effectiveGenuineListVariableDescriptorList;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public EntityDescriptor(int ordinal, SolutionDescriptor<Solution_> solutionDescriptor, Class<?> entityClass) {
        this.ordinal = ordinal;
        SolutionDescriptor.assertMutable(entityClass, "entityClass");
        this.solutionDescriptor = solutionDescriptor;
        this.entityClass = entityClass;
        isInitializedPredicate = this::isInitialized;
        if (entityClass.getPackage() == null) {
            LOGGER.warn("The entityClass ({}) should be in a proper java package.", entityClass);
        }
    }

    /**
     * A number unique within a {@link SolutionDescriptor}, increasing sequentially from zero.
     * Used for indexing in arrays to avoid object hash lookups in maps.
     *
     * @return zero or higher
     */
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * Using entityDescriptor::isInitialized directly breaks node sharing
     * because it creates multiple instances of this {@link Predicate}.
     *
     * @deprecated Prefer {@link #getHasNoNullVariablesPredicateListVar()}.
     * @return never null, always the same {@link Predicate} instance to {@link #isInitialized(Object)}
     */
    @Deprecated(forRemoval = true)
    public Predicate<Object> getIsInitializedPredicate() {
        return isInitializedPredicate;
    }

    public <A> Predicate<A> getHasNoNullVariablesPredicateBasicVar() {
        if (hasNoNullVariablesBasicVar == null) {
            hasNoNullVariablesBasicVar = this::hasNoNullVariables;
        }
        return (Predicate<A>) hasNoNullVariablesBasicVar;
    }

    public <A> Predicate<A> getHasNoNullVariablesPredicateListVar() {
        /*
         * This code depends on all entity descriptors and solution descriptor to be fully initialized.
         * For absolute safety, we only construct the predicate the first time it is requested.
         * That will be during the building of the score director, when the descriptors are already set in stone.
         */
        if (hasNoNullVariablesListVar == null) {
            var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
            if (listVariableDescriptor == null || !listVariableDescriptor.acceptsValueType(entityClass)) {
                throw new IllegalStateException(
                        "Impossible state: method called without an applicable list variable descriptor.");
            }
            var applicableShadowDescriptor = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
            if (applicableShadowDescriptor == null) {
                throw new IllegalStateException(
                        "Impossible state: method called without an applicable list variable descriptor.");
            }

            hasNoNullVariablesListVar = getHasNoNullVariablesPredicateBasicVar()
                    .and(entity -> applicableShadowDescriptor.getValue(entity) != null);
        }
        return (Predicate<A>) hasNoNullVariablesListVar;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        processEntityAnnotations();
        declaredGenuineVariableDescriptorMap = new LinkedHashMap<>();
        declaredShadowVariableDescriptorMap = new LinkedHashMap<>();
        declaredCascadingUpdateShadowVariableDecriptorMap = new HashMap<>();
        declaredPinEntityFilterList = new ArrayList<>(2);
        // Only iterate declared fields and methods, not inherited members, to avoid registering the same one twice
        var memberList = ConfigUtils.getDeclaredMembers(entityClass);
        var variableDescriptorCounter = new MutableInt(0);
        for (var member : memberList) {
            processValueRangeProviderAnnotation(descriptorPolicy, member);
            processPlanningVariableAnnotation(variableDescriptorCounter, descriptorPolicy, member);
            processPlanningPinAnnotation(descriptorPolicy, member);
        }
        if (declaredGenuineVariableDescriptorMap.isEmpty() && declaredShadowVariableDescriptorMap.isEmpty()) {
            throw new IllegalStateException("The entityClass (" + entityClass
                    + ") should have at least 1 getter method or 1 field with a "
                    + PlanningVariable.class.getSimpleName() + " annotation or a shadow variable annotation.");
        }
        processVariableAnnotations(descriptorPolicy);
    }

    private void processEntityAnnotations() {
        PlanningEntity entityAnnotation = entityClass.getAnnotation(PlanningEntity.class);
        if (entityAnnotation == null) {
            throw new IllegalStateException("The entityClass (" + entityClass
                    + ") has been specified as a planning entity in the configuration," +
                    " but does not have a @" + PlanningEntity.class.getSimpleName() + " annotation.");
        }
        processMovable(entityAnnotation);
        processDifficulty(entityAnnotation);
    }

    private void processMovable(PlanningEntity entityAnnotation) {
        var pinningFilterClass = entityAnnotation.pinningFilter();
        var hasPinningFilter = pinningFilterClass != PlanningEntity.NullPinningFilter.class;
        if (hasPinningFilter) {
            var pinningFilter = ConfigUtils.newInstance(this::toString, "pinningFilterClass",
                    (Class<? extends PinningFilter<Solution_, Object>>) pinningFilterClass);
            declaredMovableEntityFilter = (solution, selection) -> !pinningFilter.accept(solution, selection);
        }
    }

    private void processDifficulty(PlanningEntity entityAnnotation) {
        Class<? extends Comparator> difficultyComparatorClass = entityAnnotation.difficultyComparatorClass();
        if (difficultyComparatorClass == PlanningEntity.NullDifficultyComparator.class) {
            difficultyComparatorClass = null;
        }
        Class<? extends SelectionSorterWeightFactory> difficultyWeightFactoryClass = entityAnnotation
                .difficultyWeightFactoryClass();
        if (difficultyWeightFactoryClass == PlanningEntity.NullDifficultyWeightFactory.class) {
            difficultyWeightFactoryClass = null;
        }
        if (difficultyComparatorClass != null && difficultyWeightFactoryClass != null) {
            throw new IllegalStateException("The entityClass (" + entityClass
                    + ") cannot have a difficultyComparatorClass (" + difficultyComparatorClass.getName()
                    + ") and a difficultyWeightFactoryClass (" + difficultyWeightFactoryClass.getName()
                    + ") at the same time.");
        }
        if (difficultyComparatorClass != null) {
            Comparator<Object> difficultyComparator = ConfigUtils.newInstance(this::toString,
                    "difficultyComparatorClass", difficultyComparatorClass);
            decreasingDifficultySorter = new ComparatorSelectionSorter<>(
                    difficultyComparator, SelectionSorterOrder.DESCENDING);
        }
        if (difficultyWeightFactoryClass != null) {
            SelectionSorterWeightFactory<Solution_, Object> difficultyWeightFactory = ConfigUtils.newInstance(this::toString,
                    "difficultyWeightFactoryClass", difficultyWeightFactoryClass);
            decreasingDifficultySorter = new WeightFactorySelectionSorter<>(
                    difficultyWeightFactory, SelectionSorterOrder.DESCENDING);
        }
    }

    private void processValueRangeProviderAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
            MemberAccessor memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                    FIELD_OR_READ_METHOD, ValueRangeProvider.class, descriptorPolicy.getDomainAccessType());
            descriptorPolicy.addFromEntityValueRangeProvider(
                    memberAccessor);
        }
    }

    private void processPlanningVariableAnnotation(MutableInt variableDescriptorCounter, DescriptorPolicy descriptorPolicy,
            Member member) {
        Class<? extends Annotation> variableAnnotationClass = ConfigUtils.extractAnnotationClass(
                member, VARIABLE_ANNOTATION_CLASSES);
        if (variableAnnotationClass != null) {
            MemberAccessorFactory.MemberAccessorType memberAccessorType;
            if (variableAnnotationClass.equals(CustomShadowVariable.class)
                    || variableAnnotationClass.equals(ShadowVariable.class)
                    || variableAnnotationClass.equals(ShadowVariable.List.class)
                    || variableAnnotationClass.equals(PiggybackShadowVariable.class)
                    || variableAnnotationClass.equals(CascadingUpdateShadowVariable.class)) {
                memberAccessorType = FIELD_OR_GETTER_METHOD;
            } else {
                memberAccessorType = FIELD_OR_GETTER_METHOD_WITH_SETTER;
            }
            MemberAccessor memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                    memberAccessorType, variableAnnotationClass, descriptorPolicy.getDomainAccessType());
            registerVariableAccessor(variableDescriptorCounter.intValue(), variableAnnotationClass, memberAccessor);
            variableDescriptorCounter.increment();
        }
    }

    private void registerVariableAccessor(int nextVariableDescriptorOrdinal,
            Class<? extends Annotation> variableAnnotationClass, MemberAccessor memberAccessor) {
        var memberName = memberAccessor.getName();
        if (declaredGenuineVariableDescriptorMap.containsKey(memberName)
                || declaredShadowVariableDescriptorMap.containsKey(memberName)) {
            VariableDescriptor<Solution_> duplicate = declaredGenuineVariableDescriptorMap.get(memberName);
            if (duplicate == null) {
                duplicate = declaredShadowVariableDescriptorMap.get(memberName);
            }
            throw new IllegalStateException("""
                    The entityClass (%s) has a @%s annotated member (%s), duplicated by member for variableDescriptor (%s).
                    Maybe the annotation is defined on both the field and its getter."""
                    .formatted(entityClass, variableAnnotationClass.getSimpleName(), memberAccessor, duplicate));
        } else if (variableAnnotationClass.equals(PlanningVariable.class)) {
            var type = memberAccessor.getType();
            if (type.isArray()) {
                throw new IllegalStateException("""
                        The entityClass (%s) has a @%s annotated member (%s) that is of an array type."""
                        .formatted(entityClass, PlanningVariable.class.getSimpleName(), memberAccessor));
            }
            var variableDescriptor = new BasicVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredGenuineVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(PlanningListVariable.class)) {
            if (List.class.isAssignableFrom(memberAccessor.getType())) {
                var variableDescriptor = new ListVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
                declaredGenuineVariableDescriptorMap.put(memberName, variableDescriptor);
            } else {
                throw new IllegalStateException("""
                        The entityClass (%s) has a @%s annotated member (%s) that has an unsupported type (%s).
                        Maybe use %s."""
                        .formatted(entityClass, PlanningListVariable.class.getSimpleName(), memberAccessor,
                                memberAccessor.getType(), List.class.getCanonicalName()));
            }
        } else if (variableAnnotationClass.equals(InverseRelationShadowVariable.class)) {
            var variableDescriptor =
                    new InverseRelationShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(AnchorShadowVariable.class)) {
            var variableDescriptor = new AnchorShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(IndexShadowVariable.class)) {
            var variableDescriptor = new IndexShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(PreviousElementShadowVariable.class)) {
            var variableDescriptor =
                    new PreviousElementShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(NextElementShadowVariable.class)) {
            var variableDescriptor =
                    new NextElementShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(ShadowVariable.class)
                || variableAnnotationClass.equals(ShadowVariable.List.class)) {
            var variableDescriptor = new CustomShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(CascadingUpdateShadowVariable.class)) {
            var variableDescriptor =
                    new CascadingUpdateShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
            if (declaredCascadingUpdateShadowVariableDecriptorMap.containsKey(variableDescriptor.getTargetMethodName())) {
                // If the target method is already set,
                // it means that multiple fields define the cascading shadow variable
                // and point to the same target method.
                declaredCascadingUpdateShadowVariableDecriptorMap.get(variableDescriptor.getTargetMethodName())
                        .addTargetVariable(this, memberAccessor);
            } else {
                declaredCascadingUpdateShadowVariableDecriptorMap.put(variableDescriptor.getTargetMethodName(),
                        variableDescriptor);
            }
        } else if (variableAnnotationClass.equals(PiggybackShadowVariable.class)) {
            var variableDescriptor =
                    new PiggybackShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else if (variableAnnotationClass.equals(CustomShadowVariable.class)) {
            var variableDescriptor =
                    new LegacyCustomShadowVariableDescriptor<>(nextVariableDescriptorOrdinal, this, memberAccessor);
            declaredShadowVariableDescriptorMap.put(memberName, variableDescriptor);
        } else {
            throw new IllegalStateException("The variableAnnotationClass (%s) is not implemented."
                    .formatted(variableAnnotationClass));
        }
    }

    private void processPlanningPinAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        var annotatedMember = ((AnnotatedElement) member);
        if (annotatedMember.isAnnotationPresent(PlanningPin.class)) {
            var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                    FIELD_OR_READ_METHOD, PlanningPin.class, descriptorPolicy.getDomainAccessType());
            var type = memberAccessor.getType();
            if (!Boolean.TYPE.isAssignableFrom(type) && !Boolean.class.isAssignableFrom(type)) {
                throw new IllegalStateException(
                        "The entityClass (%s) has a %s annotated member (%s) that is not a boolean or Boolean."
                                .formatted(entityClass, PlanningPin.class.getSimpleName(), member));
            }
            declaredPinEntityFilterList.add(new PinEntityFilter<>(memberAccessor));
        }
    }

    private void processPlanningPinIndexAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        var annotatedMember = ((AnnotatedElement) member);
        if (annotatedMember.isAnnotationPresent(PlanningPinToIndex.class)) {
            if (!hasAnyGenuineListVariables()) {
                throw new IllegalStateException(
                        "The entityClass (%s) has a %s annotated member (%s) but no %s annotated member."
                                .formatted(entityClass, PlanningPinToIndex.class.getSimpleName(), member,
                                        PlanningListVariable.class.getSimpleName()));
            }
            var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                    FIELD_OR_READ_METHOD, PlanningPinToIndex.class, descriptorPolicy.getDomainAccessType());
            var type = memberAccessor.getType();
            if (!Integer.TYPE.isAssignableFrom(type)) {
                throw new IllegalStateException(
                        "The entityClass (%s) has a %s annotated member (%s) that is not a primitive int."
                                .formatted(entityClass, PlanningPinToIndex.class.getSimpleName(), member));
            }
            declaredPlanningPinIndexMemberAccessorList.add(memberAccessor);
        }
    }

    private void processVariableAnnotations(DescriptorPolicy descriptorPolicy) {
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : declaredGenuineVariableDescriptorMap.values()) {
            variableDescriptor.processAnnotations(descriptorPolicy);
        }
        for (ShadowVariableDescriptor<Solution_> variableDescriptor : declaredShadowVariableDescriptorMap.values()) {
            variableDescriptor.processAnnotations(descriptorPolicy);
        }
    }

    public void linkEntityDescriptors(DescriptorPolicy descriptorPolicy) {
        investigateParentsToLinkInherited(entityClass);
        createEffectiveVariableDescriptorMaps();
        createEffectiveMovableEntitySelectionFilter();
        // linkVariableDescriptors() is in a separate loop
    }

    private void investigateParentsToLinkInherited(Class<?> investigateClass) {
        inheritedEntityDescriptorList = new ArrayList<>(4);
        if (investigateClass == null || investigateClass.isArray()) {
            return;
        }
        linkInherited(investigateClass.getSuperclass());
        for (Class<?> superInterface : investigateClass.getInterfaces()) {
            linkInherited(superInterface);
        }
    }

    private void linkInherited(Class<?> potentialEntityClass) {
        EntityDescriptor<Solution_> entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(
                potentialEntityClass);
        if (entityDescriptor != null) {
            inheritedEntityDescriptorList.add(entityDescriptor);
        } else {
            investigateParentsToLinkInherited(potentialEntityClass);
        }
    }

    private void createEffectiveVariableDescriptorMaps() {
        effectiveGenuineVariableDescriptorMap = new LinkedHashMap<>(declaredGenuineVariableDescriptorMap.size());
        effectiveShadowVariableDescriptorMap = new LinkedHashMap<>(declaredShadowVariableDescriptorMap.size());
        for (EntityDescriptor<Solution_> inheritedEntityDescriptor : inheritedEntityDescriptorList) {
            effectiveGenuineVariableDescriptorMap.putAll(inheritedEntityDescriptor.effectiveGenuineVariableDescriptorMap);
            effectiveShadowVariableDescriptorMap.putAll(inheritedEntityDescriptor.effectiveShadowVariableDescriptorMap);
        }
        effectiveGenuineVariableDescriptorMap.putAll(declaredGenuineVariableDescriptorMap);
        effectiveShadowVariableDescriptorMap.putAll(declaredShadowVariableDescriptorMap);
        effectiveVariableDescriptorMap = CollectionUtils
                .newLinkedHashMap(effectiveGenuineVariableDescriptorMap.size() + effectiveShadowVariableDescriptorMap.size());
        effectiveVariableDescriptorMap.putAll(effectiveGenuineVariableDescriptorMap);
        effectiveVariableDescriptorMap.putAll(effectiveShadowVariableDescriptorMap);
        effectiveGenuineVariableDescriptorList = new ArrayList<>(effectiveGenuineVariableDescriptorMap.values());
        effectiveGenuineListVariableDescriptorList = effectiveGenuineVariableDescriptorList.stream()
                .filter(VariableDescriptor::isListVariable)
                .map(l -> (ListVariableDescriptor<Solution_>) l)
                .toList();
    }

    private void createEffectiveMovableEntitySelectionFilter() {
        if (declaredMovableEntityFilter != null && !hasAnyDeclaredGenuineVariableDescriptor()) {
            throw new IllegalStateException("The entityClass (" + entityClass
                    + ") has a movableEntitySelectionFilterClass (" + declaredMovableEntityFilter.getClass()
                    + "), but it has no declared genuine variables, only shadow variables.");
        }
        var movableFilterList = new ArrayList<MovableFilter<Solution_>>();
        // TODO Also add in child entity selectors
        for (var inheritedEntityDescriptor : inheritedEntityDescriptorList) {
            if (inheritedEntityDescriptor.hasEffectiveMovableEntityFilter()) {
                // Includes movable and pinned
                movableFilterList.add(inheritedEntityDescriptor.effectiveMovableEntityFilter);
            }
        }
        if (declaredMovableEntityFilter != null) {
            movableFilterList.add(declaredMovableEntityFilter);
        }
        movableFilterList.addAll(declaredPinEntityFilterList);
        if (movableFilterList.isEmpty()) {
            effectiveMovableEntityFilter = null;
        } else {
            effectiveMovableEntityFilter = movableFilterList.stream()
                    .reduce(MovableFilter::and)
                    .orElseThrow(() -> new IllegalStateException("Impossible state: no movable filters."));
        }
    }

    private void createEffectivePlanningPinIndexReader() {
        if (!hasAnyGenuineListVariables()) {
            effectivePlanningPinToIndexReader = null;
            return;
        }
        var planningPinIndexMemberAccessorList = new ArrayList<MemberAccessor>();
        for (EntityDescriptor<Solution_> inheritedEntityDescriptor : inheritedEntityDescriptorList) {
            if (inheritedEntityDescriptor.effectivePlanningPinToIndexReader != null) {
                planningPinIndexMemberAccessorList.addAll(inheritedEntityDescriptor.declaredPlanningPinIndexMemberAccessorList);
            }
        }
        planningPinIndexMemberAccessorList.addAll(declaredPlanningPinIndexMemberAccessorList);
        switch (planningPinIndexMemberAccessorList.size()) {
            case 0 -> effectivePlanningPinToIndexReader = null;
            case 1 -> {
                var memberAccessor = planningPinIndexMemberAccessorList.get(0);
                effectivePlanningPinToIndexReader = (solution, entity) -> (int) memberAccessor.executeGetter(entity);
            }
            default -> throw new IllegalStateException(
                    "The entityClass (%s) has (%d) @%s-annotated members (%s), where it should only have one."
                            .formatted(entityClass, planningPinIndexMemberAccessorList.size(),
                                    PlanningPinToIndex.class.getSimpleName(), planningPinIndexMemberAccessorList));
        }
    }

    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : declaredGenuineVariableDescriptorMap.values()) {
            variableDescriptor.linkVariableDescriptors(descriptorPolicy);
        }
        for (ShadowVariableDescriptor<Solution_> shadowVariableDescriptor : declaredShadowVariableDescriptorMap.values()) {
            shadowVariableDescriptor.linkVariableDescriptors(descriptorPolicy);
        }
        /*
         * We can only create the PlanningPinIndexReader after we have processed all list variable descriptors.
         * Only iterate declared fields and methods, not inherited members,
         * to avoid registering the same one twice.
         */
        for (Member member : ConfigUtils.getDeclaredMembers(entityClass)) {
            processPlanningPinIndexAnnotation(descriptorPolicy, member);
        }
        createEffectivePlanningPinIndexReader();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public boolean matchesEntity(Object entity) {
        return entityClass.isAssignableFrom(entity.getClass());
    }

    public boolean hasEffectiveMovableEntityFilter() {
        return effectiveMovableEntityFilter != null;
    }

    public boolean hasCascadingShadowVariables() {
        return !declaredShadowVariableDescriptorMap.isEmpty();
    }

    public boolean supportsPinning() {
        return hasEffectiveMovableEntityFilter() || effectivePlanningPinToIndexReader != null;
    }

    public BiPredicate<Solution_, Object> getEffectiveMovableEntityFilter() {
        return effectiveMovableEntityFilter;
    }

    public SelectionSorter<Solution_, Object> getDecreasingDifficultySorter() {
        return decreasingDifficultySorter;
    }

    public Collection<String> getGenuineVariableNameSet() {
        return effectiveGenuineVariableDescriptorMap.keySet();
    }

    public GenuineVariableDescriptor<Solution_> getGenuineVariableDescriptor(String variableName) {
        return effectiveGenuineVariableDescriptorMap.get(variableName);
    }

    public boolean hasAnyGenuineVariables() {
        return !effectiveGenuineVariableDescriptorMap.isEmpty();
    }

    public boolean hasAnyGenuineListVariables() {
        if (!isGenuine()) {
            return false;
        }
        return getGenuineListVariableDescriptor() != null;
    }

    public boolean isGenuine() {
        return hasAnyGenuineVariables();
    }

    public ListVariableDescriptor<Solution_> getGenuineListVariableDescriptor() {
        if (effectiveGenuineListVariableDescriptorList.isEmpty()) {
            return null;
        }
        // Earlier validation guarantees there will only ever be one.
        return effectiveGenuineListVariableDescriptorList.get(0);
    }

    public List<GenuineVariableDescriptor<Solution_>> getGenuineVariableDescriptorList() {
        return effectiveGenuineVariableDescriptorList;
    }

    public long getGenuineVariableCount() {
        return effectiveGenuineVariableDescriptorList.size();
    }

    public Collection<ShadowVariableDescriptor<Solution_>> getShadowVariableDescriptors() {
        return effectiveShadowVariableDescriptorMap.values();
    }

    public ShadowVariableDescriptor<Solution_> getShadowVariableDescriptor(String variableName) {
        return effectiveShadowVariableDescriptorMap.get(variableName);
    }

    public Map<String, VariableDescriptor<Solution_>> getVariableDescriptorMap() {
        return effectiveVariableDescriptorMap;
    }

    public boolean hasVariableDescriptor(String variableName) {
        return effectiveVariableDescriptorMap.containsKey(variableName);
    }

    public VariableDescriptor<Solution_> getVariableDescriptor(String variableName) {
        return effectiveVariableDescriptorMap.get(variableName);
    }

    public boolean hasAnyDeclaredGenuineVariableDescriptor() {
        return !declaredGenuineVariableDescriptorMap.isEmpty();
    }

    public Collection<GenuineVariableDescriptor<Solution_>> getDeclaredGenuineVariableDescriptors() {
        return declaredGenuineVariableDescriptorMap.values();
    }

    public Collection<ShadowVariableDescriptor<Solution_>> getDeclaredShadowVariableDescriptors() {
        return declaredShadowVariableDescriptorMap.values();
    }

    public Collection<CascadingUpdateShadowVariableDescriptor<Solution_>>
            getDeclaredCascadingUpdateShadowVariableDescriptors() {
        return declaredCascadingUpdateShadowVariableDecriptorMap.values();
    }

    public Collection<VariableDescriptor<Solution_>> getDeclaredVariableDescriptors() {
        Collection<VariableDescriptor<Solution_>> variableDescriptors = new ArrayList<>(
                declaredGenuineVariableDescriptorMap.size() + declaredShadowVariableDescriptorMap.size());
        variableDescriptors.addAll(declaredGenuineVariableDescriptorMap.values());
        variableDescriptors.addAll(declaredShadowVariableDescriptorMap.values());
        return variableDescriptors;
    }

    public String buildInvalidVariableNameExceptionMessage(String variableName) {
        if (!ReflectionHelper.hasGetterMethod(entityClass, variableName)
                && !ReflectionHelper.hasField(entityClass, variableName)) {
            String exceptionMessage = "The variableName (" + variableName
                    + ") for entityClass (" + entityClass
                    + ") does not exist as a getter or field on that class.\n"
                    + "Check the spelling of the variableName (" + variableName + ").";
            if (variableName.length() >= 2
                    && !Character.isUpperCase(variableName.charAt(0))
                    && Character.isUpperCase(variableName.charAt(1))) {
                String correctedVariableName = variableName.substring(0, 1).toUpperCase() + variableName.substring(1);
                exceptionMessage += "Maybe it needs to be correctedVariableName (" + correctedVariableName
                        + ") instead, if it's a getter, because the JavaBeans spec states that "
                        + "the first letter should be a upper case if the second is upper case.";
            }
            return exceptionMessage;
        }
        return "The variableName (" + variableName
                + ") for entityClass (" + entityClass
                + ") exists as a getter or field on that class,"
                + " but isn't in the planning variables (" + effectiveVariableDescriptorMap.keySet() + ").\n"
                + (Character.isUpperCase(variableName.charAt(0))
                        ? "Maybe the variableName (" + variableName + ") should start with a lowercase.\n"
                        : "")
                + "Maybe your planning entity's getter or field lacks a @" + PlanningVariable.class.getSimpleName()
                + " annotation or a shadow variable annotation.";
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    public List<Object> extractEntities(Solution_ solution) {
        List<Object> entityList = new ArrayList<>();
        visitAllEntities(solution, entityList::add);
        return entityList;
    }

    public void visitAllEntities(Solution_ solution, Consumer<Object> visitor) {
        solutionDescriptor.visitEntitiesByEntityClass(solution, entityClass, entity -> {
            visitor.accept(entity);
            return false; // Iterate over all entities.
        });
    }

    public PlanningPinToIndexReader<Solution_> getEffectivePlanningPinToIndexReader() {
        return effectivePlanningPinToIndexReader;
    }

    public long getMaximumValueCount(Solution_ solution, Object entity) {
        long maximumValueCount = 0L;
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : effectiveGenuineVariableDescriptorList) {
            maximumValueCount = Math.max(maximumValueCount, variableDescriptor.getValueRangeSize(solution, entity));
        }
        return maximumValueCount;

    }

    public void processProblemScale(Solution_ solution, Object entity, ProblemScaleTracker tracker) {
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : effectiveGenuineVariableDescriptorList) {
            long valueCount = variableDescriptor.getValueRangeSize(solution, entity);
            // TODO: When minimum Java supported is 21, this can be replaced with a sealed interface switch
            if (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor) {
                if (basicVariableDescriptor.isChained()) {
                    // An entity is a value
                    tracker.addListValueCount(1);
                    if (!isMovable(solution, entity)) {
                        tracker.addPinnedListValueCount(1);
                    }
                    // Anchors are entities
                    ValueRange<?> valueRange = variableDescriptor.getValueRangeDescriptor().extractValueRange(solution, entity);
                    if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                        Iterator<?> valueIterator = countableValueRange.createOriginalIterator();
                        while (valueIterator.hasNext()) {
                            Object value = valueIterator.next();
                            if (variableDescriptor.isValuePotentialAnchor(value)) {
                                if (tracker.isAnchorVisited(value)) {
                                    continue;
                                }
                                // Assumes anchors are not pinned
                                tracker.incrementListEntityCount(true);
                            }
                        }
                    } else {
                        throw new IllegalStateException("""
                                The value range (%s) for variable (%s) is not countable.
                                Verify that a @%s does not return a %s when it can return %s or %s.
                                """.formatted(valueRange, variableDescriptor.getSimpleEntityAndVariableName(),
                                ValueRangeProvider.class.getSimpleName(), ValueRange.class.getSimpleName(),
                                CountableValueRange.class.getSimpleName(), Collection.class.getSimpleName()));
                    }
                } else {
                    if (isMovable(solution, entity)) {
                        tracker.addBasicProblemScale(valueCount);
                    }
                }
            } else if (variableDescriptor instanceof ListVariableDescriptor<Solution_> listVariableDescriptor) {
                tracker.setListTotalValueCount((int) listVariableDescriptor.getValueRangeSize(solution, entity));
                if (isMovable(solution, entity)) {
                    tracker.incrementListEntityCount(true);
                    tracker.addPinnedListValueCount(listVariableDescriptor.getFirstUnpinnedIndex(entity));
                } else {
                    tracker.incrementListEntityCount(false);
                    tracker.addPinnedListValueCount(listVariableDescriptor.getListSize(entity));
                }
            } else {
                throw new IllegalStateException(
                        "Unhandled subclass of %s encountered (%s).".formatted(VariableDescriptor.class.getSimpleName(),
                                variableDescriptor.getClass().getSimpleName()));
            }
        }
    }

    public int countUninitializedVariables(Object entity) {
        int count = 0;
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : effectiveGenuineVariableDescriptorList) {
            if (!variableDescriptor.isInitialized(entity)) {
                count++;
            }
        }
        return count;
    }

    public boolean isInitialized(Object entity) {
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : effectiveGenuineVariableDescriptorList) {
            if (!variableDescriptor.isInitialized(entity)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNoNullVariables(Object entity) {
        return switch (effectiveGenuineVariableDescriptorList.size()) { // Avoid excessive iterator allocation.
            case 0 -> true;
            case 1 -> effectiveGenuineVariableDescriptorList.get(0).getValue(entity) != null;
            default -> {
                for (var variableDescriptor : effectiveGenuineVariableDescriptorList) {
                    if (variableDescriptor.getValue(entity) == null) {
                        yield false;
                    }
                }
                yield true;
            }
        };
    }

    public int countReinitializableVariables(Object entity) {
        int count = 0;
        for (GenuineVariableDescriptor<Solution_> variableDescriptor : effectiveGenuineVariableDescriptorList) {
            if (variableDescriptor.isReinitializable(entity)) {
                count++;
            }
        }
        return count;
    }

    public boolean isMovable(Solution_ workingSolution, Object entity) {
        return isGenuine() &&
                (effectiveMovableEntityFilter == null
                        || effectiveMovableEntityFilter.test(workingSolution, entity));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entityClass.getName() + ")";
    }

}
