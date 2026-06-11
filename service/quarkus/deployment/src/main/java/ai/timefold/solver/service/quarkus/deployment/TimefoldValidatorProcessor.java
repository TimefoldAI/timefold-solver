package ai.timefold.solver.service.quarkus.deployment;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;
import ai.timefold.solver.service.quarkus.deployment.util.ProcessorUtils;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;

class TimefoldValidatorProcessor {

    private static final Logger LOG = Logger.getLogger(TimefoldValidatorProcessor.class);

    private static final DotName MODEL_VALIDATOR = DotName.createSimple(ModelValidator.class.getName());

    private static final String GENERATED_PACKAGE = "ai.timefold.platform.generated.validator.";

    /**
     * Generates default implementation of <code>ModelValidator</code> bean if no implementation is found in the project.
     *
     * <pre>
     * {@code
     *
     * @ApplicationScoped
     * public class ModelValidatorBean implements ModelValidator<ModelInput, ModelConfigOverrides> {
     *
     * }
     *
     * }
     * </pre>
     */
    @BuildStep
    void generateModelValidatorBean(CombinedIndexBuildItem combinedIndex,
            ModelComponentsBuildItem modelComponentsBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedClasses) {
        if (ProcessorUtils.findFirstImplementorOf(MODEL_VALIDATOR, combinedIndex.getIndex()).isPresent()) {
            return;
        }

        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedClasses);
        ClassInfo modelInput = modelComponentsBuildItem.getModelInput();
        ClassInfo modelConfigOverrides = modelComponentsBuildItem.getModelConfigOverrides();
        String modelValidatorClassName =
                GENERATED_PACKAGE + modelInput.simpleName() + "_" + ModelValidator.class.getSimpleName();

        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(modelValidatorClassName)
                .signature(SignatureBuilder.forClass()
                        .addInterface(Type.parameterizedType(Type.classType(ModelValidator.class),
                                Type.classType(modelInput.name().toString()),
                                Type.classType(modelConfigOverrides.name().toString()))))
                .build();
        beanCreator.addAnnotation(ApplicationScoped.class);
        beanCreator.close();
    }

    /**
     * Generates a bean inheriting from a base <code>ModelValidator</code> for the default model.
     * This build step reacts on a base {@link ModelValidator} class being present in the project and cannot conflict
     * with the previous build step, as classes generated at build time are not detected by the Jandex indexer.
     *
     * <pre>
     * {@code
     *
     * @ApplicationScoped
     * public class ExtendedModelValidatorBean extends BaseModelValidator<ExtendedModelInput> {
     *
     * }
     *
     * }
     * </pre>
     */
    @BuildStep
    void generateExtendedModelValidatorBean(CombinedIndexBuildItem combinedIndex,
            ModelComponentsBuildItem modelComponentsBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedClasses) {
        var modelValidatorClassInfoOptional =
                ProcessorUtils.findLastInHierarchyType(MODEL_VALIDATOR, combinedIndex.getIndex()::getAllKnownImplementations);
        if (modelValidatorClassInfoOptional.isEmpty()) {
            LOG.debug(
                    "No %s found in the class hierarchy. Skipping generation of extended model validator bean.".formatted(
                            MODEL_VALIDATOR.withoutPackagePrefix()));
            return;
        }

        var modelValidatorClassInfo = modelValidatorClassInfoOptional.get();
        if (modelValidatorClassInfo.typeParameters().isEmpty()
                && modelValidatorClassInfo.annotation(ApplicationScoped.class.getName()) != null) {
            LOG.debug(
                    "The detected %s (%s) is a valid bean. Skipping generation of extended model validator bean.".formatted(
                            MODEL_VALIDATOR.withoutPackagePrefix(), modelValidatorClassInfo.name()));
            return;
        }

        if (modelValidatorClassInfo.typeParameters().size() != 1) {
            LOG.warn(
                    "The detected %s (%s) must have a single type parameter. Skipping generation of extended model validator bean."
                            .formatted(MODEL_VALIDATOR.withoutPackagePrefix(), modelValidatorClassInfo.name()));
            return;
        }

        TypeVariable typeVariable = modelValidatorClassInfo.typeParameters().getFirst();
        if (typeVariable.bounds().size() != 1) {
            LOG.warn(
                    "The detected %s (%s) is expected to have a single type parameter with a single bound. Skipping generation of extended model validator bean."
                            .formatted(MODEL_VALIDATOR.withoutPackagePrefix(), modelValidatorClassInfo.name()));
            return;
        }
        var typeVariableBound = typeVariable.bounds().getFirst();
        ClassInfo modelInput = modelComponentsBuildItem.getModelInput();
        var modelInputSuperClassType = modelInput.superClassType();

        if (!Objects.equals(typeVariableBound, modelInputSuperClassType)) {
            LOG.warn(
                    "The detected %s (%s) has a single type parameter with a bound that is not compatible with the %s (%s). Skipping generation of extended model validator bean."
                            .formatted(MODEL_VALIDATOR.withoutPackagePrefix(), modelValidatorClassInfo.name(),
                                    ModelInput.class.getSimpleName(),
                                    modelInputSuperClassType.name()));
            return;
        }

        // Generate a bean for the extended model validator class with resolved type parameter (the extended model input).
        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedClasses);
        String modelValidatorClassName =
                GENERATED_PACKAGE + modelInput.simpleName() + "_" + ModelValidator.class.getSimpleName();

        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(modelValidatorClassName)
                .signature(SignatureBuilder.forClass()
                        .setSuperClass(Type.parameterizedType(Type.classType(modelValidatorClassInfo.name()),
                                Type.classType(modelInput.name()))))
                .build();
        beanCreator.addAnnotation(ApplicationScoped.class);
        beanCreator.close();
    }
}
