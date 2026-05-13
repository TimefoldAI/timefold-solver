package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.basic;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Captures the state of one basic planning variable for one entity.
 * <p>
 * {@link #index} is the position of the {@link BasicVariableDescriptor} in the entity descriptor's list of basic variable
 * descriptors given by {@link EntityDescriptor#getBasicVariableDescriptorList()}.
 * <p>
 * This index also matches the convention used by {@link ChromosomeEntry#index()} for basic variable individuals.
 */
@NullMarked
record BasicValueState(Object entity, @Nullable Object value, int index) {
}
