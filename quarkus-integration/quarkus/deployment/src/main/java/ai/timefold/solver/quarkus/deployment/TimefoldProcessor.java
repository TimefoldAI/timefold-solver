package ai.timefold.solver.quarkus.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.StringWriter;
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
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryService;
import ai.timefold.solver.core.impl.score.stream.JoinerService;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.bean.TimefoldSolverBannerBean;
import ai.timefold.solver.quarkus.bean.UnavailableTimefoldBeanProvider;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;
import ai.timefold.solver.quarkus.deployment.config.SolverBuildTimeConfig;
import ai.timefold.solver.quarkus.deployment.config.TimefoldBuildTimeConfig;
import ai.timefold.solver.quarkus.devui.SolverConfigText;
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
    private static final String SOLVER_CONFIG_SUFFIX = "Config";
    private static final String SOLVER_CONFIG_MANAGER_SUFFIX = "ConfigManager";
    private static final String SOLVER_FACTORY_SUFFIX = "Factory";
    private static final String SOLVER_MANAGER_SUFFIX = "Manager";
    private static final String SOLVER_SCORE_MANAGER_SUFFIX = "ScoreManager";
    private static final String SOLVER_SOLUTION_MANAGER_SUFFIX = "SolutionManager";

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
    @Record(STATIC_INIT)
    public CardPageBuildItem registerDevUICard(
            TimefoldDevUIRecorder devUIRecorder,
            SolverConfigBuildItem solverConfigBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        // We map each solver config according to the solver name
        Map<String, String> solverConfigText = new HashMap<>();
        solverConfigBuildItem.getSolverNames().forEach(solverName -> {
            if (solverConfigBuildItem.getSolverConfig(solverName) != null) {
                StringWriter effectiveSolverConfigWriter = new StringWriter();
                SolverConfigIO solverConfigIO = new SolverConfigIO();
                solverConfigIO.write(solverConfigBuildItem.getSolverConfig(solverName), effectiveSolverConfigWriter);
                solverConfigText.put(solverName, effectiveSolverConfigWriter.toString());
            } else {
                solverConfigText.put(solverName, "");
            }
        });
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(SolverConfigText.class)
                .scope(ApplicationScoped.class)
                .supplier(devUIRecorder.solverConfigTextSupplier(solverConfigText))
                .done());

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

        // Validate the planning entities settings
        assertNoMemberAnnotationWithoutClassAnnotation(indexView);

        // Quarkus extensions must always use getContextClassLoader()
        // Internally, Timefold defaults the ClassLoader to getContextClassLoader() too
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Map<String, SolverConfig> allSolverConfig = new HashMap<>();
        Set<Class<?>> reflectiveClassSet = new LinkedHashSet<>();
        // If the config map is empty, we build the config using the default solver name
        if (timefoldBuildTimeConfig.solver().isEmpty()) {
            allSolverConfig.put(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME,
                    generateSolverConfig(classLoader, indexView, reflectiveHierarchyClass,
                            TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME, reflectiveClassSet));
        } else {
            // One config per solver mapped name
            this.timefoldBuildTimeConfig.solver().keySet().forEach(solverName -> allSolverConfig.put(solverName,
                    generateSolverConfig(classLoader, indexView, reflectiveHierarchyClass, solverName, reflectiveClassSet)));
        }

        // Register all annotated domain model classes
        registerClassesFromAnnotations(indexView, reflectiveClassSet);

        // Register only distinct constraint providers
        allSolverConfig.values()
                .stream()
                .filter(config -> config.getScoreDirectorFactoryConfig().getConstraintProviderClass() != null)
                .map(config -> config.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName())
                .distinct()
                .map(constraintName -> allSolverConfig.entrySet().stream().filter(entryConfig -> entryConfig.getValue()
                        .getScoreDirectorFactoryConfig().getConstraintProviderClass().getName().equals(constraintName))
                        .findFirst().get())
                .forEach(
                        entryConfig -> generateConstraintVerifier(entryConfig.getValue(), syntheticBeanBuildItemBuildProducer));

        GeneratedGizmoClasses generatedGizmoClasses = generateDomainAccessors(allSolverConfig, indexView, generatedBeans,
                generatedClasses, transformers, reflectiveClassSet);

        additionalBeans.produce(new AdditionalBeanBuildItem(TimefoldSolverBannerBean.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TimefoldRuntimeConfig.class));
        return new SolverConfigBuildItem(allSolverConfig, generatedGizmoClasses);
    }

    private SolverConfig generateSolverConfig(ClassLoader classLoader, IndexView indexView,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass, String solverName,
            Set<Class<?>> reflectiveClassSet) {
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

    /**
     * The build step executes at runtime to fetch an updated instance of properties from {@link TimefoldRuntimeConfig}.
     * <p>
     * The reason we need to register the managed beans at runtime is because {@code Arc.container().instance()} does
     * not return an instance of {@link TimefoldRuntimeConfig} when using interfaces instead of classes. Defining
     * configuration properties as interfaces is the only way to use {@code @WithUnnamedKey}. This is the default approach
     * documented in both Quarkus and Smallrye pages.
     * <p>
     * Finally, recording the bean at runtime is necessary to use updated instances of configuration properties, and
     * annotating {@link TimefoldRuntimeConfig} with {@code @StaticInitSafe} has no effect.
     */
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

        solverConfigBuildItem.getAllSolverConfigurations().forEach((key, value) -> {
            // Register the config per solver and set the default bean name as <solver-name> + SOLVER_CONFIG_SUFFIX
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configDescriptor =
                    SyntheticBeanBuildItem.configure(SolverConfig.class)
                            .scope(Singleton.class)
                            .named(key + SOLVER_CONFIG_SUFFIX) // We add a suffix to avoid ambiguous bean names
                            .setRuntimeInit()
                            .supplier(recorder.solverConfigSupplier(key, value, runtimeConfig,
                                    GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                            solverConfigBuildItem
                                                    .getGeneratedGizmoClasses().generatedGizmoMemberAccessorClassSet),
                                    GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                            solverConfigBuildItem
                                                    .getGeneratedGizmoClasses().generatedGizmoSolutionClonerClassSet)));
            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                configDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(configDescriptor.done());

            // Register the solver manager config per solver and set the default bean name as <solver-name> + SOLVER_CONFIG_MANAGER_SUFFIX
            SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configManagerDescriptor =
                    SyntheticBeanBuildItem.configure(SolverManagerConfig.class)
                            .scope(Singleton.class)
                            .named(key + SOLVER_CONFIG_MANAGER_SUFFIX) // We add a suffix to avoid ambiguous bean names
                            .setRuntimeInit()
                            .supplier(recorder.solverManagerConfig(solverManagerConfig, runtimeConfig));
            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                configManagerDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(configManagerDescriptor.done());

            // Register the solver factory per solver and set the default bean name as <solver-name> + SOLVER_FACTORY_SUFFIX
            String solverFactoryName = key + SOLVER_FACTORY_SUFFIX;
            SyntheticBeanBuildItem.ExtendedBeanConfigurator factoryDescriptor =
                    SyntheticBeanBuildItem.configure(SolverFactory.class)
                            .scope(Singleton.class)
                            .addType(ParameterizedType.create(DotName.createSimple(SolverFactory.class.getName()),
                                    new Type[] {
                                            Type.create(DotName.createSimple(value.getSolutionClass().getName()),
                                                    Type.Kind.CLASS)
                                    }, null))
                            .named(solverFactoryName) // We add a suffix to avoid ambiguous bean names
                            .setRuntimeInit()
                            .supplier(recorder.solverFactory(key + SOLVER_CONFIG_SUFFIX));

            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                factoryDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(factoryDescriptor.done());

            // Register the solver manager per solver and set the default bean name as <solver-name> + SOLVER_MANAGER_SUFFIX
            // To ensure the SolverManager instance can be injected when using basic types for generic type definition,
            // some Java basic types are used as parametrized types
            List<String> basicJavaTypes = List.of(
                    String.class.getName(),
                    Short.class.getName(),
                    Integer.class.getName(),
                    Long.class.getName(),
                    Float.class.getName(),
                    Double.class.getName());
            SyntheticBeanBuildItem.ExtendedBeanConfigurator managerDescriptor =
                    SyntheticBeanBuildItem.configure(SolverManager.class)
                            .scope(Singleton.class);
            basicJavaTypes.forEach(clazz -> managerDescriptor
                    .addType(ParameterizedType.create(DotName.createSimple(SolverManager.class.getName()),
                            new Type[] {
                                    Type.create(DotName.createSimple(value.getSolutionClass().getName()),
                                            Type.Kind.CLASS),
                                    Type.create(DotName.createSimple(clazz), Type.Kind.CLASS)
                            }, null)));

            managerDescriptor.named(key + SOLVER_MANAGER_SUFFIX) // We add a suffix to avoid ambiguous bean names
                    .setRuntimeInit()
                    .supplier(recorder.solverManager(solverFactoryName, solverManagerConfig));
            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                managerDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(managerDescriptor.done());

            List<String> scoreTypeClasses = List.of(
                    Score.class.getName(),
                    SimpleScore.class.getName(),
                    SimpleLongScore.class.getName(),
                    SimpleBigDecimalScore.class.getName(),
                    HardSoftScore.class.getName(),
                    HardSoftLongScore.class.getName(),
                    HardSoftBigDecimalScore.class.getName(),
                    HardMediumSoftScore.class.getName(),
                    HardMediumSoftLongScore.class.getName(),
                    HardMediumSoftBigDecimalScore.class.getName(),
                    BendableScore.class.getName(),
                    BendableLongScore.class.getName(),
                    BendableBigDecimalScore.class.getName());

            // Register the solver score managers per solver and set the default bean name as <solver-name> + SOLVER_SCORE_MANAGER_SUFFIX
            // To ensure the ScoreManager instance can be injected when using score types for generic type definition,
            // all score types are used as parametrized types
            SyntheticBeanBuildItem.ExtendedBeanConfigurator scoreManagerDescriptor =
                    SyntheticBeanBuildItem.configure(ScoreManager.class)
                            .scope(Singleton.class);

            scoreTypeClasses.forEach(clazz -> scoreManagerDescriptor
                    .addType(ParameterizedType.create(DotName.createSimple(ScoreManager.class.getName()),
                            new Type[] {
                                    Type.create(DotName.createSimple(value.getSolutionClass().getName()),
                                            Type.Kind.CLASS),
                                    Type.create(DotName.createSimple(clazz), Type.Kind.CLASS)
                            }, null)));

            scoreManagerDescriptor.named(key + SOLVER_SCORE_MANAGER_SUFFIX) // We add a suffix to avoid ambiguous bean names
                    .setRuntimeInit()
                    .supplier(recorder.scoreManager(solverFactoryName));
            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                scoreManagerDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(scoreManagerDescriptor.done());

            // Register the solver solution managers per solver and set the default bean name as <solver-name> + SOLVER_SOLUTION_MANAGER_SUFFIX
            // To ensure the SolutionManager instance can be injected when using score types for generic type definition,
            // all score types are used as parametrized types
            SyntheticBeanBuildItem.ExtendedBeanConfigurator solutionManagerDescriptor =
                    SyntheticBeanBuildItem.configure(SolutionManager.class)
                            .scope(Singleton.class);

            scoreTypeClasses.forEach(clazz -> solutionManagerDescriptor
                    .addType(ParameterizedType.create(DotName.createSimple(SolutionManager.class.getName()),
                            new Type[] {
                                    Type.create(DotName.createSimple(value.getSolutionClass().getName()),
                                            Type.Kind.CLASS),
                                    Type.create(DotName.createSimple(clazz), Type.Kind.CLASS)
                            }, null)));

            solutionManagerDescriptor.named(key + SOLVER_SOLUTION_MANAGER_SUFFIX) // We add a suffix to avoid ambiguous bean names
                    .setRuntimeInit()
                    .supplier(recorder.solutionManager(solverFactoryName));
            if (timefoldBuildTimeConfig.isDefaultSolverConfig(key)) {
                solutionManagerDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(solutionManagerDescriptor.done());
        });
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
            solverConfig.setSolutionClass(findSolutionClass(indexView));
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

    private Class<?> findSolutionClass(IndexView indexView) {
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_SOLUTION);
        if (annotationInstances.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + convertAnnotationInstancesToString(annotationInstances)
                    + ") found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
        }
        if (annotationInstances.isEmpty()) {
            throw new IllegalStateException("No classes (" + convertAnnotationInstancesToString(annotationInstances)
                    + ") found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
        }
        AnnotationTarget solutionTarget = annotationInstances.iterator().next().target();
        if (solutionTarget.kind() != AnnotationTarget.Kind.CLASS) {
            throw new IllegalStateException("A target (" + solutionTarget
                    + ") with a @" + PlanningSolution.class.getSimpleName() + " must be a class.");
        }
        return convertClassInfoToClass(solutionTarget.asClass());
    }

    private List<Class<?>> findEntityClassList(IndexView indexView) {
        Collection<AnnotationInstance> annotationInstances = indexView.getAnnotations(DotNames.PLANNING_ENTITY);
        if (annotationInstances.isEmpty()) {
            throw new IllegalStateException("No classes (" + convertAnnotationInstancesToString(annotationInstances)
                    + ") found with a @" + PlanningEntity.class.getSimpleName() + " annotation.");
        }
        List<AnnotationTarget> targetList = annotationInstances.stream()
                .map(AnnotationInstance::target)
                .toList();
        if (targetList.stream().anyMatch(target -> target.kind() != AnnotationTarget.Kind.CLASS)) {
            throw new IllegalStateException("All targets (" + targetList
                    + ") with a @" + PlanningEntity.class.getSimpleName() + " must be a class.");
        }
        return targetList.stream()
                .map(target -> (Class<?>) convertClassInfoToClass(target.asClass()))
                .collect(Collectors.toList());
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
                findImplementingClass(DotNames.EASY_SCORE_CALCULATOR, indexView));
        scoreDirectorFactoryConfig.setConstraintProviderClass(
                findImplementingClass(DotNames.CONSTRAINT_PROVIDER, indexView));
        scoreDirectorFactoryConfig.setIncrementalScoreCalculatorClass(
                findImplementingClass(DotNames.INCREMENTAL_SCORE_CALCULATOR, indexView));
        if (scoreDirectorFactoryConfig.getEasyScoreCalculatorClass() == null
                && scoreDirectorFactoryConfig.getConstraintProviderClass() == null
                && scoreDirectorFactoryConfig.getIncrementalScoreCalculatorClass() == null) {
            throw new IllegalStateException("No classes found that implement "
                    + EasyScoreCalculator.class.getSimpleName() + ", "
                    + ConstraintProvider.class.getSimpleName() + " or "
                    + IncrementalScoreCalculator.class.getSimpleName() + ".");
        }
        return scoreDirectorFactoryConfig;
    }

    private <T> Class<? extends T> findImplementingClass(DotName targetDotName, IndexView indexView) {
        Collection<ClassInfo> classInfos = indexView.getAllKnownImplementors(targetDotName);
        if (classInfos.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + convertClassInfosToString(classInfos)
                    + ") found that implement the interface " + targetDotName + ".");
        }
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

    private String convertClassInfosToString(Collection<ClassInfo> classInfos) {
        return "[" + classInfos.stream().map(instance -> instance.name().toString())
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
                    solverConfig.entrySet().stream().map(e -> String.format("quarkus.timefold.\"%s\".domain-access-type=%s",
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
