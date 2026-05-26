package ai.timefold.solver.model.quarkus.deployment.rest;

import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Modifier;

import jakarta.inject.Inject;

import ai.timefold.solver.model.quarkus.deployment.GeneratedJaxRsResourceGizmoAdaptor;
import ai.timefold.solver.model.quarkus.deployment.builditem.ModelComponentsBuildItem;
import ai.timefold.solver.model.quarkus.deployment.util.ProcessorUtils;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;

/**
 * Generates the REST API resource for the model based on the provided marker interface and other detected components.
 * <p>
 * Example:
 *
 * <pre>
 *     {@code
 *     class SolverModelName_EndpointName extends AbstractModelAPIResource<...> implements RestInterface {
 *
 *         public SolverModelName_EndpointName() {
 *             // required for code generation as CDI bean
 *         }
 *
 *         public SolverModelName_EndpointName(ModelValidator<...> modelValidator,
 *                 ScoreAnalysisFacadeBase scoreAnalysisFacade,
 *                 AbstractStorageService<...> storageService,
 *                 &#64;Channel(SolverChannels.DATASET_POSTED)
 *                 &#64;Broadcast
 *                 Emitter<DatasetPostedEvent> datasetPostedEmitter,
 *                 &#64;Channel(SolverChannels.START)
 *                 Emitter<ItemStartCommand> scheduleStartEmitter,
 *                 &#64;Channel(SolverChannels.TERMINATE)
 *                 MutinyEmitter<ItemTerminateCommand> scheduleTerminateEmitter) {
 *             super(modelValidator, scoreAnalysisFacade, storageService, datasetPostedEmitter, scheduleStartEmitter,
 *                     scheduleTerminateEmitter);
 *         }
 *     }
 *     }
 * </pre>
 *
 */
public final class ModelResourceBeanGenerator {

    private final IndexView combinedIndex;

    private final String generatedPackage;

    public ModelResourceBeanGenerator(IndexView combinedIndex, String generatedPackage) {
        this.combinedIndex = combinedIndex;
        this.generatedPackage = generatedPackage;
    }

    public void generate(ModelResourceTypeInfo resourceTypeInfo, ModelComponentsBuildItem modelComponents,
            ClassInfo restAPI, GeneratedJaxRsResourceGizmoAdaptor classOutput) {

        String generatedName =
                generatedPackage + modelComponents.getSolverModel().simpleName() + "_" + restAPI.simpleName();
        classOutput.setGeneratedName(generatedName);

        var modelResourceSuperClassType = resourceTypeInfo.modelResourceSuperClassType(modelComponents);
        ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .signature(SignatureBuilder.forClass()
                        .setSuperClass(modelResourceSuperClassType)
                        .addInterface(Type.classType(restAPI.name())))
                .build();

        createConstructors(generatedName, resourceTypeInfo, modelComponents, beanCreator);

        // copy annotation from interface otherwise OpenAPI generation does not see the endpoint
        for (AnnotationInstance annotation : restAPI.annotations().stream()
                .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                .toList()) {
            beanCreator.addAnnotation(annotation);
        }

        copyMethodPathAnnotations(restAPI, beanCreator);

        beanCreator.close();
    }

    /**
     * Creates constructors matching the super class:
     *
     * <ul>
     * <li>default constructor</li>
     * <li>constructor for dependency injection</li>
     * </ul>
     */
    private void createConstructors(String beanName, ModelResourceTypeInfo resourceBeanTypeInfo,
            ModelComponentsBuildItem modelComponents,
            ClassCreator beanCreator) {
        final ClassInfo restResourceSuperClassInfo =
                combinedIndex.getClassByName(resourceBeanTypeInfo.modelResourceSuperClassName());

        // Create the default constructor
        ProcessorUtils.requireDefaultAndParameterizedConstructor(restResourceSuperClassInfo);
        ProcessorUtils.createDefaultConstructor(beanCreator, beanName);

        final var constructorSignature = resourceBeanTypeInfo.constructorSignature(modelComponents);
        final var constructorParameterTypes = resourceBeanTypeInfo.constructorParameterTypes(modelComponents);

        // create the full constructor
        MethodCreator constructorParams = beanCreator
                .getMethodCreator(MethodDescriptor.ofConstructor(beanName, constructorParameterTypes))
                .setSignature(constructorSignature);

        constructorParams.addAnnotation(Inject.class);
        ResultHandle thisObj = constructorParams.getThis();

        // add channel annotation to arguments that are used for sending events
        resourceBeanTypeInfo.channelConstructorParameterIndices()
                .forEach((channelName, parameterIndex) -> {
                    AnnotatedElement parameter = constructorParams.getParameterAnnotations(parameterIndex);
                    var parameterAnnotator = resourceBeanTypeInfo.channelConstructorParameterAnnotators().get(channelName);
                    parameterAnnotator.accept(parameter);
                });

        ResultHandle[] params = new ResultHandle[constructorParameterTypes.length];
        for (int i = 0; i < params.length; i++) {
            params[i] = constructorParams.getMethodParam(i);
        }

        // Call the constructor of the super class
        constructorParams.invokeSpecialMethod(
                restResourceSuperClassInfo.constructors().get(1),
                thisObj,
                params);

        constructorParams.returnValue(thisObj);
        constructorParams.close();
    }

    /**
     * Check if there are any methods defined with Path annotation and implement them on the generated resource
     * so they are found by both REST framework and OpenAPI generation.
     */
    private void copyMethodPathAnnotations(ClassInfo restAPI, ClassCreator beanCreator) {
        for (MethodInfo method : restAPI.methods()) {
            if (method.hasAnnotation(jakarta.ws.rs.Path.class)) {
                MethodCreator additionalRestMethod = beanCreator
                        .getMethodCreator(method.name(), method.returnType().toString(),
                                method.parameters().stream().map(p -> p.type().toString()).toArray(String[]::new))
                        .setModifiers(ACC_PUBLIC);

                int i = 0;
                ResultHandle[] methodParams = new ResultHandle[method.parameters().size()];

                for (MethodParameterInfo param : method.parameters()) {
                    if (!param.annotations().isEmpty()) {

                        for (AnnotationInstance a : param.annotations().stream()
                                .filter(a -> a.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER).toList()) {
                            additionalRestMethod.getParameterAnnotations(i).addAnnotation(a);
                        }

                    }
                    methodParams[i] = additionalRestMethod.getMethodParam(i);
                    i++;
                }

                // invoke super (default interface) method
                ResultHandle response = additionalRestMethod.invokeSpecialInterfaceMethod(
                        MethodDescriptor.of(method),
                        additionalRestMethod.getThis(),
                        methodParams);

                additionalRestMethod.returnValue(response);
                additionalRestMethod.close();
            } else if (method.descriptorParametersCount() == 0 && Modifier.isAbstract(method.flags())) {
                // process no arg methods that are expected to be injection points
                // methods that act as accessor to injected type and by that return type is the type to be injected

                // create field that is annotated with Inject
                FieldCreator injectionField =
                        beanCreator.getFieldCreator(method.name(), DescriptorUtils.typeToString(method.returnType()))
                                .setModifiers(ACC_PROTECTED);
                injectionField.addAnnotation(Inject.class);

                // implement and return value of the injected field
                MethodCreator returnInjectedFieldMethod = beanCreator
                        .getMethodCreator(MethodDescriptor.of(method))
                        .setModifiers(ACC_PUBLIC);

                ResultHandle fieldData = returnInjectedFieldMethod.readInstanceField(injectionField.getFieldDescriptor(),
                        returnInjectedFieldMethod.getThis());
                returnInjectedFieldMethod.returnValue(fieldData);
                returnInjectedFieldMethod.close();
            }
        }
    }
}
