package ai.timefold.solver.core.impl.score.director;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * Each before/after event needs to look up a variable descriptor.
 * These operations are not cheap, they are very frequent and they often end up looking up the same descriptor.
 * These caches are used to avoid looking up the same descriptor multiple times.
 *
 * @param <Solution_>
 */
public final class VariableDescriptorCache<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private Class<?> basicVarDescritorCacheClass = null;
    private String basicVarDescriptorCacheName = null;
    private VariableDescriptor<Solution_> basicVarDescriptorCache = null;
    private Class<?> listVarDescritorCacheClass = null;
    private String listVarDescriptorCacheName = null;
    private ListVariableDescriptor<Solution_> listVarDescriptorCache = null;

    public VariableDescriptorCache(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
    }

    public VariableDescriptor<Solution_> getVariableDescriptor(Object entity, String variableName) {
        if (!Objects.equals(basicVarDescritorCacheClass, entity.getClass())
                || !Objects.equals(basicVarDescriptorCacheName, variableName)) {
            basicVarDescritorCacheClass = entity.getClass();
            basicVarDescriptorCacheName = variableName;
            basicVarDescriptorCache = solutionDescriptor.findVariableDescriptorOrFail(entity, variableName);
        }
        return basicVarDescriptorCache;
    }

    public ListVariableDescriptor<Solution_> getListVariableDescriptor(Object entity, String variableName) {
        if (!Objects.equals(listVarDescritorCacheClass, entity.getClass())
                || !Objects.equals(listVarDescriptorCacheName, variableName)) {
            listVarDescritorCacheClass = entity.getClass();
            listVarDescriptorCacheName = variableName;
            listVarDescriptorCache =
                    (ListVariableDescriptor<Solution_>) solutionDescriptor.findVariableDescriptorOrFail(entity, variableName);
        }
        return listVarDescriptorCache;
    }

}
