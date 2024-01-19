package ai.timefold.solver.quarkus.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryService;
import ai.timefold.solver.core.impl.score.stream.JoinerService;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.bean.DefaultTimefoldBeanProvider;
import ai.timefold.solver.quarkus.bean.TimefoldSolverBannerBean;
import ai.timefold.solver.quarkus.bean.UnavailableTimefoldBeanProvider;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;
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
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigurationException;

class TimefoldProcessor {

    private static final Logger log = Logger.getLogger(TimefoldProcessor.class.getName());

    TimefoldBuildTimeConfig timefoldBuildTimeConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("timefold-solver");
    }

    @BuildStep
    void registerSpi(BuildProducer<ServiceProviderBuildItem> services) {
        Stream.of(ScoreDirectorFactoryService.class, JoinerService.class, TimefoldSolverEnterpriseService.class)
                .forEach(service -> registerSpi(service, services));
    }

    private static void registerSpi(Class<?> serviceClass, BuildProducer<ServiceProviderBuildItem> services) {
        String serviceName = serviceClass.getName();
        String service = "META-INF/services/" + serviceName;
        try {
            // Find out all the provider implementation classes listed in the service files.
            Set<String> implementations =
                    ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(), service);
            // Register every listed implementation class, so they can be instantiated in native-image at run-time.
            services.produce(new ServiceProviderBuildItem(serviceName, implementations.toArray(new String[0])));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible state: Failed registering service " + serviceClass.getCanonicalName(),
                    e);
        }
    }

    @BuildStep
    void watchSolverConfigXml(BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        String solverConfigXML = timefoldBuildTimeConfig.solverConfigXml()
                .orElse(TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL);
        Set<String> solverConfigXMLFiles = new HashSet<>();
        solverConfigXMLFiles.add(solverConfigXML);
        timefoldBuildTimeConfig.solver().values().stream()
                .map(SolverBuildTimeConfig::solverConfigXml)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(solverConfigXMLFiles::add);
        solverConfigXMLFiles.forEach(file -> hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(file)));
    }

    @BuildStep
    IndexDependencyBuildItem indexDependencyBuildItem() {
        // Add @PlanningEntity and other annotations in the Jandex index for Gizmo
        return new IndexDependencyBuildItem("ai.timefold.solver", "timefold-solver-core-impl");
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
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

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
        IndexView indexView = combinedIndex.getIndex();

        // Only skip this extension if everything is missing. Otherwise, if some parts are missing, fail fast later.
        if (indexView.getAnnotations(DotNames.PLANNING_SOLUTION).isEmpty()
                && indexView.getAnnotations(DotNames.PLANNING_ENTITY).isEmpty()) {
            log.warn("Skipping Timefold extension because there are no @" + PlanningSolution.class.getSimpleName()
                    + " or @" + PlanningEntity.class.getSimpleName() + " annotated classes."
                    + "\nIf your domain classes are located in a dependency of this project, maybe try generating"
                    + " the Jandex index by using the jandex-maven-plugin in that dependency, or by adding"
                    + "application.properties entries (quarkus.index-dependency.<name>.group-id"
                    + " and quarkus.index-dependency.<name>.artifact-id).");
            additionalBeans.produce(new AdditionalBeanBuildItem(UnavailableTimefoldBeanProvider.class));
            Map<String, SolverConfig> solverConfig = new HashMap<>();
            this.timefoldBuildTimeConfig.solver().keySet().forEach(solverName -> solverConfig.put(solverName, null));
            return new SolverConfigBuildItem(solverConfig, null);
        }

        // Quarkus extensions must always use getContextClassLoader()
        // Internally, Timefold defaults the ClassLoader to getContextClassLoader() too
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Map<String, SolverConfig> solverConfigMap = new HashMap<>();
        // Step 1 - create all SolverConfig
        // If the config map is empty, we build the config using the default solver name
        if (timefoldBuildTimeConfig.solver().isEmpty()) {
            solverConfigMap.put(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME,
                    createSolverConfig(classLoader, TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME));
        } else {
            // One config per solver mapped name
            this.timefoldBuildTimeConfig.solver().keySet().forEach(solverName -> solverConfigMap.put(solverName,
                    createSolverConfig(classLoader, solverName)));
        }

        // Step 2 - validate all SolverConfig definitions
        assertNoMemberAnnotationWithoutClassAnnotation(indexView);
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
        Collection<AnnotationInstance> timefoldFieldAnnotations = new HashSet<>();

        for (DotName annotationName : DotNames.PLANNING_ENTITY_FIELD_ANNOTATIONS) {
            timefoldFieldAnnotations.addAll(indexView.getAnnotationsWithRepeatable(annotationName, indexView));
        }

        for (AnnotationInstance annotationInstance : timefoldFieldAnnotations) {
            AnnotationTarget annotationTarget = annotationInstance.target();
            ClassInfo declaringClass;
            String prefix;
            switch (annotationTarget.kind()) {
                case FIELD:
                    prefix = "The field (" + annotationTarget.asField().name() + ") ";
                    declaringClass = annotationTarget.asField().declaringClass();
                    break;
                case METHOD:
                    prefix = "The method (" + annotationTarget.asMethod().name() + ") ";
                    declaringClass = annotationTarget.asMethod().declaringClass();
                    break;
                default:
                    throw new IllegalStateException(
                            "Member annotation @" + annotationInstance.name().withoutPackagePrefix() + " is on ("
                                    + annotationTarget +
                                    "), which is an invalid target type (" + annotationTarget.kind() +
                                    ") for @" + annotationInstance.name().withoutPackagePrefix() + ".");
            }

            if (!declaringClass.annotationsMap().containsKey(DotNames.PLANNING_ENTITY)) {
                throw new IllegalStateException(prefix + "with a @" +
                        annotationInstance.name().withoutPackagePrefix() +
                        " annotation is in a class (" + declaringClass.name()
                        + ") that does not have a @" + PlanningEntity.class.getSimpleName() +
                        " annotation.\n" +
                        "Maybe add a @" + PlanningEntity.class.getSimpleName() +
                        " annotation on the class (" + declaringClass.name() + ").");
            }
        }
    }

    private void assertSolverConfigSolutionClasses(IndexView indexView, Map<String, SolverConfig> solverConfigMap) {
        // Validate the solution class        
        // No solution class
        assertEmptyInstances(indexView, DotNames.PLANNING_SOLUTION);
        // Multiple classes and single solver
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_SOLUTION);
        if (annotationInstances.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException("Multiple classes (%s) found in the classpath with a @%s annotation.".formatted(
                    convertAnnotationInstancesToString(annotationInstances), PlanningSolution.class.getSimpleName()));
        }
        // Multiple classes and at least one solver config does not specify the solution class
        List<String> solverConfigWithoutSolutionClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getSolutionClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (annotationInstances.size() > 1 && !solverConfigWithoutSolutionClassList.isEmpty()) {
            throw new IllegalStateException(
                    """
                            Some solver configs (%s) don't specify a %s class, yet there are multiple available (%s) on the classpath.
                            Maybe set the XML config file to the related solver configs, or add the missing solution classes to the XML files,
                            or remove the unnecessary solution classes from the classpath.
                            """
                            .formatted(String.join(", ", solverConfigWithoutSolutionClassList),
                                    PlanningSolution.class.getSimpleName(),
                                    convertAnnotationInstancesToString(annotationInstances)));
        }
        // Unused solution classes
        List<String> unusedSolutionClasses = annotationInstances.stream()
                .map(planningClass -> planningClass.target().asClass().name().toString())
                .filter(planningClassName -> solverConfigMap.values().stream().filter(c -> c.getSolutionClass() != null)
                        .noneMatch(c -> c.getSolutionClass().getName().equals(planningClassName)))
                .toList();
        if (annotationInstances.size() > 1 && !unusedSolutionClasses.isEmpty()) {
            throw new IllegalStateException(
                    "Unused classes ([%s]) found with a @%s annotation.".formatted(String.join(", ", unusedSolutionClasses),
                            PlanningSolution.class.getSimpleName()));
        }
        // Validate the solution classes target types
        List<AnnotationTarget> targetList = annotationInstances.stream()
                .map(AnnotationInstance::target)
                .toList();
        assertTargetClasses(targetList, DotNames.PLANNING_SOLUTION);
    }

    private void assertSolverConfigEntityClasses(IndexView indexView) {
        // No entity classes
        assertEmptyInstances(indexView, DotNames.PLANNING_ENTITY);
        // Validate the entity classes target types
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_ENTITY);
        List<AnnotationTarget> targetList = annotationInstances.stream()
                .map(AnnotationInstance::target)
                .toList();
        assertTargetClasses(targetList, DotNames.PLANNING_ENTITY);
    }

    private void assertSolverConfigConstraintClasses(IndexView indexView, Map<String, SolverConfig> solverConfigMap) {
        Collection<ClassInfo> simpleScoreClasses = indexView.getAllKnownImplementors(DotNames.EASY_SCORE_CALCULATOR);
        Collection<ClassInfo> constraintScoreClasses = indexView.getAllKnownImplementors(DotNames.CONSTRAINT_PROVIDER);
        Collection<ClassInfo> incrementalScoreClasses =
                indexView.getAllKnownImplementors(DotNames.INCREMENTAL_SCORE_CALCULATOR);
        // No score classes
        if (simpleScoreClasses.isEmpty() && constraintScoreClasses.isEmpty() && incrementalScoreClasses.isEmpty()) {
            throw new IllegalStateException(
                    "No classes found that implement %s, %s, or %s.".formatted(EasyScoreCalculator.class.getSimpleName(),
                            ConstraintProvider.class.getSimpleName(), IncrementalScoreCalculator.class.getSimpleName()));
        }
        // Multiple classes and single solver
        String errorMessage = "Multiple score classes classes (%s) that implements %s were found in the classpath.";
        if (simpleScoreClasses.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    simpleScoreClasses.stream().map(c -> c.name().toString()).collect(Collectors.joining(", ")),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        if (constraintScoreClasses.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    constraintScoreClasses.stream().map(c -> c.name().toString()).collect(Collectors.joining(", ")),
                    ConstraintProvider.class.getSimpleName()));
        }
        if (incrementalScoreClasses.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    incrementalScoreClasses.stream().map(c -> c.name().toString()).collect(Collectors.joining(", ")),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
        // Multiple classes and at least one solver config does not specify the score class
        errorMessage = """
                Some solver configs (%s) don't specify a %s score class, yet there are multiple available (%s) on the classpath.
                Maybe set the XML config file to the related solver configs, or add the missing score classes to the XML files,
                or remove the unnecessary score classes from the classpath.
                """;
        List<String> solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (simpleScoreClasses.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    EasyScoreCalculator.class.getSimpleName(),
                    simpleScoreClasses.stream().map(c -> c.name().toString()).collect(Collectors.joining(", "))));
        }
        solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getConstraintProviderClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (constraintScoreClasses.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    ConstraintProvider.class.getSimpleName(),
                    constraintScoreClasses.stream().map(c -> c.name().toString()).collect(Collectors.joining(", "))));
        }
        solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (incrementalScoreClasses.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    IncrementalScoreCalculator.class.getSimpleName(),
                    incrementalScoreClasses.stream().map(c -> c.name().toString()).collect(Collectors.joining(", "))));
        }
        // Unused score classes
        List<String> solverConfigWithUnusedSolutionClassList = simpleScoreClasses.stream()
                .map(clazz -> clazz.name().toString())
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass().getName()
                                .equals(className)))
                .toList();
        errorMessage = "Unused classes ([%s]) that implements %s were found.";
        if (simpleScoreClasses.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        solverConfigWithUnusedSolutionClassList = constraintScoreClasses.stream()
                .map(clazz -> clazz.name().toString())
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getConstraintProviderClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName()
                                .equals(className)))
                .toList();
        if (constraintScoreClasses.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    ConstraintProvider.class.getSimpleName()));
        }
        solverConfigWithUnusedSolutionClassList = incrementalScoreClasses.stream()
                .map(clazz -> clazz.name().toString())
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass().getName()
                                .equals(className)))
                .toList();
        if (incrementalScoreClasses.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
    }

    private void assertEmptyInstances(IndexView indexView, DotName dotName) {
        // Validate the solution class
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(dotName);
        // No solution class
        if (annotationInstances.isEmpty()) {
            try {
                throw new IllegalStateException(
                        "No classes found with a @%s annotation.".formatted(Class.forName(dotName.local()).getSimpleName()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void assertTargetClasses(List<AnnotationTarget> targetList, DotName dotName) {
        if (targetList.stream().anyMatch(target -> target.kind() != AnnotationTarget.Kind.CLASS)) {
            throw new IllegalStateException(
                    "All classes ([%s]) annotated with @%s must be a class.".formatted(
                            targetList.stream().map(t -> t.asClass().name().toString()).collect(Collectors.joining(", ")),
                            dotName.local()));
        }
    }

    private SolverConfig createSolverConfig(ClassLoader classLoader, String solverName) {
        // 1 - The solver configuration takes precedence over root and default settings
        Optional<String> solverConfigXml = this.timefoldBuildTimeConfig.getSolverConfig(solverName)
                .flatMap(SolverBuildTimeConfig::solverConfigXml);

        // 2 - Root settings
        if (solverConfigXml.isEmpty()) {
            solverConfigXml = this.timefoldBuildTimeConfig.solverConfigXml();
        }

        SolverConfig solverConfig;
        if (solverConfigXml.isPresent()) {
            String solverUrl = solverConfigXml.get();
            if (classLoader.getResource(solverUrl) == null) {
                String message = "Invalid quarkus.timefold.solverConfigXML property ("
                        + solverUrl + "): that classpath resource does not exist.";
                if (!solverName.equals(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME)) {
                    message = "Invalid quarkus.timefold.\"" + solverName + "\".solverConfigXML property ("
                            + solverUrl + "): that classpath resource does not exist.";
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

        if (solverConfig.getSolutionClass() != null) {
            // Need to register even when using GIZMO so annotations are preserved
            Type jandexType = Type.create(DotName.createSimple(solverConfig.getSolutionClass().getName()), Type.Kind.CLASS);
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
    @Record(RUNTIME_INIT)
    void recordAndRegisterRuntimeBeans(TimefoldRecorder recorder, RecorderContext recorderContext,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            SolverConfigBuildItem solverConfigBuildItem,
            TimefoldRuntimeConfig runtimeConfig) {
        // Skip this extension if everything is missing.
        if (solverConfigBuildItem.getGeneratedGizmoClasses() == null) {
            return;
        }

        // Using the same name for synthetic beans is impossible, even if they are different types. Therefore, we allow
        // only the injection of SolverManager, except for the default solver, which can inject all resources to be
        // retro-compatible.
        solverConfigBuildItem.getAllSolverConfigurations().forEach((key, value) -> {
            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                // The two configuration resources are required for DefaultTimefoldBeanProvider produce all available
                // managed beans for the default solver
                syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(SolverConfig.class)
                        .scope(Singleton.class)
                        .supplier(recorder.solverConfigSupplier(key, value, runtimeConfig,
                                GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                        solverConfigBuildItem
                                                .getGeneratedGizmoClasses().generatedGizmoMemberAccessorClassSet),
                                GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                        solverConfigBuildItem
                                                .getGeneratedGizmoClasses().generatedGizmoSolutionClonerClassSet)))
                        .setRuntimeInit()
                        .defaultBean()
                        .done());

                SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
                syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(SolverManagerConfig.class)
                        .scope(Singleton.class)
                        .supplier(recorder.solverManagerConfig(solverManagerConfig, runtimeConfig))
                        .setRuntimeInit()
                        .defaultBean()
                        .done());
            }

            // The default SolveManager instance is generated by DefaultTimefoldBeanProvider
            if (!timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                syntheticBeanBuildItemBuildProducer.produce(
                        // We generate all required resources only to create a SolveManager and set it as managed bean
                        SyntheticBeanBuildItem.configure(SolverManager.class)
                                .scope(Singleton.class)
                                .addType(ParameterizedType.create(DotName.createSimple(SolverManager.class.getName()),
                                        Type.create(DotName.createSimple(value.getSolutionClass().getName()),
                                                Type.Kind.CLASS),
                                        TypeVariable.create(Object.class.getName())))
                                .supplier(recorder.solverManager(key, value, runtimeConfig,
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
            TimefoldRuntimeConfig runtimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(DevUISolverConfig.class)
                .scope(ApplicationScoped.class)
                .supplier(devUIRecorder.solverConfigSupplier(solverConfigBuildItem.getAllSolverConfigurations(), runtimeConfig,
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
        String constraintVerifierClassName = DotNames.CONSTRAINT_VERIFIER.toString();
        if (solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass() != null &&
                isClassDefined(constraintVerifierClassName)) {
            final Class<?> constraintProviderClass = solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass();
            final Class<?> planningSolutionClass = solverConfig.getSolutionClass();
            final List<Class<?>> planningEntityClasses = solverConfig.getEntityClassList();
            // TODO Don't duplicate defaults by using ConstraintVerifier.create(solverConfig) instead
            SyntheticBeanBuildItem.ExtendedBeanConfigurator constraintDescriptor =
                    SyntheticBeanBuildItem.configure(DotNames.CONSTRAINT_VERIFIER)
                            .scope(Singleton.class)
                            .creator(methodCreator -> {
                                ResultHandle constraintProviderResultHandle =
                                        methodCreator.newInstance(MethodDescriptor.ofConstructor(constraintProviderClass));
                                ResultHandle planningSolutionClassResultHandle = methodCreator.loadClass(planningSolutionClass);

                                ResultHandle planningEntityClassesResultHandle =
                                        methodCreator.newArray(Class.class, planningEntityClasses.size());
                                for (int i = 0; i < planningEntityClasses.size(); i++) {
                                    ResultHandle planningEntityClassResultHandle =
                                            methodCreator.loadClass(planningEntityClasses.get(i));
                                    methodCreator.writeArrayValue(planningEntityClassesResultHandle, i,
                                            planningEntityClassResultHandle);
                                }

                                // Got incompatible class change error when trying to invoke static method on
                                // ConstraintVerifier.build(ConstraintProvider, Class, Class...)
                                ResultHandle solutionDescriptorResultHandle = methodCreator.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(SolutionDescriptor.class, "buildSolutionDescriptor",
                                                SolutionDescriptor.class, Class.class, Class[].class),
                                        planningSolutionClassResultHandle, planningEntityClassesResultHandle);
                                ResultHandle constraintVerifierResultHandle = methodCreator.newInstance(
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
        timefoldBuildTimeConfig.getSolverConfig(solverName).flatMap(SolverBuildTimeConfig::environmentMode)
                .ifPresent(solverConfig::setEnvironmentMode);
        timefoldBuildTimeConfig.getSolverConfig(solverName).flatMap(SolverBuildTimeConfig::daemon)
                .ifPresent(solverConfig::setDaemon);
        timefoldBuildTimeConfig.getSolverConfig(solverName).flatMap(SolverBuildTimeConfig::domainAccessType)
                .ifPresent(solverConfig::setDomainAccessType);

        if (solverConfig.getDomainAccessType() == null) {
            solverConfig.setDomainAccessType(DomainAccessType.GIZMO);
        }
        // Termination properties are set at runtime
    }

    private Class<?> findFirstSolutionClass(IndexView indexView) {
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_SOLUTION);
        AnnotationTarget solutionTarget = annotationInstances.iterator().next().target();
        return convertClassInfoToClass(solutionTarget.asClass());
    }

    private List<Class<?>> findEntityClassList(IndexView indexView) {
        return indexView.getAnnotations(DotNames.PLANNING_ENTITY).stream()
                .map(AnnotationInstance::target)
                .map(target -> (Class<?>) convertClassInfoToClass(target.asClass()))
                .collect(toList());
    }

    private void registerClassesFromAnnotations(IndexView indexView, Set<Class<?>> reflectiveClassSet) {
        for (DotNames.BeanDefiningAnnotations beanDefiningAnnotation : DotNames.BeanDefiningAnnotations.values()) {
            for (AnnotationInstance annotationInstance : indexView
                    .getAnnotationsWithRepeatable(beanDefiningAnnotation.getAnnotationDotName(), indexView)) {
                for (String parameterName : beanDefiningAnnotation.getParameterNames()) {
                    AnnotationValue value = annotationInstance.value(parameterName);

                    // We don't care about the default/null type.
                    if (value != null) {
                        Type type = value.asClass();
                        try {
                            Class<?> beanClass = Class.forName(type.name().toString(), false,
                                    Thread.currentThread().getContextClassLoader());
                            reflectiveClassSet.add(beanClass);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Cannot find bean class (" + type.name() +
                                    ") referenced in annotation (" + annotationInstance + ").");
                        }
                    }
                }
            }
        }
    }

    protected void applyScoreDirectorFactoryProperties(IndexView indexView, SolverConfig solverConfig) {
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = defaultScoreDirectoryFactoryConfig(indexView);
            solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        }
    }

    private boolean isClassDefined(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private ScoreDirectorFactoryConfig defaultScoreDirectoryFactoryConfig(IndexView indexView) {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(
                findFirstImplementingClass(DotNames.EASY_SCORE_CALCULATOR, indexView));
        scoreDirectorFactoryConfig.setConstraintProviderClass(
                findFirstImplementingClass(DotNames.CONSTRAINT_PROVIDER, indexView));
        scoreDirectorFactoryConfig.setIncrementalScoreCalculatorClass(
                findFirstImplementingClass(DotNames.INCREMENTAL_SCORE_CALCULATOR, indexView));
        return scoreDirectorFactoryConfig;
    }

    private <T> Class<? extends T> findFirstImplementingClass(DotName targetDotName, IndexView indexView) {
        Collection<ClassInfo> classInfos = indexView.getAllKnownImplementors(targetDotName);
        if (classInfos.isEmpty()) {
            return null;
        }
        ClassInfo classInfo = classInfos.iterator().next();
        return convertClassInfoToClass(classInfo);
    }

    private String convertAnnotationInstancesToString(Collection<AnnotationInstance> annotationInstances) {
        return "[" + annotationInstances.stream().map(instance -> instance.target().toString())
                .collect(Collectors.joining(", ")) + "]";
    }

    private <T> Class<? extends T> convertClassInfoToClass(ClassInfo classInfo) {
        String className = classInfo.name().toString();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return (Class<? extends T>) classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (" + className
                    + ") cannot be created during deployment.", e);
        }
    }

    private GeneratedGizmoClasses generateDomainAccessors(Map<String, SolverConfig> solverConfig, IndexView indexView,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers, Set<Class<?>> reflectiveClassSet) {
        // Use mvn quarkus:dev -Dquarkus.debug.generated-classes-dir=dump-classes
        // to dump generated classes
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        ClassOutput beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        Set<String> generatedMemberAccessorsClassNameSet = new HashSet<>();
        Set<String> gizmoSolutionClonerClassNameSet = new HashSet<>();

        /*
         * TODO consistently change the name "entity" to something less confusing
         * "entity" in this context means both "planning solution",
         * "planning entity" and other things as well.
         */
        assertSolverDomainAccessType(solverConfig);
        GizmoMemberAccessorEntityEnhancer entityEnhancer = new GizmoMemberAccessorEntityEnhancer();
        if (solverConfig.values().stream().anyMatch(c -> c.getDomainAccessType() == DomainAccessType.GIZMO)) {
            Collection<AnnotationInstance> membersToGeneratedAccessorsFor = new ArrayList<>();

            // Every entity and solution gets scanned for annotations.
            // Annotated members get their accessors generated.
            for (DotName dotName : DotNames.GIZMO_MEMBER_ACCESSOR_ANNOTATIONS) {
                membersToGeneratedAccessorsFor.addAll(indexView.getAnnotationsWithRepeatable(dotName, indexView));
            }
            membersToGeneratedAccessorsFor.removeIf(this::shouldIgnoreMember);

            // Fail fast on auto-discovery.
            Collection<AnnotationInstance> planningSolutionAnnotationInstanceCollection =
                    indexView.getAnnotations(DotNames.PLANNING_SOLUTION);
            List<String> unusedSolutionClasses = planningSolutionAnnotationInstanceCollection.stream()
                    .map(planningClass -> planningClass.target().asClass().name().toString())
                    .filter(planningClassName -> reflectiveClassSet.stream()
                            .noneMatch(clazz -> clazz.getName().equals(planningClassName)))
                    .toList();
            if (planningSolutionAnnotationInstanceCollection.isEmpty()) {
                throw new IllegalStateException(
                        "No classes found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
            } else if (planningSolutionAnnotationInstanceCollection.size() > 1 && !unusedSolutionClasses.isEmpty()) {
                throw new IllegalStateException(
                        "Unused classes (" + String.join(", ", unusedSolutionClasses) + ") found with a @" +
                                PlanningSolution.class.getSimpleName() + " annotation.");
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
                            and explicitly annotate the fields or getters with annotations such as @%s, @%s or @%s.
                            """
                            .strip()
                            .formatted(
                                    AutoDiscoverMemberType.class.getSimpleName(),
                                    PlanningSolution.class.getSimpleName(),
                                    PlanningScore.class.getSimpleName(),
                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                    ProblemFactCollectionProperty.class.getSimpleName()));
                }
            });

            for (AnnotationInstance annotatedMember : membersToGeneratedAccessorsFor) {
                switch (annotatedMember.target().kind()) {
                    case FIELD: {
                        FieldInfo fieldInfo = annotatedMember.target().asField();
                        ClassInfo classInfo = fieldInfo.declaringClass();

                        try {
                            generatedMemberAccessorsClassNameSet.add(
                                    entityEnhancer.generateFieldAccessor(annotatedMember, classOutput, fieldInfo,
                                            transformers));
                        } catch (ClassNotFoundException | NoSuchFieldException e) {
                            throw new IllegalStateException("Fail to generate member accessor for field (" +
                                    fieldInfo.name() + ") of the class( " +
                                    classInfo.name().toString() + ").", e);
                        }
                        break;
                    }
                    case METHOD: {
                        MethodInfo methodInfo = annotatedMember.target().asMethod();
                        ClassInfo classInfo = methodInfo.declaringClass();

                        try {
                            generatedMemberAccessorsClassNameSet.add(entityEnhancer.generateMethodAccessor(annotatedMember,
                                    classOutput, classInfo, methodInfo, transformers));
                        } catch (ClassNotFoundException | NoSuchMethodException e) {
                            throw new IllegalStateException("Failed to generate member accessor for the method (" +
                                    methodInfo.name() + ") of the class (" +
                                    classInfo.name() + ").", e);
                        }
                        break;
                    }
                    default: {
                        throw new IllegalStateException("The member (" + annotatedMember + ") is not on a field or method.");
                    }
                }
            }
            // Using REFLECTION domain access type so Timefold doesn't try to generate GIZMO code
            solverConfig.values().forEach(c -> {
                SolutionDescriptor solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(DomainAccessType.REFLECTION,
                        c.getSolutionClass(), null, null, c.getEntityClassList());
                gizmoSolutionClonerClassNameSet
                        .add(entityEnhancer.generateSolutionCloner(solutionDescriptor, classOutput, indexView, transformers));
            });
        }

        entityEnhancer.generateGizmoBeanFactory(beanClassOutput, reflectiveClassSet, transformers);
        return new GeneratedGizmoClasses(generatedMemberAccessorsClassNameSet, gizmoSolutionClonerClassNameSet);
    }

    private void assertSolverDomainAccessType(Map<String, SolverConfig> solverConfig) {
        // All solver must use the same domain access type
        if (solverConfig.values().stream().map(SolverConfig::getDomainAccessType).distinct().count() > 1) {
            throw new ConfigurationException("The domain access type must be unique across all Solver configurations.\n" +
                    solverConfig.entrySet().stream().map(e -> format("quarkus.timefold.\"%s\".domain-access-type=%s",
                            e.getKey(), e.getValue().getDomainAccessType()))
                            .collect(Collectors.joining("\n")));
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
                        "Annotation (" + annotationInstance.name() + ") can only be applied to methods and fields.");
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
