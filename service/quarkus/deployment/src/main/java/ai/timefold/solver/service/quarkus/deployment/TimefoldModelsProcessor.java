package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.util.ProcessorUtils.excludeType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.quarkus.deployment.api.ConstraintMetaModelBuildItem;
import ai.timefold.solver.service.definition.api.AbstractSimpleModel;
import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.validation.AbstractIssue;
import ai.timefold.solver.service.definition.impl.validation.AbstractLegacyIssue;
import ai.timefold.solver.service.definition.impl.validation.JsonMappingError;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;
import ai.timefold.solver.service.quarkus.deployment.util.ProcessorUtils;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;

class TimefoldModelsProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimefoldModelsProcessor.class);

    private static final String FEATURE = "platform-models";

    private static final DotName ENTERPRISE_FEATURES =
            DotName.createSimple("ai.timefold.solver.enterprise.service.definition.impl.EnterpriseFeatures");

    private static final DotName MODEL_INPUT = DotName.createSimple(ModelInput.class.getName());

    private static final DotName SOLVER_MODEL = DotName.createSimple(SolverModel.class.getName());

    private static final DotName MODEL_OUTPUT = DotName.createSimple(ModelOutput.class.getName());

    private static final DotName MODEL_CONFIG_OVERRIDES = DotName.createSimple(ModelConfigOverrides.class.getName());

    private static final DotName MODEL_INPUT_METRICS = DotName.createSimple(ModelInputMetrics.class.getName());

    private static final DotName MODEL_OUTPUT_METRICS = DotName.createSimple(ModelOutputMetrics.class.getName());

    private static final DotName MODEL_CONSTRAINT_JUSTIFICATION =
            DotName.createSimple(ModelConstraintJustification.class.getName());

    private static final DotName ABSTRACT_ISSUE = DotName.createSimple(AbstractIssue.class.getName());

    private static final DotName ABSTRACT_SIMPLE_MODEL = DotName.createSimple(AbstractSimpleModel.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void discoverModelComponents(CombinedIndexBuildItem combinedIndex,
            ConstraintMetaModelBuildItem constraintMetaModelBuildItem,
            BuildProducer<ModelComponentsBuildItem> modelComponentsProducer) {

        ClassInfo solverModel = ProcessorUtils.findRequiredLastInHierarchyType(SOLVER_MODEL,
                excludeType(combinedIndex.getIndex()::getAllKnownImplementations, ABSTRACT_SIMPLE_MODEL));
        ClassInfo modelInput = ProcessorUtils.findRequiredLastInHierarchyType(MODEL_INPUT,
                excludeType(combinedIndex.getIndex()::getAllKnownImplementations, ABSTRACT_SIMPLE_MODEL));
        ClassInfo modelOutput = ProcessorUtils.findRequiredLastInHierarchyType(MODEL_OUTPUT,
                excludeType(combinedIndex.getIndex()::getAllKnownImplementations, ABSTRACT_SIMPLE_MODEL));
        ClassInfo modelConfig = ProcessorUtils.findLastInHierarchyType(MODEL_CONFIG_OVERRIDES, combinedIndex.getIndex());
        ClassInfo modelInputMetrics = ProcessorUtils.findLastInHierarchyType(MODEL_INPUT_METRICS, combinedIndex.getIndex());
        ClassInfo modelOutputMetrics = ProcessorUtils.findLastInHierarchyType(MODEL_OUTPUT_METRICS, combinedIndex.getIndex());
        /*
         * The ModelConstraintJustification usually has as many implementations as there are different constraints
         * in the model.
         * The model then defines a sub-interface of ModelConstraintJustification to control the polymorphism e.g.
         * for the purpose of serialization and validation.
         */
        ClassInfo modelConstraintJustification =
                ProcessorUtils.getFirstDirectSubInterfaceOrImplementorOf(MODEL_CONSTRAINT_JUSTIFICATION,
                        combinedIndex.getIndex());

        ClassInfo validationIssueSuperType = detectValidationIssueSuperType(combinedIndex.getIndex());

        Optional<Type> scoreClazzType = lookupScoreClass(combinedIndex);

        // if no Score class is detected, fail fast
        ClassInfo modelScoreClass = scoreClazzType.map(type -> combinedIndex.getIndex().getClassByName(type.name()))
                .orElseThrow(() -> new IllegalStateException("At least one %s class has to provide a %s."
                        .formatted(PlanningSolution.class.getSimpleName(), PlanningScore.class.getSimpleName())));

        ConstraintMetaModel constraintMetaModel = createConstraintMetaModel(constraintMetaModelBuildItem);

        boolean enterprise = combinedIndex.getIndex().getClassByName(ENTERPRISE_FEATURES) != null;

        ModelComponentsBuildItem modelComponentsBuildItem =
                new ModelComponentsBuildItem(enterprise, solverModel, modelInput, modelOutput, modelConfig, modelInputMetrics,
                        modelOutputMetrics, modelConstraintJustification, validationIssueSuperType, modelScoreClass,
                        constraintMetaModel);
        LOGGER.debug("Detected model components: {}", modelComponentsBuildItem);
        modelComponentsProducer.produce(modelComponentsBuildItem);
    }

    private ClassInfo detectValidationIssueSuperType(IndexView indexView) {
        Optional<ClassInfo> validationLegacyIssueSuperType =
                ProcessorUtils.getDirectSubclassExcluding(indexView, DotName.createSimple(AbstractLegacyIssue.class),
                        // defined by the SDK
                        DotName.createSimple(JsonMappingError.class));
        if (validationLegacyIssueSuperType.isPresent()) {
            return validationLegacyIssueSuperType.get();
        } else {
            return ProcessorUtils.getDirectSubclassExcluding(indexView, ABSTRACT_ISSUE,
                    DotName.createSimple(AbstractLegacyIssue.class))
                    .orElse(indexView.getClassByName(ABSTRACT_ISSUE));
        }
    }

    private static ConstraintMetaModel createConstraintMetaModel(ConstraintMetaModelBuildItem constraintMetaModelBuildItem) {
        if (constraintMetaModelBuildItem.constraintMetaModelsBySolverNames().isEmpty()) {
            return null;
        }
        if (constraintMetaModelBuildItem.constraintMetaModelsBySolverNames().size() > 1) {
            throw new IllegalStateException("Only a single solver configuration is supported.");
        }

        return constraintMetaModelBuildItem.constraintMetaModelsBySolverNames().values().iterator().next();
    }

    @BuildStep
    void registerTypesForReflection(ModelComponentsBuildItem modelComponentsBuildItem,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchyProducer) {

        ClassInfo modelInput = modelComponentsBuildItem.getModelInput();
        ClassInfo modelOutput = modelComponentsBuildItem.getModelOutput();
        ClassInfo modelConfig = modelComponentsBuildItem.getModelConfigOverrides();
        ClassInfo modelInputMetrics = modelComponentsBuildItem.getModelInputMetrics();
        ClassInfo modelOutputMetrics = modelComponentsBuildItem.getModelOutputMetrics();

        // register all model specific classes that are accessible via REST api
        reflectiveClassHierarchyProducer
                .produce(ReflectiveHierarchyBuildItem.builder(
                        modelInput.name())
                        .serialization(true).build());
        reflectiveClassHierarchyProducer
                .produce(ReflectiveHierarchyBuildItem.builder(
                        modelOutput.name())
                        .serialization(true).build());
        reflectiveClassHierarchyProducer
                .produce(ReflectiveHierarchyBuildItem.builder(
                        modelConfig.name())
                        .serialization(true).build());
        reflectiveClassHierarchyProducer
                .produce(ReflectiveHierarchyBuildItem.builder(
                        modelInputMetrics.name())
                        .serialization(true).build());
        reflectiveClassHierarchyProducer
                .produce(ReflectiveHierarchyBuildItem.builder(
                        modelOutputMetrics.name())
                        .serialization(true).build());
    }

    @BuildStep
    public ReflectiveHierarchyIgnoreWarningBuildItem ignoreJavaClassWarnings() {
        return new ReflectiveHierarchyIgnoreWarningBuildItem(
                dotName -> dotName.toString().equals(Score.class.getCanonicalName()));
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.micrometer", "micrometer-core"));
    }

    private Optional<Type> lookupScoreClass(CombinedIndexBuildItem combinedIndex) {
        Collection<AnnotationInstance> planningSolutions = combinedIndex.getIndex().getAnnotations(PlanningSolution.class)
                .stream().filter(planningSolution -> !planningSolution.target().asClass().name()
                        .equals(ABSTRACT_SIMPLE_MODEL))
                .toList();

        if (planningSolutions.isEmpty()) {
            // if no planning solutions found, use AbstractSimpleModel's score
            return abstractSimpleModelScoreType(combinedIndex);
        }
        if (planningSolutions.size() == 1) {
            // look up for planning score class used within planning solution
            ClassInfo planningSolution = planningSolutions.iterator().next().target().asClass();
            return detectScoreClass(combinedIndex, planningSolution);
        } else {
            // more than 1 planning solutions found other than AbstractSimpleModel
            List<Type> planningScoreTypes =
                    planningSolutions.stream().map(planningSolution -> detectScoreClass(combinedIndex,
                            planningSolution.target().asClass())).filter(Optional::isPresent).map(Optional::get)
                            .filter(Objects::nonNull)
                            .toList();
            if (planningScoreTypes.size() > 1) {
                String planningSolutionClassNames =
                        planningSolutions.stream().map(planningSolution -> planningSolution.target().asClass().simpleName())
                                .collect(Collectors.joining(","));
                throw new IllegalStateException("Multiple planning solution classes that declare a %s found: %s"
                        .formatted(PlanningScore.class.getSimpleName(), planningSolutionClassNames));
            }

            return planningScoreTypes.stream().findFirst();
        }
    }

    private Optional<Type> abstractSimpleModelScoreType(CombinedIndexBuildItem combinedIndex) {
        ClassInfo simpleModelClass = combinedIndex.getIndex().getClassByName(ABSTRACT_SIMPLE_MODEL);
        return detectScoreClass(combinedIndex, simpleModelClass);
    }

    private Optional<Type> detectScoreClass(CombinedIndexBuildItem combinedIndex, ClassInfo planningSolution) {
        ClassInfo classToCheckForScore = planningSolution;
        Optional<Type> scoreType = Optional.empty();

        while (scoreType.isEmpty() && classToCheckForScore != null) {
            final ClassInfo classToCheck = classToCheckForScore;
            scoreType = combinedIndex.getIndex().getAnnotations(PlanningScore.class).stream()
                    .map(annotationInstance -> {
                        if (annotationInstance.target().kind() == AnnotationTarget.Kind.FIELD
                                && annotationInstance.target().asField().declaringClass() == classToCheck) {
                            return annotationInstance.target().asField().type();
                        }

                        if (annotationInstance.target().kind() == AnnotationTarget.Kind.METHOD
                                && annotationInstance.target().asMethod().declaringClass() == classToCheck) {
                            return annotationInstance.target().asMethod().returnType();
                        }

                        return null;
                    }).filter(Objects::nonNull).findFirst();
            classToCheckForScore = combinedIndex.getIndex().getClassByName(classToCheckForScore.superClassType().name());
        }
        return scoreType;
    }

}
