package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableUpdater;

public record RootVariableSource<Entity_, Value_>(
        Class<? extends Entity_> rootEntity,
        BiConsumer<? extends Entity_, Consumer<Value_>> valueEntityFunction,
        List<VariableSourceReference> variableSourceReferences) {

    private static final String COLLECTION_REFERENCE_SUFFIX = "[]";
    private record VariablePath(Class<?> variableEntityClass,
            String variableName,
            List<MemberAccessor> memberAccessorsBeforeEntity,
            List<MemberAccessor> memberAccessorsAfterEntity) {
        public BiConsumer<Object, Consumer<Object>> getValueVisitorFromVariableEntity() {
            return (entity, consumer) -> {
                var currentEntity = entity;
                for (var member : memberAccessorsAfterEntity) {
                    currentEntity = member.executeGetter(currentEntity);
                    if (currentEntity == null) {
                        return;
                    }
                }
                consumer.accept(currentEntity);
            };
        }
    }

    public static <Entity_, Value_> RootVariableSource<Entity_, Value_> from(
            Class<? extends Entity_> rootEntityClass,
            String targetVariableName,
            String variablePath,
            MemberAccessorFactory memberAccessorFactory,
            DescriptorPolicy descriptorPolicy) {
        var pathParts = variablePath.split("\\.");
        var variablePaths = new ArrayList<VariablePath>();
        List<MemberAccessor> chainToVariable = new ArrayList<>();
        List<MemberAccessor> listMemberAccessors = new ArrayList<>();
        var hasListMemberAccessor = false;
        List<List<MemberAccessor>> chainAfterVariable = new ArrayList<>();
        boolean afterVariable = false;
        Class<?> currentEntity = rootEntityClass;

        for (var pathPart : pathParts) {
            if (pathPart.endsWith(COLLECTION_REFERENCE_SUFFIX)) {
                if (afterVariable) {
                    throw new IllegalArgumentException("Cannot reference a collection on a variable.");
                }
                if (hasListMemberAccessor) {
                    throw new IllegalArgumentException("Cannot reference a collection on a collection.");
                }
                var memberName = pathPart.substring(0, pathPart.length() - COLLECTION_REFERENCE_SUFFIX.length());
                var memberAccessor = getMemberAccessor(currentEntity, memberName, memberAccessorFactory, descriptorPolicy);
                listMemberAccessors.add(memberAccessor);
                chainToVariable = new ArrayList<>();

                currentEntity = ConfigUtils.extractGenericTypeParameterOrFail(ShadowVariableUpdater.class.getSimpleName(),
                        currentEntity,
                        memberAccessor.getType(), memberAccessor.getGenericType(), ShadowVariableUpdater.class,
                        memberAccessor.getName());

                hasListMemberAccessor = true;
            } else {
                var memberAccessor = getMemberAccessor(currentEntity, pathPart,
                        memberAccessorFactory, descriptorPolicy);

                if (!hasListMemberAccessor) {
                    listMemberAccessors.add(memberAccessor);
                }

                var isVariable = false;

                for (var annotation : EntityDescriptor.getVariableAnnotationClasses()) {
                    if (getAnnotation(currentEntity, memberAccessor.getName(), annotation) != null) {
                        isVariable = true;
                        break;
                    }
                }

                chainToVariable.add(memberAccessor);
                for (var chain : chainAfterVariable) {
                    chain.add(memberAccessor);
                }
                if (isVariable) {
                    List<MemberAccessor> chainAfter = new ArrayList<>();

                    chainAfter.add(memberAccessor);
                    chainAfterVariable.add(chainAfter);

                    var chainBefore = new ArrayList<>(chainToVariable.subList(0, chainToVariable.size() - 1));
                    variablePaths.add(new VariablePath(currentEntity, pathPart, chainBefore, chainAfter));
                    afterVariable = true;
                }
                currentEntity = memberAccessor.getType();
            }
        }

        @SuppressWarnings("unchecked")
        BiConsumer<? extends Entity_, Consumer<Value_>> valueEntityFunction;

        List<MemberAccessor> finalChainToVariable = chainToVariable.subList(0, chainToVariable.size() - 1);
        if (!hasListMemberAccessor) {
            valueEntityFunction = (entity, consumer) -> {
                Object current = entity;
                for (var accessor : finalChainToVariable) {
                    current = accessor.executeGetter(current);
                    if (current == null) {
                        return;
                    }
                }
                consumer.accept((Value_) current);
            };
        } else {
            valueEntityFunction = (entity, consumer) -> {
                Object current = entity;
                for (var accessor : listMemberAccessors) {
                    current = accessor.executeGetter(current);
                    if (current == null) {
                        return;
                    }
                }

                Iterable<Object> iterable = (Iterable<Object>) current;
                outer: for (var item : iterable) {
                    current = item;
                    for (var accessor : finalChainToVariable) {
                        current = accessor.executeGetter(current);
                        if (current == null) {
                            continue outer;
                        }
                    }
                    consumer.accept((Value_) current);
                }
            };
        }

        List<VariableSourceReference> variableSourceReferences = new ArrayList<>();
        boolean isTopLevel = true;
        for (var afterChain : chainAfterVariable) {
            var variableMemberAccessor = afterChain.get(0);
            var sourceVariablePath = new VariablePath(variableMemberAccessor.getDeclaringClass(),
                    variableMemberAccessor.getName(),
                    chainToVariable.subList(0, chainToVariable.size() - afterChain.size()),
                    afterChain);
            VariableId downstreamDeclarativeVariable = null;

            var maybeDownstreamVariable = afterChain.remove(afterChain.size() - 1);
            if (isDeclarativeShadowVariable(maybeDownstreamVariable)) {
                downstreamDeclarativeVariable =
                        new VariableId(maybeDownstreamVariable.getDeclaringClass(), maybeDownstreamVariable.getName());
            }
            variableSourceReferences.add(new VariableSourceReference(
                    variableMemberAccessor.getDeclaringClass(),
                    variableMemberAccessor.getName(),
                    sourceVariablePath.memberAccessorsBeforeEntity,
                    isTopLevel,
                    isDeclarativeShadowVariable(variableMemberAccessor),
                    new VariableId(rootEntityClass, targetVariableName),
                    downstreamDeclarativeVariable,
                    sourceVariablePath.getValueVisitorFromVariableEntity()));
            isTopLevel = false;
        }

        if (variableSourceReferences.isEmpty()) {
            throw new IllegalArgumentException("The source path \"%s\" on entity class %s does not reference any variables."
                    .formatted(variablePath, rootEntityClass));
        }

        for (var variableSourceReference : variableSourceReferences) {
            VariableId sourceVariableId =
                    new VariableId(variableSourceReference.entityClass(), variableSourceReference.variableName());
            if (variableSourceReference.isDeclarative()
                    && variableSourceReference.downstreamDeclarativeVariable() != null
                    && !variableSourceReference.downstreamDeclarativeVariable()
                            .equals(sourceVariableId)) {
                throw new IllegalArgumentException(
                        "The source path \"%s\" accesses a declarative shadow variable \"%s\" from another declarative shadow variable \"%s\"."
                                .formatted(variablePath,
                                        variableSourceReference.downstreamDeclarativeVariable(),
                                        sourceVariableId));
            }
            if (!variableSourceReference.isDeclarative() && !variableSourceReference.chainToVariable().isEmpty()) {
                throw new IllegalArgumentException(
                        "The source path \"%s\" accesses a non-declarative shadow variable \"%s\" not from the root entity or collection."
                                .formatted(variablePath,
                                        variableSourceReference.variableName()));
            }
        }

        return new RootVariableSource<>(rootEntityClass,
                valueEntityFunction,
                variableSourceReferences);
    }

    private static MemberAccessor getMemberAccessor(Class<?> declaringClass, String memberName,
            MemberAccessorFactory memberAccessorFactory, DescriptorPolicy descriptorPolicy) {
        Member member = ReflectionHelper.getDeclaredField(declaringClass, memberName);
        if (member == null) {
            member = ReflectionHelper.getDeclaredGetterMethod(declaringClass, memberName);
            if (member == null) {
                throw new IllegalArgumentException("Class %s does not have member %s."
                        .formatted(declaringClass.getSimpleName(), memberName));
            }
        }
        return memberAccessorFactory.buildAndCacheMemberAccessor(member,
                MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD,
                descriptorPolicy.getDomainAccessType());
    }

    private static <T extends Annotation> T getAnnotation(Class<?> declaringClass, String memberName,
            Class<? extends T> annotationClass) {
        var field = ReflectionHelper.getDeclaredField(declaringClass, memberName);
        var getterMethod = ReflectionHelper.getDeclaredGetterMethod(declaringClass, memberName);

        if (field != null && field.getAnnotation(annotationClass) != null) {
            return field.getAnnotation(annotationClass);
        }
        if (getterMethod != null && getterMethod.getAnnotation(annotationClass) != null) {
            return getterMethod.getAnnotation(annotationClass);
        }
        return null;
    }

    private static boolean isDeclarativeShadowVariable(MemberAccessor memberAccessor) {
        var shadowVariable = getAnnotation(memberAccessor.getDeclaringClass(), memberAccessor.getName(),
                ShadowVariable.class);
        if (shadowVariable == null) {
            return false;
        }
        return !shadowVariable.method().isEmpty();
    }
}
