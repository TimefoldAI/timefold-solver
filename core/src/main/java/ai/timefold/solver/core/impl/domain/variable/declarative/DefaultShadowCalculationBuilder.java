package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowCalculationBuilder;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.SingleVariableReference;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.VariableReference;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultShadowCalculationBuilder<Solution_, Entity_, Value_> implements ShadowCalculationBuilder<Entity_, Value_> {
    final DefaultShadowVariableFactory<Solution_> variableFactory;
    final SolutionDescriptor<Solution_> solutionDescriptor;
    final SupplyManager supplyManager;
    final Class<? extends Entity_> entityClass;
    final ShadowVariableCalculation<Solution_, Entity_, Value_> calculation;

    public DefaultShadowCalculationBuilder(DefaultShadowVariableFactory<Solution_> variableFactory,
            SolutionDescriptor<Solution_> solutionDescriptor, SupplyManager supplyManager,
            Class<? extends Entity_> entityClass,
            ShadowVariableCalculation<Solution_, Entity_, Value_> calculation) {
        this.variableFactory = variableFactory;
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.entityClass = entityClass;
        this.calculation = calculation;
    }

    @Override
    public <A> ShadowCalculationBuilder<Entity_, Value_> elseComputeIfHasAll(VariableReference<Entity_, A> a,
            BiFunction<Entity_, A, Value_> function) {
        try {
            return new DefaultShadowCalculationBuilder<>(variableFactory, solutionDescriptor, supplyManager, entityClass,
                    calculation.withFallback(new ShadowVariableCalculation<>(variableFactory,
                            new InnerVariableReference[] {
                                    (InnerVariableReference<?, ?, ?>) a
                            }, BiFunction.class.getMethod("apply", Object.class, Object.class),
                            function)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <A, B> ShadowCalculationBuilder<Entity_, Value_> elseComputeIfHasAll(VariableReference<Entity_, A> a,
            VariableReference<Entity_, B> b, TriFunction<Entity_, A, B, Value_> function) {
        try {
            return new DefaultShadowCalculationBuilder<>(variableFactory, solutionDescriptor, supplyManager, entityClass,
                    calculation.withFallback(new ShadowVariableCalculation<>(variableFactory,
                            new InnerVariableReference[] {
                                    (InnerVariableReference<?, ?, ?>) a,
                                    (InnerVariableReference<?, ?, ?>) b,
                            }, TriFunction.class.getMethod("apply", Object.class, Object.class, Object.class),
                            function)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <A, B, C> ShadowCalculationBuilder<Entity_, Value_> elseComputeIfHasAll(VariableReference<Entity_, A> a,
            VariableReference<Entity_, B> b, VariableReference<Entity_, C> c, QuadFunction<Entity_, A, B, C, Value_> function) {
        try {
            return new DefaultShadowCalculationBuilder<>(variableFactory, solutionDescriptor, supplyManager, entityClass,
                    calculation.withFallback(new ShadowVariableCalculation<>(variableFactory,
                            new InnerVariableReference[] {
                                    (InnerVariableReference<?, ?, ?>) a,
                                    (InnerVariableReference<?, ?, ?>) b,
                                    (InnerVariableReference<?, ?, ?>) c,
                            }, QuadFunction.class.getMethod("apply", Object.class, Object.class, Object.class, Object.class),
                            function)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <A, B, C, D> ShadowCalculationBuilder<Entity_, Value_> elseComputeIfHasAll(VariableReference<Entity_, A> a,
            VariableReference<Entity_, B> b, VariableReference<Entity_, C> c, VariableReference<Entity_, D> d,
            PentaFunction<Entity_, A, B, C, D, Value_> function) {
        try {
            return new DefaultShadowCalculationBuilder<>(variableFactory, solutionDescriptor, supplyManager, entityClass,
                    calculation.withFallback(new ShadowVariableCalculation<>(variableFactory,
                            new InnerVariableReference[] {
                                    (InnerVariableReference<?, ?, ?>) a,
                                    (InnerVariableReference<?, ?, ?>) b,
                                    (InnerVariableReference<?, ?, ?>) c,
                                    (InnerVariableReference<?, ?, ?>) d
                            }, PentaFunction.class.getMethod("apply", Object.class, Object.class, Object.class, Object.class,
                                    Object.class),
                            function)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ShadowCalculationBuilder<Entity_, Value_> elseDefaultTo(Function<Entity_, Value_> function) {
        try {
            return new DefaultShadowCalculationBuilder<>(variableFactory, solutionDescriptor, supplyManager, entityClass,
                    calculation.withFallback(new ShadowVariableCalculation<>(variableFactory,
                            new InnerVariableReference[] {}, Function.class.getMethod("apply", Object.class),
                            function)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SingleVariableReference<Entity_, Value_> as(String variableName) {
        var out = new ShadowVariableReference<>(
                solutionDescriptor,
                supplyManager,
                solutionDescriptor.getEntityDescriptorStrict(entityClass)
                        .getVariableDescriptor(variableName),
                calculation,
                variableFactory.getShadowVariableReferences(variableName),
                entityClass,
                (Class<? extends Value_>) Object.class,
                false);
        variableFactory.addShadowVariable(out);
        return out;
    }

    @Override
    public SingleVariableReference<Entity_, Value_> asNullable(String variableName) {
        var out = new ShadowVariableReference<>(
                solutionDescriptor,
                supplyManager,
                solutionDescriptor.getEntityDescriptorStrict(entityClass)
                        .getVariableDescriptor(variableName),
                calculation,
                variableFactory.getShadowVariableReferences(variableName),
                entityClass,
                (Class<? extends Value_>) Object.class,
                true);
        variableFactory.addShadowVariable(out);
        return out;
    }
}
