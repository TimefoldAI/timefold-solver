package ai.timefold.solver.model.quarkus.deployment;

import static ai.timefold.solver.model.quarkus.deployment.util.ProcessorUtils.excludeType;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.model.definition.api.data.AbstractBasicDemoDataGenerator;
import ai.timefold.solver.model.definition.api.data.DemoDataGenerator;
import ai.timefold.solver.model.definition.api.domain.Configuration;
import ai.timefold.solver.model.definition.api.domain.ModelRequest;
import ai.timefold.solver.model.definition.api.domain.ScoreAnalysisRequest;
import ai.timefold.solver.model.quarkus.deployment.builditem.ModelComponentsBuildItem;
import ai.timefold.solver.model.quarkus.deployment.builditem.ModelConvertorBuildItem;
import ai.timefold.solver.model.quarkus.deployment.builditem.RestComponentsBuildItem;
import ai.timefold.solver.model.quarkus.deployment.rest.DefaultModelResourceTypeInfo;
import ai.timefold.solver.model.quarkus.deployment.rest.EnterpriseModelResourceTypeInfo;
import ai.timefold.solver.model.quarkus.deployment.rest.EnterpriseModelResourceWithMapsTypeInfo;
import ai.timefold.solver.model.quarkus.deployment.rest.ModelResourceBeanGenerator;
import ai.timefold.solver.model.quarkus.deployment.rest.ModelResourceTypeInfo;
import ai.timefold.solver.model.quarkus.deployment.rest.ModelResourceWithMapsTypeInfo;
import ai.timefold.solver.model.quarkus.deployment.rest.io.AbstractModelConfigMessageBodyReaders;
import ai.timefold.solver.model.quarkus.deployment.rest.io.AbstractModelRequestMessageBodyReaders;
import ai.timefold.solver.model.quarkus.deployment.rest.io.AbstractScoreAnalysisRequestMessageBodyReaders;
import ai.timefold.solver.model.quarkus.deployment.util.ProcessorUtils;
import ai.timefold.solver.model.rest.api.ModelRest;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;

class TimefoldRESTProcessor {

    private static final Logger LOG = Logger.getLogger(TimefoldRESTProcessor.class);

    private static final DotName REST_API = DotName.createSimple(ModelRest.class.getName());

    private static final DotName ABSTRACT_DEMO_DATA_RESOURCE =
            DotName.createSimple("ai.timefold.solver.model.rest.impl.AbstractDemoDataResource");
    private static final DotName DEMO_DATA_GENERATOR = DotName.createSimple(DemoDataGenerator.class);
    private static final DotName ABSTRACT_DEMO_DATA_GENERATOR =
            DotName.createSimple(AbstractBasicDemoDataGenerator.class);

    private static final DotName WAYPOINT_EXTRACTOR =
            DotName.createSimple("ai.timefold.solver.model.maps.service.integration.api.WaypointsExtractor");

    private static final String REST_GENERATED_PACKAGE = "ai.timefold.platform.generated.rest.";

    public static final String DEMO_DATA_RESOURCE_PATH = "/demo-data";

    @BuildStep
    void detectRest(CombinedIndexBuildItem combinedIndex, BuildProducer<RestComponentsBuildItem> buildProducer) {
        // look for interface implementing REST api of the model.
        Optional<ClassInfo> restAPIDeclaration =
                ProcessorUtils.getFirstDirectSubInterfaceOf(REST_API, combinedIndex.getIndex());
        buildProducer.produce(new RestComponentsBuildItem(restAPIDeclaration));
    }

    /**
     * Generate various components required for REST endpoint of the mode such as
     * <ul>
     * <li>Model Request message body reader</li>
     * </ul>
     *
     * @param combinedIndex
     * @param generatedClasses
     */
    @BuildStep
    void generateRestComponents(CombinedIndexBuildItem combinedIndex,
            ModelComponentsBuildItem modelComponentsBuildItem,
            RestComponentsBuildItem restComponentsBuildItem,
            ModelConvertorBuildItem modelConvertorBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedClasses,
            BuildProducer<AdditionalResourceClassBuildItem> additionalResourceClasses) {
        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedClasses);

        ClassInfo solverModel = modelComponentsBuildItem.getSolverModel();
        ClassInfo modelInput = modelComponentsBuildItem.getModelInput();
        ClassInfo modelConfigOverrides = modelComponentsBuildItem.getModelConfigOverrides();

        // generate message body reader for ModelRequest
        generateModelMessageBodyReader(ModelRequest.class, AbstractModelRequestMessageBodyReaders.class, modelInput,
                modelConfigOverrides, classOutput);
        // generate message body reader for ScoreAnalysisRequest
        if (modelComponentsBuildItem.isEnterprise()) {
            generateModelMessageBodyReader(ScoreAnalysisRequest.class, AbstractScoreAnalysisRequestMessageBodyReaders.class,
                    modelInput,
                    modelConfigOverrides, classOutput);
        }
        // generate message body reader for Configuration
        generateModelMessageBodyReader(Configuration.class, AbstractModelConfigMessageBodyReaders.class,
                modelConfigOverrides, classOutput);

        if (restComponentsBuildItem.getRestResource().isPresent()) {

            ClassInfo restAPI = restComponentsBuildItem.getRestResource().orElseThrow();

            // Path annotation must be present on the interface to be able to bind the REST api to the endpoint path
            AnnotationInstance pathAnnotation = restAPI.annotations(Path.class).stream()
                    .filter(a -> a.target().kind() == Kind.CLASS).findFirst().orElse(null);
            if (pathAnnotation == null) {
                LOG.warn("ModelAPI class '" + restAPI.name().toString()
                        + "' does not have Path annotation that is required, skipping REST API generation");
                return;
            }
            // generate the Model REST api resource with all default endpoints
            String restResourcePath = pathAnnotation.value().asString();
            GeneratedJaxRsResourceGizmoAdaptor restClassOutput =
                    new GeneratedJaxRsResourceGizmoAdaptor(combinedIndex, additionalResourceClasses, generatedClasses,
                            restResourcePath);
            generateRestAPI(combinedIndex, modelComponentsBuildItem, restAPI, restClassOutput);

            // generate Demo data resource that provides the demo data based on demo generator if present
            var demoDataGeneratorOptional =
                    ProcessorUtils.findLastInHierarchyType(DEMO_DATA_GENERATOR,
                            excludeType(combinedIndex.getIndex()::getAllKnownImplementations, ABSTRACT_DEMO_DATA_GENERATOR));
            if (demoDataGeneratorOptional.isPresent()) {
                // if the resource path does not use version, do not use version for demo data endpoint as well
                String demoDataResourcePath = DEMO_DATA_RESOURCE_PATH;
                // extract version number from resource path to be used in the demo data endpoint path - same version of the api and the demo data
                Pattern versionExtractorPattern = Pattern.compile("^/v([0-9]+)/(.*)");
                Matcher matcher = versionExtractorPattern.matcher(restResourcePath);
                if (matcher.matches()) {
                    demoDataResourcePath = "/v" + matcher.group(1) + DEMO_DATA_RESOURCE_PATH;
                }
                GeneratedJaxRsResourceGizmoAdaptor demoDataClassOutput =
                        new GeneratedJaxRsResourceGizmoAdaptor(combinedIndex, additionalResourceClasses, generatedClasses,
                                demoDataResourcePath);
                generateDemoDataRestAPI(combinedIndex, demoDataResourcePath, modelInput, solverModel,
                        modelConfigOverrides, demoDataClassOutput);
            }
        }
    }

    /*
     * Helper methods
     */

    private void generateModelMessageBodyReader(Class<?> type, Class<?> abstractType, ClassInfo modelInput,
            ClassInfo modelConfig,
            GeneratedBeanGizmoAdaptor classOutput) {
        var modelRequestType = Type.parameterizedType(Type.classType(type),
                Type.classType(modelInput.name()),
                Type.classType(modelConfig.name()));

        String generatedName =
                REST_GENERATED_PACKAGE + type.getSimpleName() + "_RequestMessageBodyReader";
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .signature(
                        SignatureBuilder.forClass().setSuperClass(Type.classType(abstractType))
                                .addInterface(Type.parameterizedType(Type.classType(MessageBodyReader.class),
                                        modelRequestType)))
                .build();

        beanCreator.addAnnotation(Provider.class);

        String generatedNameConfigTypeRef =
                REST_GENERATED_PACKAGE + type.getSimpleName() + "_TypeReference";
        ClassCreator beanCreatorConfigTypeRef =
                ClassCreator.builder().classOutput(classOutput).className(generatedNameConfigTypeRef)
                        .signature(SignatureBuilder.forClass()
                                .setSuperClass(Type.parameterizedType(Type.classType(TypeReference.class.getCanonicalName()),
                                        modelRequestType)))
                        .build();

        beanCreatorConfigTypeRef.close();

        MethodCreator getConfigurationClassMethod = beanCreator
                .getMethodCreator("typeRef", TypeReference.class)
                .setModifiers(ACC_PROTECTED);
        MethodDescriptor typeRefConfig = MethodDescriptor.ofConstructor(generatedNameConfigTypeRef);
        getConfigurationClassMethod.returnValue(getConfigurationClassMethod.newInstance(typeRefConfig));
        beanCreator.close();
    }

    private void generateModelMessageBodyReader(Class<?> type, Class<?> abstractType,
            ClassInfo modelConfig,
            GeneratedBeanGizmoAdaptor classOutput) {
        var modelRequestType = Type.parameterizedType(Type.classType(type),
                Type.classType(modelConfig.name()));

        String generatedName =
                REST_GENERATED_PACKAGE + type.getSimpleName() + "_RequestMessageBodyReader";
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .signature(
                        SignatureBuilder.forClass().setSuperClass(Type.classType(abstractType))
                                .addInterface(Type.parameterizedType(Type.classType(MessageBodyReader.class),
                                        modelRequestType)))
                .build();

        beanCreator.addAnnotation(Provider.class);

        String generatedNameConfigTypeRef =
                REST_GENERATED_PACKAGE + type.getSimpleName() + "_TypeReference";
        ClassCreator beanCreatorConfigTypeRef =
                ClassCreator.builder().classOutput(classOutput).className(generatedNameConfigTypeRef)
                        .signature(SignatureBuilder.forClass()
                                .setSuperClass(Type.parameterizedType(Type.classType(TypeReference.class.getCanonicalName()),
                                        modelRequestType)))
                        .build();

        beanCreatorConfigTypeRef.close();

        MethodCreator getConfigurationClassMethod = beanCreator
                .getMethodCreator("typeRef", TypeReference.class)
                .setModifiers(ACC_PROTECTED);
        MethodDescriptor typeRefConfig = MethodDescriptor.ofConstructor(generatedNameConfigTypeRef);
        getConfigurationClassMethod.returnValue(getConfigurationClassMethod.newInstance(typeRefConfig));
        beanCreator.close();
    }

    private void generateRestAPI(CombinedIndexBuildItem combinedIndex, ModelComponentsBuildItem modelComponents,
            ClassInfo restAPI, GeneratedJaxRsResourceGizmoAdaptor classOutput) {

        var modelResourceBeanGenerator = new ModelResourceBeanGenerator(combinedIndex.getIndex(), REST_GENERATED_PACKAGE);

        // check if there is maps REST api required by looking up for waypoint service
        boolean isMapRequired =
                ProcessorUtils.findType(WAYPOINT_EXTRACTOR,
                        combinedIndex.getIndex()::getAllKnownImplementations).isPresent();

        ModelResourceTypeInfo modelResourceTypeInfo = resourceTypeInfo(modelComponents.isEnterprise(), isMapRequired);
        modelResourceBeanGenerator.generate(modelResourceTypeInfo, modelComponents, restAPI, classOutput);
    }

    private ModelResourceTypeInfo resourceTypeInfo(boolean enterprise, boolean maps) {
        if (enterprise) {
            return maps ? new EnterpriseModelResourceWithMapsTypeInfo() : new EnterpriseModelResourceTypeInfo();
        } else {
            return maps ? new ModelResourceWithMapsTypeInfo() : new DefaultModelResourceTypeInfo();
        }
    }

    private void generateDemoDataRestAPI(CombinedIndexBuildItem combinedIndex,
            String resourcePath,
            ClassInfo modelInput, ClassInfo solverModel, ClassInfo modelConfig,
            GeneratedJaxRsResourceGizmoAdaptor classOutput) {

        String generatedName =
                REST_GENERATED_PACKAGE + solverModel.simpleName() + "_DemoDataResource";
        classOutput.setGeneratedName(generatedName);
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .signature(SignatureBuilder.forClass()
                        .setSuperClass(Type.parameterizedType(Type.classType(ABSTRACT_DEMO_DATA_RESOURCE),
                                Type.classType(modelInput.name()),
                                Type.classType(modelConfig.name()))))
                .build();

        beanCreator.addAnnotation(Path.class).addValue("value", resourcePath);
        beanCreator.addAnnotation(org.eclipse.microprofile.openapi.annotations.tags.Tag.class)
                .add("description", "Generated demo data for the model").add("name", "Demo data");

        ClassInfo restResourceClassInfo = combinedIndex.getIndex().getClassByName(ABSTRACT_DEMO_DATA_RESOURCE);
        List<MethodInfo> constructors = restResourceClassInfo.constructors();

        if (constructors.size() > 2) {
            throw new IllegalStateException("The AbstractDemoDataResource class must have at most two constructors: " +
                    "a mandatory one without any parameters for recording, " +
                    "an optional one with parameters for injection.");
        }
        if (constructors.get(0).parametersCount() != 0 &&
                constructors.get(constructors.size() - 1).parametersCount() != 0) {
            throw new IllegalStateException(
                    "The AbstractDemoDataResource class is missing the mandatory constructor with no parameters.");
        }

        // create default empty constructor
        MethodCreator constructor = beanCreator.getMethodCreator(MethodDescriptor.ofConstructor(generatedName));
        ResultHandle thisObj = constructor.getThis();

        ResultHandle[] params = new ResultHandle[0];

        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(beanCreator.getSuperClass()),
                thisObj,
                params);

        constructor.returnValue(thisObj);
        constructor.close();

        MethodCreator constructorParams = beanCreator
                .getMethodCreator(MethodDescriptor.ofConstructor(generatedName,
                        DemoDataGenerator.class.getCanonicalName()))
                .setSignature(SignatureBuilder.forMethod()
                        .addParameterType(Type.classType(DemoDataGenerator.class))
                        .build());
        constructorParams.addAnnotation(Inject.class);
        thisObj = constructorParams.getThis();

        params = new ResultHandle[1];
        params[0] = constructorParams.getMethodParam(0);

        constructorParams.invokeSpecialMethod(
                constructors.get(1),
                thisObj,
                params);

        constructorParams.returnValue(thisObj);
        constructorParams.close();

        beanCreator.close();
    }
}
