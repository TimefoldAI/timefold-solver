package ai.timefold.solver.quarkus.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

import ai.timefold.solver.core.MultithreadedSolvingAdvancedService;
import ai.timefold.solver.core.NearbySelectionAdvancedService;
import ai.timefold.solver.core.PartitionedSearchAdvancedService;
import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryService;
import ai.timefold.solver.core.impl.score.stream.JoinerService;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.bean.DefaultTimefoldBeanProvider;
import ai.timefold.solver.quarkus.bean.UnavailableTimefoldBeanProvider;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;
import ai.timefold.solver.quarkus.deployment.config.TimefoldBuildTimeConfig;
import ai.timefold.solver.quarkus.devui.TimefoldDevUIPropertiesSupplier;
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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigurationException;

class TimefoldProcessor {

    private static final Logger log = Logger.getLogger(TimefoldProcessor.class.getName());

    TimefoldBuildTimeConfig timefoldBuildTimeConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("timefold");
    }

    @BuildStep
    void registerSpi(BuildProducer<ServiceProviderBuildItem> services) {
        Stream.of(ScoreDirectorFactoryService.class, JoinerService.class, MultithreadedSolvingAdvancedService.class,
                PartitionedSearchAdvancedService.class, NearbySelectionAdvancedService.class)
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
    HotDeploymentWatchedFileBuildItem watchSolverConfigXml() {
        String solverConfigXML = timefoldBuildTimeConfig.solverConfigXml
                .orElse(TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL);
        return new HotDeploymentWatchedFileBuildItem(solverConfigXML);
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
    public DevConsoleRuntimeTemplateInfoBuildItem getSolverConfig(SolverConfigBuildItem solverConfigBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        SolverConfig solverConfig = solverConfigBuildItem.getSolverConfig();
        if (solverConfig != null) {
            StringWriter effectiveSolverConfigWriter = new StringWriter();
            SolverConfigIO solverConfigIO = new SolverConfigIO();
            solverConfigIO.write(solverConfig, effectiveSolverConfigWriter);
            return new DevConsoleRuntimeTemplateInfoBuildItem("solverConfigProperties",
                    new TimefoldDevUIPropertiesSupplier(effectiveSolverConfigWriter.toString()), this.getClass(),
                    curateOutcomeBuildItem);
        } else {
            return new DevConsoleRuntimeTemplateInfoBuildItem("solverConfigProperties",
                    new TimefoldDevUIPropertiesSupplier(), this.getClass(), curateOutcomeBuildItem);
        }
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
            return new SolverConfigBuildItem(null);
        }

        // Quarkus extensions must always use getContextClassLoader()
        // Internally, Timefold defaults the ClassLoader to getContextClassLoader() too
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SolverConfig solverConfig;
        if (timefoldBuildTimeConfig.solverConfigXml.isPresent()) {
            String solverConfigXML = timefoldBuildTimeConfig.solverConfigXml.get();
            if (classLoader.getResource(solverConfigXML) == null) {
                throw new ConfigurationException("Invalid quarkus.timefold.solverConfigXML property ("
                        + solverConfigXML + "): that classpath resource does not exist.");
            }
            solverConfig = SolverConfig.createFromXmlResource(solverConfigXML);
        } else if (classLoader.getResource(TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL) != null) {
            solverConfig = SolverConfig.createFromXmlResource(
                    TimefoldBuildTimeConfig.DEFAULT_SOLVER_CONFIG_URL);
        } else {
            solverConfig = new SolverConfig();
        }

        applySolverProperties(indexView, solverConfig);
        assertNoMemberAnnotationWithoutClassAnnotation(indexView);

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

        Set<Class<?>> reflectiveClassSet = new LinkedHashSet<>();

        registerClassesFromAnnotations(indexView, reflectiveClassSet);
        registerCustomClassesFromSolverConfig(solverConfig, reflectiveClassSet);
        generateConstraintVerifier(solverConfig, syntheticBeanBuildItemBuildProducer);
        GeneratedGizmoClasses generatedGizmoClasses = generateDomainAccessors(solverConfig, indexView, generatedBeans,
                generatedClasses, transformers, reflectiveClassSet);

        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();

        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(SolverConfig.class)
                .scope(Singleton.class)
                .defaultBean()
                .supplier(recorder.solverConfigSupplier(solverConfig,
                        GizmoMemberAccessorEntityEnhancer.getGeneratedGizmoMemberAccessorMap(recorderContext,
                                generatedGizmoClasses.generatedGizmoMemberAccessorClassSet),
                        GizmoMemberAccessorEntityEnhancer.getGeneratedSolutionClonerMap(recorderContext,
                                generatedGizmoClasses.generatedGizmoSolutionClonerClassSet)))
                .done());

        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(SolverManagerConfig.class)
                .scope(Singleton.class)
                .defaultBean()
                .supplier(recorder.solverManagerConfig(solverManagerConfig)).done());

        additionalBeans.produce(new AdditionalBeanBuildItem(DefaultTimefoldBeanProvider.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(TimefoldRuntimeConfig.class));
        return new SolverConfigBuildItem(solverConfig);
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
            final ConstraintStreamImplType constraintStreamImplType =
                    solverConfig.getScoreDirectorFactoryConfig().getConstraintStreamImplType();
            syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(DotNames.CONSTRAINT_VERIFIER)
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

                        if (constraintStreamImplType != null) { // Use the default if not specified.
                            constraintVerifierResultHandle = methodCreator.invokeInterfaceMethod(
                                    MethodDescriptor.ofMethod(constraintVerifierClassName,
                                            "withConstraintStreamImplType",
                                            constraintVerifierClassName,
                                            ConstraintStreamImplType.class),
                                    constraintVerifierResultHandle,
                                    methodCreator.load(constraintStreamImplType));
                        }

                        methodCreator.returnValue(constraintVerifierResultHandle);
                    })
                    .addType(ParameterizedType.create(DotNames.CONSTRAINT_VERIFIER,
                            new Type[] {
                                    Type.create(DotName.createSimple(constraintProviderClass.getName()), Type.Kind.CLASS),
                                    Type.create(DotName.createSimple(planningSolutionClass.getName()), Type.Kind.CLASS)
                            }, null))
                    .forceApplicationClass()
                    .defaultBean()
                    .done());
        }
    }

    private void applySolverProperties(IndexView indexView, SolverConfig solverConfig) {
        if (solverConfig.getSolutionClass() == null) {
            solverConfig.setSolutionClass(findSolutionClass(indexView));
        }
        if (solverConfig.getEntityClassList() == null) {
            solverConfig.setEntityClassList(findEntityClassList(indexView));
        }
        applyScoreDirectorFactoryProperties(indexView, solverConfig);
        timefoldBuildTimeConfig.solver.environmentMode.ifPresent(solverConfig::setEnvironmentMode);
        timefoldBuildTimeConfig.solver.daemon.ifPresent(solverConfig::setDaemon);
        timefoldBuildTimeConfig.solver.domainAccessType.ifPresent(solverConfig::setDomainAccessType);
        timefoldBuildTimeConfig.solver.constraintStreamImplType.ifPresent(solverConfig::withConstraintStreamImplType);

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
                .collect(Collectors.toList());
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
            timefoldFieldAnnotations.addAll(indexView.getAnnotations(annotationName));
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
                    .getAnnotations(beanDefiningAnnotation.getAnnotationDotName())) {
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

    private GeneratedGizmoClasses generateDomainAccessors(SolverConfig solverConfig, IndexView indexView,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers, Set<Class<?>> reflectiveClassSet) {
        // Use mvn quarkus:dev -Dquarkus.debug.generated-classes-dir=dump-classes
        // to dump generated classes
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        ClassOutput beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);

        Set<String> generatedMemberAccessorsClassNameSet = new HashSet<>();
        Set<String> gizmoSolutionClonerClassNameSet = new HashSet<>();

        GizmoMemberAccessorEntityEnhancer entityEnhancer = new GizmoMemberAccessorEntityEnhancer();
        if (solverConfig.getDomainAccessType() == DomainAccessType.GIZMO) {
            Collection<AnnotationInstance> membersToGeneratedAccessorsFor = new ArrayList<>();

            for (DotName dotName : DotNames.GIZMO_MEMBER_ACCESSOR_ANNOTATIONS) {
                membersToGeneratedAccessorsFor.addAll(indexView.getAnnotations(dotName));
            }
            membersToGeneratedAccessorsFor.removeIf(this::shouldIgnoreMember);

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
            SolutionDescriptor solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(DomainAccessType.REFLECTION,
                    solverConfig.getSolutionClass(), null, null, solverConfig.getEntityClassList());
            gizmoSolutionClonerClassNameSet
                    .add(entityEnhancer.generateSolutionCloner(solutionDescriptor, classOutput, indexView, transformers));
        }

        entityEnhancer.generateGizmoBeanFactory(beanClassOutput, reflectiveClassSet, transformers);
        return new GeneratedGizmoClasses(generatedMemberAccessorsClassNameSet, gizmoSolutionClonerClassNameSet);
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
