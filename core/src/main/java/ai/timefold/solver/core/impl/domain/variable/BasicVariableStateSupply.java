package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.inverserelation.CollectionInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedBasicVariableListener;

import org.jspecify.annotations.NullMarked;

/**
 * Single source of truth for the inverse relation of a basic
 * {@link ai.timefold.solver.core.api.domain.variable.PlanningVariable}.
 * If the {@link InverseRelationShadowVariableDescriptor} is externalized,
 * there is a field on an entity holding the inverse collection and that field is used.
 * Otherwise, an internal map is used to track the inverse collection.
 *
 * @param <Solution_>
 */
@NullMarked
public interface BasicVariableStateSupply<Solution_>
        extends SourcedBasicVariableListener<Solution_, Object>, CollectionInverseVariableSupply {

    void externalize(InverseRelationShadowVariableDescriptor<Solution_> descriptor);

}
