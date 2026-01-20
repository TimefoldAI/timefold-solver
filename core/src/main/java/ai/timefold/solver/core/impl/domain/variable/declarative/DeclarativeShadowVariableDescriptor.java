package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.Nullable;

public class DeclarativeShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {
    MemberAccessor calculator;
    RootVariableSource<?, ?>[] sources;
    String[] sourcePaths;
    String alignmentKey;
    Function<Object, Object> alignmentKeyMap;

    public DeclarativeShadowVariableDescriptor(int ordinal,
            EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        var annotation = variableMemberAccessor.getAnnotation(ShadowVariable.class);
        var methodName = annotation.supplierName();
        if (methodName.isEmpty()) {
            throw new IllegalStateException("DeclarativeShadowVariableDescriptor was created when method is empty.");
        }

        var solutionClass = entityDescriptor.getSolutionDescriptor().getSolutionClass();
        var method = ReflectionHelper.getDeclaredMethod(variableMemberAccessor.getDeclaringClass(), methodName);
        if (method == null) {
            // Retry with the solution class
            method = ReflectionHelper.getDeclaredMethod(variableMemberAccessor.getDeclaringClass(), methodName, solutionClass);
        }

        if (method == null) {
            throw new IllegalArgumentException("""
                    @%s (%s) defines a supplierMethod (%s) that does not exist inside its declaring class (%s).
                    Maybe you misspelled the supplierMethod name?
                    Maybe you have included a unallowed parameter, which does not match the expected solution class: %s"""
                    .formatted(ShadowVariable.class.getSimpleName(), variableName, methodName,
                            variableMemberAccessor.getDeclaringClass().getCanonicalName(), solutionClass.getName()));
        }

        var shadowVariableUpdater = method.getAnnotation(ShadowSources.class);
        if (shadowVariableUpdater == null) {
            throw new IllegalArgumentException("""
                    Method "%s" referenced from @%s member %s is not annotated with @%s.
                    Maybe annotate the method %s with @%s?
                    """.formatted(methodName, ShadowVariable.class.getSimpleName(), variableMemberAccessor,
                    ShadowSources.class.getSimpleName(),
                    methodName, ShadowSources.class.getSimpleName()));
        }
        this.calculator =
                entityDescriptor.getSolutionDescriptor().getMemberAccessorFactory().buildAndCacheMemberAccessor(method,
                        MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                        ShadowSources.class,
                        descriptorPolicy.getDomainAccessType());

        sourcePaths = shadowVariableUpdater.value();
        if (sourcePaths.length == 0) {
            throw new IllegalArgumentException("""
                    Method "%s" referenced from @%s member %s has no sources.
                    A shadow variable must have at least one source (since otherwise it a constant).
                    Maybe add one source?
                    """.formatted(methodName, ShadowVariable.class.getSimpleName(), variableMemberAccessor));
        }

        if (shadowVariableUpdater.alignmentKey() != null && !shadowVariableUpdater.alignmentKey().isEmpty()) {
            alignmentKey = shadowVariableUpdater.alignmentKey();
        } else {
            alignmentKey = null;
        }
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<?>> getVariableListenerClasses() {
        return Collections.emptyList();
    }

    @Override
    public Demand<?> getProvidedDemand() {
        return null;
    }

    @Override
    public Iterable<VariableListenerWithSources> buildVariableListeners(SupplyManager supplyManager) {
        return Collections.emptyList();
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        sources = new RootVariableSource[sourcePaths.length];
        var solutionMetamodel = entityDescriptor.getSolutionDescriptor().getMetaModel();
        var memberAccessorFactory = entityDescriptor.getSolutionDescriptor().getMemberAccessorFactory();

        for (int i = 0; i < sources.length; i++) {
            sources[i] = RootVariableSource.from(
                    solutionMetamodel,
                    entityDescriptor.getEntityClass(),
                    variableMemberAccessor.getName(),
                    sourcePaths[i],
                    memberAccessorFactory,
                    descriptorPolicy);
        }

        var alignmentKeyMember = getAlignmentKeyMemberForEntityProperty(solutionMetamodel,
                entityDescriptor.getEntityClass(),
                calculator,
                variableName,
                alignmentKey);
        if (alignmentKeyMember != null) {
            alignmentKeyMap = memberAccessorFactory.buildAndCacheMemberAccessor(alignmentKeyMember,
                    MemberAccessorFactory.MemberAccessorType.FIELD_OR_GETTER_METHOD, ShadowSources.class,
                    descriptorPolicy.getDomainAccessType())::executeGetter;
        } else {
            alignmentKeyMap = null;
        }
    }

    protected static Member getAlignmentKeyMemberForEntityProperty(
            PlanningSolutionMetaModel<?> solutionMetamodel,
            Class<?> entityClass,
            MemberAccessor calculator,
            String variableName,
            String propertyName) {
        if (propertyName == null) {
            return null;
        }
        Member member = RootVariableSource.getMember(entityClass,
                propertyName, entityClass,
                propertyName);
        if (RootVariableSource.isVariable(solutionMetamodel, member.getDeclaringClass(),
                member.getName())) {
            throw new IllegalArgumentException(
                    """
                            The @%s-annotated supplier method (%s) for variable (%s) on class (%s) uses a alignmentKey (%s) that is a variable.
                            A alignmentKey must be a problem fact and cannot change during solving.
                            """
                            .formatted(ShadowSources.class.getSimpleName(),
                                    calculator.getName(),
                                    variableName,
                                    entityClass.getCanonicalName(),
                                    propertyName));
        }
        return member;
    }

    void visitAllReferencedEntities(Object entity, BiConsumer<RootVariableSource<?, ?>, Object> visitor) {
        for (var source : sources) {
            source.visitAllReferencedEntities(entity, referencedEntity -> visitor.accept(source, referencedEntity));
        }
    }

    @Override
    public boolean isListVariableSource() {
        return false;
    }

    public MemberAccessor getMemberAccessor() {
        return variableMemberAccessor;
    }

    public MemberAccessor getCalculator() {
        return calculator;
    }

    public Function<Object, Object> getAlignmentKeyMap() {
        return alignmentKeyMap;
    }

    public @Nullable String getAlignmentKeyName() {
        return alignmentKey;
    }

    public RootVariableSource<?, ?>[] getSources() {
        return sources;
    }
}
