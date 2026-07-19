package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Single source of truth for all information about elements inside {@link PlanningListVariable list variables}.
 * Shadow variables can be connected to this class to save on iteration costs
 * that would've been incurred otherwise if each shadow variable were updated independently.
 * This way, a single component updates all such shadow variables,
 * and therefore only a single iteration to update all the information.
 *
 * <p>
 * If a particular shadow variable is externalized,
 * it means that there is a field on an entity holding the value of the shadow variable.
 * In this case, we will attempt to use that value.
 * Otherwise, we will keep an internal track of all the possible shadow variables
 * ({@link IndexShadowVariable}, {@link InverseRelationShadowVariable},
 * {@link PreviousElementShadowVariable}, {@link NextElementShadowVariable}),
 * and use values from this internal representation.
 * 
 * @param <Solution_>
 * @see ListVariableState The logic of switching between internal and externalized shadow variables.
 * @see ExternalizedListVariableStateSupply The external representation of these shadow variables,
 *      which doesn't care whether the variable is internal or externalized.
 */
@NullMarked
public interface ListVariableStateSupply<Solution_, Entity_, Element_>
        extends ListVariableChangeHandler<Solution_> {

    /**
     * Called when the entire working solution changes. In this event, the other before..()/after...() methods will not
     * be called.
     * At this point, implementations should clear state, if any.
     */
    @Override
    default void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        // No need to do anything for stateless implementations.
    }

    /**
     * Called before this {@link ListVariableStateSupply} is thrown away and not used anymore.
     */
    @Override
    default void close() {
        // No need to do anything for stateless implementations.
    }

    void externalize(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalize(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalize(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    void externalize(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor);

    /**
     * Get {@code planningValue}'s index in the {@link PlanningListVariable list variable} it is an element of.
     *
     * @param planningValue never null
     * @return {@code planningValue}'s index in the list variable it is an element of
     * @throws IllegalStateException if the value is unassigned
     */
    int getIndexOrFail(Object planningValue);

    /**
     * Get {@code planningValue}'s index in the {@link PlanningListVariable list variable} it is an element of.
     *
     * @param planningValue never null
     * @param defaultValue the value to return if {@code planningValue} is unassigned
     * @return {@code planningValue}'s index in the list variable it is an element of or {@code defaultValue} if the value is
     *         unassigned
     */
    int getIndexOrElse(Object planningValue, int defaultValue);

    /**
     * If entity1.varA = x then the inverse of x is entity1.
     *
     * @param planningValue never null
     * @return sometimes null, an entity for which the planning variable is the planningValue.
     */
    @Nullable
    Object getInverseSingleton(Object planningValue);

    ListVariableDescriptor<Solution_> getSourceVariableDescriptor();

    /**
     *
     * @param element never null
     * @return true if the element is contained in a list variable of any entity.
     */
    boolean isAssigned(Element_ element);

    /**
     *
     * @param element never null
     * @return true if the element is in a pinned part of a list variable of any entity
     */
    boolean isPinned(Element_ element);

    /**
     *
     * @param value never null
     * @return never null
     */
    ElementPosition getElementPosition(Element_ value);

    /**
     * Consider calling this before {@link #isAssigned(Object)} to eliminate some map accesses.
     * If unassigned count is 0, {@link #isAssigned(Object)} is guaranteed to return true.
     *
     * @return number of elements for which {@link #isAssigned(Object)} would return false.
     */
    int getUnassignedCount();

    /**
     *
     * @param element never null
     * @return null if the element is the first element in the list
     */
    @Nullable
    Element_ getPreviousElement(Element_ element);

    /**
     *
     * @param element never null
     * @return null if the element is the last element in the list
     */
    @Nullable
    Element_ getNextElement(Element_ element);

}
