package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

public interface VariableDescriptorAwareScoreDirector<Solution_>
        extends ScoreDirector<Solution_> {

    SolutionDescriptor<Solution_> getSolutionDescriptor();

    // ************************************************************************
    // Basic variable
    // ************************************************************************

    void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity);

    void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity);

    void changeVariableFacade(VariableDescriptor<Solution_> variableDescriptor, Object entity, Object newValue);

    // ************************************************************************
    // List variable
    // ************************************************************************

    /**
     * Call this for each element that will be assigned (added to a list variable of one entity without being removed
     * from a list variable of another entity).
     *
     * @param variableDescriptor the list variable descriptor
     * @param element the assigned element
     */
    void beforeListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    /**
     * Call this for each element that was assigned (added to a list variable of one entity without being removed
     * from a list variable of another entity).
     *
     * @param variableDescriptor the list variable descriptor
     * @param element the assigned element
     */
    void afterListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    /**
     * Call this for each element that will be unassigned (removed from a list variable of one entity without being added
     * to a list variable of another entity).
     *
     * @param variableDescriptor the list variable descriptor
     * @param element the unassigned element
     */
    void beforeListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    /**
     * Call this for each element that was unassigned (removed from a list variable of one entity without being added
     * to a list variable of another entity).
     *
     * @param variableDescriptor the list variable descriptor
     * @param element the unassigned element
     */
    void afterListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    /**
     * Notify the score director before a list variable changes.
     * <p>
     * The list variable change includes:
     * <ul>
     * <li>Changing position (index) of one or more elements.</li>
     * <li>Removing one or more elements from the list variable.</li>
     * <li>Adding one or more elements to the list variable.</li>
     * <li>Any mix of the above.</li>
     * </ul>
     * For the sake of variable listeners' efficiency, the change notification requires an index range that contains elements
     * affected by the change. The range starts at {@code fromIndex} (inclusive) and ends at {@code toIndex} (exclusive).
     * <p>
     * The range has to comply with the following contract:
     * <ol>
     * <li>{@code fromIndex} must be greater than or equal to 0; {@code toIndex} must be less than or equal to the list variable
     * size.</li>
     * <li>{@code toIndex} must be greater than or equal to {@code fromIndex}.</li>
     * <li>The range must contain all elements that are going to be changed.</li>
     * <li>The range is allowed to contain elements that are not going to be changed.</li>
     * <li>The range may be empty ({@code fromIndex} equals {@code toIndex}) if none of the existing list variable elements
     * are going to be changed.</li>
     * </ol>
     * <p>
     * {@link #beforeListVariableElementUnassigned(ListVariableDescriptor, Object)} must be called for each element
     * that will be unassigned (removed from a list variable of one entity without being added
     * to a list variable of another entity).
     *
     * @param variableDescriptor descriptor of the list variable being changed
     * @param entity the entity owning the list variable being changed
     * @param fromIndex low endpoint (inclusive) of the changed range
     * @param toIndex high endpoint (exclusive) of the changed range
     */
    void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex);

    /**
     * Notify the score director after a list variable changes.
     * <p>
     * The list variable change includes:
     * <ul>
     * <li>Changing position (index) of one or more elements.</li>
     * <li>Removing one or more elements from the list variable.</li>
     * <li>Adding one or more elements to the list variable.</li>
     * <li>Any mix of the above.</li>
     * </ul>
     * For the sake of variable listeners' efficiency, the change notification requires an index range that contains elements
     * affected by the change. The range starts at {@code fromIndex} (inclusive) and ends at {@code toIndex} (exclusive).
     * <p>
     * The range has to comply with the following contract:
     * <ol>
     * <li>{@code fromIndex} must be greater than or equal to 0; {@code toIndex} must be less than or equal to the list variable
     * size.</li>
     * <li>{@code toIndex} must be greater than or equal to {@code fromIndex}.</li>
     * <li>The range must contain all elements that have changed.</li>
     * <li>The range is allowed to contain elements that have not changed.</li>
     * <li>The range may be empty ({@code fromIndex} equals {@code toIndex}) if none of the existing list variable elements
     * have changed.</li>
     * </ol>
     * <p>
     * {@link #afterListVariableElementUnassigned(ListVariableDescriptor, Object)} must be called for each element
     * that was unassigned (removed from a list variable of one entity without being added
     * to a list variable of another entity).
     *
     * @param variableDescriptor descriptor of the list variable being changed
     * @param entity the entity owning the list variable being changed
     * @param fromIndex low endpoint (inclusive) of the changed range
     * @param toIndex high endpoint (exclusive) of the changed range
     */
    void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex);

    // ************************************************************************
    // Overloads without known variable descriptors
    // ************************************************************************

    VariableDescriptorCache<Solution_> getVariableDescriptorCache();

    @Override
    default void beforeVariableChanged(Object entity, String variableName) {
        beforeVariableChanged(getVariableDescriptorCache().getVariableDescriptor(entity, variableName), entity);
    }

    @Override
    default void afterVariableChanged(Object entity, String variableName) {
        afterVariableChanged(getVariableDescriptorCache().getVariableDescriptor(entity, variableName), entity);
    }

    @Override
    default void beforeListVariableElementAssigned(Object entity, String variableName, Object element) {
        beforeListVariableElementAssigned(getVariableDescriptorCache().getListVariableDescriptor(entity, variableName),
                element);
    }

    @Override
    default void afterListVariableElementAssigned(Object entity, String variableName, Object element) {
        afterListVariableElementAssigned(getVariableDescriptorCache().getListVariableDescriptor(entity, variableName), element);
    }

    @Override
    default void beforeListVariableElementUnassigned(Object entity, String variableName, Object element) {
        beforeListVariableElementUnassigned(getVariableDescriptorCache().getListVariableDescriptor(entity, variableName),
                element);
    }

    @Override
    default void afterListVariableElementUnassigned(Object entity, String variableName, Object element) {
        afterListVariableElementUnassigned(getVariableDescriptorCache().getListVariableDescriptor(entity, variableName),
                element);
    }

    @Override
    default void beforeListVariableChanged(Object entity, String variableName, int fromIndex, int toIndex) {
        beforeListVariableChanged(getVariableDescriptorCache().getListVariableDescriptor(entity, variableName), entity,
                fromIndex,
                toIndex);
    }

    @Override
    default void afterListVariableChanged(Object entity, String variableName, int fromIndex, int toIndex) {
        afterListVariableChanged(getVariableDescriptorCache().getListVariableDescriptor(entity, variableName), entity,
                fromIndex,
                toIndex);
    }

}
