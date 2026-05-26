package ai.timefold.solver.model.quarkus.deployment;

import static org.objectweb.asm.Opcodes.ACC_PROTECTED;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.api.domain.Configuration;
import ai.timefold.solver.model.definition.impl.storage.inmemory.InMemoryStorage;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.model.definition.internal.storage.Storage;
import ai.timefold.solver.model.definition.internal.storage.SupportedStorages;
import ai.timefold.solver.model.quarkus.deployment.builditem.ModelComponentsBuildItem;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationInstanceBuilder;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;

class TimefoldStorageProcessor {

    public static final DotName STORAGE = DotName.createSimple(Storage.class.getName());

    public static final DotName STORAGE_SERVICE = DotName.createSimple(AbstractStorageService.class.getName());

    public static final DotName ENTERPRISE_STORAGE_SERVICE =
            DotName.createSimple("ai.timefold.solver.enterprise.model.storage.core.internal.AbstractStorageEnterpriseService");

    private static final String GENERATED_PACKAGE = "ai.timefold.platform.generated.storage.";

    /**
     * Generating concrete implementation of <code>ai.timefold.solver.model.api.storage.Storage<ModelOutput_></code> based on
     * project dependency with underlying object store such as S3, Google Cloud Storage or Azure BlobStore.
     * <p>
     * Looks up what implements the <code>ai.timefold.solver.model.api.ModelOutput</code> and uses it as the actual type of data
     * to be stored.
     *
     * @param combinedIndex - index that is used to find types implementing interfaces
     * @param generatedClasses - producer to push generated classes
     */
    @BuildStep
    void generateStorageImpl(CombinedIndexBuildItem combinedIndex,
            ModelComponentsBuildItem modelComponentsBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedClasses) {

        Collection<ClassInfo> storageImpl = combinedIndex.getIndex().getAllKnownImplementations(STORAGE);

        for (ClassInfo storageClass : storageImpl) {

            ClassInfo modelOutput = modelComponentsBuildItem.getModelOutput();
            GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedClasses);

            String generatedName =
                    GENERATED_PACKAGE + modelOutput.simpleName() + "_" + storageClass.simpleName();
            // create class definition that extends the concrete implementation and implements storage with user model as param
            ClassCreator beanCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                    .signature(SignatureBuilder.forClass().setSuperClass(Type.classType(storageClass.name()))
                            .addInterface(Type.parameterizedType(Type.classType(Storage.class.getCanonicalName()),
                                    Type.classType(modelOutput.name().toString()))))
                    .build();
            beanCreator.addAnnotation(ApplicationScoped.class);

            String storageName = storageClass.simpleName()
                    .substring(0, storageClass.simpleName().indexOf(Storage.class.getSimpleName())).toLowerCase();

            AnnotationInstanceBuilder builder = AnnotationInstance.builder(LookupIfProperty.class)
                    .add("name", SupportedStorages.STORAGE_TYPE_PROPERTY).add("stringValue", storageName);

            if (storageClass.name().toString().equals(InMemoryStorage.class.getCanonicalName())) {
                builder.add("lookupIfMissing", true);
            }

            beanCreator.addAnnotation(builder.build());
            List<MethodInfo> constructors = storageClass.constructors();
            List<String> parameterTypes = new ArrayList<>();
            if (constructors.size() > 2) {
                throw new IllegalStateException("The Storage class must have at most two constructors: " +
                        "a mandatory one without any parameters for recording, " +
                        "an optional one with parameters for injection.");
            }
            if (constructors.getFirst().parametersCount() != 0 &&
                    constructors.getLast().parametersCount() != 0) {
                throw new IllegalStateException("The Storage class is missing the mandatory constructor with no parameters.");
            }

            for (MethodInfo constructorInfo : constructors) {
                parameterTypes.clear();

                for (MethodParameterInfo param : constructorInfo.parameters()) {
                    parameterTypes.add(DescriptorUtils.typeToString(param.type()));
                }

                // create constructor with matching parameters of the super class
                MethodCreator constructor =
                        beanCreator.getMethodCreator(
                                MethodDescriptor.ofConstructor(beanCreator.getSuperClass(),
                                        parameterTypes.toArray(String[]::new)));

                ResultHandle thisObj = constructor.getThis();

                ResultHandle[] params = new ResultHandle[parameterTypes.size()];

                for (int i = 0; i < params.length; i++) {
                    params[i] = constructor.getMethodParam(i);
                }

                // Invoke Object's constructor
                constructor.invokeSpecialMethod(
                        MethodDescriptor.ofConstructor(beanCreator.getSuperClass(), parameterTypes.toArray(String[]::new)),
                        thisObj,
                        params);

                if (constructors.size() == 1 || !parameterTypes.isEmpty()) {
                    constructor.addAnnotation(Inject.class);
                }

                if (Modifier.isPrivate(constructorInfo.flags()) ||
                        !(Modifier.isProtected(constructorInfo.flags()) ||
                                Modifier.isPublic(constructorInfo.flags()))) {
                    throw new IllegalStateException(
                            "Storage Constructor cannot be private or package-private; use protected or public.");
                }

                // annotate any of the parameters with config property
                int index = 0;
                for (MethodParameterInfo param : constructorInfo.parameters()) {

                    Collection<AnnotationInstance> configAnnotations = param.annotations(ConfigProperty.class);

                    if (!configAnnotations.isEmpty()) {

                        for (AnnotationInstance annotation : configAnnotations) {
                            AnnotationCreator an =
                                    constructor.getParameterAnnotations(index).addAnnotation(annotation.name().toString());

                            for (AnnotationValue value : annotation.values()) {
                                an.add(value.name(), value.value());
                            }
                        }
                    }
                    index++;
                }
                constructor.returnValue(thisObj);
            }

            // add implementation of clazz method to return the class of the user model
            MethodCreator clazzMethod = beanCreator
                    .getMethodCreator("clazz", Class.class)
                    .setModifiers(ACC_PROTECTED);

            clazzMethod.returnValue(clazzMethod.loadClassFromTCCL(modelOutput));

            // override postconstruct and predestroy methods if exists so they are invoked by CDI
            List<MethodInfo> methods = storageClass.methods();

            for (MethodInfo method : methods) {

                if (method.hasAnnotation(PostConstruct.class)) {
                    MethodCreator postConstructMethod = beanCreator.getMethodCreator(method.name(), "V");
                    postConstructMethod.addAnnotation(PostConstruct.class);
                    postConstructMethod.invokeSpecialMethod(MethodDescriptor.of(method), postConstructMethod.getThis());
                    postConstructMethod.returnVoid();
                }

                if (method.hasAnnotation(PreDestroy.class)) {
                    MethodCreator preDestroyMethod = beanCreator.getMethodCreator(method.name(), "V");
                    preDestroyMethod.addAnnotation(PreDestroy.class);
                    preDestroyMethod.invokeSpecialMethod(MethodDescriptor.of(method), preDestroyMethod.getThis());
                    preDestroyMethod.returnVoid();
                }
            }

            beanCreator.close();
        }

    }

    /**
     * Generate concrete implementation of
     * <code>ai.timefold.solver.model.api.storage.AbstractStorageService<ModelInput_, ModelConfigOverrides_, OutputMetrics_, ModelOutput_, Score_, Justification_></code>
     * with model's specific model types of
     *
     * <ul>
     * <li>ModelInput_</li>
     * <li>ModelOutput_</li>
     * <li>ModelConfigOverrides</li>
     * <li>ModelOutputMetrics_</li>
     * <li>Score_</li>
     * <li>ModelConstraintJustification</li>
     * </ul>
     * <p>
     * Populates the generic types of the AbstractStorageService with found types implementing Models interfaces.
     *
     * @param combinedIndex
     * @param generatedClasses
     */
    @BuildStep
    void generateStorageServiceImpl(CombinedIndexBuildItem combinedIndex,
            ModelComponentsBuildItem modelComponentsBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedClasses) {

        DotName superClassName = modelComponentsBuildItem.isEnterprise() ? ENTERPRISE_STORAGE_SERVICE : STORAGE_SERVICE;
        ClassInfo storageServiceClass = combinedIndex.getIndex().getClassByName(superClassName);

        ClassInfo modelInput = modelComponentsBuildItem.getModelInput();
        ClassInfo modelOutput = modelComponentsBuildItem.getModelOutput();
        ClassInfo modelConfigOverrides = modelComponentsBuildItem.getModelConfigOverrides();
        ClassInfo modelInputMetrics = modelComponentsBuildItem.getModelInputMetrics();
        ClassInfo modelOutputMetrics = modelComponentsBuildItem.getModelOutputMetrics();

        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedClasses);

        String generatedName =
                GENERATED_PACKAGE + modelOutput.simpleName() + "_" + storageServiceClass.simpleName();
        // create class definition that extends the concrete implementation and implements storage with user model as param

        ClassCreator beanCreator =
                storageServiceClassCreator(generatedName, superClassName, modelComponentsBuildItem, classOutput);
        beanCreator.addAnnotation(ApplicationScoped.class);

        List<MethodInfo> constructors = storageServiceClass.constructors();
        List<String> parameterTypes = new ArrayList<>();
        if (constructors.size() > 2) {
            throw new IllegalStateException("The StorageService class must have at most two constructors: " +
                    "a mandatory one without any parameters for recording, " +
                    "an optional one with parameters for injection.");
        }
        if (constructors.getFirst().parametersCount() != 0 &&
                constructors.getLast().parametersCount() != 0) {
            throw new IllegalStateException(
                    "The StorageService class is missing the mandatory constructor with no parameters.");
        }

        for (MethodInfo constructorInfo : constructors) {
            parameterTypes.clear();
            for (MethodParameterInfo param : constructorInfo.parameters()) {
                parameterTypes.add(DescriptorUtils.typeToString(param.type()));
            }
            // create constructor with matching parameters of the super class
            MethodCreator constructor =
                    beanCreator.getMethodCreator(
                            MethodDescriptor.ofConstructor(beanCreator.getSuperClass(),
                                    parameterTypes.toArray(String[]::new)));

            ResultHandle thisObj = constructor.getThis();

            ResultHandle[] params = new ResultHandle[parameterTypes.size()];

            for (int i = 0; i < params.length; i++) {
                params[i] = constructor.getMethodParam(i);
            }

            // Invoke Object's constructor
            constructor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor(beanCreator.getSuperClass(), parameterTypes.toArray(String[]::new)),
                    thisObj,
                    params);

            if (Modifier.isPrivate(constructorInfo.flags()) ||
                    !(Modifier.isProtected(constructorInfo.flags()) ||
                            Modifier.isPublic(constructorInfo.flags()))) {
                throw new IllegalStateException(
                        "Storage Constructor cannot be private or package-private; use protected or public.");
            }

            // annotate any of the parameters with config property
            int index = 0;
            for (MethodParameterInfo param : constructorInfo.parameters()) {

                Collection<AnnotationInstance> configAnnotations = param.annotations(ConfigProperty.class);

                if (!configAnnotations.isEmpty()) {
                    for (AnnotationInstance annotation : configAnnotations) {
                        AnnotationCreator an =
                                constructor.getParameterAnnotations(index).addAnnotation(annotation.name().toString());

                        for (AnnotationValue value : annotation.values()) {
                            an.add(value.name(), value.value());
                        }
                    }
                }
                index++;
            }
            constructor.returnValue(thisObj);
        }

        // add implementation of getModelInputClass method to return the class of the user model
        MethodCreator getModelInputClassMethod = beanCreator
                .getMethodCreator("getModelInputClass", Class.class)
                .setModifiers(ACC_PROTECTED);

        getModelInputClassMethod.returnValue(getModelInputClassMethod.loadClassFromTCCL(modelInput));

        // add implementation of getInputMetricsClass method to return the class of the user model
        MethodCreator getInputMetricsClassMethod = beanCreator
                .getMethodCreator("getInputMetricsClass", Class.class)
                .setModifiers(ACC_PROTECTED);

        getInputMetricsClassMethod.returnValue(getInputMetricsClassMethod.loadClassFromTCCL(modelInputMetrics));

        // add implementation of getOutputMetricsClass method to return the class of the user model
        MethodCreator getOutputMetricsClassMethod = beanCreator
                .getMethodCreator("getOutputMetricsClass", Class.class)
                .setModifiers(ACC_PROTECTED);

        getOutputMetricsClassMethod.returnValue(getOutputMetricsClassMethod.loadClassFromTCCL(modelOutputMetrics));

        // add implementation of getConfigurationClass method to return the class of the user model
        String generatedNameConfigTypeRef =
                GENERATED_PACKAGE + modelOutput.simpleName() + "_ConfigTypeReference";
        // create class definition that extends the concrete implementation and implements TypeReference with config and overrides as param
        ClassCreator beanCreatorConfigTypeRef =
                ClassCreator.builder().classOutput(classOutput).className(generatedNameConfigTypeRef)
                        .signature(SignatureBuilder.forClass()
                                .setSuperClass(Type.parameterizedType(Type.classType(TypeReference.class.getCanonicalName()),
                                        Type.parameterizedType(Type.classType(Configuration.class.getCanonicalName()),
                                                Type.classType(modelConfigOverrides.name())))))
                        .build();

        beanCreatorConfigTypeRef.close();
        MethodCreator getConfigurationClassMethod = beanCreator
                .getMethodCreator("getConfigurationClass", TypeReference.class)
                .setModifiers(ACC_PROTECTED);
        MethodDescriptor typeRefConfig = MethodDescriptor.ofConstructor(generatedNameConfigTypeRef);
        getConfigurationClassMethod.returnValue(getConfigurationClassMethod.newInstance(typeRefConfig));

        beanCreator.close();

    }

    private ClassCreator storageServiceClassCreator(String className, DotName superClassName,
            ModelComponentsBuildItem modelComponents,
            GeneratedBeanGizmoAdaptor classOutput) {
        return ClassCreator.builder().classOutput(classOutput).className(className)
                .signature(SignatureBuilder.forClass()
                        .setSuperClass(Type.parameterizedType(Type.classType(superClassName),
                                Type.classType(modelComponents.getModelInput().name()),
                                Type.classType(modelComponents.getModelConfigOverrides().name()),
                                Type.classType(modelComponents.getModelInputMetrics().name()),
                                Type.classType(modelComponents.getModelOutputMetrics().name()),
                                Type.classType(modelComponents.getModelOutput().name()),
                                Type.classType(modelComponents.getModelScoreClass().name()),
                                Type.classType(modelComponents.getModelConstraintJustification().name()))))
                .build();
    }

}
