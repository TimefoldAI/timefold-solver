package ai.timefold.solver.core.impl.domain.specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification;
import ai.timefold.solver.core.api.domain.specification.CloningSpecificationBuilder;
import ai.timefold.solver.core.api.domain.specification.ConstraintWeightSpecification;
import ai.timefold.solver.core.api.domain.specification.EntityCollectionSpecification;
import ai.timefold.solver.core.api.domain.specification.EntitySpecification;
import ai.timefold.solver.core.api.domain.specification.EntitySpecificationBuilder;
import ai.timefold.solver.core.api.domain.specification.FactSpecification;
import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.domain.specification.ScoreSpecification;
import ai.timefold.solver.core.api.domain.specification.SolutionSpecificationBuilder;
import ai.timefold.solver.core.api.domain.specification.ValueRangeSpecification;
import ai.timefold.solver.core.api.score.Score;

public final class DefaultSolutionSpecificationBuilder<S> implements SolutionSpecificationBuilder<S> {

    private final Class<S> solutionClass;
    private ScoreSpecification<S> score;
    private final List<FactSpecification<S>> facts = new ArrayList<>();
    private final List<EntityCollectionSpecification<S>> entityCollections = new ArrayList<>();
    private final List<ValueRangeSpecification<S>> valueRanges = new ArrayList<>();
    private final List<EntitySpecification<S>> entities = new ArrayList<>();
    private CloningSpecification<S> cloning;
    private ConstraintWeightSpecification<S> constraintWeights;

    public DefaultSolutionSpecificationBuilder(Class<S> solutionClass) {
        this.solutionClass = solutionClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> SolutionSpecificationBuilder<S> score(
            Class<Score_> scoreType, Function<S, Score_> getter, BiConsumer<S, Score_> setter) {
        this.score = new ScoreSpecification<>(scoreType,
                (Function<S, ?>) getter,
                (BiConsumer<S, Object>) (BiConsumer<S, ?>) setter);
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> problemFact(String name, Function<S, ?> getter) {
        facts.add(new FactSpecification<>(name, getter, false));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> problemFact(String name, Function<S, ?> getter, BiConsumer<S, Object> setter) {
        facts.add(new FactSpecification<>(name, getter, setter, false, null));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> problemFacts(String name, Function<S, ? extends Collection<?>> getter) {
        facts.add(new FactSpecification<>(name, getter, true));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SolutionSpecificationBuilder<S> problemFacts(String name, Function<S, ? extends Collection<?>> getter,
            BiConsumer<S, Object> setter) {
        facts.add(new FactSpecification<>(name, getter, setter, true, null));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> entityCollection(String name, Function<S, ? extends Collection<?>> getter) {
        entityCollections.add(new EntityCollectionSpecification<>(name, getter));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> entityCollection(String name, Function<S, ? extends Collection<?>> getter,
            BiConsumer<S, Object> setter) {
        entityCollections.add(new EntityCollectionSpecification<>(name, getter, setter, false));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> valueRange(String id, Function<S, ?> getter) {
        valueRanges.add(new ValueRangeSpecification<>(id, getter, solutionClass, false));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> valueRange(Function<S, ?> getter) {
        valueRanges.add(new ValueRangeSpecification<>(null, getter, solutionClass, false));
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> constraintWeightOverrides(Function<S, ConstraintWeightOverrides<?>> getter) {
        this.constraintWeights = new ConstraintWeightSpecification<>(getter);
        return this;
    }

    @Override
    public <E> SolutionSpecificationBuilder<S> entity(Class<E> entityClass,
            Consumer<EntitySpecificationBuilder<S, E>> config) {
        var builder = new DefaultEntitySpecificationBuilder<S, E>(entityClass);
        config.accept(builder);
        entities.add(builder.build());
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> cloning(Consumer<CloningSpecificationBuilder<S>> config) {
        var builder = new DefaultCloningSpecificationBuilder<S>();
        config.accept(builder);
        this.cloning = builder.build();
        return this;
    }

    @Override
    public SolutionSpecificationBuilder<S> solutionCloner(SolutionCloner<S> cloner) {
        this.cloning = new CloningSpecification<>(null, null, null, null, null, cloner);
        return this;
    }

    @Override
    public PlanningSpecification<S> build() {
        if (score == null) {
            throw new IllegalStateException("Score specification is required. Call score() before build().");
        }
        if (entities.isEmpty()) {
            throw new IllegalStateException("At least one entity is required. Call entity() before build().");
        }
        if (entityCollections.isEmpty()) {
            throw new IllegalStateException(
                    "At least one entity collection is required. Call entityCollection() before build().");
        }
        return new PlanningSpecification<>(
                solutionClass,
                score,
                List.copyOf(facts),
                List.copyOf(entityCollections),
                List.copyOf(valueRanges),
                List.copyOf(entities),
                cloning,
                constraintWeights);
    }
}
