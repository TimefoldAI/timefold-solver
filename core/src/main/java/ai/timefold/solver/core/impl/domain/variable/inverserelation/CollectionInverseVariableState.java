package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.Collection;

import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

@FunctionalInterface
public interface CollectionInverseVariableState extends Supply {

    /**
     * If entity1.varA = x then an inverse of x is entity1.
     *
     * @param planningValue never null
     * @return never null, a {@link Collection} of entities for which the planning variable is the planningValue.
     */
    <Entity_> Collection<Entity_> getInverseCollection(Object planningValue);

}
