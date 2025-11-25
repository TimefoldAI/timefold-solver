package ai.timefold.solver.quarkus.deployment;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberAccessorImplementor;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberDescriptor;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberInfo;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoCloningUtils;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionClonerFactory;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionClonerImplementor;
import ai.timefold.solver.core.impl.domain.solution.cloner.gizmo.GizmoSolutionOrEntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.quarkus.gizmo.TimefoldGizmoBeanFactory;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.RuntimeValue;

final class GizmoMemberAccessorEntityEnhancer {

    private final Set<Class<?>> visitedClasses = new HashSet<>();

    // This keep track of fields we add virtual getters/setters for
    private final Set<Field> visitedFields = new HashSet<>();
    // This keep track of what fields we made non-final
    private final Set<Field> visitedFinalFields = new HashSet<>();
    private final Set<MethodInfo> visitedMethods = new HashSet<>();

    private static String getVirtualGetterName(boolean isField, String name) {
        return "$get$timefold$__" + ((isField) ? "field$__" : "method$__") + name;
    }

    private static String getVirtualSetterName(boolean isField, String name) {
        return "$set$timefold$__" + ((isField) ? "field$__" : "method$__") + name;
    }

    /**
     * Generates the bytecode for the member accessor for the specified field.
     * Additionally enhances the class that declares the field with public simple
     * getters/setters methods for the field if the field is private.
     *
     * @param annotationInstance The annotations on the field
     * @param classOutput Where to output the bytecode
     * @param fieldInfo The field to generate the MemberAccessor for
     * @param transformers BuildProducer of BytecodeTransformers
     */
    public String generateFieldAccessor(AnnotationInstance annotationInstance, ClassOutput classOutput, FieldInfo fieldInfo,
            BuildProducer<BytecodeTransformerBuildItem> transformers) throws ClassNotFoundException, NoSuchFieldException {
        Class<?> declaringClass = Class.forName(fieldInfo.declaringClass().name().toString(), false,
                Thread.currentThread().getContextClassLoader());
        Field fieldMember = declaringClass.getDeclaredField(fieldInfo.name());
        GizmoMemberDescriptor member = createMemberDescriptorForField(fieldMember, transformers);
        GizmoMemberInfo memberInfo = new GizmoMemberInfo(member, true,
                (Class<? extends Annotation>) Class.forName(annotationInstance.name().toString(), false,
                        Thread.currentThread().getContextClassLoader()));
        String generatedClassName = GizmoMemberAccessorFactory.getGeneratedClassName(fieldMember);
        GizmoMemberAccessorImplementor.defineAccessorFor(generatedClassName, classOutput, memberInfo);
        return generatedClassName;
    }

    private void addVirtualFieldGetter(Class<?> classInfo, Field fieldInfo,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (!visitedFields.contains(fieldInfo)) {
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.getName(),
                    (className, classVisitor) -> new TimefoldFieldEnhancingClassVisitor(classInfo, classVisitor,
                            fieldInfo)));
            visitedFields.add(fieldInfo);
        }
    }

    private void makeFieldNonFinal(Field finalField, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (visitedFinalFields.contains(finalField)) {
            return;
        }
        transformers.produce(new BytecodeTransformerBuildItem(finalField.getDeclaringClass().getName(),
                (className, classVisitor) -> new TimefoldFinalFieldEnhancingClassVisitor(classVisitor, finalField)));
        visitedFinalFields.add(finalField);
    }

    private static String getMemberName(Member member) {
        return Objects.requireNonNullElse(ReflectionHelper.getGetterPropertyName(member), member.getName());
    }

    private static Optional<MethodDesc> getSetterDescriptor(ClassInfo classInfo, MethodInfo methodInfo, String name) {
        if (methodInfo.name().startsWith("get") || methodInfo.name().startsWith("is")) {
            // ex: for methodInfo = Integer getValue(), name = value,
            // return void setValue(Integer value)
            // i.e. capitalize first letter of name, and take a parameter
            // of the getter return type.
            return Optional.ofNullable(classInfo.method("set" + name.substring(0, 1)
                    .toUpperCase(Locale.ROOT) +
                    name.substring(1),
                    methodInfo.returnType())).map(
                            setterInfo -> ClassMethodDesc.of(
                                    ClassDesc.ofDescriptor(classInfo.descriptor()),
                                    setterInfo.name(),
                                    ClassDesc.ofDescriptor(setterInfo.returnType().descriptor()),
                                    setterInfo.parameterTypes().stream()
                                            .map(parameter -> ClassDesc.ofDescriptor(parameter.descriptor()))
                                            .toArray(ClassDesc[]::new)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Generates the bytecode for the member accessor for the specified method.
     * Additionally enhances the class that declares the method with public simple
     * read/(optionally write if getter method and setter present) methods for the method
     * if the method is private.
     *
     * @param annotationInstance The annotations on the field
     * @param classOutput Where to output the bytecode
     * @param classInfo The declaring class for the field
     * @param methodInfo The method to generate the MemberAccessor for
     * @param transformers BuildProducer of BytecodeTransformers
     */
    public String generateMethodAccessor(AnnotationInstance annotationInstance, ClassOutput classOutput,
            ClassInfo classInfo, MethodInfo methodInfo, boolean requiredReturnType,
            BuildProducer<BytecodeTransformerBuildItem> transformers)
            throws ClassNotFoundException, NoSuchMethodException {
        var declaringClass = Class.forName(methodInfo.declaringClass().name().toString(), false,
                Thread.currentThread().getContextClassLoader());
        var methodMember = declaringClass.getDeclaredMethod(methodInfo.name());
        var generatedClassName = GizmoMemberAccessorFactory.getGeneratedClassName(methodMember);
        GizmoMemberDescriptor member;
        var name = getMemberName(methodMember);
        var setterDescriptor = getSetterDescriptor(classInfo, methodInfo, name);

        var memberDescriptor = ClassMethodDesc.of(ClassDesc.ofDescriptor(methodInfo.declaringClass().descriptor()),
                methodInfo.name(), ClassDesc.ofDescriptor(methodInfo.returnType().descriptor()),
                methodInfo.parameterTypes().stream()
                        .map(parameterType -> ClassDesc.ofDescriptor(parameterType.descriptor()))
                        .toArray(ClassDesc[]::new));

        if (Modifier.isPublic(methodInfo.flags())) {
            member = new GizmoMemberDescriptor(name, memberDescriptor, declaringClass, setterDescriptor.orElse(null));
        } else {
            setterDescriptor = addVirtualMethodGetter(classInfo, methodInfo, name, setterDescriptor, transformers);
            var methodName = getVirtualGetterName(false, name);
            var newMethodDescriptor =
                    ClassMethodDesc.of(ClassDesc.ofDescriptor(declaringClass.descriptorString()), methodName,
                            memberDescriptor.returnType());
            member = new GizmoMemberDescriptor(name, newMethodDescriptor, memberDescriptor, declaringClass,
                    setterDescriptor.orElse(null));
        }
        Class<? extends Annotation> annotationClass = null;
        if (requiredReturnType || annotationInstance != null) {
            annotationClass = (Class<? extends Annotation>) Class.forName(annotationInstance.name().toString(), false,
                    Thread.currentThread().getContextClassLoader());
        }
        GizmoMemberInfo memberInfo = new GizmoMemberInfo(member, requiredReturnType, annotationClass);
        GizmoMemberAccessorImplementor.defineAccessorFor(generatedClassName, classOutput, memberInfo);
        return generatedClassName;
    }

    private Optional<MethodDesc> addVirtualMethodGetter(ClassInfo classInfo, MethodInfo methodInfo, String name,
            Optional<MethodDesc> setterDescriptor,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (!visitedMethods.contains(methodInfo)) {
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(),
                    (className, classVisitor) -> new TimefoldMethodEnhancingClassVisitor(classInfo, classVisitor, methodInfo,
                            name, setterDescriptor)));
            visitedMethods.add(methodInfo);
        }
        return setterDescriptor.map(md -> ClassMethodDesc
                .of(ClassDesc.ofDescriptor(classInfo.descriptor()), getVirtualSetterName(false, name),
                        md.returnType(), md.parameterTypes()));
    }

    public String generateSolutionCloner(SolutionDescriptor solutionDescriptor, ClassOutput classOutput,
            IndexView indexView, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        String generatedClassName = GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor);
        var gizmo = Gizmo.create(classOutput);
        gizmo.class_(generatedClassName, classCreator -> {
            classCreator.implements_(GizmoSolutionCloner.class);
            classCreator.final_();

            Set<Class<?>> solutionSubclassSet =
                    indexView.getAllKnownSubclasses(DotName.createSimple(solutionDescriptor.getSolutionClass().getName()))
                            .stream()
                            .map(classInfo -> {
                                try {
                                    return Class.forName(classInfo.name().toString(), false,
                                            Thread.currentThread().getContextClassLoader());
                                } catch (ClassNotFoundException e) {
                                    throw new IllegalStateException("Unable to find class (" + classInfo.name() +
                                            "), which is a known subclass of the solution class (" +
                                            solutionDescriptor.getSolutionClass() + ").", e);
                                }
                            }).collect(Collectors.toCollection(LinkedHashSet::new));
            solutionSubclassSet.add(solutionDescriptor.getSolutionClass());

            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedGizmoSolutionOrEntityDescriptorForClassMap =
                    new HashMap<>();

            for (Class<?> solutionSubclass : solutionSubclassSet) {
                getGizmoSolutionOrEntityDescriptorForEntity(solutionDescriptor,
                        solutionSubclass,
                        memoizedGizmoSolutionOrEntityDescriptorForClassMap,
                        transformers);
            }

            // IDEA gave error on entityClass being a Class...
            for (Object entityClass : solutionDescriptor.getEntityClassSet()) {
                getGizmoSolutionOrEntityDescriptorForEntity(solutionDescriptor,
                        (Class<?>) entityClass,
                        memoizedGizmoSolutionOrEntityDescriptorForClassMap,
                        transformers);
            }

            Set<Class<?>> solutionAndEntitySubclassSet = new HashSet<>(solutionSubclassSet);
            for (Object entityClassObject : solutionDescriptor.getEntityClassSet()) {
                Class<?> entityClass = (Class<?>) entityClassObject;
                Collection<ClassInfo> classInfoCollection;

                // getAllKnownSubclasses returns an empty collection for interfaces (silent failure); thus:
                // for interfaces, we use getAllKnownImplementors; otherwise we use getAllKnownSubclasses
                if (entityClass.isInterface()) {
                    classInfoCollection = indexView.getAllKnownImplementors(DotName.createSimple(entityClass.getName()));
                } else {
                    classInfoCollection = indexView.getAllKnownSubclasses(DotName.createSimple(entityClass.getName()));
                }

                classInfoCollection.stream().map(classInfo -> {
                    try {
                        return Class.forName(classInfo.name().toString(), false,
                                Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Unable to find class (" + classInfo.name() +
                                "), which is a known subclass of the entity class (" +
                                entityClass + ").", e);
                    }
                }).forEach(solutionAndEntitySubclassSet::add);
            }
            Set<Class<?>> deepClonedClassSet =
                    GizmoCloningUtils.getDeepClonedClasses(solutionDescriptor, solutionAndEntitySubclassSet);

            for (Class<?> deepCloningClass : deepClonedClassSet) {
                makeConstructorAccessible(deepCloningClass, transformers);
                if (!memoizedGizmoSolutionOrEntityDescriptorForClassMap.containsKey(deepCloningClass)) {
                    getGizmoSolutionOrEntityDescriptorForEntity(solutionDescriptor,
                            deepCloningClass,
                            memoizedGizmoSolutionOrEntityDescriptorForClassMap,
                            transformers);
                }
            }

            GizmoSolutionClonerImplementor.defineClonerFor(QuarkusGizmoSolutionClonerImplementor::new,
                    classCreator,
                    solutionDescriptor, solutionSubclassSet,
                    memoizedGizmoSolutionOrEntityDescriptorForClassMap, deepClonedClassSet);
        });

        return generatedClassName;
    }

    private void makeConstructorAccessible(Class<?> clazz, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        try {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                return;
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers()) && !visitedClasses.contains(clazz)) {
                transformers.produce(new BytecodeTransformerBuildItem(clazz.getName(),
                        (className, classVisitor) -> new TimefoldConstructorEnhancingClassVisitor(classVisitor)));
                visitedClasses.add(clazz);
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Class (" + clazz.getName() + ") must have a no-args constructor so it can be constructed by Timefold.");
        }
    }

    private GizmoSolutionOrEntityDescriptor getGizmoSolutionOrEntityDescriptorForEntity(
            SolutionDescriptor solutionDescriptor, Class<?> entityClass,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedMap,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        Map<Field, GizmoMemberDescriptor> solutionFieldToMemberDescriptor = new HashMap<>();

        Class<?> currentClass = entityClass;
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                solutionFieldToMemberDescriptor.put(field, createMemberDescriptorForField(field, transformers));
            }
            currentClass = currentClass.getSuperclass();
        }
        GizmoSolutionOrEntityDescriptor out =
                new GizmoSolutionOrEntityDescriptor(solutionDescriptor, entityClass, solutionFieldToMemberDescriptor);
        memoizedMap.put(entityClass, out);
        return out;
    }

    private GizmoMemberDescriptor createMemberDescriptorForField(Field field,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (Modifier.isFinal(field.getModifiers())) {
            makeFieldNonFinal(field, transformers);
        }

        var declaringClass = field.getDeclaringClass();
        var memberDescriptor = FieldDesc.of(field);
        var name = field.getName();

        // Not being recorded, so can use Type and annotated element directly
        if (Modifier.isPublic(field.getModifiers())) {
            return new GizmoMemberDescriptor(name, memberDescriptor, declaringClass);
        } else {
            addVirtualFieldGetter(declaringClass, field, transformers);
            var getterName = getVirtualGetterName(true, name);
            var getterDescriptor =
                    ClassMethodDesc.of(ClassDesc.ofDescriptor(declaringClass.descriptorString()), getterName, field.getType());
            var setterName = getVirtualSetterName(true, name);
            var setterDescriptor =
                    ClassMethodDesc.of(ClassDesc.ofDescriptor(declaringClass.descriptorString()), setterName, void.class,
                            field.getType());
            return new GizmoMemberDescriptor(name, getterDescriptor, memberDescriptor, declaringClass, setterDescriptor);
        }
    }

    public static Map<String, RuntimeValue<MemberAccessor>> getGeneratedGizmoMemberAccessorMap(RecorderContext recorderContext,
            Set<String> generatedMemberAccessorsClassNames) {
        Map<String, RuntimeValue<MemberAccessor>> generatedGizmoMemberAccessorNameToInstanceMap = new HashMap<>();
        for (String className : generatedMemberAccessorsClassNames) {
            generatedGizmoMemberAccessorNameToInstanceMap.put(className, recorderContext.newInstance(className));
        }
        return generatedGizmoMemberAccessorNameToInstanceMap;
    }

    public static Map<String, RuntimeValue<SolutionCloner>> getGeneratedSolutionClonerMap(RecorderContext recorderContext,
            Set<String> generatedSolutionClonersClassNames) {
        Map<String, RuntimeValue<SolutionCloner>> generatedGizmoSolutionClonerNameToInstanceMap = new HashMap<>();
        for (String className : generatedSolutionClonersClassNames) {
            generatedGizmoSolutionClonerNameToInstanceMap.put(className, recorderContext.newInstance(className));
        }
        return generatedGizmoSolutionClonerNameToInstanceMap;
    }

    public String generateGizmoBeanFactory(ClassOutput classOutput, Set<Class<?>> beanClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {
        String generatedClassName = TimefoldGizmoBeanFactory.class.getName() + "$Implementation";

        var gizmo = Gizmo.create(classOutput);
        gizmo.class_(generatedClassName, classCreator -> {
            classCreator.implements_(TimefoldGizmoBeanFactory.class);

            classCreator.addAnnotation(ApplicationScoped.class);
            classCreator.defaultConstructor();
            classCreator.method("newInstance", methodCreator -> {
                var query = methodCreator.parameter("query", Class.class);
                methodCreator.returning(Object.class);
                methodCreator.body(blockCreator -> {
                    for (Class<?> beanClass : beanClasses) {
                        if (beanClass.isInterface() || Modifier.isAbstract(beanClass.getModifiers())) {
                            continue;
                        }
                        makeConstructorAccessible(beanClass, transformers);
                        var beanClassHandle = Const.of(beanClass);
                        blockCreator.if_(blockCreator.objEquals(beanClassHandle, query), isQueryBranch -> {
                            isQueryBranch.return_(isQueryBranch.new_(beanClass));
                        });
                    }
                    blockCreator.returnNull();
                });
            });
        });

        return generatedClassName;
    }

    private static class TimefoldFinalFieldEnhancingClassVisitor extends ClassVisitor {
        final Field finalField;

        public TimefoldFinalFieldEnhancingClassVisitor(ClassVisitor outputClassVisitor, Field finalField) {
            super(io.quarkus.gizmo.Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.finalField = finalField;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (name.equals(finalField.getName())) {
                // x & ~bitFlag = x without bitFlag set
                return super.visitField(access & ~Opcodes.ACC_FINAL, name, descriptor, signature, value);
            } else {
                return super.visitField(access, name, descriptor, signature, value);
            }
        }
    }

    private static class TimefoldConstructorEnhancingClassVisitor extends ClassVisitor {
        public TimefoldConstructorEnhancingClassVisitor(ClassVisitor outputClassVisitor) {
            super(io.quarkus.gizmo.Gizmo.ASM_API_VERSION, outputClassVisitor);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String desc,
                String signature,
                String[] exceptions) {
            if (name.equals("<init>")) {
                return cv.visitMethod(
                        ACC_PUBLIC,
                        name,
                        desc,
                        signature,
                        exceptions);
            }
            return cv.visitMethod(
                    access, name, desc, signature, exceptions);
        }
    }

    private static class TimefoldFieldEnhancingClassVisitor extends ClassVisitor {
        private final Field fieldInfo;
        private final Class<?> clazz;
        private final String fieldTypeDescriptor;

        public TimefoldFieldEnhancingClassVisitor(Class<?> classInfo, ClassVisitor outputClassVisitor,
                Field fieldInfo) {
            super(io.quarkus.gizmo.Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.fieldInfo = fieldInfo;
            clazz = classInfo;
            fieldTypeDescriptor = Type.getDescriptor(fieldInfo.getType());
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            addGetter(this.cv);
            addSetter(this.cv);
        }

        private void addSetter(ClassVisitor classWriter) {
            String methodName = getVirtualSetterName(true, fieldInfo.getName());
            MethodVisitor mv;
            mv = classWriter.visitMethod(ACC_PUBLIC, methodName, "(" + fieldTypeDescriptor + ")V",
                    null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(Type.getType(fieldTypeDescriptor).getOpcode(ILOAD), 1);
            mv.visitFieldInsn(PUTFIELD, Type.getInternalName(clazz), fieldInfo.getName(), fieldTypeDescriptor);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
        }

        private void addGetter(ClassVisitor classWriter) {
            String methodName = getVirtualGetterName(true, fieldInfo.getName());
            MethodVisitor mv;
            mv = classWriter.visitMethod(ACC_PUBLIC, methodName, "()" + fieldTypeDescriptor,
                    null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, Type.getInternalName(clazz), fieldInfo.getName(), fieldTypeDescriptor);
            mv.visitInsn(Type.getType(fieldTypeDescriptor).getOpcode(IRETURN));
            mv.visitMaxs(0, 0);
        }
    }

    private static class TimefoldMethodEnhancingClassVisitor extends ClassVisitor {
        private final MethodInfo methodInfo;
        private final Class<?> clazz;
        private final String returnTypeDescriptor;
        private final MethodDesc setter;
        private final String name;

        public TimefoldMethodEnhancingClassVisitor(ClassInfo classInfo, ClassVisitor outputClassVisitor,
                MethodInfo methodInfo, String name, Optional<MethodDesc> maybeSetter) {
            super(io.quarkus.gizmo.Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.methodInfo = methodInfo;
            this.name = name;
            this.setter = maybeSetter.orElse(null);
            try {
                clazz = Class.forName(classInfo.name().toString(), false, Thread.currentThread().getContextClassLoader());
                returnTypeDescriptor = methodInfo.returnType().descriptor();
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            addGetter(this.cv);
            if (setter != null) {
                addSetter(this.cv);
            }
        }

        private void addGetter(ClassVisitor classWriter) {
            String methodName = getVirtualGetterName(false, name);
            MethodVisitor mv;
            mv = classWriter.visitMethod(ACC_PUBLIC, methodName, "()" + returnTypeDescriptor,
                    null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(clazz), methodInfo.name(),
                    "()" + returnTypeDescriptor, false);
            mv.visitInsn(Type.getType(returnTypeDescriptor).getOpcode(IRETURN));
            mv.visitMaxs(0, 0);
        }

        private void addSetter(ClassVisitor classWriter) {
            if (setter == null) {
                return;
            }
            String methodName = getVirtualSetterName(false, name);
            MethodVisitor mv;
            mv = classWriter.visitMethod(ACC_PUBLIC, methodName, setter.type().descriptorString(),
                    null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(clazz), setter.name(),
                    setter.type().descriptorString(), false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
        }
    }
}
