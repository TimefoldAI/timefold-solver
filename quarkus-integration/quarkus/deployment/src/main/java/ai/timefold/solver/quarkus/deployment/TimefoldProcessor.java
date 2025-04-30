package ai.timefold.solver.quarkus.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static java.lang.String.format;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.RootVariableSource;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.bean.BeanUtil;
import ai.timefold.solver.quarkus.bean.DefaultTimefoldBeanProvider;
import ai.timefold.solver.quarkus.bean.TimefoldSolverBannerBean;
import ai.timefold.solver.quarkus.bean.UnavailableTimefoldBeanProvider;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;
import ai.timefold.solver.quarkus.deployment.api.ConstraintMetaModelBuildItem;
import ai.timefold.solver.quarkus.deployment.config.SolverBuildTimeConfig;
import ai.timefold.solver.quarkus.deployment.config.TimefoldBuildTimeConfig;
import ai.timefold.solver.quarkus.devui.DevUISolverConfig;
import ai.timefold.solver.quarkus.devui.TimefoldDevUIPropertiesRPCService;
import ai.timefold.solver.quarkus.devui.TimefoldDevUIRecorder;
import ai.timefold.solver.quarkus.gizmo.TimefoldGizmoBeanFactory;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.runtime.configuration.ConfigurationException;

class TimefoldProcessor {

    private static final Logger log = Logger.getLogger(TimefoldProcessor.class.getName());

    TimefoldBuildTimeConfig timefoldBuildTimeConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("timefold-solver");
    }

    @BuildStep
    void watchSolverConfigXml(BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        var solverConfigXML = timefoldBuildTimeConfig.solverConfigXml()
                .orElse(TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL);
        var solverCongigXmlFileSet = new HashSet<String>();
        solverCongigXmlFileSet.add(solverConfigXML);
        timefoldBuildTimeConfig.solver().values().stream()
                .map(SolverBuildTimeConfig::solverConfigXml)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(solverCongigXmlFileSet::add);
        solverCongigXmlFileSet.forEach(file -> hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(file)));
    }

    @BuildStep
    IndexDependencyBuildItem indexDependencyBuildItem() {
        // Add @PlanningEntity and other annotations in the Jandex index for Gizmo
        return new IndexDependencyBuildItem("ai.timefold.solver", "timefold-solver-core");
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void makeGizmoBeanFactoryUnremovable(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TimefoldGizmoBeanFactory.class));
    }

    @BuildStep(onlyIfNot = NativeBuild.class)
    DetermineIfNativeBuildItem ifNotNativeBuild() {
        return new DetermineIfNativeBuildItem(false);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    DetermineIfNativeBuildItem ifNativeBuild() {
        return new DetermineIfNativeBuildItem(true);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem registerDevUICard() {
        var cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Configuration")
                .icon("font-awesome-solid:wrench")
                .componentLink("config-component.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Model")
                .icon("font-awesome-solid:wrench")
                .componentLink("model-component.js"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Constraints")
                .icon("font-awesome-solid:wrench")
                .componentLink("constraints-component.js"));

        return cardPageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public JsonRPCProvidersBuildItem registerRPCService() {
        return new JsonRPCProvidersBuildItem("Timefold Solver", TimefoldDevUIPropertiesRPCService.class);
    }

    /**
     * The DevConsole injects the SolverFactory bean programmatically, which is not detected by ArC. As a result,
     * the bean is removed as unused unless told otherwise via the {@link UnremovableBeanBuildItem}.
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void makeSolverFactoryUnremovableInDevMode(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(SolverFactory.class));
    }

    @BuildStep
    SolverConfigBuildItem recordAndRegisterBuildTimeBeans(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        var indexView = combinedIndex.getIndex();

        // Step 0 - determine list of names used for injected solver components
        var solverNames = new HashSet<String>();
        var solverConfigMap = new HashMap<String, SolverConfig>();
        for (var namedItem : indexView.getAnnotations(DotNames.NAMED)) {
            var target = namedItem.target();
            var type = switch (target.kind()) {
                case CLASS -> target.asClass().name();
                case FIELD -> target.asField().type().name();
                case METHOD_PARAMETER -> target.asMethodParameter().type().name();
                case RECORD_COMPONENT -> target.asRecordComponent().type().name();
                case TYPE, METHOD -> null;
            };
            if (type != null && DotNames.SOLVER_INJECTABLE_TYPES.contains(type)) {
                var annotationValue = namedItem.value();
                var value = (annotationValue != null) ? annotationValue.asString() : "";
                solverNames.add(value);
            }
        }

        // Only skip this extension if everything is missing. Otherwise, if some parts are missing, fail fast later.
        if (indexView.getAnnotations(DotNames.PLANNING_SOLUTION).isEmpty()
                && indexView.getAnnotations(DotNames.PLANNING_ENTITY).isEmpty()) {
            log.warn(
                    """
                            Skipping Timefold extension because there are no @%s or @%s annotated classes.
                            If your domain classes are located in a dependency of this project, maybe try generating the \
                            Jandex index by using the jandex-maven-plugin in that dependency, or by addingapplication.properties entries \
                            (quarkus.index-dependency.<name>.group-id and quarkus.index-dependency.<name>.artifact-id)."""
                            .formatted(PlanningSolution.class.getSimpleName(), PlanningEntity.class.getSimpleName()));
            additionalBeans.produce(new AdditionalBeanBuildItem(UnavailableTimefoldBeanProvider.class));
            solverNames.forEach(solverName -> solverConfigMap.put(solverName, null));
            return new SolverConfigBuildItem(solverConfigMap, null);
        }

        // Quarkus extensions must always use getContextClassLoader()
        // Internally, Timefold defaults the ClassLoader to getContextClassLoader() too
        var classLoader = Thread.currentThread().getContextClassLoader();
        TimefoldRecorder.assertNoUnmatchedProperties(solverNames,
                timefoldBuildTimeConfig.solver().keySet());

        // Step 1 - create all SolverConfig
        // If the config map is empty, we build the config using the default solver name
        if (solverNames.isEmpty()) {
            solverConfigMap.put(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME,
                    createSolverConfig(classLoader, TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME));
        } else {
            // One config per solver mapped name
            solverNames.forEach(solverName -> solverConfigMap.put(solverName,
                    createSolverConfig(classLoader, solverName)));
        }

        // Step 2 - validate all SolverConfig definitions
        assertNoMemberAnnotationWithoutClassAnnotation(indexView);
        assertNodeSharingDisabled(solverConfigMap);
        assertSolverConfigSolutionClasses(indexView, solverConfigMap);
        assertSolverConfigEntityClasses(indexView);
        assertSolverConfigConstraintClasses(indexView, solverConfigMap);

        // Step 3 - load all additional information per SolverConfig
        Set<Class<?>> reflectiveClassSet = new LinkedHashSet<>();
        solverConfigMap.forEach((solverName, solverConfig) -> loadSolverConfig(indexView, reflectiveHierarchyClass,
                solverConfig, solverName, reflectiveClassSet));

        // Register all annotated domain model classes
        registerClassesFromAnnotations(indexView, reflectiveClassSet);

        // Register only distinct constraint providers
        solverConfigMap.values()
                .stream()
                .filter(config -> config.getScoreDirectorFactoryConfig().getConstraintProviderClass() != null)
                .map(config -> config.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName())
                .distinct()
                .map(constraintName -> solverConfigMap.entrySet().stream().filter(entryConfig -> entryConfig.getValue()
                        .getScoreDirectorFactoryConfig().getConstraintProviderClass().getName().equals(constraintName))
                        .findFirst().get())
                .forEach(
                        entryConfig -> generateConstraintVerifier(entryConfig.getValue(), syntheticBeanBuildItemBuildProducer));

        GeneratedGizmoClasses generatedGizmoClasses = generateDomainAccessors(solverConfigMap, indexView, generatedBeans,
                generatedClasses, transformers, reflectiveClassSet);

        additionalBeans.produce(new AdditionalBeanBuildItem(TimefoldSolverBannerBean.class));
        if (solverConfigMap.size() <= 1) {
            // Only registered for the default solver
            additionalBeans.produce(new AdditionalBeanBuildItem(DefaultTimefoldBeanProvider.class));
        }
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TimefoldRuntimeConfig.class));
        return new SolverConfigBuildItem(solverConfigMap, generatedGizmoClasses);
    }

    private void assertNoMemberAnnotationWithoutClassAnnotation(IndexView indexView) {
        Collection<AnnotationInstance> timefoldFieldAnnotationCollection = new HashSet<>();

        for (DotName annotationName : DotNames.PLANNING_ENTITY_FIELD_ANNOTATIONS) {
            timefoldFieldAnnotationCollection.addAll(indexView.getAnnotationsWithRepeatable(annotationName, indexView));
        }

        for (AnnotationInstance annotationInstance : timefoldFieldAnnotationCollection) {
            var annotationTarget = annotationInstance.target();
            ClassInfo declaringClass;
            String prefix;
            declaringClass = switch (annotationTarget.kind()) {
                case FIELD -> {
                    prefix = "The field (%s)".formatted(annotationTarget.asField().name());
                    yield annotationTarget.asField().declaringClass();
                }
                case METHOD -> {
                    prefix = "The method (%s)".formatted(annotationTarget.asMethod().name());
                    yield annotationTarget.asMethod().declaringClass();
                }
                default -> throw new IllegalStateException(
                        "Member annotation @%s is on (%s), which is an invalid target type (%s) for @%s.".formatted(
                                annotationInstance.name().withoutPackagePrefix(), annotationTarget, annotationTarget.kind(),
                                annotationInstance.name().withoutPackagePrefix()));
            };

            if (!declaringClass.annotationsMap().containsKey(DotNames.PLANNING_ENTITY)) {
                throw new IllegalStateException(
                        """
                                %s with a @%s annotation is in a class (%s) that does not have a @%s annotation.
                                Maybe add a @%s annotation on the class (%s)."""
                                .formatted(prefix, annotationInstance.name().withoutPackagePrefix(), declaringClass.name(),
                                        PlanningEntity.class.getSimpleName(), PlanningEntity.class.getSimpleName(),
                                        declaringClass.name()));
            }
        }
    }

    private void assertSolverConfigSolutionClasses(IndexView indexView, Map<String, SolverConfig> solverConfigMap) {
        // Validate the solution class        
        // No solution class
        assertEmptyInstances(indexView, DotNames.PLANNING_SOLUTION);
        // Multiple classes and single solver
        var annotationInstanceList = getAllConcreteSolutionClasses(indexView);
        var firstConfig = solverConfigMap.values().stream().findFirst().orElse(null);
        if (annotationInstanceList.size() > 1 && solverConfigMap.size() == 1 && firstConfig != null
                && firstConfig.getSolutionClass() == null) {
            throw new IllegalStateException("Multiple classes (%s) found in the classpath with a @%s annotation.".formatted(
                    convertAnnotationInstancesToString(annotationInstanceList), PlanningSolution.class.getSimpleName()));
        }
        // Multiple classes and at least one solver config do not specify the solution class
        // We do not fail if all configurations define the solution,
        // even though there are additional "unused" solution classes in the classpath.
        var unconfiguredSolverConfigList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getSolutionClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (annotationInstanceList.size() > 1 && !unconfiguredSolverConfigList.isEmpty()) {
            throw new IllegalStateException(
                    """
                            Some solver configs (%s) don't specify a %s class, yet there are multiple available (%s) on the classpath.
                            Maybe set the XML config file to the related solver configs, or add the missing solution classes to the XML files,
                            or remove the unnecessary solution classes from the classpath."""
                            .formatted(String.join(", ", unconfiguredSolverConfigList),
                                    PlanningSolution.class.getSimpleName(),
                                    convertAnnotationInstancesToString(annotationInstanceList)));
        }
        // Unused solution classes
        // When inheritance is used, we ignore the parent classes.
        var unusedSolutionClassList = annotationInstanceList.stream()
                .map(planningClass -> planningClass.target().asClass().name().toString())
                .filter(planningClassName -> solverConfigMap.values().stream().filter(c -> c.getSolutionClass() != null)
                        .noneMatch(c -> c.getSolutionClass().getName().equals(planningClassName)
                                || c.getSolutionClass().getSuperclass().getName().equals(planningClassName)))
                .toList();
        if (annotationInstanceList.size() > 1 && !unusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(
                    "Unused classes ([%s]) found with a @%s annotation.".formatted(String.join(", ", unusedSolutionClassList),
                            PlanningSolution.class.getSimpleName()));
        }
    }

    private void assertNodeSharingDisabled(Map<String, SolverConfig> solverConfigMap) {
        for (var entry : solverConfigMap.entrySet()) {
            var solverConfig = entry.getValue();
            var scoreDirectorFactoryConfig = solverConfig.getScoreDirectorFactoryConfig();
            if (scoreDirectorFactoryConfig != null &&
                    Boolean.TRUE.equals(scoreDirectorFactoryConfig.getConstraintStreamAutomaticNodeSharing())) {
                throw new IllegalStateException("""
                        SolverConfig %s enabled automatic node sharing via SolverConfig, which is not allowed.
                        Enable automatic node sharing with the property %s instead."""
                        .formatted(
                                entry.getKey(),
                                "quarkus.timefold.solver.constraint-stream-automatic-node-sharing=true"));
            }
        }
    }

    private void assertSolverConfigEntityClasses(IndexView indexView) {
        // No entity classes
        assertEmptyInstances(indexView, DotNames.PLANNING_ENTITY);
    }

    private void assertSolverConfigConstraintClasses(IndexView indexView, Map<String, SolverConfig> solverConfigMap) {
        // We filter out abstract classes
        var simpleScoreClassCollection = indexView.getAllKnownImplementations(DotNames.EASY_SCORE_CALCULATOR)
                .stream().filter(clazz -> !clazz.isAbstract())
                .toList();
        var constraintScoreClassCollection =
                indexView.getAllKnownImplementations(DotNames.CONSTRAINT_PROVIDER)
                        .stream().filter(clazz -> !clazz.isAbstract())
                        .toList();
        var incrementalScoreClassCollection =
                indexView.getAllKnownImplementations(DotNames.INCREMENTAL_SCORE_CALCULATOR)
                        .stream().filter(clazz -> !clazz.isAbstract())
                        .toList();
        // No score classes
        if (simpleScoreClassCollection.isEmpty() && constraintScoreClassCollection.isEmpty()
                && incrementalScoreClassCollection.isEmpty()) {
            throw new IllegalStateException(
                    "No classes found that implement %s, %s, or %s.".formatted(EasyScoreCalculator.class.getSimpleName(),
                            ConstraintProvider.class.getSimpleName(), IncrementalScoreCalculator.class.getSimpleName()));
        }
        // Multiple classes and single solver
        var errorMessage = "Multiple score classes classes (%s) that implements %s were found in the classpath.";
        if (simpleScoreClassCollection.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    simpleScoreClassCollection.stream().map(c -> c.name().toString()).collect(Collectors.joining(", ")),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        if (constraintScoreClassCollection.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    constraintScoreClassCollection.stream().map(c -> c.name().toString()).collect(Collectors.joining(", ")),
                    ConstraintProvider.class.getSimpleName()));
        }
        if (incrementalScoreClassCollection.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    incrementalScoreClassCollection.stream().map(c -> c.name().toString()).collect(Collectors.joining(", ")),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
        // Multiple classes and at least one solver config does not specify the score class
        errorMessage = """
                Some solver configs (%s) don't specify a %s score class, yet there are multiple available (%s) on the classpath.
                Maybe set the XML config file to the related solver configs, or add the missing score classes to the XML files,
                or remove the unnecessary score classes from the classpath.""";
        var solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (simpleScoreClassCollection.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    EasyScoreCalculator.class.getSimpleName(),
                    simpleScoreClassCollection.stream().map(c -> c.name().toString()).collect(Collectors.joining(", "))));
        }
        solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getConstraintProviderClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (constraintScoreClassCollection.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    ConstraintProvider.class.getSimpleName(),
                    constraintScoreClassCollection.stream().map(c -> c.name().toString()).collect(Collectors.joining(", "))));
        }
        solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (incrementalScoreClassCollection.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    IncrementalScoreCalculator.class.getSimpleName(),
                    incrementalScoreClassCollection.stream().map(c -> c.name().toString()).collect(Collectors.joining(", "))));
        }
        // Unused score classes
        var solverConfigWithUnusedSolutionClassList = simpleScoreClassCollection.stream()
                .map(clazz -> clazz.name().toString())
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass().getName()
                                .equals(className)))
                .toList();
        errorMessage = "Unused classes ([%s]) that implements %s were found.";
        if (simpleScoreClassCollection.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        solverConfigWithUnusedSolutionClassList = constraintScoreClassCollection.stream()
                .map(clazz -> clazz.name().toString())
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getConstraintProviderClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName()
                                .equals(className)))
                .toList();
        if (constraintScoreClassCollection.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    ConstraintProvider.class.getSimpleName()));
        }
        solverConfigWithUnusedSolutionClassList = incrementalScoreClassCollection.stream()
                .map(clazz -> clazz.name().toString())
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass().getName()
                                .equals(className)))
                .toList();
        if (incrementalScoreClassCollection.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
    }

    private void assertEmptyInstances(IndexView indexView, DotName dotName) {
        var annotationInstanceCollection = indexView.getAnnotations(dotName);
        if (annotationInstanceCollection.isEmpty()) {
            try {
                throw new IllegalStateException(
                        "No classes were found with a @%s annotation."
                                .formatted(Class.forName(dotName.local()).getSimpleName()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SolverConfig createSolverConfig(ClassLoader classLoader, String solverName) {
        // 1 - The solver configuration takes precedence over root and default settings
        var solverConfigXml = this.timefoldBuildTimeConfig.getSolverConfig(solverName)
                .flatMap(SolverBuildTimeConfig::solverConfigXml);

        // 2 - Root settings
        if (solverConfigXml.isEmpty()) {
            solverConfigXml = this.timefoldBuildTimeConfig.solverConfigXml();
        }

        SolverConfig solverConfig;
        if (solverConfigXml.isPresent()) {
            String solverUrl = solverConfigXml.get();
            if (classLoader.getResource(solverUrl) == null) {
                String message =
                        "Invalid quarkus.timefold.solverConfigXML property (%s): that classpath resource does not exist."
                                .formatted(solverUrl);
                if (!solverName.equals(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME)) {
                    message =
                            "Invalid quarkus.timefold.solver.\"%s\".solverConfigXML property (%s): that classpath resource does not exist."
                                    .formatted(solverName, solverUrl);
                }
                throw new ConfigurationException(message);
            }
            solverConfig = SolverConfig.createFromXmlResource(solverUrl);
        } else if (classLoader.getResource(TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL) != null) {
            // 3 - Default file URL
            solverConfig = SolverConfig.createFromXmlResource(
                    TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL);
        } else {
            solverConfig = new SolverConfig();
        }

        return solverConfig;
    }

    private SolverConfig loadSolverConfig(IndexView indexView,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass, SolverConfig solverConfig, String solverName,
            Set<Class<?>> reflectiveClassSet) {

        // Configure planning problem models and score director per solver
        applySolverProperties(indexView, solverName, solverConfig);

        var solutionClass = solverConfig.getSolutionClass();
        if (solutionClass != null) {
            // Need to register even when using GIZMO so annotations are preserved
            Type jandexType = Type.create(DotName.createSimple(solutionClass.getName()), Type.Kind.CLASS);
            reflectiveHierarchyClass.produce(new ReflectiveHierarchyBuildItem.Builder()
                    .type(jandexType)
                    // Ignore only the packages from timefold-solver-core
                    // (Can cause a hard to diagnose issue when creating a test/example
                    // in the package "ai.timefold.solver").
                    .ignoreTypePredicate(
                            dotName -> ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)
                                    || dotName.toString().startsWith("ai.timefold.solver.api")
                                    || dotName.toString().startsWith("ai.timefold.solver.config")
                                    || dotName.toString().startsWith("ai.timefold.solver.impl"))
                    .build());
        }
        // Register solver's specific custom classes
        registerCustomClassesFromSolverConfig(solverConfig, reflectiveClassSet);
        return solverConfig;
    }

    @BuildStep
    void buildConstraintMetaModel(SolverConfigBuildItem solverConfigBuildItem,
            BuildProducer<ConstraintMetaModelBuildItem> constraintMetaModelBuildItemBuildProducer) {
        if (solverConfigBuildItem.getSolverConfigMap().isEmpty()) {
            return;
        }

        var constraintMetaModelsBySolverNames = new HashMap<String, ConstraintMetaModel>();
        solverConfigBuildItem.getSolverConfigMap().forEach((solverName, solverConfig) -> {
            // Gizmo-generated member accessors are not yet available at build time.
            var originalDomainAccessType = solverConfig.getDomainAccessType();
            solverConfig.setDomainAccessType(DomainAccessType.REFLECTION);

            var solverFactory = SolverFactory.create(solverConfig);
            var constraintMetaModel = BeanUtil.buildConstraintMetaModel(solverFactory);
            // Avoid changing the original solver config.
            solverConfig.setDomainAccessType(originalDomainAccessType);
            constraintMetaModelsBySolverNames.put(solverName, constraintMetaModel);
        });

        constraintMetaModelBuildItemBuildProducer.produce(new ConstraintMetaModelBuildItem(constraintMetaModelsBySolverNames));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void recordAndRegisterRuntimeBeans(TimefoldRecorder recorder, RecorderContext recorderContext,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            SolverConfigBuildItem solverConfigBuildItem) {
        // Skip this extension if everything is missing.
        if (solverConfigBuildItem.getGeneratedGizmoClasses() == null) {
            return;
        }

        recorder.assertNoUnmatchedRuntimeProperties(solverConfigBuildItem.getSolverConfigMap().keySet());
        // Using the same name for synthetic beans is impossible, even if they are different types.
        // Therefore, we allow only the injection of SolverManager, except for the default solver,
        // which can inject all resources to be retro-compatible.
        solverConfigBuildItem.getSolverConfigMap().forEach((key, value) -> {
            if (solverConfigBuildItem.isDefaultSolverConfig(key)) {
                // The two configuration resources are required for DefaultTimefoldBeanProvider
                // to produce all available managed beans for the default solver.
                syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(SolverConfig.class)
                        .scope(Singleton.class)
                        .supplier(recorder.solverConfigSupplier(key, value,
                                GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                        solverConfigBuildItem
                                                .getGeneratedGizmoClasses().generatedGizmoMemberAccessorClassSet),
                                GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                        solverConfigBuildItem
                                                .getGeneratedGizmoClasses().generatedGizmoSolutionClonerClassSet)))
                        .setRuntimeInit()
                        .defaultBean()
                        .done());

                var solverManagerConfig = new SolverManagerConfig();
                syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(SolverManagerConfig.class)
                        .scope(Singleton.class)
                        .supplier(recorder.solverManagerConfig(solverManagerConfig))
                        .setRuntimeInit()
                        .defaultBean()
                        .done());
            }
            if (!TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME.equals(key)) {
                // The default SolverManager instance is generated by DefaultTimefoldBeanProvider
                syntheticBeanBuildItemBuildProducer.produce(
                        // We generate all required resources only to create a SolverManager and set it as managed bean
                        SyntheticBeanBuildItem.configure(SolverManager.class)
                                .scope(Singleton.class)
                                .addType(ParameterizedType.create(DotName.createSimple(SolverManager.class.getName()),
                                        Type.create(DotName.createSimple(value.getSolutionClass().getName()),
                                                Type.Kind.CLASS),
                                        TypeVariable.create(Object.class.getName())))
                                .supplier(recorder.solverManager(key, value,
                                        GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                                solverConfigBuildItem
                                                        .getGeneratedGizmoClasses().generatedGizmoMemberAccessorClassSet),
                                        GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                                solverConfigBuildItem
                                                        .getGeneratedGizmoClasses().generatedGizmoSolutionClonerClassSet)))
                                .setRuntimeInit()
                                .named(key)
                                .done());
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(RUNTIME_INIT)
    public void recordAndRegisterDevUIBean(
            TimefoldDevUIRecorder devUIRecorder,
            RecorderContext recorderContext,
            SolverConfigBuildItem solverConfigBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        if (solverConfigBuildItem.getGeneratedGizmoClasses() == null) {
            // Extension was skipped, so no solver configs
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(DevUISolverConfig.class)
                    .scope(ApplicationScoped.class)
                    .supplier(devUIRecorder.solverConfigSupplier(Collections.emptyMap(),
                            Collections.emptyMap(),
                            Collections.emptyMap()))
                    .defaultBean()
                    .setRuntimeInit()
                    .done());
            return;
        }
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(DevUISolverConfig.class)
                .scope(ApplicationScoped.class)
                .supplier(devUIRecorder.solverConfigSupplier(solverConfigBuildItem.getSolverConfigMap(),
                        GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                solverConfigBuildItem
                                        .getGeneratedGizmoClasses().generatedGizmoMemberAccessorClassSet),
                        GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                solverConfigBuildItem
                                        .getGeneratedGizmoClasses().generatedGizmoSolutionClonerClassSet)))
                .defaultBean()
                .setRuntimeInit()
                .done());
    }

    private void generateConstraintVerifier(SolverConfig solverConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        var constraintVerifierClassName = DotNames.CONSTRAINT_VERIFIER.toString();
        var scoreDirectorFactoryConfig = Objects.requireNonNull(solverConfig.getScoreDirectorFactoryConfig());
        var constraintProviderClass = scoreDirectorFactoryConfig.getConstraintProviderClass();
        if (constraintProviderClass != null && isClassDefined(constraintVerifierClassName)) {
            final var planningSolutionClass = Objects.requireNonNull(solverConfig.getSolutionClass());
            final var planningEntityClassList = Objects.requireNonNull(solverConfig.getEntityClassList());
            // TODO Don't duplicate defaults by using ConstraintVerifier.create(solverConfig) instead
            SyntheticBeanBuildItem.ExtendedBeanConfigurator constraintDescriptor =
                    SyntheticBeanBuildItem.configure(DotNames.CONSTRAINT_VERIFIER)
                            .scope(Singleton.class)
                            .creator(methodCreator -> {
                                var constraintProviderResultHandle =
                                        methodCreator.newInstance(MethodDescriptor.ofConstructor(constraintProviderClass));
                                var planningSolutionClassResultHandle = methodCreator.loadClass(planningSolutionClass);

                                var planningEntityClassesResultHandle =
                                        methodCreator.newArray(Class.class, planningEntityClassList.size());
                                for (var i = 0; i < planningEntityClassList.size(); i++) {
                                    var planningEntityClassResultHandle =
                                            methodCreator.loadClass(planningEntityClassList.get(i));
                                    methodCreator.writeArrayValue(planningEntityClassesResultHandle, i,
                                            planningEntityClassResultHandle);
                                }

                                var enabledPreviewFeatureSet = methodCreator.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(
                                                EnumSet.class, "noneOf", EnumSet.class, Class.class),
                                        methodCreator.loadClass(PreviewFeature.class));
                                if (solverConfig.getEnablePreviewFeatureSet() != null) {
                                    for (var enabledPreviewFeature : solverConfig.getEnablePreviewFeatureSet()) {
                                        methodCreator.invokeVirtualMethod(
                                                MethodDescriptor.ofMethod(EnumSet.class, "add", boolean.class, Object.class),
                                                enabledPreviewFeatureSet, methodCreator.load(enabledPreviewFeature));
                                    }
                                }
                                for (var i = 0; i < planningEntityClassList.size(); i++) {
                                    var planningEntityClassResultHandle =
                                            methodCreator.loadClass(planningEntityClassList.get(i));
                                    methodCreator.writeArrayValue(planningEntityClassesResultHandle, i,
                                            planningEntityClassResultHandle);
                                }

                                // Got incompatible class change error when trying to invoke static method on
                                // ConstraintVerifier.build(ConstraintProvider, Class, Class...)
                                var solutionDescriptorResultHandle = methodCreator.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(SolutionDescriptor.class, "buildSolutionDescriptor",
                                                SolutionDescriptor.class, Set.class, Class.class, Class[].class),
                                        enabledPreviewFeatureSet, planningSolutionClassResultHandle,
                                        planningEntityClassesResultHandle);
                                var constraintVerifierResultHandle = methodCreator.newInstance(
                                        MethodDescriptor.ofConstructor(
                                                "ai.timefold.solver.test.impl.score.stream.DefaultConstraintVerifier",
                                                ConstraintProvider.class, SolutionDescriptor.class),
                                        constraintProviderResultHandle, solutionDescriptorResultHandle);

                                methodCreator.returnValue(constraintVerifierResultHandle);
                            })
                            .addType(ParameterizedType.create(DotNames.CONSTRAINT_VERIFIER,
                                    new Type[] {
                                            Type.create(DotName.createSimple(constraintProviderClass.getName()),
                                                    Type.Kind.CLASS),
                                            Type.create(DotName.createSimple(planningSolutionClass.getName()), Type.Kind.CLASS)
                                    }, null))
                            .forceApplicationClass()
                            .defaultBean();
            syntheticBeanBuildItemBuildProducer.produce(constraintDescriptor.done());
        }
    }

    private void applySolverProperties(IndexView indexView, String solverName, SolverConfig solverConfig) {
        if (solverConfig.getSolutionClass() == null) {
            solverConfig.setSolutionClass(findFirstSolutionClass(indexView));
        }
        if (solverConfig.getEntityClassList() == null) {
            solverConfig.setEntityClassList(findEntityClassList(indexView));
        }

        applyScoreDirectorFactoryProperties(indexView, solverConfig);

        // Override the current configuration with values from the solver properties
        timefoldBuildTimeConfig.getSolverConfig(solverName).flatMap(SolverBuildTimeConfig::domainAccessType)
                .ifPresent(solverConfig::setDomainAccessType);

        if (solverConfig.getDomainAccessType() == null) {
            solverConfig.setDomainAccessType(DomainAccessType.GIZMO);
        }

        timefoldBuildTimeConfig.getSolverConfig(solverName)
                .flatMap(SolverBuildTimeConfig::enabledPreviewFeatures)
                .ifPresent(solverConfig::setEnablePreviewFeatureSet);

        timefoldBuildTimeConfig.getSolverConfig(solverName)
                .flatMap(SolverBuildTimeConfig::nearbyDistanceMeterClass)
                .ifPresent(clazz -> {
                    // We need to check the data type, as the Smallrye converter does not enforce it
                    if (!NearbyDistanceMeter.class.isAssignableFrom(clazz)) {
                        throw new IllegalArgumentException(
                                "The Nearby Selection Meter class (%s) of the solver config (%s) does not implement NearbyDistanceMeter."
                                        .formatted(clazz, solverName));
                    }
                    solverConfig.withNearbyDistanceMeterClass((Class<? extends NearbyDistanceMeter<?, ?>>) clazz);
                });

        // Termination properties are set at runtime
    }

    private static List<AnnotationInstance> getAllConcreteSolutionClasses(IndexView indexView) {
        return indexView.getAnnotations(DotNames.PLANNING_SOLUTION).stream()
                .filter(annotationInstance -> !annotationInstance.target().asClass().isAbstract())
                .toList();
    }

    private Class<?> findFirstSolutionClass(IndexView indexView) {
        var annotationInstanceCollection = getAllConcreteSolutionClasses(indexView);
        var solutionTarget = annotationInstanceCollection.iterator().next().target();
        return convertClassInfoToClass(solutionTarget.asClass());
    }

    private List<Class<?>> findEntityClassList(IndexView indexView) {
        // All annotated classes
        var entityList = new ArrayList<Class<?>>(indexView.getAnnotations(DotNames.PLANNING_ENTITY).stream()
                .map(AnnotationInstance::target)
                .map(target -> (Class<?>) convertClassInfoToClass(target.asClass()))
                .toList());
        // Now we search for child classes as well
        var childEntityList = new ArrayList<>(entityList);
        while (!childEntityList.isEmpty()) {
            var entityClass = childEntityList.remove(0);
            // Check all subclasses
            var childEntityClassList = indexView.getAllKnownSubclasses(entityClass).stream()
                    .map(target -> (Class<?>) convertClassInfoToClass(target.asClass()))
                    .toList();
            if (!childEntityClassList.isEmpty()) {
                entityList.addAll(childEntityClassList.stream().filter(c -> !entityList.contains(c)).toList());
                childEntityList.addAll(childEntityClassList);
            }
            // Check all subinterfaces
            var childEntityInterfaceList = indexView.getAllKnownSubinterfaces(entityClass).stream()
                    .map(target -> (Class<?>) convertClassInfoToClass(target.asClass()))
                    .toList();
            if (!childEntityInterfaceList.isEmpty()) {
                entityList.addAll(childEntityInterfaceList.stream().filter(c -> !entityList.contains(c)).toList());
                childEntityList.addAll(childEntityInterfaceList);
            }
            // Check all implementors
            var childEntityImplementorList = indexView.getAllKnownImplementations(entityClass).stream()
                    .map(target -> (Class<?>) convertClassInfoToClass(target.asClass()))
                    .toList();
            if (!childEntityImplementorList.isEmpty()) {
                entityList.addAll(childEntityImplementorList.stream().filter(c -> !entityList.contains(c)).toList());
                childEntityList.addAll(childEntityImplementorList);
            }
        }
        return entityList;
    }

    private void registerClassesFromAnnotations(IndexView indexView, Set<Class<?>> reflectiveClassSet) {
        for (var beanDefiningAnnotation : DotNames.BeanDefiningAnnotations.values()) {
            for (var annotationInstance : indexView
                    .getAnnotationsWithRepeatable(beanDefiningAnnotation.getAnnotationDotName(), indexView)) {
                for (var parameterName : beanDefiningAnnotation.getParameterNames()) {
                    var value = annotationInstance.value(parameterName);

                    // We don't care about the default/null type.
                    if (value != null) {
                        var type = value.asClass();
                        try {
                            var beanClass = Class.forName(type.name().toString(), false,
                                    Thread.currentThread().getContextClassLoader());
                            reflectiveClassSet.add(beanClass);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Cannot find bean class (%s) referenced in annotation (%s)."
                                    .formatted(type.name(), annotationInstance));
                        }
                    }
                }
            }
        }
    }

    protected void applyScoreDirectorFactoryProperties(IndexView indexView, SolverConfig solverConfig) {
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            var scoreDirectorFactoryConfig = defaultScoreDirectoryFactoryConfig(indexView);
            solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        }
    }

    private boolean isClassDefined(String className) {
        var classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private ScoreDirectorFactoryConfig defaultScoreDirectoryFactoryConfig(IndexView indexView) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(
                findFirstImplementingConcreteClass(DotNames.EASY_SCORE_CALCULATOR, indexView));
        scoreDirectorFactoryConfig.setConstraintProviderClass(
                findFirstImplementingConcreteClass(DotNames.CONSTRAINT_PROVIDER, indexView));
        scoreDirectorFactoryConfig.setIncrementalScoreCalculatorClass(
                findFirstImplementingConcreteClass(DotNames.INCREMENTAL_SCORE_CALCULATOR, indexView));
        return scoreDirectorFactoryConfig;
    }

    private <T> Class<? extends T> findFirstImplementingConcreteClass(DotName targetDotName, IndexView indexView) {
        var classInfoCollection = indexView.getAllKnownImplementations(targetDotName).stream()
                .filter(classInfo -> !classInfo.isAbstract())
                .toList();
        if (classInfoCollection.isEmpty()) {
            return null;
        }
        var classInfo = classInfoCollection.iterator().next();
        return convertClassInfoToClass(classInfo);
    }

    private String convertAnnotationInstancesToString(Collection<AnnotationInstance> annotationInstanceCollection) {
        return "[%s]".formatted(annotationInstanceCollection.stream().map(instance -> instance.target().toString())
                .collect(Collectors.joining(", ")));
    }

    private <T> Class<? extends T> convertClassInfoToClass(ClassInfo classInfo) {
        var className = classInfo.name().toString();
        var classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return (Class<? extends T>) classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (%s) cannot be created during deployment.".formatted(className), e);
        }
    }

    private GeneratedGizmoClasses generateDomainAccessors(Map<String, SolverConfig> solverConfigMap, IndexView indexView,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers, Set<Class<?>> reflectiveClassSet) {
        // Use mvn quarkus:dev -Dquarkus.debug.generated-classes-dir=dump-classes
        // to dump generated classes
        var classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        var beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        var generatedMemberAccessorsClassNameSet = new HashSet<String>();
        var gizmoSolutionClonerClassNameSet = new HashSet<String>();

        /*
         * TODO consistently change the name "entity" to something less confusing
         * "entity" in this context means both "planning solution",
         * "planning entity" and other things as well.
         */
        assertSolverDomainAccessType(solverConfigMap);
        var entityEnhancer = new GizmoMemberAccessorEntityEnhancer();
        if (solverConfigMap.values().stream().anyMatch(c -> c.getDomainAccessType() == DomainAccessType.GIZMO)) {
            var membersToGeneratedAccessorsForCollection = new ArrayList<AnnotationInstance>();

            // Every entity and solution gets scanned for annotations.
            // Annotated members get their accessors generated.
            for (var dotName : DotNames.GIZMO_MEMBER_ACCESSOR_ANNOTATIONS) {
                membersToGeneratedAccessorsForCollection.addAll(indexView.getAnnotationsWithRepeatable(dotName, indexView));
            }
            for (var shadowSources : indexView.getAnnotations(DotNames.SHADOW_SOURCES)) {
                var rootType = shadowSources.target().asMethod().declaringClass();
                var sources = shadowSources.value().asStringArray();
                for (var source : sources) {
                    var currentType = rootType;
                    var parts = source.split(RootVariableSource.MEMBER_SEPERATOR_REGEX);
                    for (var part : parts) {
                        var memberName = part.endsWith(RootVariableSource.COLLECTION_REFERENCE_SUFFIX)
                                ? part.substring(0, part.length() - RootVariableSource.COLLECTION_REFERENCE_SUFFIX.length())
                                : part;
                        ClassInfo nextType = null;
                        AnnotationTarget target = null;
                        var field = currentType.field(memberName);
                        if (field == null) {
                            var method = currentType
                                    .method("get" + memberName.substring(0, 1).toUpperCase() + memberName.substring(1));
                            if (method == null) {
                                throw new IllegalStateException(
                                        "Cannot find field or getter for (%s) in class (%s) which is referenced by the %s path (%s) with root entity (%s)."
                                                .formatted(memberName, currentType.simpleName(),
                                                        ShadowSources.class.getSimpleName(), source, rootType.simpleName()));
                            }
                            target = method;
                            nextType = method.declaringClass();
                        } else {
                            target = field;
                            nextType = field.declaringClass();
                        }
                        // Create a fake annotation for it
                        membersToGeneratedAccessorsForCollection.add(
                                AnnotationInstance.builder(DotNames.SHADOW_SOURCES)
                                        .value(source)
                                        .buildWithTarget(target));
                        currentType = nextType;
                    }
                }
            }
            membersToGeneratedAccessorsForCollection.removeIf(this::shouldIgnoreMember);

            // Fail fast on auto-discovery.
            var planningSolutionAnnotationInstanceCollection = getAllConcreteSolutionClasses(indexView);
            var unconfiguredSolverConfigList = solverConfigMap.entrySet().stream()
                    .filter(e -> e.getValue().getSolutionClass() == null)
                    .map(Map.Entry::getKey)
                    .toList();
            var unusedSolutionClassList = planningSolutionAnnotationInstanceCollection.stream()
                    .map(planningClass -> planningClass.target().asClass().name().toString())
                    .filter(planningClassName -> reflectiveClassSet.stream()
                            .noneMatch(clazz -> clazz.getName().equals(planningClassName)))
                    .toList();
            if (planningSolutionAnnotationInstanceCollection.isEmpty()) {
                throw new IllegalStateException(
                        "No classes found with a @%s annotation.".formatted(PlanningSolution.class.getSimpleName()));
            } else if (planningSolutionAnnotationInstanceCollection.size() > 1 && !unconfiguredSolverConfigList.isEmpty()
                    && !unusedSolutionClassList.isEmpty()) {
                throw new IllegalStateException(
                        "Unused classes (%s) found with a @%s annotation.".formatted(String.join(", ", unusedSolutionClassList),
                                PlanningSolution.class.getSimpleName()));
            }

            planningSolutionAnnotationInstanceCollection.forEach(planningSolutionAnnotationInstance -> {
                var autoDiscoverMemberType = planningSolutionAnnotationInstance.values().stream()
                        .filter(v -> v.name().equals("autoDiscoverMemberType"))
                        .findFirst()
                        .map(AnnotationValue::asEnum)
                        .map(AutoDiscoverMemberType::valueOf)
                        .orElse(AutoDiscoverMemberType.NONE);

                if (autoDiscoverMemberType != AutoDiscoverMemberType.NONE) {
                    throw new UnsupportedOperationException("""
                            Auto-discovery of members using %s is not supported under Quarkus.
                            Remove the autoDiscoverMemberType property from the @%s annotation
                            and explicitly annotate the fields or getters with annotations such as @%s, @%s or @%s."""
                            .strip()
                            .formatted(
                                    AutoDiscoverMemberType.class.getSimpleName(),
                                    PlanningSolution.class.getSimpleName(),
                                    PlanningScore.class.getSimpleName(),
                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                    ProblemFactCollectionProperty.class.getSimpleName()));
                }
            });

            for (var annotatedMember : membersToGeneratedAccessorsForCollection) {
                switch (annotatedMember.target().kind()) {
                    case FIELD -> {
                        var fieldInfo = annotatedMember.target().asField();
                        var classInfo = fieldInfo.declaringClass();
                        buildFieldAccessor(annotatedMember, generatedMemberAccessorsClassNameSet, entityEnhancer, classOutput,
                                classInfo, fieldInfo, transformers);
                        if (annotatedMember.name().equals(DotNames.CASCADING_UPDATE_SHADOW_VARIABLE)) {
                            // The source method name also must be included
                            // targetMethodName is a required field and is always present
                            var targetMethodName = annotatedMember.value("targetMethodName").asString();
                            var methodInfo = classInfo.method(targetMethodName);
                            buildMethodAccessor(null, generatedMemberAccessorsClassNameSet, entityEnhancer, classOutput,
                                    classInfo, methodInfo, false, transformers);
                        } else if (annotatedMember.name().equals(DotNames.SHADOW_VARIABLE)
                                && annotatedMember.value("supplierName") != null) {
                            // The source method name also must be included
                            var targetMethodName = annotatedMember.value("supplierName")
                                    .asString();
                            var methodInfo = classInfo.method(targetMethodName);
                            buildMethodAccessor(annotatedMember, generatedMemberAccessorsClassNameSet, entityEnhancer,
                                    classOutput,
                                    classInfo, methodInfo, true, transformers);
                        }
                    }
                    case METHOD -> {
                        var methodInfo = annotatedMember.target().asMethod();
                        var classInfo = methodInfo.declaringClass();
                        buildMethodAccessor(annotatedMember, generatedMemberAccessorsClassNameSet, entityEnhancer, classOutput,
                                classInfo, methodInfo, true, transformers);
                    }
                    default -> {
                        throw new IllegalStateException(
                                "The member (%s) is not on a field or method.".formatted(annotatedMember));
                    }
                }
            }
            // The ConstraintWeightOverrides field is not annotated, but it needs a member accessor
            var solutionClassInstance = planningSolutionAnnotationInstanceCollection.iterator().next();
            var solutionClassInfo = solutionClassInstance.target().asClass();
            var constraintFieldInfo = solutionClassInfo.fields().stream()
                    .filter(f -> f.type().name().equals(DotNames.CONSTRAINT_WEIGHT_OVERRIDES))
                    .findFirst()
                    .orElse(null);
            if (constraintFieldInfo != null) {
                // Prefer method to field
                var solutionClass = convertClassInfoToClass(solutionClassInfo);
                var constraintMethod =
                        ReflectionHelper.getGetterMethod(solutionClass, constraintFieldInfo.name());
                var constraintMethodInfo = solutionClassInfo.methods().stream()
                        .filter(m -> constraintMethod != null && m.name().equals(constraintMethod.getName())
                                && m.parametersCount() == 0)
                        .findFirst()
                        .orElse(null);
                if (constraintMethodInfo != null) {
                    buildMethodAccessor(solutionClassInstance, generatedMemberAccessorsClassNameSet, entityEnhancer,
                            classOutput, solutionClassInfo, constraintMethodInfo, true, transformers);
                } else {
                    buildFieldAccessor(solutionClassInstance, generatedMemberAccessorsClassNameSet, entityEnhancer, classOutput,
                            solutionClassInfo, constraintFieldInfo, transformers);
                }
            }
            // Using REFLECTION domain access type so Timefold doesn't try to generate GIZMO code
            solverConfigMap.values().forEach(c -> {
                var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(
                        c.getEnablePreviewFeatureSet(), DomainAccessType.REFLECTION,
                        c.getSolutionClass(), null, null, c.getEntityClassList());
                gizmoSolutionClonerClassNameSet
                        .add(entityEnhancer.generateSolutionCloner(solutionDescriptor, classOutput, indexView, transformers));
            });
        }

        entityEnhancer.generateGizmoBeanFactory(beanClassOutput, reflectiveClassSet, transformers);
        return new GeneratedGizmoClasses(generatedMemberAccessorsClassNameSet, gizmoSolutionClonerClassNameSet);
    }

    private static void buildFieldAccessor(AnnotationInstance annotatedMember, Set<String> generatedMemberAccessorsClassNameSet,
            GizmoMemberAccessorEntityEnhancer entityEnhancer, ClassOutput classOutput, ClassInfo classInfo, FieldInfo fieldInfo,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        try {
            generatedMemberAccessorsClassNameSet.add(
                    entityEnhancer.generateFieldAccessor(annotatedMember, classOutput, fieldInfo,
                            transformers));
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new IllegalStateException("Fail to generate member accessor for field (%s) of the class(%s)."
                    .formatted(fieldInfo.name(), classInfo.name().toString()), e);
        }
    }

    private static void buildMethodAccessor(AnnotationInstance annotatedMember,
            Set<String> generatedMemberAccessorsClassNameSet,
            GizmoMemberAccessorEntityEnhancer entityEnhancer, ClassOutput classOutput, ClassInfo classInfo,
            MethodInfo methodInfo, boolean requiredReturnType, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        try {
            generatedMemberAccessorsClassNameSet.add(entityEnhancer.generateMethodAccessor(annotatedMember,
                    classOutput, classInfo, methodInfo, requiredReturnType, transformers));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Failed to generate member accessor for the method (%s) of the class (%s)."
                            .formatted(methodInfo.name(), classInfo.name()),
                    e);
        }
    }

    private void assertSolverDomainAccessType(Map<String, SolverConfig> solverConfigMap) {
        // All solver must use the same domain access type
        if (solverConfigMap.values().stream().map(SolverConfig::getDomainAccessType).distinct().count() > 1) {
            throw new ConfigurationException(
                    """
                            The domain access type must be unique across all Solver configurations.
                            %s""".formatted(solverConfigMap.entrySet().stream()
                            .map(e -> format("quarkus.timefold.\"%s\".domain-access-type=%s",
                                    e.getKey(), e.getValue().getDomainAccessType()))
                            .collect(Collectors.joining("\n"))));
        }
    }

    private boolean shouldIgnoreMember(AnnotationInstance annotationInstance) {
        switch (annotationInstance.target().kind()) {
            case FIELD:
                return (annotationInstance.target().asField().flags() & Modifier.STATIC) != 0;
            case METHOD:
                return (annotationInstance.target().asMethod().flags() & Modifier.STATIC) != 0;
            default:
                throw new IllegalArgumentException(
                        "Annotation (%s) can only be applied to methods and fields.".formatted(annotationInstance.name()));
        }
    }

    private void registerCustomClassesFromSolverConfig(SolverConfig solverConfig, Set<Class<?>> reflectiveClassSet) {
        solverConfig.visitReferencedClasses(clazz -> {
            if (clazz != null) {
                reflectiveClassSet.add(clazz);
            }
        });
    }

}
