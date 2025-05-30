package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record RootVariableSource<Entity_, Value_>(
        Class<? extends Entity_> rootEntity,
        List<MemberAccessor> listMemberAccessors,
        BiConsumer<Object, Consumer<Value_>> valueEntityFunction,
        List<VariableSourceReference> variableSourceReferences) {

    public static final String COLLECTION_REFERENCE_SUFFIX = "[]";
    public static final String MEMBER_SEPERATOR_REGEX = "\\.";

    private record VariablePath(Class<?> variableEntityClass,
            String variableName,
            List<MemberAccessor> memberAccessorsBeforeEntity,
            List<MemberAccessor> memberAccessorsAfterEntity) {

        public @Nullable Object findTargetEntity(Object entity) {
            var currentEntity = entity;
            for (var member : memberAccessorsAfterEntity) {
                currentEntity = member.executeGetter(currentEntity);
                if (currentEntity == null) {
                    return null;
                }
            }
            return currentEntity;
        }
    }

    public static Iterator<PathPart> pathIterator(Class<?> rootEntity, String path) {
        final var parts = path.split(MEMBER_SEPERATOR_REGEX);
        return new PathPartIterator(rootEntity, parts, path);
    }

    public static <Entity_, Value_> RootVariableSource<Entity_, Value_> from(
            PlanningSolutionMetaModel<?> solutionMetaModel,
            Class<? extends Entity_> rootEntityClass,
            String targetVariableName,
            String variablePath,
            MemberAccessorFactory memberAccessorFactory,
            DescriptorPolicy descriptorPolicy) {
        List<MemberAccessor> chainToVariable = new ArrayList<>();
        List<MemberAccessor> listMemberAccessors = new ArrayList<>();
        var hasListMemberAccessor = false;
        List<List<MemberAccessor>> chainStartingFromSourceVariableList = new ArrayList<>();
        boolean isAfterVariable = false;
        Class<?> currentEntity = rootEntityClass;
        var factCountSinceLastVariable = 0;

        for (var iterator = pathIterator(rootEntityClass, variablePath); iterator.hasNext();) {
            var pathPart = iterator.next();
            if (pathPart.isCollection()) {
                if (isAfterVariable) {
                    throw new IllegalArgumentException(
                            "The source path (%s) starting from root class (%s) accesses a collection (%s[]) after a variable (%s), which is not allowed."
                                    .formatted(variablePath, rootEntityClass.getSimpleName(), pathPart.name(),
                                            chainStartingFromSourceVariableList.get(0).get(0).getName()));
                }
                if (hasListMemberAccessor) {
                    throw new IllegalArgumentException(
                            "The source path (%s) starting from root class (%s) accesses a collection (%s[]) after another collection (%s), which is not allowed."
                                    .formatted(variablePath, rootEntityClass.getSimpleName(), pathPart.name(),
                                            listMemberAccessors.get(listMemberAccessors.size() - 1).getName()));
                }
                var memberAccessor =
                        getMemberAccessor(pathPart.member(), memberAccessorFactory,
                                descriptorPolicy);
                listMemberAccessors.add(memberAccessor);
                chainToVariable = new ArrayList<>();
                factCountSinceLastVariable = 0;

                currentEntity = ConfigUtils.extractGenericTypeParameterOrFail(ShadowSources.class.getSimpleName(),
                        currentEntity,
                        memberAccessor.getType(), memberAccessor.getGenericType(), ShadowSources.class,
                        memberAccessor.getName());

                hasListMemberAccessor = true;
            } else {
                var memberAccessor = getMemberAccessor(pathPart.member(),
                        memberAccessorFactory, descriptorPolicy);

                if (!hasListMemberAccessor) {
                    listMemberAccessors.add(memberAccessor);
                }

                var isVariable = isVariable(solutionMetaModel, memberAccessor.getDeclaringClass(), pathPart.name());
                chainToVariable.add(memberAccessor);
                for (var chain : chainStartingFromSourceVariableList) {
                    chain.add(memberAccessor);
                }
                if (isVariable) {
                    List<MemberAccessor> chainStartingFromSourceVariable = new ArrayList<>();

                    chainStartingFromSourceVariable.add(memberAccessor);
                    chainStartingFromSourceVariableList.add(chainStartingFromSourceVariable);

                    isAfterVariable = true;
                    factCountSinceLastVariable = 0;
                } else {
                    factCountSinceLastVariable++;
                    if (factCountSinceLastVariable == 2) {
                        throw new IllegalArgumentException(
                                "The source path (%s) starting from root entity (%s) referencing multiple facts (%s, %s) in a row."
                                        .formatted(variablePath, rootEntityClass.getSimpleName(),
                                                chainToVariable.get(chainToVariable.size() - 2).getName(),
                                                chainToVariable.get(chainToVariable.size() - 1).getName()));
                    }
                }
                currentEntity = memberAccessor.getType();
            }
        }

        BiConsumer<Object, Consumer<Value_>> valueEntityFunction;
        List<MemberAccessor> chainToVariableEntity = chainToVariable.subList(0, chainToVariable.size() - 1);
        if (!hasListMemberAccessor) {
            valueEntityFunction = getRegularSourceEntityVisitor(chainToVariableEntity);
            listMemberAccessors.clear();
        } else {
            valueEntityFunction = getCollectionSourceEntityVisitor(listMemberAccessors, chainToVariableEntity);
        }

        List<VariableSourceReference> variableSourceReferences = new ArrayList<>();
        for (var i = 0; i < chainStartingFromSourceVariableList.size(); i++) {
            var chainStartingFromSourceVariable = chainStartingFromSourceVariableList.get(i);
            var newSourceReference =
                    createVariableSourceReferenceFromChain(variablePath, variableSourceReferences, listMemberAccessors,
                            solutionMetaModel,
                            rootEntityClass, targetVariableName, chainStartingFromSourceVariable,
                            chainToVariable,
                            i == 0,
                            i == chainStartingFromSourceVariableList.size() - 1);
            variableSourceReferences.add(newSourceReference);
        }

        if (variableSourceReferences.isEmpty()) {
            throw new IllegalArgumentException(
                    "The source path (%s) starting from root entity class (%s) does not reference any variables."
                            .formatted(variablePath, rootEntityClass.getSimpleName()));
        }

        if (factCountSinceLastVariable != 0) {
            throw new IllegalArgumentException(
                    "The source path (%s) starting from root entity class (%s) does not end on a variable."
                            .formatted(variablePath, rootEntityClass.getSimpleName()));
        }

        for (var variableSourceReference : variableSourceReferences) {
            assertIsValidVariableReference(rootEntityClass, variablePath, variableSourceReference);
        }

        return new RootVariableSource<>(rootEntityClass,
                listMemberAccessors,
                valueEntityFunction,
                variableSourceReferences);
    }

    public @NonNull BiConsumer<Object, Consumer<Object>> getEntityVisitor(List<MemberAccessor> chainToEntity) {
        if (listMemberAccessors.isEmpty()) {
            return getRegularSourceEntityVisitor(chainToEntity);
        } else {
            return getCollectionSourceEntityVisitor(listMemberAccessors, chainToEntity);
        }
    }

    private static <Value_> @NonNull BiConsumer<Object, Consumer<Value_>> getRegularSourceEntityVisitor(
            List<MemberAccessor> finalChainToVariable) {
        return (entity, consumer) -> {
            Object current = entity;
            for (var accessor : finalChainToVariable) {
                current = accessor.executeGetter(current);
                if (current == null) {
                    return;
                }
            }
            consumer.accept((Value_) current);
        };
    }

    private static <Value_> @NonNull BiConsumer<Object, Consumer<Value_>> getCollectionSourceEntityVisitor(
            List<MemberAccessor> listMemberAccessors, List<MemberAccessor> finalChainToVariable) {
        var entityListMemberAccessor = RootVariableSource.<Iterable<Object>> getRegularSourceEntityVisitor(listMemberAccessors);
        var elementSourceEntityVisitor = RootVariableSource.<Value_> getRegularSourceEntityVisitor(finalChainToVariable);
        return (entity, consumer) -> entityListMemberAccessor.accept(entity, iterable -> {
            for (var item : iterable) {
                elementSourceEntityVisitor.accept(item, consumer);
            }
        });
    }

    private static <Entity_> @NonNull VariableSourceReference createVariableSourceReferenceFromChain(
            String variablePath, List<VariableSourceReference> variableSourceReferences,
            List<MemberAccessor> listMemberAccessors,
            PlanningSolutionMetaModel<?> solutionMetaModel,
            Class<? extends Entity_> rootEntityClass, String targetVariableName, List<MemberAccessor> afterChain,
            List<MemberAccessor> chainToVariable, boolean isTopLevel, boolean isBottomLevel) {
        var variableMemberAccessor = afterChain.get(0);
        var sourceVariablePath = new VariablePath(variableMemberAccessor.getDeclaringClass(),
                variableMemberAccessor.getName(),
                chainToVariable.subList(0, chainToVariable.size() - afterChain.size()),
                afterChain);
        VariableMetaModel<?, ?, ?> downstreamDeclarativeVariable = null;

        var maybeDownstreamVariable = afterChain.remove(afterChain.size() - 1);
        if (isDeclarativeShadowVariable(maybeDownstreamVariable)) {
            downstreamDeclarativeVariable =
                    solutionMetaModel.entity(maybeDownstreamVariable.getDeclaringClass())
                            .variable(maybeDownstreamVariable.getName());
        }
        var isDeclarative = isDeclarativeShadowVariable(variableMemberAccessor);
        if (!isDeclarative) {
            for (var previousVariableSourceReference : variableSourceReferences) {
                if (!previousVariableSourceReference.isDeclarative()) {
                    throw new IllegalArgumentException(
                            """
                                    The source path (%s) starting from root entity class (%s) \
                                    accesses a non-declarative shadow variable (%s) \
                                    after another non-declarative shadow variable (%s)."""
                                    .formatted(
                                            variablePath, rootEntityClass.getSimpleName(), variableMemberAccessor.getName(),
                                            previousVariableSourceReference.variableMetaModel().name()));
                }
            }
        }

        return new VariableSourceReference(
                solutionMetaModel.entity(variableMemberAccessor.getDeclaringClass()).variable(variableMemberAccessor.getName()),
                sourceVariablePath.memberAccessorsBeforeEntity,
                isTopLevel && sourceVariablePath.memberAccessorsBeforeEntity.isEmpty() && listMemberAccessors.isEmpty(),
                isTopLevel,
                isBottomLevel,
                isDeclarative,
                solutionMetaModel.entity(rootEntityClass).variable(targetVariableName),
                downstreamDeclarativeVariable,
                sourceVariablePath::findTargetEntity);
    }

    private static void assertIsValidVariableReference(Class<?> rootEntityClass, String variablePath,
            VariableSourceReference variableSourceReference) {
        var sourceVariableId = variableSourceReference.variableMetaModel();
        if (variableSourceReference.isDeclarative()
                && variableSourceReference.downstreamDeclarativeVariableMetamodel() != null
                && !variableSourceReference.isBottomLevel()) {
            throw new IllegalArgumentException(
                    "The source path (%s) starting from root entity class (%s) accesses a declarative shadow variable (%s) from another declarative shadow variable (%s)."
                            .formatted(variablePath,
                                    rootEntityClass.getSimpleName(),
                                    variableSourceReference.downstreamDeclarativeVariableMetamodel().name(),
                                    sourceVariableId.name()));
        }
    }

    public static Member getMember(Class<?> rootClass, String sourcePath, Class<?> declaringClass,
            String memberName) {
        var field = ReflectionHelper.getDeclaredField(declaringClass, memberName);
        var getterMethod = ReflectionHelper.getDeclaredGetterMethod(declaringClass, memberName);
        if (field == null && getterMethod == null) {
            throw new IllegalArgumentException(
                    "The source path (%s) starting from root class (%s) references a member (%s) on class (%s) that does not exist."
                            .formatted(sourcePath, rootClass.getSimpleName(), memberName, declaringClass.getSimpleName()));
        } else if (field != null && getterMethod == null) {
            return field;
        } else if (field == null) { // method is not guaranteed to not be null
            return getterMethod;
        } else {
            var fieldType = field.getType();
            var methodType = getterMethod.getReturnType();
            if (fieldType.equals(methodType)) {
                // Prefer getter if types are the same
                return getterMethod;
            } else if (fieldType.isAssignableFrom(methodType)) {
                // Getter is more specific than field
                return getterMethod;
            } else if (methodType.isAssignableFrom(fieldType)) {
                // Field is more specific than getter
                return field;
            } else {
                // Field and getter are not covariant; prefer method
                return getterMethod;
            }
        }
    }

    private static MemberAccessor getMemberAccessor(Member member, MemberAccessorFactory memberAccessorFactory,
            DescriptorPolicy descriptorPolicy) {
        return memberAccessorFactory.buildAndCacheMemberAccessor(member,
                MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD,
                descriptorPolicy.getDomainAccessType());
    }

    public static boolean isVariable(PlanningSolutionMetaModel<?> metaModel, Class<?> declaringClass, String memberName) {
        if (!metaModel.hasEntity(declaringClass)) {
            return false;
        }
        return metaModel.entity(declaringClass).hasVariable(memberName);
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
        return !shadowVariable.supplierName().isEmpty();
    }

}
