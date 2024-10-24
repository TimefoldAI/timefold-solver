package ai.timefold.solver.core.api.score.stream;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.api.solver.SolutionManager;

import org.jspecify.annotations.NonNull;

/**
 * A constraint stream is a declaration on how to match {@link UniConstraintStream one}, {@link BiConstraintStream two}
 * or more objects.
 * Constraint streams are similar to a declaration of a JDK {@link Stream} or an SQL query,
 * but they support incremental score calculation and {@link SolutionManager#analyze(Object) score analysis}.
 * <p>
 * An object that passes through constraint streams is called a fact.
 * It's either a {@link ProblemFactCollectionProperty problem fact} or a {@link PlanningEntity planning entity}.
 * <p>
 * A constraint stream is typically created with {@link ConstraintFactory#forEach(Class)}
 * or {@link UniConstraintStream#join(UniConstraintStream, BiJoiner)} by joining another constraint stream}.
 * Constraint streams form a directed, non-cyclic graph, with multiple start nodes (which listen to fact changes)
 * and one end node per {@link Constraint} (which affect the {@link Score}).
 * <p>
 * Throughout this documentation, we will be using the following terminology:
 *
 * <dl>
 * <dt>Constraint Stream</dt>
 * <dd>A chain of different operations, originated by {@link ConstraintFactory#forEach(Class)} (or similar
 * methods) and terminated by a penalization or reward operation.</dd>
 * <dt>Operation</dt>
 * <dd>Operations (implementations of {@link ConstraintStream}) are parts of a constraint stream which mutate
 * it.
 * They may remove tuples from further evaluation, expand or contract streams. Every constraint stream has
 * a terminal operation, which is either a penalization or a reward.</dd>
 * <dt>Fact</dt>
 * <dd>Object instance entering the constraint stream.</dd>
 * <dt>Genuine Fact</dt>
 * <dd>Fact that enters the constraint stream either through a from(...) call or through a join(...) call.
 * Genuine facts are either planning entities (see {@link PlanningEntity}) or problem facts (see
 * {@link ProblemFactProperty} or {@link ProblemFactCollectionProperty}).</dd>
 * <dt>Inferred Fact</dt>
 * <dd>Fact that enters the constraint stream through a computation.
 * This would typically happen through an operation such as groupBy(...).</dd>
 * <dt>Tuple</dt>
 * <dd>A collection of facts that the constraint stream operates on, propagating them from operation to
 * operation.
 * For example, {@link UniConstraintStream} operates on single-fact tuples {A} and {@link BiConstraintStream}
 * operates on two-fact tuples {A, B}.
 * Putting facts into a tuple implies a relationship exists between these facts.</dd>
 * <dt>Match</dt>
 * <dd>Match is a tuple that reached the terminal operation of a constraint stream and is therefore either
 * penalized or rewarded.</dd>
 * <dt>Cardinality</dt>
 * <dd>The number of facts in a tuple. Uni constraint streams have a cardinality of 1, bi constraint streams
 * have a cardinality of 2, etc.</dd>
 * <dt>Conversion</dt>
 * <dd>An operation that changes the cardinality of a constraint stream.
 * This typically happens through join(...) or a groupBy(...) operations.</dd>
 * </dl>
 */
public interface ConstraintStream {

    /**
     * The {@link ConstraintFactory} that build this.
     */
    @NonNull
    ConstraintFactory getConstraintFactory();

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight for each match.
     * <p>
     * To avoid hard-coding the constraintWeight, to allow end-users to tweak it,
     * use {@link #penalizeConfigurable(String)} and a {@link ConstraintConfiguration} instead.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to the package of the {@link PlanningSolution} class.
     *
     * @deprecated Prefer {@link UniConstraintStream#penalize(Score)} and equivalent bi/tri/... overloads.
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint penalize(@NonNull String constraintName, @NonNull Score<?> constraintWeight);

    /**
     * As defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link UniConstraintStream#penalize(Score)} and equivalent bi/tri/... overloads.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint penalize(@NonNull String constraintPackage, @NonNull String constraintName, @NonNull Score<?> constraintWeight);

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} for each match.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to
     * {@link ConstraintConfiguration#constraintPackage()}.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     * @deprecated Prefer {@code penalize()} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint penalizeConfigurable(@NonNull String constraintName);

    /**
     * As defined by {@link #penalizeConfigurable(String)}.
     *
     * @deprecated Prefer {@code penalize()} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint penalizeConfigurable(@NonNull String constraintPackage, @NonNull String constraintName);

    /**
     * Positively impact the {@link Score}: add the constraintWeight for each match.
     * <p>
     * To avoid hard-coding the constraintWeight, to allow end-users to tweak it,
     * use {@link #penalizeConfigurable(String)} and a {@link ConstraintConfiguration} instead.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to the package of the {@link PlanningSolution} class.
     *
     * @deprecated Prefer {@link UniConstraintStream#reward(Score)} and equivalent bi/tri/... overloads.
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint reward(@NonNull String constraintName, @NonNull Score<?> constraintWeight);

    /**
     * As defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link UniConstraintStream#reward(Score)} and equivalent bi/tri/... overloads.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint reward(@NonNull String constraintPackage, @NonNull String constraintName, @NonNull Score<?> constraintWeight);

    /**
     * Positively impact the {@link Score}: add the {@link ConstraintWeight} for each match.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to
     * {@link ConstraintConfiguration#constraintPackage()}.
     *
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     * @deprecated Prefer {@code reward()} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint rewardConfigurable(@NonNull String constraintName);

    /**
     * As defined by {@link #rewardConfigurable(String)}.
     *
     * @deprecated Prefer {@code reward()} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint rewardConfigurable(@NonNull String constraintPackage, @NonNull String constraintName);

    /**
     * Positively or negatively impact the {@link Score} by the constraintWeight for each match.
     * <p>
     * Use {@code penalize(...)} or {@code reward(...)} instead, unless this constraint can both have positive and
     * negative weights.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to the package of the {@link PlanningSolution} class.
     *
     * @deprecated Prefer {@link UniConstraintStream#impact(Score)} and equivalent bi/tri/... overloads.
     * @param constraintName shows up in {@link ConstraintMatchTotal} during score justification
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint impact(@NonNull String constraintName, @NonNull Score<?> constraintWeight);

    /**
     * As defined by {@link #impact(String, Score)}.
     *
     * @deprecated Prefer {@link UniConstraintStream#impact(Score)} and equivalent bi/tri/... overloads.
     */
    @Deprecated(forRemoval = true)
    @NonNull
    Constraint impact(@NonNull String constraintPackage, @NonNull String constraintName, @NonNull Score<?> constraintWeight);

}
