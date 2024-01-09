package ai.timefold.solver.quarkus.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryService;
import ai.timefold.solver.core.impl.score.stream.JoinerService;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.bean.DefaultTimefoldBeanProvider;
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

    private void assertSolverPropertiesConfiguration() {
        // TODO - Add test case to hot reload of multiple solver config files
        // TODO - Add test case to fail fast for multiple solvers -> set only solver config
        // TODO - Add test case to fail fast for multiple solvers -> set only solver2 config
        // TODO - Add test case to fail fast for multiple solvers -> set root and solver config
        // TODO - Add test case to fail fast for multiple solvers -> set root config, solver and solver 2 config

        // Enforce the config file is set at the correct place for the default solver
        if (timefoldBuildTimeConfig.hasOnlyDefaultSolverConfig() &&
                (timefoldBuildTimeConfig.solverConfigXml().isEmpty()
                        && timefoldBuildTimeConfig.getDefaultSolverConfig().get().solverConfigXml().isPresent())
                || (timefoldBuildTimeConfig.solverConfigXml().isPresent()
                        && timefoldBuildTimeConfig.getDefaultSolverConfig().get().solverConfigXml().isPresent())) {
            throw new ConfigurationException("The default Solver configuration is invalid. Only the property" +
                    " quarkus.timefold.solverConfigXML can be set for the default Solver.");
        }

        // Enforce individual files when multiple solvers are defined
        if (!timefoldBuildTimeConfig.hasOnlyDefaultSolverConfig() && timefoldBuildTimeConfig.solverConfigXml().isPresent()) {
            throw new ConfigurationException("Invalid quarkus.timefold.solverConfigXML property ("
                    + timefoldBuildTimeConfig.solverConfigXml().get()
                    + "): the property must not be set when there are multiple Solvers.");
        }

        // Enforce mapped properties cannot be used to set a single solver other than the default
        if (!timefoldBuildTimeConfig.hasOnlyDefaultSolverConfig() && timefoldBuildTimeConfig.solver().size() == 1) {
            throw new ConfigurationException("Invalid use of mapped property (" +
                    "quarkus.timefold.\"" + timefoldBuildTimeConfig.solver().keySet().iterator().next() + "\"" +
                    "): the mapped properties must be used only to configure multiple solvers.");
        }
    }

    @BuildStep
    void watchSolverConfigXml(BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles) {
        // Validate the solver configuration properties
        assertSolverPropertiesConfiguration();

        String solverConfigXML = timefoldBuildTimeConfig.solverConfigXml()
                .orElse(TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL);

        // Root config file
        hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(solverConfigXML));

        // Config files from Solvers
        timefoldBuildTimeConfig.solver().values().stream().filter(c -> c.solverConfigXml().isPresent())
                .map(c -> c.solverConfigXml().get())
                .forEach(c -> hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(c)));
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

        // TODO - test default case, root config and no additional solver
        // TODO - test multiples solvers config: two with config, only one with config
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
    @Record(STATIC_INIT)
    SolverConfigBuildItem recordAndRegisterBeans(TimefoldRecorder recorder, RecorderContext recorderContext,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        IndexView indexView = combinedIndex.getIndex();

        // TODO - test skipping for the default case
        // TODO - test skipping for multiple solvers, all configurations must be null
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
            return new SolverConfigBuildItem(solverConfig);
        }

        // Validate the planning entities settings
        assertNoMemberAnnotationWithoutClassAnnotation(indexView);

        // TODO - test loading the default case
        // TODO - test loading two solver configs with XMLs
        // TODO - test override the XML generated config with solver properties file
        // TODO - test loading the default file URL
        // TODO - test config with same planning entities and solution to validate GIZMO
        // Quarkus extensions must always use getContextClassLoader()
        // Internally, Timefold defaults the ClassLoader to getContextClassLoader() too
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Map<String, SolverConfig> allSolverConfig = new HashMap<>();
        Set<Class<?>> reflectiveClassSet = new LinkedHashSet<>();
        this.timefoldBuildTimeConfig.solver().keySet().forEach(solverName -> {
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
            allSolverConfig.put(solverName, solverConfig);
        });

        // Register all annotated domain model classes
        registerClassesFromAnnotations(indexView, reflectiveClassSet);

        // Register only distinct constraint providers
        List<Entry<String, SolverConfig>> distinctConstraintProviders = CollectionUtils.toDistinctList(
                new LinkedList<>(allSolverConfig.entrySet())
                        .stream()
                        .filter(entryConfig -> entryConfig.getValue().getScoreDirectorFactoryConfig()
                                .getConstraintProviderClass() != null)
                        .toList(),
                (Entry<String, SolverConfig> entryConfig) -> entryConfig.getValue().getScoreDirectorFactoryConfig()
                        .getConstraintProviderClass().getName());
        distinctConstraintProviders
                .forEach(entryConfig -> generateConstraintVerifier(entryConfig.getKey(), entryConfig.getValue(),
                        syntheticBeanBuildItemBuildProducer));

        GeneratedGizmoClasses generatedGizmoClasses = generateDomainAccessors(allSolverConfig, indexView, generatedBeans,
                generatedClasses, transformers, reflectiveClassSet);

        allSolverConfig.forEach((key, value) -> {
            // Register the SolverConfig for each mapped solver or to the default
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configDescriptor =
                    SyntheticBeanBuildItem.configure(SolverConfig.class)
                            .scope(Singleton.class)
                            .named(key + "Config") // We add a suffix to avoid ambiguous bean names
                            .supplier(recorder.solverConfigSupplier(key, value,
                                    GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                            generatedGizmoClasses.generatedGizmoMemberAccessorClassSet),
                                    GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                            generatedGizmoClasses.generatedGizmoSolutionClonerClassSet)));
            if (key.equals(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME)) {
                configDescriptor.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(configDescriptor.done());

            SolverManagerConfig solverManagerDescriptor = new SolverManagerConfig();
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configManagerSupplier =
                    SyntheticBeanBuildItem.configure(SolverManagerConfig.class)
                            .scope(Singleton.class)
                            .named(key + "ConfigManager") // We add a suffix to avoid ambiguous bean names
                            .supplier(recorder.solverManagerConfig(solverManagerDescriptor));
            if (key.equals(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME)) {
                configManagerSupplier.defaultBean();
            }
            syntheticBeanBuildItemBuildProducer.produce(configManagerSupplier.done());
        });

        additionalBeans.produce(new AdditionalBeanBuildItem(TimefoldSolverBannerBean.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(DefaultTimefoldBeanProvider.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TimefoldRuntimeConfig.class));
        return new SolverConfigBuildItem(allSolverConfig);
    }

    private void generateConstraintVerifier(String solverName, SolverConfig solverConfig,
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
                            .named(solverName + "ConstraintVerifier");
            if (solverName.equals(TimefoldRuntimeConfig.DEFAULT_SOLVER_NAME)) {
                constraintDescriptor.defaultBean();
            }
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
            var planningSolutionAnnotationInstanceCollection =
                    indexView.getAnnotations(DotNames.PLANNING_SOLUTION);
            if (planningSolutionAnnotationInstanceCollection.isEmpty()) {
                throw new IllegalStateException(
                        "No classes found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
            } else if (planningSolutionAnnotationInstanceCollection.size() > 1) {
                throw new IllegalStateException("Multiple classes (" + convertAnnotationInstancesToString(
                        planningSolutionAnnotationInstanceCollection) + ") found with a @" +
                        PlanningSolution.class.getSimpleName() + " annotation.");
            }
            var planningSolutionAnnotationInstance =
                    planningSolutionAnnotationInstanceCollection.stream().findFirst().orElseThrow();
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
