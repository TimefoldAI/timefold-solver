package ai.timefold.solver.core.impl.domain.solution.descriptor;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD;
import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD;
import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor.extractInheritedClasses;
import static java.util.stream.Stream.concat;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.common.accessor.ReflectionFieldMemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.lookup.LookUpStrategyResolver;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.score.descriptor.ScoreDescriptor;
import ai.timefold.solver.core.impl.domain.solution.ConstraintConfigurationBasedConstraintWeightSupplier;
import ai.timefold.solver.core.impl.domain.solution.ConstraintWeightSupplier;
import ai.timefold.solver.core.impl.domain.solution.OverridesBasedConstraintWeightSupplier;
import ai.timefold.solver.core.impl.domain.solution.cloner.FieldAccessingSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionClonerFactory;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.EntityIndependentValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.impl.util.MutableLong;
import ai.timefold.solver.core.impl.util.MutablePair;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link ai.timefold.solver.core.api.domain.solution.PlanningSolution}
 *        annotation
 */
public class SolutionDescriptor<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionDescriptor.class);
    private static final EntityDescriptor<?> NULL_ENTITY_DESCRIPTOR = new EntityDescriptor<>(-1, null, PlanningEntity.class);
    protected static final Class[] ANNOTATED_MEMBERS_CLASSES = {
            ProblemFactCollectionProperty.class,
            ValueRangeProvider.class,
            PlanningEntityCollectionProperty.class,
            PlanningScore.class };

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(Class<Solution_> solutionClass,
            Class<?>... entityClasses) {
        return buildSolutionDescriptor(solutionClass, Arrays.asList(entityClasses));
    }

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(
            Class<Solution_> solutionClass,
            List<Class<?>> entityClassList) {
        return buildSolutionDescriptor(EnumSet.noneOf(PreviewFeature.class), solutionClass, entityClassList);
    }

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(
            Set<PreviewFeature> enabledPreviewFeaturesSet,
            Class<Solution_> solutionClass,
            Class<?>... entityClasses) {
        return buildSolutionDescriptor(enabledPreviewFeaturesSet, solutionClass, List.of(entityClasses));
    }

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(
            Set<PreviewFeature> enabledPreviewFeaturesSet,
            Class<Solution_> solutionClass,
            List<Class<?>> entityClassList) {
        return buildSolutionDescriptor(enabledPreviewFeaturesSet, DomainAccessType.REFLECTION, solutionClass, null,
                null, entityClassList);
    }

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(
            Set<PreviewFeature> enabledPreviewFeatureSet,
            DomainAccessType domainAccessType,
            Class<Solution_> solutionClass, Map<String, MemberAccessor> memberAccessorMap,
            Map<String, SolutionCloner> solutionClonerMap, List<Class<?>> entityClassList) {
        assertMutable(solutionClass, "solutionClass");
        assertSingleInheritance(solutionClass);
        assertValidAnnotatedMembers(solutionClass);
        solutionClonerMap = Objects.requireNonNullElse(solutionClonerMap, Collections.emptyMap());
        var solutionDescriptor = new SolutionDescriptor<>(solutionClass, memberAccessorMap);
        var descriptorPolicy = new DescriptorPolicy();
        if (enabledPreviewFeatureSet != null) {
            descriptorPolicy.setEnabledPreviewFeatureSet(enabledPreviewFeatureSet);
        }
        descriptorPolicy.setDomainAccessType(domainAccessType);
        descriptorPolicy.setGeneratedSolutionClonerMap(solutionClonerMap);
        descriptorPolicy.setMemberAccessorFactory(solutionDescriptor.getMemberAccessorFactory());

        solutionDescriptor.processUnannotatedFieldsAndMethods(descriptorPolicy);
        solutionDescriptor.processAnnotations(descriptorPolicy, entityClassList);
        var ordinal = 0;
        // Before iterating over the entity classes, we need to read the inheritance chain,
        // add all parent and child classes, and sort them.
        var updatedEntityClassList = new ArrayList<>(entityClassList);
        for (var entityClass : entityClassList) {
            var inheritedEntityClasses = extractInheritedClasses(entityClass);
            var filteredInheritedEntityClasses = inheritedEntityClasses.stream()
                    .filter(c -> !updatedEntityClassList.contains(c)).toList();
            updatedEntityClassList.addAll(filteredInheritedEntityClasses);
        }
        for (var entityClass : sortEntityClassList(updatedEntityClassList)) {
            var entityDescriptor = new EntityDescriptor<>(ordinal++, solutionDescriptor, entityClass);
            solutionDescriptor.addEntityDescriptor(entityDescriptor);
            entityDescriptor.processAnnotations(descriptorPolicy);
        }
        solutionDescriptor.afterAnnotationsProcessed(descriptorPolicy);
        if (solutionDescriptor.constraintWeightSupplier != null) {
            // The scoreDescriptor is definitely initialized at this point.
            solutionDescriptor.constraintWeightSupplier.initialize(solutionDescriptor,
                    descriptorPolicy.getMemberAccessorFactory(), descriptorPolicy.getDomainAccessType());
        }
        return solutionDescriptor;
    }

    public static void assertMutable(Class<?> clz, String classType) {
        if (clz.isRecord()) {
            throw new IllegalArgumentException("""
                    The %s (%s) cannot be a record as it needs to be mutable.
                    Use a regular class instead."""
                    .formatted(classType, clz.getCanonicalName()));
        } else if (clz.isEnum()) {
            throw new IllegalArgumentException("""
                    The %s (%s) cannot be an enum as it needs to be mutable.
                    Use a regular class instead."""
                    .formatted(classType, clz.getCanonicalName()));
        }
    }

    /**
     * If a class declares any annotated member, it must be annotated as a solution,
     * even if a supertype already has the annotation.
     */
    public static void assertValidAnnotatedMembers(Class<?> clazz) {
        // We first check the entity class
        if (clazz.getAnnotation(PlanningSolution.class) == null && hasAnyAnnotatedMembers(clazz)) {
            var annotatedMembers = extractAnnotatedMembers(clazz).stream()
                    .map(Member::getName)
                    .toList();
            throw new IllegalStateException(
                    """
                            The class %s is not annotated with @PlanningSolution but defines annotated members.
                            Maybe annotate %s with @PlanningSolution.
                            Maybe remove the annotated members (%s)."""
                            .formatted(clazz.getName(), clazz.getName(), annotatedMembers));
        }
        // We check the first level of the inheritance chain
        var otherClazz = clazz.getSuperclass();
        if (otherClazz != null && otherClazz.getAnnotation(PlanningSolution.class) == null
                && hasAnyAnnotatedMembers(otherClazz)) {
            var annotatedMembers = extractAnnotatedMembers(otherClazz).stream()
                    .map(Member::getName)
                    .toList();
            throw new IllegalStateException(
                    """
                            The class %s is not annotated with @PlanningSolution but defines annotated members.
                            Maybe annotate %s with @PlanningSolution.
                            Maybe remove the annotated members (%s)."""
                            .formatted(otherClazz.getName(), otherClazz.getName(), annotatedMembers));
        }
    }

    public static void assertSingleInheritance(Class<?> solutionClass) {
        var inheritedClassList =
                ConfigUtils.getAllAnnotatedLineageClasses(solutionClass.getSuperclass(), PlanningSolution.class);
        if (inheritedClassList.size() > 1) {
            throw new IllegalStateException(
                    """
                            The class %s inherits its @%s annotation from multiple classes (%s).
                            Remove solution class(es) from the inheritance chain to create a single-level inheritance structure."""
                            .formatted(solutionClass.getName(), PlanningSolution.class.getSimpleName(), inheritedClassList));
        }
    }

    private static List<Class<?>> sortEntityClassList(List<Class<?>> entityClassList) {
        var sortedEntityClassList = new ArrayList<Class<?>>(entityClassList.size());
        for (var entityClass : entityClassList) {
            var added = false;
            for (var i = 0; i < sortedEntityClassList.size(); i++) {
                var sortedEntityClass = sortedEntityClassList.get(i);
                if (entityClass.isAssignableFrom(sortedEntityClass)) {
                    sortedEntityClassList.add(i, entityClass);
                    added = true;
                    break;
                }
            }
            if (!added) {
                sortedEntityClassList.add(entityClass);
            }
        }
        return sortedEntityClassList;
    }

    private static List<Member> extractAnnotatedMembers(Class<?> solutionClass) {
        var membersList = ConfigUtils.getDeclaredMembers(solutionClass);
        return membersList.stream()
                .filter(member -> !ConfigUtils.extractAnnotationClasses(member, ANNOTATED_MEMBERS_CLASSES).isEmpty())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static boolean hasAnyAnnotatedMembers(Class<?> solutionClass) {
        return !extractAnnotatedMembers(solutionClass).isEmpty();
    }

    // ************************************************************************
    // Non-static members
    // ************************************************************************

    private final Class<Solution_> solutionClass;
    private final MemberAccessorFactory memberAccessorFactory;

    private DomainAccessType domainAccessType;
    private AutoDiscoverMemberType autoDiscoverMemberType;
    private LookUpStrategyResolver lookUpStrategyResolver;

    /**
     * @deprecated {@link ConstraintConfiguration} was replaced by {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    private MemberAccessor constraintConfigurationMemberAccessor;
    private final Map<String, MemberAccessor> problemFactMemberAccessorMap = new LinkedHashMap<>();
    private final Map<String, MemberAccessor> problemFactCollectionMemberAccessorMap = new LinkedHashMap<>();
    private final Map<String, MemberAccessor> entityMemberAccessorMap = new LinkedHashMap<>();
    private final Map<String, MemberAccessor> entityCollectionMemberAccessorMap = new LinkedHashMap<>();
    private Set<Class<?>> problemFactOrEntityClassSet;
    private List<ListVariableDescriptor<Solution_>> listVariableDescriptorList;
    private ScoreDescriptor<?> scoreDescriptor;

    private ConstraintWeightSupplier<Solution_, ?> constraintWeightSupplier;
    private final Map<Class<?>, EntityDescriptor<Solution_>> entityDescriptorMap = new LinkedHashMap<>();
    private final List<Class<?>> reversedEntityClassList = new ArrayList<>();
    private final ConcurrentMap<Class<?>, EntityDescriptor<Solution_>> lowestEntityDescriptorMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, MemberAccessor> planningIdMemberAccessorMap = new ConcurrentHashMap<>();

    private PlanningSolutionMetaModel<Solution_> planningSolutionMetaModel;
    private SolutionCloner<Solution_> solutionCloner;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    private SolutionDescriptor(Class<Solution_> solutionClass, Map<String, MemberAccessor> memberAccessorMap) {
        this.solutionClass = solutionClass;
        if (solutionClass.getPackage() == null) {
            LOGGER.warn("The solutionClass ({}) should be in a proper java package.", solutionClass);
        }
        this.memberAccessorFactory = new MemberAccessorFactory(memberAccessorMap);
    }

    public void addEntityDescriptor(EntityDescriptor<Solution_> entityDescriptor) {
        var entityClass = entityDescriptor.getEntityClass();
        for (var otherEntityClass : entityDescriptorMap.keySet()) {
            if (entityClass.isAssignableFrom(otherEntityClass)) {
                throw new IllegalArgumentException(
                        "An earlier entityClass (%s) should not be a subclass of a later entityClass (%s). Switch their declaration so superclasses are defined earlier."
                                .formatted(otherEntityClass, entityClass));
            }
        }
        entityDescriptorMap.put(entityClass, entityDescriptor);
        reversedEntityClassList.add(0, entityClass);
        lowestEntityDescriptorMap.put(entityClass, entityDescriptor);
    }

    public void processUnannotatedFieldsAndMethods(DescriptorPolicy descriptorPolicy) {
        processConstraintWeights(descriptorPolicy);
    }

    private void processConstraintWeights(DescriptorPolicy descriptorPolicy) {
        for (var lineageClass : ConfigUtils.getAllParents(solutionClass)) {
            var memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            var constraintWeightFieldList = memberList.stream()
                    .filter(member -> member instanceof Field field
                            && ConstraintWeightOverrides.class.isAssignableFrom(field.getType()))
                    .map(f -> ((Field) f))
                    .toList();
            switch (constraintWeightFieldList.size()) {
                case 0:
                    break;
                case 1:
                    if (constraintWeightSupplier != null) {
                        // The bottom-most class wins, they are parsed first due to ConfigUtil.getAllParents().
                        throw new IllegalStateException(
                                "The solutionClass (%s) has a field of type (%s) which was already found on its parent class."
                                        .formatted(lineageClass, ConstraintWeightOverrides.class));
                    }
                    constraintWeightSupplier = OverridesBasedConstraintWeightSupplier.create(this, descriptorPolicy,
                            constraintWeightFieldList.get(0));
                    break;
                default:
                    throw new IllegalStateException("The solutionClass (%s) has more than one field (%s) of type %s."
                            .formatted(solutionClass, constraintWeightFieldList, ConstraintWeightOverrides.class));
            }
        }
    }

    public void processAnnotations(DescriptorPolicy descriptorPolicy, List<Class<?>> entityClassList) {
        domainAccessType = descriptorPolicy.getDomainAccessType();
        processSolutionAnnotations(descriptorPolicy);
        var potentiallyOverwritingMethodList = new ArrayList<Method>();
        // Iterate inherited members too
        var lineageClassList = ConfigUtils.getAllAnnotatedLineageClasses(solutionClass, PlanningSolution.class);
        if (lineageClassList.isEmpty() && solutionClass.getSuperclass().isAnnotationPresent(PlanningSolution.class)) {
            lineageClassList = ConfigUtils.getAllAnnotatedLineageClasses(solutionClass.getSuperclass(), PlanningSolution.class);
        }
        for (var lineageClass : lineageClassList) {
            var memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            for (var member : memberList) {
                if (member instanceof Method method && potentiallyOverwritingMethodList.stream().anyMatch(
                        m -> member.getName().equals(m.getName()) // Shortcut to discard negatives faster
                                && ReflectionHelper.isMethodOverwritten(method, m.getDeclaringClass()))) {
                    // Ignore member because it is an overwritten method
                    continue;
                }
                processValueRangeProviderAnnotation(descriptorPolicy, member);
                processFactEntityOrScoreAnnotation(descriptorPolicy, member, entityClassList);
            }
            potentiallyOverwritingMethodList.ensureCapacity(potentiallyOverwritingMethodList.size() + memberList.size());
            memberList.stream().filter(Method.class::isInstance)
                    .forEach(member -> potentiallyOverwritingMethodList.add((Method) member));
        }
        if (entityCollectionMemberAccessorMap.isEmpty() && entityMemberAccessorMap.isEmpty()) {
            throw new IllegalStateException(
                    "The solutionClass (%s) must have at least 1 member with a %s annotation or a %s annotation.".formatted(
                            solutionClass, PlanningEntityCollectionProperty.class.getSimpleName(),
                            PlanningEntityProperty.class.getSimpleName()));
        }
        // Do not check if problemFactCollectionMemberAccessorMap and problemFactMemberAccessorMap are empty
        // because they are only required for ConstraintStreams.
        if (scoreDescriptor == null) {
            throw new IllegalStateException(
                    """
                            The solutionClass (%s) must have 1 member with a @%s annotation.
                            Maybe add a getScore() method with a @%s annotation."""
                            .formatted(solutionClass, PlanningScore.class.getSimpleName(),
                                    PlanningScore.class.getSimpleName()));
        }
    }

    private void processSolutionAnnotations(DescriptorPolicy descriptorPolicy) {
        var annotation = extractMostRelevantPlanningSolutionAnnotation();
        autoDiscoverMemberType = annotation.autoDiscoverMemberType();
        var solutionClonerClass = annotation.solutionCloner();
        if (solutionClonerClass != PlanningSolution.NullSolutionCloner.class) {
            solutionCloner = ConfigUtils.newInstance(this::toString, "solutionClonerClass", solutionClonerClass);
        }
        var lookUpStrategyType = annotation.lookUpStrategyType();
        lookUpStrategyResolver =
                new LookUpStrategyResolver(descriptorPolicy, lookUpStrategyType);
    }

    private @NonNull PlanningSolution extractMostRelevantPlanningSolutionAnnotation() {
        var solutionAnnotation = solutionClass.getAnnotation(PlanningSolution.class);
        if (solutionAnnotation != null) {
            return solutionAnnotation;
        }
        var solutionSuperclass = solutionClass.getSuperclass(); // Null if interface.
        if (solutionSuperclass == null) {
            throw new IllegalStateException("""
                    The solutionClass (%s) has been specified as a solution in the configuration, \
                    but does not have a @%s annotation."""
                    .formatted(solutionClass.getCanonicalName(), PlanningSolution.class.getSimpleName()));
        }
        var parentSolutionAnnotation = solutionSuperclass.getAnnotation(PlanningSolution.class);
        if (parentSolutionAnnotation == null) {
            throw new IllegalStateException("""
                    The solutionClass (%s) has been specified as a solution in the configuration, \
                    but neither it nor its superclass (%s) have a @%s annotation."""
                    .formatted(solutionClass.getCanonicalName(), solutionSuperclass.getCanonicalName(),
                            PlanningSolution.class.getSimpleName()));
        }
        return parentSolutionAnnotation;
    }

    private void processValueRangeProviderAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
            var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                    FIELD_OR_READ_METHOD, ValueRangeProvider.class, descriptorPolicy.getDomainAccessType());
            descriptorPolicy.addFromSolutionValueRangeProvider(memberAccessor);
        }
    }

    private void processFactEntityOrScoreAnnotation(DescriptorPolicy descriptorPolicy,
            Member member, List<Class<?>> entityClassList) {
        var annotationClass = extractFactEntityOrScoreAnnotationClassOrAutoDiscover(
                member, entityClassList);
        if (annotationClass == null) {
            return;
        }
        if (annotationClass.equals(ConstraintConfigurationProvider.class)) {
            processConstraintConfigurationProviderAnnotation(descriptorPolicy, member, annotationClass);
        } else if (annotationClass.equals(ProblemFactProperty.class)
                || annotationClass.equals(ProblemFactCollectionProperty.class)) {
            processProblemFactPropertyAnnotation(descriptorPolicy, member, annotationClass);
        } else if (annotationClass.equals(PlanningEntityProperty.class)
                || annotationClass.equals(PlanningEntityCollectionProperty.class)) {
            processPlanningEntityPropertyAnnotation(descriptorPolicy, member, annotationClass);
        } else if (annotationClass.equals(PlanningScore.class)) {
            if (scoreDescriptor == null) {
                // Bottom class wins. Bottom classes are parsed first due to ConfigUtil.getAllAnnotatedLineageClasses().
                scoreDescriptor = ScoreDescriptor.buildScoreDescriptor(descriptorPolicy, member, solutionClass);
            } else {
                scoreDescriptor.failFastOnDuplicateMember(descriptorPolicy, member, solutionClass);
            }
        }
    }

    private Class<? extends Annotation> extractFactEntityOrScoreAnnotationClassOrAutoDiscover(
            Member member, List<Class<?>> entityClassList) {
        var annotationClass = ConfigUtils.extractAnnotationClass(member,
                ConstraintConfigurationProvider.class,
                ProblemFactProperty.class,
                ProblemFactCollectionProperty.class,
                PlanningEntityProperty.class, PlanningEntityCollectionProperty.class,
                PlanningScore.class);
        if (annotationClass == null) {
            Class<?> type;
            if (autoDiscoverMemberType == AutoDiscoverMemberType.FIELD
                    && member instanceof Field field) {
                type = field.getType();
            } else if (autoDiscoverMemberType == AutoDiscoverMemberType.GETTER
                    && (member instanceof Method method) && ReflectionHelper.isGetterMethod(method)) {
                type = method.getReturnType();
            } else {
                type = null;
            }
            if (type != null) {
                if (Score.class.isAssignableFrom(type)) {
                    annotationClass = PlanningScore.class;
                } else if (Collection.class.isAssignableFrom(type) || type.isArray()) {
                    Class<?> elementType;
                    if (Collection.class.isAssignableFrom(type)) {
                        var genericType = (member instanceof Field f) ? f.getGenericType()
                                : ((Method) member).getGenericReturnType();
                        var memberName = member.getName();
                        if (!(genericType instanceof ParameterizedType)) {
                            throw new IllegalArgumentException(
                                    """
                                            The solutionClass (%s) has a auto discovered member (%s) with a member type (%s) that returns a %s which has no generic parameters.
                                            Maybe the member (%s) should return a typed %s."""
                                            .formatted(solutionClass, memberName, type, Collection.class.getSimpleName(),
                                                    memberName, Collection.class.getSimpleName()));
                        }
                        elementType = ConfigUtils.extractGenericTypeParameter("solutionClass", solutionClass, type, genericType,
                                null, member.getName()).orElse(Object.class);
                    } else {
                        elementType = type.getComponentType();
                    }
                    if (entityClassList.stream().anyMatch(entityClass -> entityClass.isAssignableFrom(elementType))) {
                        annotationClass = PlanningEntityCollectionProperty.class;
                    } else if (elementType.isAnnotationPresent(ConstraintConfiguration.class)) {
                        throw new IllegalStateException(
                                """
                                        The autoDiscoverMemberType (%s) cannot accept a member (%s) of type (%s) with an elementType (%s) that has a @%s annotation.
                                        Maybe use a member of the type (%s) directly instead of a %s or array of that type."""
                                        .formatted(autoDiscoverMemberType, member, type, elementType,
                                                ConstraintConfiguration.class.getSimpleName(), elementType,
                                                Collection.class.getSimpleName()));
                    } else {
                        annotationClass = ProblemFactCollectionProperty.class;
                    }
                } else if (Map.class.isAssignableFrom(type)) {
                    throw new IllegalStateException(
                            "The autoDiscoverMemberType (%s) does not yet support the member (%s) of type (%s) which is an implementation of %s."
                                    .formatted(autoDiscoverMemberType, member, type, Map.class.getSimpleName()));
                } else if (entityClassList.stream().anyMatch(entityClass -> entityClass.isAssignableFrom(type))) {
                    annotationClass = PlanningEntityProperty.class;
                } else if (type.isAnnotationPresent(ConstraintConfiguration.class)) {
                    annotationClass = ConstraintConfigurationProvider.class;
                } else {
                    annotationClass = ProblemFactProperty.class;
                }
            }
        }
        return annotationClass;
    }

    /**
     * @deprecated {@link ConstraintConfiguration} was replaced by {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    private void processConstraintConfigurationProviderAnnotation(DescriptorPolicy descriptorPolicy, Member member,
            Class<? extends Annotation> annotationClass) {
        if (constraintWeightSupplier != null) {
            throw new IllegalStateException("""
                    The solution class (%s) has both a %s member and a %s-annotated member.
                    %s is deprecated, please remove it from your codebase and keep %s only."""
                    .formatted(solutionClass, ConstraintWeightOverrides.class.getSimpleName(),
                            ConstraintConfigurationProvider.class.getSimpleName(),
                            ConstraintConfigurationProvider.class.getSimpleName(),
                            ConstraintWeightOverrides.class.getSimpleName()));
        }
        var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                FIELD_OR_READ_METHOD, annotationClass, descriptorPolicy.getDomainAccessType());
        if (constraintConfigurationMemberAccessor != null) {
            if (!constraintConfigurationMemberAccessor.getName().equals(memberAccessor.getName())
                    || !constraintConfigurationMemberAccessor.getClass().equals(memberAccessor.getClass())) {
                throw new IllegalStateException(
                        """
                                The solutionClass (%s) has a @%s annotated member (%s) that is duplicated by another member (%s).
                                Maybe the annotation is defined on both the field and its getter."""
                                .formatted(solutionClass, ConstraintConfigurationProvider.class.getSimpleName(), memberAccessor,
                                        constraintConfigurationMemberAccessor));
            }
            // Bottom class wins. Bottom classes are parsed first due to ConfigUtil.getAllAnnotatedLineageClasses()
            return;
        }
        assertNoFieldAndGetterDuplicationOrConflict(memberAccessor, annotationClass);
        constraintConfigurationMemberAccessor = memberAccessor;
        // Every ConstraintConfiguration is also a problem fact
        problemFactMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);

        var constraintConfigurationClass = constraintConfigurationMemberAccessor.getType();
        if (!constraintConfigurationClass.isAnnotationPresent(ConstraintConfiguration.class)) {
            throw new IllegalStateException(
                    "The solutionClass (%s) has a @%s annotated member (%s) that does not return a class (%s) that has a %s annotation."
                            .formatted(solutionClass, ConstraintConfigurationProvider.class.getSimpleName(), member,
                                    constraintConfigurationClass, ConstraintConfiguration.class.getSimpleName()));
        }
        constraintWeightSupplier =
                ConstraintConfigurationBasedConstraintWeightSupplier.create(this, constraintConfigurationClass);
    }

    private void processProblemFactPropertyAnnotation(DescriptorPolicy descriptorPolicy, Member member,
            Class<? extends Annotation> annotationClass) {
        var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                FIELD_OR_READ_METHOD, annotationClass, descriptorPolicy.getDomainAccessType());
        Class<?> problemFactType;
        assertNoFieldAndGetterDuplicationOrConflict(memberAccessor, annotationClass);
        if (annotationClass == ProblemFactProperty.class) {
            problemFactMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
            problemFactType = memberAccessor.getType();
        } else if (annotationClass == ProblemFactCollectionProperty.class) {
            var type = memberAccessor.getType();
            if (!(Collection.class.isAssignableFrom(type) || type.isArray())) {
                throw new IllegalStateException(
                        "The solutionClass (%s) has a @%s-annotated member (%s) that does not return a %s or an array."
                                .formatted(solutionClass, ProblemFactCollectionProperty.class.getSimpleName(), member,
                                        Collection.class.getSimpleName()));
            }
            problemFactCollectionMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);

            if (type.isArray()) {
                problemFactType = type.getComponentType();
            } else {
                problemFactType = ConfigUtils.extractGenericTypeParameterOrFail(PlanningSolution.class.getSimpleName(),
                        memberAccessor.getDeclaringClass(),
                        type, memberAccessor.getGenericType(), annotationClass, memberAccessor.getName());
            }
        } else {
            throw new IllegalStateException("Impossible situation with annotationClass (" + annotationClass + ").");
        }
        if (problemFactType.isAnnotationPresent(PlanningEntity.class)) {
            throw new IllegalStateException("""
                    The solutionClass (%s) has a @%s-annotated member (%s) that returns a @%s.
                    Maybe use @%s instead?"""
                    .formatted(solutionClass, annotationClass.getSimpleName(), memberAccessor.getName(),
                            PlanningEntity.class.getSimpleName(),
                            ((annotationClass == ProblemFactProperty.class)
                                    ? PlanningEntityProperty.class.getSimpleName()
                                    : PlanningEntityCollectionProperty.class.getSimpleName())));
        }
    }

    private void processPlanningEntityPropertyAnnotation(DescriptorPolicy descriptorPolicy, Member member,
            Class<? extends Annotation> annotationClass) {
        var memberAccessor = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(member,
                FIELD_OR_GETTER_METHOD, annotationClass, descriptorPolicy.getDomainAccessType());
        assertNoFieldAndGetterDuplicationOrConflict(memberAccessor, annotationClass);
        if (annotationClass == PlanningEntityProperty.class) {
            entityMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
        } else if (annotationClass == PlanningEntityCollectionProperty.class) {
            var type = memberAccessor.getType();
            if (!(Collection.class.isAssignableFrom(type) || type.isArray())) {
                throw new IllegalStateException(
                        "The solutionClass (%s) has a @%s annotated member (%s) that does not return a %s or an array."
                                .formatted(solutionClass, PlanningEntityCollectionProperty.class.getSimpleName(), member,
                                        Collection.class.getSimpleName()));
            }
            entityCollectionMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
        } else {
            throw new IllegalStateException("Impossible situation with annotationClass (" + annotationClass + ").");
        }
    }

    private void assertNoFieldAndGetterDuplicationOrConflict(
            MemberAccessor memberAccessor, Class<? extends Annotation> annotationClass) {
        MemberAccessor duplicate;
        Class<? extends Annotation> otherAnnotationClass;
        var memberName = memberAccessor.getName();
        if (constraintConfigurationMemberAccessor != null
                && constraintConfigurationMemberAccessor.getName().equals(memberName)) {
            duplicate = constraintConfigurationMemberAccessor;
            otherAnnotationClass = ConstraintConfigurationProvider.class;
        } else if (problemFactMemberAccessorMap.containsKey(memberName)) {
            duplicate = problemFactMemberAccessorMap.get(memberName);
            otherAnnotationClass = ProblemFactProperty.class;
        } else if (problemFactCollectionMemberAccessorMap.containsKey(memberName)) {
            duplicate = problemFactCollectionMemberAccessorMap.get(memberName);
            otherAnnotationClass = ProblemFactCollectionProperty.class;
        } else if (entityMemberAccessorMap.containsKey(memberName)) {
            duplicate = entityMemberAccessorMap.get(memberName);
            otherAnnotationClass = PlanningEntityProperty.class;
        } else if (entityCollectionMemberAccessorMap.containsKey(memberName)) {
            duplicate = entityCollectionMemberAccessorMap.get(memberName);
            otherAnnotationClass = PlanningEntityCollectionProperty.class;
        } else {
            return;
        }
        throw new IllegalStateException("""
                The solutionClass (%s) has a @%s annotated member (%s) that is duplicated by a @%s annotated member (%s).
                %s""".formatted(solutionClass, annotationClass.getSimpleName(), memberAccessor,
                otherAnnotationClass.getSimpleName(),
                duplicate, annotationClass.equals(otherAnnotationClass)
                        ? "Maybe the annotation is defined on both the field and its getter."
                        : "Maybe 2 mutually exclusive annotations are configured."));
    }

    private void afterAnnotationsProcessed(DescriptorPolicy descriptorPolicy) {
        for (var entityDescriptor : entityDescriptorMap.values()) {
            entityDescriptor.linkEntityDescriptors(descriptorPolicy);
        }
        for (var entityDescriptor : entityDescriptorMap.values()) {
            entityDescriptor.linkVariableDescriptors(descriptorPolicy);
        }
        determineGlobalShadowOrder();
        problemFactOrEntityClassSet = collectEntityAndProblemFactClasses();
        listVariableDescriptorList = findListVariableDescriptors();
        validateListVariableDescriptors();

        // And finally log the successful completion of processing.
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("    Model annotations parsed for solution {}:", solutionClass.getSimpleName());
            for (var entry : entityDescriptorMap.entrySet()) {
                var entityDescriptor = entry.getValue();
                LOGGER.trace("        Entity {}:", entityDescriptor.getEntityClass().getSimpleName());
                for (var variableDescriptor : entityDescriptor.getDeclaredVariableDescriptors()) {
                    LOGGER.trace("            {} variable {} ({})",
                            variableDescriptor instanceof GenuineVariableDescriptor ? "Genuine" : "Shadow",
                            variableDescriptor.getVariableName(),
                            variableDescriptor.getMemberAccessorSpeedNote());
                }
            }
        }
        initSolutionCloner(descriptorPolicy);
    }

    private void determineGlobalShadowOrder() {
        // Topological sorting with Kahn's algorithm
        var pairList = new ArrayList<MutablePair<ShadowVariableDescriptor<Solution_>, Integer>>();
        var shadowToPairMap =
                new HashMap<ShadowVariableDescriptor<Solution_>, MutablePair<ShadowVariableDescriptor<Solution_>, Integer>>();
        for (var entityDescriptor : entityDescriptorMap.values()) {
            for (var shadow : entityDescriptor.getDeclaredShadowVariableDescriptors()) {
                var sourceSize = shadow.getSourceVariableDescriptorList().size();
                var pair = MutablePair.of(shadow, sourceSize);
                pairList.add(pair);
                shadowToPairMap.put(shadow, pair);
            }
        }
        for (var entityDescriptor : entityDescriptorMap.values()) {
            for (var genuine : entityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                for (var sink : genuine.getSinkVariableDescriptorList()) {
                    var sinkPair = shadowToPairMap.get(sink);
                    sinkPair.setValue(sinkPair.getValue() - 1);
                }
            }
        }
        var globalShadowOrder = 0;
        while (!pairList.isEmpty()) {
            pairList.sort(Comparator.comparingInt(MutablePair::getValue));
            var pair = pairList.remove(0);
            var shadow = pair.getKey();
            if (pair.getValue() != 0) {
                if (pair.getValue() < 0) {
                    throw new IllegalStateException(
                            "Impossible state because the shadowVariable (%s) cannot be used more as a sink than it has sources."
                                    .formatted(shadow.getSimpleEntityAndVariableName()));
                }
                throw new IllegalStateException(
                        "There is a cyclic shadow variable path that involves the shadowVariable (%s) because it must be later than its sources (%s) and also earlier than its sinks (%s)."
                                .formatted(shadow.getSimpleEntityAndVariableName(), shadow.getSourceVariableDescriptorList(),
                                        shadow.getSinkVariableDescriptorList()));
            }
            for (var sink : shadow.getSinkVariableDescriptorList()) {
                var sinkPair = shadowToPairMap.get(sink);
                sinkPair.setValue(sinkPair.getValue() - 1);
            }
            shadow.setGlobalShadowOrder(globalShadowOrder);
            globalShadowOrder++;
        }
    }

    private void validateListVariableDescriptors() {
        if (listVariableDescriptorList.isEmpty()) {
            return;
        }
        if (listVariableDescriptorList.size() > 1) {
            throw new UnsupportedOperationException(
                    "Defining multiple list variables (%s) across the model is currently not supported."
                            .formatted(listVariableDescriptorList));
        }

        var listVariableDescriptor = listVariableDescriptorList.get(0);
        var listVariableEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
        // We will not support chained and list variables at the same entity,
        // and the validation can be removed once we discontinue support for chained variables.
        if (hasChainedVariable()) {
            var basicVariableDescriptorList = new ArrayList<>(listVariableEntityDescriptor.getGenuineVariableDescriptorList());
            basicVariableDescriptorList.remove(listVariableDescriptor);
            throw new UnsupportedOperationException(
                    "Combining chained variables (%s) with list variables (%s) on a single planning entity (%s) is not supported."
                            .formatted(basicVariableDescriptorList, listVariableDescriptor,
                                    listVariableDescriptor.getEntityDescriptor().getEntityClass().getCanonicalName()));
        }
    }

    private Set<Class<?>> collectEntityAndProblemFactClasses() {
        // Figure out all problem fact or entity types that are used within this solution,
        // using the knowledge we've already gained by processing all the annotations.
        var entityClassStream = entityDescriptorMap.keySet()
                .stream();
        var factClassStream = problemFactMemberAccessorMap
                .values()
                .stream()
                .map(MemberAccessor::getType);
        var problemFactOrEntityClassStream = concat(entityClassStream, factClassStream);
        var factCollectionClassStream = problemFactCollectionMemberAccessorMap.values()
                .stream()
                .map(accessor -> ConfigUtils
                        .extractGenericTypeParameter("solutionClass", getSolutionClass(), accessor.getType(),
                                accessor.getGenericType(), ProblemFactCollectionProperty.class, accessor.getName())
                        .orElse(Object.class));
        problemFactOrEntityClassStream = concat(problemFactOrEntityClassStream, factCollectionClassStream);
        // Add constraint configuration, if configured.
        if (constraintWeightSupplier != null) {
            problemFactOrEntityClassStream = concat(problemFactOrEntityClassStream,
                    Stream.of(constraintWeightSupplier.getProblemFactClass()));
        }
        return problemFactOrEntityClassStream.collect(Collectors.toSet());
    }

    private List<ListVariableDescriptor<Solution_>> findListVariableDescriptors() {
        return getGenuineEntityDescriptors().stream()
                .map(EntityDescriptor::getGenuineVariableDescriptorList)
                .flatMap(Collection::stream)
                .filter(VariableDescriptor::isListVariable)
                .map(variableDescriptor -> ((ListVariableDescriptor<Solution_>) variableDescriptor))
                .toList();
    }

    private void initSolutionCloner(DescriptorPolicy descriptorPolicy) {
        solutionCloner = solutionCloner == null ? descriptorPolicy.getGeneratedSolutionClonerMap()
                .get(GizmoSolutionClonerFactory.getGeneratedClassName(this))
                : solutionCloner;

        if (solutionCloner instanceof GizmoSolutionCloner<Solution_> gizmoSolutionCloner) {
            gizmoSolutionCloner.setSolutionDescriptor(this);
        }
        if (solutionCloner == null) {
            switch (descriptorPolicy.getDomainAccessType()) {
                case GIZMO:
                    solutionCloner = GizmoSolutionClonerFactory.build(this, memberAccessorFactory.getGizmoClassLoader());
                    break;
                case REFLECTION:
                    solutionCloner = new FieldAccessingSolutionCloner<>(this);
                    break;
                default:
                    throw new IllegalStateException("The domainAccessType (" + domainAccessType
                            + ") is not implemented.");
            }
        }
    }

    public Class<Solution_> getSolutionClass() {
        return solutionClass;
    }

    public MemberAccessorFactory getMemberAccessorFactory() {
        return memberAccessorFactory;
    }

    public DomainAccessType getDomainAccessType() {
        return domainAccessType;
    }

    public <Score_ extends Score<Score_>> ScoreDefinition<Score_> getScoreDefinition() {
        return this.<Score_> getScoreDescriptor().getScoreDefinition();
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ScoreDescriptor<Score_> getScoreDescriptor() {
        return (ScoreDescriptor<Score_>) scoreDescriptor;
    }

    public Map<String, MemberAccessor> getProblemFactMemberAccessorMap() {
        return problemFactMemberAccessorMap;
    }

    public Map<String, MemberAccessor> getProblemFactCollectionMemberAccessorMap() {
        return problemFactCollectionMemberAccessorMap;
    }

    public Map<String, MemberAccessor> getEntityMemberAccessorMap() {
        return entityMemberAccessorMap;
    }

    public Map<String, MemberAccessor> getEntityCollectionMemberAccessorMap() {
        return entityCollectionMemberAccessorMap;
    }

    public Set<Class<?>> getProblemFactOrEntityClassSet() {
        return problemFactOrEntityClassSet;
    }

    public ListVariableDescriptor<Solution_> getListVariableDescriptor() {
        return listVariableDescriptorList.isEmpty() ? null : listVariableDescriptorList.get(0);
    }

    public SolutionCloner<Solution_> getSolutionCloner() {
        return solutionCloner;
    }

    // ************************************************************************
    // Model methods
    // ************************************************************************

    public PlanningSolutionMetaModel<Solution_> getMetaModel() {
        if (planningSolutionMetaModel == null) {
            var metaModel = new DefaultPlanningSolutionMetaModel<>(this);
            for (var entityDescriptor : getEntityDescriptors()) {
                var entityMetaModel = new DefaultPlanningEntityMetaModel<>(metaModel, entityDescriptor);
                for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
                    if (variableDescriptor.isListVariable()) {
                        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) variableDescriptor;
                        var listVariableMetaModel = new DefaultPlanningListVariableMetaModel<>(entityMetaModel,
                                listVariableDescriptor);
                        entityMetaModel.addVariable(listVariableMetaModel);
                    } else {
                        var basicVariableDescriptor = (BasicVariableDescriptor<Solution_>) variableDescriptor;
                        var basicVariableMetaModel =
                                new DefaultPlanningVariableMetaModel<>(entityMetaModel, basicVariableDescriptor);
                        entityMetaModel.addVariable(basicVariableMetaModel);
                    }
                }
                for (var shadowVariableDescriptor : entityDescriptor.getShadowVariableDescriptors()) {
                    var shadowVariableMetaModel =
                            new DefaultShadowVariableMetaModel<>(entityMetaModel, shadowVariableDescriptor);
                    entityMetaModel.addVariable(shadowVariableMetaModel);
                }
                metaModel.addEntity(entityMetaModel);
            }
            this.planningSolutionMetaModel = metaModel;
        }
        return planningSolutionMetaModel;
    }

    public List<BasicVariableDescriptor<Solution_>> getBasicVariableDescriptorList() {
        return getGenuineEntityDescriptors().stream()
                .flatMap(entityDescriptor -> entityDescriptor.getGenuineBasicVariableDescriptorList().stream())
                .map(descriptor -> (BasicVariableDescriptor<Solution_>) descriptor)
                .toList();
    }

    public boolean hasBasicVariable() {
        return !getBasicVariableDescriptorList().isEmpty();
    }

    public boolean hasChainedVariable() {
        return getGenuineEntityDescriptors().stream().anyMatch(EntityDescriptor::hasAnyGenuineChainedVariables);
    }

    public boolean hasListVariable() {
        return getListVariableDescriptor() != null;
    }

    public boolean hasBothBasicAndListVariables() {
        return hasBasicVariable() && hasListVariable();
    }

    /**
     * @deprecated {@link ConstraintConfiguration} was replaced by {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    public MemberAccessor getConstraintConfigurationMemberAccessor() {
        return constraintConfigurationMemberAccessor;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ConstraintWeightSupplier<Solution_, Score_> getConstraintWeightSupplier() {
        return (ConstraintWeightSupplier<Solution_, Score_>) constraintWeightSupplier;
    }

    public Set<Class<?>> getEntityClassSet() {
        return entityDescriptorMap.keySet();
    }

    public Collection<EntityDescriptor<Solution_>> getEntityDescriptors() {
        return entityDescriptorMap.values();
    }

    public Collection<EntityDescriptor<Solution_>> getGenuineEntityDescriptors() {
        var genuineEntityDescriptorList = new ArrayList<EntityDescriptor<Solution_>>(entityDescriptorMap.size());
        for (var entityDescriptor : entityDescriptorMap.values()) {
            if (entityDescriptor.hasAnyDeclaredGenuineVariableDescriptor()) {
                genuineEntityDescriptorList.add(entityDescriptor);
            }
        }
        return genuineEntityDescriptorList;
    }

    public EntityDescriptor<Solution_> getEntityDescriptorStrict(Class<?> entityClass) {
        return entityDescriptorMap.get(entityClass);
    }

    public boolean hasEntityDescriptor(Class<?> entitySubclass) {
        var entityDescriptor = findEntityDescriptor(entitySubclass);
        return entityDescriptor != null;
    }

    public EntityDescriptor<Solution_> findEntityDescriptorOrFail(Class<?> entitySubclass) {
        var entityDescriptor = findEntityDescriptor(entitySubclass);
        if (entityDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            A planning entity is an instance of a class (%s) that is not configured as a planning entity class (%s).
                            If that class (%s) (or superclass thereof) is not a @%s annotated class, maybe your @%s annotated class has an incorrect @%s or @%s annotated member.
                            Otherwise, if you're not using the Quarkus extension or Spring Boot starter, maybe that entity class (%s) is missing from your solver configuration."""
                            .formatted(entitySubclass, getEntityClassSet(), entitySubclass.getSimpleName(),
                                    PlanningEntity.class.getSimpleName(), PlanningSolution.class.getSimpleName(),
                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                    PlanningEntityProperty.class.getSimpleName(), entitySubclass.getSimpleName()));
        }
        return entityDescriptor;
    }

    public EntityDescriptor<Solution_> findEntityDescriptor(Class<?> entitySubclass) {
        /*
         * A slightly optimized variant of map.computeIfAbsent(...).
         * computeIfAbsent(...) would require the creation of a capturing lambda every time this method is called,
         * which is created, executed once, and immediately thrown away.
         * This is a micro-optimization, but it is valuable on the hot path.
         */
        var cachedEntityDescriptor = lowestEntityDescriptorMap.get(entitySubclass);
        if (cachedEntityDescriptor == NULL_ENTITY_DESCRIPTOR) { // Cache hit, no descriptor found.
            return null;
        } else if (cachedEntityDescriptor != null) { // Cache hit, descriptor found.
            return cachedEntityDescriptor;
        }
        // Cache miss, look for the descriptor.
        var newEntityDescriptor = innerFindEntityDescriptor(entitySubclass);
        if (newEntityDescriptor == null) {
            // Dummy entity descriptor value, as ConcurrentMap does not allow null values.
            lowestEntityDescriptorMap.put(entitySubclass, (EntityDescriptor<Solution_>) NULL_ENTITY_DESCRIPTOR);
            return null;
        } else {
            lowestEntityDescriptorMap.put(entitySubclass, newEntityDescriptor);
            return newEntityDescriptor;
        }
    }

    private EntityDescriptor<Solution_> innerFindEntityDescriptor(Class<?> entitySubclass) {
        // Reverse order to find the nearest ancestor
        for (var entityClass : reversedEntityClassList) {
            if (entityClass.isAssignableFrom(entitySubclass)) {
                return entityDescriptorMap.get(entityClass);
            }
        }
        return null;
    }

    public VariableDescriptor<Solution_> findVariableDescriptorOrFail(Object entity, String variableName) {
        var entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
        var variableDescriptor = entityDescriptor.getVariableDescriptor(variableName);
        if (variableDescriptor == null) {
            throw new IllegalArgumentException(entityDescriptor.buildInvalidVariableNameExceptionMessage(variableName));
        }
        return variableDescriptor;
    }

    // ************************************************************************
    // Look up methods
    // ************************************************************************

    public LookUpStrategyResolver getLookUpStrategyResolver() {
        return lookUpStrategyResolver;
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    public Collection<Object> getAllEntitiesAndProblemFacts(Solution_ solution) {
        var facts = new ArrayList<>();
        visitAll(solution, facts::add);
        return facts;
    }

    /**
     * @param solution never null
     * @return {@code >= 0}
     */
    public int getGenuineEntityCount(Solution_ solution) {
        var entityCount = new MutableInt();
        // Need to go over every element in every entity collection, as some of the entities may not be genuine.
        visitAllEntities(solution, fact -> {
            var entityDescriptor = findEntityDescriptorOrFail(fact.getClass());
            if (entityDescriptor.isGenuine()) {
                entityCount.increment();
            }
        });
        return entityCount.intValue();
    }

    /**
     * Return accessor for a given member of a given class, if present,
     * and cache it for future use.
     *
     * @param factClass never null
     * @return null if no such member exists
     */
    public MemberAccessor getPlanningIdAccessor(Class<?> factClass) {
        var memberAccessor = planningIdMemberAccessorMap.get(factClass);
        if (memberAccessor == null) {
            memberAccessor =
                    ConfigUtils.findPlanningIdMemberAccessor(factClass, getMemberAccessorFactory(), getDomainAccessType());
            var nonNullMemberAccessor = Objects.requireNonNullElse(memberAccessor, DummyMemberAccessor.INSTANCE);
            planningIdMemberAccessorMap.put(factClass, nonNullMemberAccessor);
            return memberAccessor;
        } else if (memberAccessor == DummyMemberAccessor.INSTANCE) {
            return null;
        } else {
            return memberAccessor;
        }
    }

    public void visitAllEntities(Solution_ solution, Consumer<Object> visitor) {
        visitAllEntities(solution, visitor, collection -> collection.forEach(visitor));
    }

    private void visitAllEntities(Solution_ solution, Consumer<Object> visitor,
            Consumer<Collection<Object>> collectionVisitor) {
        for (var entityMemberAccessor : entityMemberAccessorMap.values()) {
            var entity = extractMemberObject(entityMemberAccessor, solution);
            if (entity != null) {
                visitor.accept(entity);
            }
        }
        for (var entityCollectionMemberAccessor : entityCollectionMemberAccessorMap.values()) {
            var entityCollection = extractMemberCollectionOrArray(entityCollectionMemberAccessor, solution, false);
            collectionVisitor.accept(entityCollection);
        }
    }

    /**
     *
     * @param solution solution to extract the entities from
     * @param entityClass class of the entity to be visited, including subclasses
     * @param visitor never null; applied to every entity, iteration stops if it returns true
     */
    public void visitEntitiesByEntityClass(Solution_ solution, Class<?> entityClass, Predicate<Object> visitor) {
        for (var entityMemberAccessor : entityMemberAccessorMap.values()) {
            var entity = extractMemberObject(entityMemberAccessor, solution);
            if (entityClass.isInstance(entity) && visitor.test(entity)) {
                return;
            }

        }
        for (var entityCollectionMemberAccessor : entityCollectionMemberAccessorMap.values()) {
            var optionalTypeParameter = ConfigUtils.extractGenericTypeParameter("solutionClass",
                    entityCollectionMemberAccessor.getDeclaringClass(), entityCollectionMemberAccessor.getType(),
                    entityCollectionMemberAccessor.getGenericType(), null, entityCollectionMemberAccessor.getName());
            boolean collectionGuaranteedToContainOnlyGivenEntityType = optionalTypeParameter
                    .map(entityClass::isAssignableFrom)
                    .orElse(false);
            if (collectionGuaranteedToContainOnlyGivenEntityType) {
                /*
                 * In a typical case typeParameter is specified and it is the expected entity or its superclass.
                 * Therefore we can simply apply the visitor on each element.
                 */
                var entityCollection = extractMemberCollectionOrArray(entityCollectionMemberAccessor, solution, false);
                for (var o : entityCollection) {
                    if (visitor.test(o)) {
                        return;
                    }
                }
                continue;
            }
            // The collection now is either raw, or it is not of an entity type, such as perhaps a parent interface.
            boolean collectionCouldPossiblyContainGivenEntityType = optionalTypeParameter
                    .map(e -> e.isAssignableFrom(entityClass))
                    .orElse(true);
            if (!collectionCouldPossiblyContainGivenEntityType) {
                // There is no way how this collection could possibly contain entities of the given type.
                continue;
            }
            // We need to go over every collection member and check if it is an entity of the given type.
            var entityCollection = extractMemberCollectionOrArray(entityCollectionMemberAccessor, solution, false);
            for (var entity : entityCollection) {
                if (entityClass.isInstance(entity) && visitor.test(entity)) {
                    return;
                }

            }
        }
    }

    public void visitAllProblemFacts(Solution_ solution, Consumer<Object> visitor) {
        // Visits facts.
        for (var accessor : problemFactMemberAccessorMap.values()) {
            var object = extractMemberObject(accessor, solution);
            if (object != null) {
                visitor.accept(object);
            }
        }
        // Visits problem facts from problem fact collections.
        for (var accessor : problemFactCollectionMemberAccessorMap.values()) {
            var objects = extractMemberCollectionOrArray(accessor, solution, true);
            for (var object : objects) {
                visitor.accept(object);
            }
        }
    }

    public void visitAll(Solution_ solution, Consumer<Object> visitor) {
        visitAllProblemFacts(solution, visitor);
        visitAllEntities(solution, visitor);
    }

    /**
     * @param scoreDirector never null
     * @return {@code >= 0}
     */
    public boolean hasMovableEntities(ScoreDirector<Solution_> scoreDirector) {
        var workingSolution = scoreDirector.getWorkingSolution();
        return extractAllEntitiesStream(workingSolution)
                .anyMatch(entity -> findEntityDescriptorOrFail(entity.getClass()).isMovable(workingSolution, entity));
    }

    /**
     * @param solution never null
     * @return {@code >= 0}
     */
    public long getGenuineVariableCount(Solution_ solution) {
        var result = new MutableLong();
        visitAllEntities(solution, entity -> {
            var entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                result.add(entityDescriptor.getGenuineVariableCount());
            }
        });
        return result.longValue();
    }

    /**
     * @param solution never null
     * @return {@code >= 0}
     */
    public long getApproximateValueCount(Solution_ solution) {
        var genuineVariableDescriptorSet =
                Collections.newSetFromMap(new IdentityHashMap<GenuineVariableDescriptor<Solution_>, Boolean>());
        visitAllEntities(solution, entity -> {
            var entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                genuineVariableDescriptorSet.addAll(entityDescriptor.getGenuineVariableDescriptorList());
            }
        });
        var out = new MutableLong();
        for (var variableDescriptor : genuineVariableDescriptorSet) {
            var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
            if (valueRangeDescriptor.isEntityIndependent()) {
                var entityIndependentVariableDescriptor =
                        (EntityIndependentValueRangeDescriptor<Solution_>) valueRangeDescriptor;
                out.add(entityIndependentVariableDescriptor.extractValueRangeSize(solution));
            } else {
                visitEntitiesByEntityClass(solution,
                        variableDescriptor.getEntityDescriptor().getEntityClass(),
                        entity -> {
                            out.add(valueRangeDescriptor.extractValueRangeSize(solution, entity));
                            return false;
                        });
            }
        }
        return out.longValue();
    }

    public long getMaximumValueRangeSize(Solution_ solution) {
        return extractAllEntitiesStream(solution)
                .mapToLong(entity -> {
                    var entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
                    return entityDescriptor.isGenuine() ? entityDescriptor.getMaximumValueCount(solution, entity) : 0L;
                })
                .max()
                .orElse(0L);
    }

    /**
     * Calculates an indication on how big this problem instance is.
     * This is approximately the base 10 log of the search space size.
     *
     * @param solution never null
     * @return {@code >= 0}
     */
    public double getProblemScale(Solution_ solution) {
        var logBase = Math.max(2, getMaximumValueRangeSize(solution));
        var problemScaleTracker = new ProblemScaleTracker(logBase);
        visitAllEntities(solution, entity -> {
            var entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                entityDescriptor.processProblemScale(solution, entity, problemScaleTracker);
            }
        });
        var result = problemScaleTracker.getBasicProblemScaleLog();
        if (problemScaleTracker.getListTotalEntityCount() != 0L) {
            // List variables do not support from entity value ranges
            var totalListValueCount = problemScaleTracker.getListTotalValueCount();
            var totalListMovableValueCount = totalListValueCount - problemScaleTracker.getListPinnedValueCount();
            var possibleTargetsForListValue = problemScaleTracker.getListMovableEntityCount();
            var listVariableDescriptor = getListVariableDescriptor();
            if (listVariableDescriptor != null && listVariableDescriptor.allowsUnassignedValues()) {
                // Treat unassigned values as assigned to a single virtual vehicle for the sake of this calculation
                possibleTargetsForListValue++;
            }

            result += MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, logBase,
                    totalListMovableValueCount, possibleTargetsForListValue);
        }
        var scale = (result / (double) MathUtils.LOG_PRECISION) / MathUtils.getLogInBase(logBase, 10d);
        if (Double.isNaN(scale) || Double.isInfinite(scale)) {
            return 0;
        }
        return scale;
    }

    public List<ShadowVariableDescriptor<Solution_>> getAllShadowVariableDescriptors() {
        var out = new ArrayList<ShadowVariableDescriptor<Solution_>>();
        for (var entityDescriptor : entityDescriptorMap.values()) {
            out.addAll(entityDescriptor.getShadowVariableDescriptors());
        }
        return out;
    }

    public List<DeclarativeShadowVariableDescriptor<Solution_>> getDeclarativeShadowVariableDescriptors() {
        var out = new HashSet<DeclarativeShadowVariableDescriptor<Solution_>>();
        for (var entityDescriptor : entityDescriptorMap.values()) {
            entityDescriptor.getShadowVariableDescriptors();
            for (var shadowVariableDescriptor : entityDescriptor.getShadowVariableDescriptors()) {
                if (shadowVariableDescriptor instanceof DeclarativeShadowVariableDescriptor<Solution_> declarativeShadowVariableDescriptor) {
                    out.add(declarativeShadowVariableDescriptor);
                }
            }
        }
        return new ArrayList<>(out);
    }

    public ProblemSizeStatistics getProblemSizeStatistics(Solution_ solution) {
        return new ProblemSizeStatistics(
                getGenuineEntityCount(solution),
                getGenuineVariableCount(solution),
                getApproximateValueCount(solution),
                getProblemScale(solution));
    }

    public SolutionInitializationStatistics computeInitializationStatistics(Solution_ solution) {
        return computeInitializationStatistics(solution, null);
    }

    public SolutionInitializationStatistics computeInitializationStatistics(Solution_ solution, Consumer<Object> finisher) {
        /*
         * The score director requires all of these data points,
         * so we calculate them all in a single pass over the entities.
         * This is an important performance improvement,
         * as there are potentially thousands of entities.
         */
        var uninitializedEntityCount = new MutableInt();
        var uninitializedVariableCount = new MutableInt();
        var unassignedValueCount = new MutableInt();
        var genuineEntityCount = new MutableInt();
        var shadowEntityCount = new MutableInt();
        for (var listVariableDescriptor : listVariableDescriptorList) {
            if (listVariableDescriptor.allowsUnassignedValues()) { // Unassigned elements count as assigned.
                continue;
            }
            // We count every possibly unassigned element in every list variable.
            // And later we subtract the assigned elements.
            unassignedValueCount.add((int) listVariableDescriptor.getValueRangeSize(solution, null));
        }
        visitAllEntities(solution, entity -> {
            var entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                genuineEntityCount.increment();
                var uninitializedVariableCountForEntity = entityDescriptor.countUninitializedVariables(entity);
                if (uninitializedVariableCountForEntity > 0) {
                    uninitializedEntityCount.increment();
                    uninitializedVariableCount.add(uninitializedVariableCountForEntity);
                }
            } else {
                shadowEntityCount.increment();
            }
            if (finisher != null) {
                finisher.accept(entity);
            }
            if (!entityDescriptor.hasAnyGenuineListVariables()) {
                return;
            }
            for (var listVariableDescriptor : listVariableDescriptorList) {
                if (listVariableDescriptor.allowsUnassignedValues()) { // Unassigned elements count as assigned.
                    continue;
                }
                var listVariableEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
                if (listVariableEntityDescriptor.matchesEntity(entity)) {
                    unassignedValueCount.subtract(listVariableDescriptor.getListSize(entity));
                }
                // TODO maybe detect duplicates and elements that are outside the value range
            }
        });
        return new SolutionInitializationStatistics(genuineEntityCount.intValue(), shadowEntityCount.intValue(),
                uninitializedEntityCount.intValue(), uninitializedVariableCount.intValue(), unassignedValueCount.intValue());
    }

    public record SolutionInitializationStatistics(int genuineEntityCount, int shadowEntityCount,
            int uninitializedEntityCount, int uninitializedVariableCount, int unassignedValueCount) {

        public int getInitCount() {
            return uninitializedVariableCount + uninitializedEntityCount;
        }

        public boolean isInitialized() {
            return getInitCount() == 0;
        }

    }

    private Stream<Object> extractAllEntitiesStream(Solution_ solution) {
        var stream = Stream.empty();
        for (var memberAccessor : entityMemberAccessorMap.values()) {
            var entity = extractMemberObject(memberAccessor, solution);
            if (entity != null) {
                stream = concat(stream, Stream.of(entity));
            }
        }
        for (var memberAccessor : entityCollectionMemberAccessorMap.values()) {
            var entityCollection = extractMemberCollectionOrArray(memberAccessor, solution, false);
            stream = concat(stream, entityCollection.stream());
        }
        return stream;
    }

    private Object extractMemberObject(MemberAccessor memberAccessor, Solution_ solution) {
        return memberAccessor.executeGetter(solution);
    }

    private Collection<Object> extractMemberCollectionOrArray(MemberAccessor memberAccessor, Solution_ solution,
            boolean isFact) {
        Collection<Object> collection;
        if (memberAccessor.getType().isArray()) {
            var arrayObject = memberAccessor.executeGetter(solution);
            collection = ReflectionHelper.transformArrayToList(arrayObject);
        } else {
            collection = (Collection<Object>) memberAccessor.executeGetter(solution);
        }
        if (collection == null) {
            throw new IllegalArgumentException(
                    """
                            The solutionClass (%s)'s %s (%s) should never return null.
                            %sMaybe that property (%s) was set with null instead of an empty collection/array when the class (%s) instance was created."""
                            .formatted(solutionClass, isFact ? "factCollectionProperty" : "entityCollectionProperty",
                                    memberAccessor, memberAccessor instanceof ReflectionFieldMemberAccessor ? ""
                                            : "Maybe the getter/method always returns null instead of the actual data.\n",
                                    memberAccessor.getName(), solutionClass.getSimpleName()));
        }
        return collection;
    }

    /**
     * @param solution never null
     * @return sometimes null, if the {@link Score} hasn't been calculated yet
     */
    public <Score_ extends Score<Score_>> Score_ getScore(Solution_ solution) {
        return this.<Score_> getScoreDescriptor().getScore(solution);
    }

    /**
     * Called when the {@link Score} has been calculated or predicted.
     *
     * @param solution never null
     * @param score sometimes null, in rare occasions to indicate that the old {@link Score} is stale,
     *        but no new ones has been calculated
     */
    public <Score_ extends Score<Score_>> void setScore(Solution_ solution, Score_ score) {
        this.<Score_> getScoreDescriptor().setScore(solution, score);
    }

    public PlanningSolutionDiff<Solution_> diff(Solution_ oldSolution, Solution_ newSolution) {
        // Genuine entities first, then sort by class name.
        var oldEntities = sortEntitiesForDiff(oldSolution);
        var newEntities = sortEntitiesForDiff(newSolution);

        var removedOldEntities = new LinkedHashSet<>(oldEntities.size());
        var oldToNewEntities = new LinkedHashMap<>(newEntities.size());
        for (var entry : oldEntities.entrySet()) {
            var entityClassName = entry.getKey();
            for (var oldEntity : entry.getValue()) {
                var newEntity = newEntities.getOrDefault(entityClassName, Collections.emptySet())
                        .stream()
                        .filter(e -> Objects.equals(e, oldEntity))
                        .findFirst()
                        .orElse(null);
                if (newEntity == null) {
                    removedOldEntities.add(oldEntity);
                } else {
                    oldToNewEntities.put(oldEntity, newEntity);
                }
            }
        }

        var addedNewEntities = newEntities.values().stream()
                .flatMap(Collection::stream)
                .filter(newEntity -> !oldToNewEntities.containsValue(newEntity))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Genuine variables first, then sort by ordinal.
        var variableDescriptorComparator = Comparator.<VariableDescriptor<Solution_>, String> comparing(
                variableDescriptor -> variableDescriptor instanceof GenuineVariableDescriptor<Solution_> ? "0" : "1")
                .thenComparingInt(VariableDescriptor::getOrdinal);
        var solutionDiff = new DefaultPlanningSolutionDiff<>(getMetaModel(), oldSolution, newSolution, removedOldEntities,
                addedNewEntities);
        for (var entry : oldToNewEntities.entrySet()) {
            var oldEntity = entry.getKey();
            var newEntity = entry.getValue();
            var entityDescriptor = findEntityDescriptorOrFail(oldEntity.getClass());
            var entityDiff = new DefaultPlanningEntityDiff<>(solutionDiff, entry.getKey());
            entityDescriptor.getVariableDescriptorMap().values().stream()
                    .sorted(variableDescriptorComparator)
                    .flatMap(variableDescriptor -> {
                        var oldValue = variableDescriptor.getValue(oldEntity);
                        var newValue = variableDescriptor.getValue(newEntity);
                        if (Objects.equals(oldValue, newValue)) {
                            return Stream.empty();
                        }
                        var variableMetaModel = entityDiff.entityMetaModel().variable(variableDescriptor.getVariableName());
                        var variableDiff = new DefaultPlanningVariableDiff<>(entityDiff, variableMetaModel, oldValue, newValue);
                        return Stream.of(variableDiff);
                    }).forEach(entityDiff::addVariableDiff);
            if (!entityDiff.variableDiffs().isEmpty()) {
                solutionDiff.addEntityDiff(entityDiff);
            }
        }
        return solutionDiff;
    }

    private SortedMap<String, Set<Object>> sortEntitiesForDiff(Solution_ solution) {
        return getEntityDescriptors().stream()
                .map(descriptor -> descriptor.extractEntities(solution))
                .flatMap(Collection::stream)
                // TreeMap and LinkedHashSet for fully reproducible ordering of entities and variables.
                .collect(Collectors.groupingBy(s -> s.getClass().getCanonicalName(), TreeMap::new,
                        Collectors.toCollection(LinkedHashSet::new)));
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(getClass().getSimpleName(), solutionClass.getName());
    }

}
