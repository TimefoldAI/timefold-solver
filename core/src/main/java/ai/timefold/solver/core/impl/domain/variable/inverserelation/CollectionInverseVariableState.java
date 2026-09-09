package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

public interface CollectionInverseVariableState extends Supply {

    CollectionInverseVariableState EMPTY = new CollectionInverseVariableState() {
        @Override
        public <Entity_> Collection<Entity_> getInverseCollection(Object planningValue) {
            return Collections.emptyList();
        }
    };

    /**
     * If entity1.varA = x then an inverse of x is entity1.
     *
     * @param planningValue never null
     * @return never null, a {@link Collection} of entities for which the planning variable is the planningValue.
     */
    <Entity_> Collection<Entity_> getInverseCollection(Object planningValue);

}
