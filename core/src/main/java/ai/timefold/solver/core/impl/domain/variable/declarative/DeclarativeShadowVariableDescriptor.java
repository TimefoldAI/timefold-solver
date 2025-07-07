package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
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
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

public class DeclarativeShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {
    MemberAccessor calculator;
    RootVariableSource<?, ?>[] sources;
    String[] sourcePaths;

    public DeclarativeShadowVariableDescriptor(int ordinal,
            EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        if (!descriptorPolicy.isPreviewFeatureEnabled(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)) {
            throw new IllegalStateException(
                    """
                            The member (%s) on the entity class (%s) is a declarative shadow variable, but the declarative shadow variable preview feature is disabled.
                            Maybe enable declarative shadow variables in your %s?
                            """
                            .formatted(variableMemberAccessor.getName(), entityDescriptor.getEntityClass().getName(),
                                    SolverConfig.class.getSimpleName()));
        }
        var annotation = variableMemberAccessor.getAnnotation(ShadowVariable.class);
        var methodName = annotation.supplierName();
        if (methodName.isEmpty()) {
            throw new IllegalStateException("DeclarativeShadowVariableDescriptor was created when method is empty.");
        }

        var method = ReflectionHelper.getDeclaredMethod(variableMemberAccessor.getDeclaringClass(), methodName);

        if (method == null) {
            throw new IllegalArgumentException("""
                    @%s (%s) defines a supplierMethod (%s) that does not exist inside its declaring class (%s).
                    Maybe you misspelled the supplierMethod name?"""
                    .formatted(ShadowVariable.class.getSimpleName(), variableName, methodName,
                            variableMemberAccessor.getDeclaringClass().getCanonicalName()));
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
                        MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD, ShadowSources.class,
                        descriptorPolicy.getDomainAccessType());

        sourcePaths = shadowVariableUpdater.value();
        if (sourcePaths.length == 0) {
            throw new IllegalArgumentException("""
                    Method "%s" referenced from @%s member %s has no sources.
                    A shadow variable must have at least one source (since otherwise it a constant).
                    Maybe add one source?
                    """.formatted(methodName, ShadowVariable.class.getSimpleName(), variableMemberAccessor));
        }
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return Collections.emptyList();
    }

    @Override
    public Demand<?> getProvidedDemand() {
        return null;
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        return Collections.emptyList();
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        sources = new RootVariableSource[sourcePaths.length];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = RootVariableSource.from(
                    entityDescriptor.getSolutionDescriptor().getMetaModel(),
                    entityDescriptor.getEntityClass(),
                    variableMemberAccessor.getName(),
                    sourcePaths[i],
                    entityDescriptor.getSolutionDescriptor().getMemberAccessorFactory(),
                    descriptorPolicy);
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

    public RootVariableSource<?, ?>[] getSources() {
        return sources;
    }
}
