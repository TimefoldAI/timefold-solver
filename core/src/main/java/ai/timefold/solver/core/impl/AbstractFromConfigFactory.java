package ai.timefold.solver.core.impl;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;

public abstract class AbstractFromConfigFactory<Solution_, Config_ extends AbstractConfig<Config_>> {

    protected final Config_ config;

    protected AbstractFromConfigFactory(Config_ config) {
        this.config = config;
    }

    public static <Solution_> EntitySelectorConfig getDefaultEntitySelectorConfigForEntity(
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor) {
        var entityClass = entityDescriptor.getEntityClass();
        var entitySelectorConfig = new EntitySelectorConfig()
                .withId(entityClass.getName())
                .withEntityClass(entityClass);
        return deduceEntitySortManner(configPolicy, entityDescriptor, entitySelectorConfig);
    }

    public static <Solution_> EntitySelectorConfig deduceEntitySortManner(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, EntitySelectorConfig entitySelectorConfig) {
        if (configPolicy.getEntitySorterManner() != null
                && EntitySelectorConfig.hasSorter(configPolicy.getEntitySorterManner(), entityDescriptor)) {
            entitySelectorConfig = entitySelectorConfig.withCacheType(SelectionCacheType.PHASE)
                    .withSelectionOrder(SelectionOrder.SORTED)
                    .withSorterManner(configPolicy.getEntitySorterManner());
        }
        return entitySelectorConfig;
    }

    protected EntityDescriptor<Solution_> deduceEntityDescriptor(HeuristicConfigPolicy<Solution_> configPolicy,
            Class<?> entityClass) {
        var solutionDescriptor = configPolicy.getSolutionDescriptor();
        return entityClass == null
                ? getTheOnlyEntityDescriptor(solutionDescriptor)
                : getEntityDescriptorForClass(solutionDescriptor, entityClass);
    }

    private EntityDescriptor<Solution_> getEntityDescriptorForClass(SolutionDescriptor<Solution_> solutionDescriptor,
            Class<?> entityClass) {
        var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(entityClass);
        if (entityDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The config (%s) has an entityClass (%s) that is not a known planning entity.
                            Check your solver configuration. If that class (%s) is not in the entityClassSet (%s), check your @%s implementation's annotated methods too."""
                            .formatted(config, entityClass, entityClass.getSimpleName(), solutionDescriptor.getEntityClassSet(),
                                    PlanningSolution.class.getSimpleName()));
        }
        return entityDescriptor;
    }

    protected EntityDescriptor<Solution_> getTheOnlyEntityDescriptor(SolutionDescriptor<Solution_> solutionDescriptor) {
        var entityDescriptors = solutionDescriptor.getGenuineEntityDescriptors();
        if (entityDescriptors.size() != 1) {
            throw new IllegalArgumentException(
                    "The config (%s) has no entityClass configured and because there are multiple in the entityClassSet (%s), it cannot be deduced automatically."
                            .formatted(config, solutionDescriptor.getEntityClassSet()));
        }
        return entityDescriptors.iterator().next();
    }

    protected EntityDescriptor<Solution_>
            getTheOnlyEntityDescriptorWithBasicVariables(SolutionDescriptor<Solution_> solutionDescriptor) {
        var entityDescriptors = solutionDescriptor.getGenuineEntityDescriptors()
                .stream()
                .filter(EntityDescriptor::hasAnyGenuineBasicVariables)
                .toList();
        if (entityDescriptors.size() != 1) {
            throw new IllegalArgumentException(
                    "The config (%s) has no entityClass configured and because there are multiple in the entityClassSet (%s) defining basic variables, it cannot be deduced automatically."
                            .formatted(config, solutionDescriptor.getEntityClassSet()));
        }
        return entityDescriptors.iterator().next();
    }

    protected EntityDescriptor<Solution_>
            getTheOnlyEntityDescriptorWithListVariable(SolutionDescriptor<Solution_> solutionDescriptor) {
        var entityDescriptors = solutionDescriptor.getGenuineEntityDescriptors()
                .stream()
                .filter(EntityDescriptor::hasAnyGenuineListVariables)
                .toList();
        if (entityDescriptors.size() != 1) {
            throw new IllegalArgumentException(
                    "Impossible state: the config (%s) has no entityClass configured and because there are multiple in the entityClassSet (%s), it cannot be deduced automatically."
                            .formatted(config, solutionDescriptor.getEntityClassSet()));
        }
        return entityDescriptors.iterator().next();
    }

    protected GenuineVariableDescriptor<Solution_> deduceGenuineVariableDescriptor(EntityDescriptor<Solution_> entityDescriptor,
            String variableName) {
        return variableName == null
                ? getTheOnlyVariableDescriptor(entityDescriptor)
                : getVariableDescriptorForName(entityDescriptor, variableName);
    }

    protected GenuineVariableDescriptor<Solution_> getVariableDescriptorForName(EntityDescriptor<Solution_> entityDescriptor,
            String variableName) {
        var variableDescriptor = entityDescriptor.getGenuineVariableDescriptor(variableName);
        if (variableDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The config (%s) has a variableName (%s) which is not a valid planning variable on entityClass (%s).
                            %s""".formatted(config, variableName, entityDescriptor.getEntityClass(),
                            entityDescriptor.buildInvalidVariableNameExceptionMessage(variableName)));
        }
        return variableDescriptor;
    }

    protected GenuineVariableDescriptor<Solution_> getTheOnlyVariableDescriptor(EntityDescriptor<Solution_> entityDescriptor) {
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();
        if (variableDescriptorList.size() != 1) {
            throw new IllegalArgumentException(
                    "The config (%s) has no configured variableName for entityClass (%s) and because there are multiple variableNames (%s), it cannot be deduced automatically."
                            .formatted(config, entityDescriptor.getEntityClass(),
                                    entityDescriptor.getGenuineVariableNameSet()));
        }
        return variableDescriptorList.iterator().next();
    }

    protected List<GenuineVariableDescriptor<Solution_>> deduceVariableDescriptorList(
            EntityDescriptor<Solution_> entityDescriptor, List<String> variableNameIncludeList) {
        Objects.requireNonNull(entityDescriptor);
        var variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();
        if (variableNameIncludeList == null) {
            return variableDescriptorList;
        }

        return variableNameIncludeList.stream()
                .map(variableNameInclude -> variableDescriptorList.stream()
                        .filter(variableDescriptor -> variableDescriptor.getVariableName().equals(variableNameInclude))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "The config (%s) has a variableNameInclude (%s) which does not exist in the entity (%s)'s variableDescriptorList (%s)."
                                        .formatted(config, variableNameInclude, entityDescriptor.getEntityClass(),
                                                variableDescriptorList))))
                .toList();
    }

    protected List<GenuineVariableDescriptor<Solution_>> deduceBasicVariableDescriptorList(
            EntityDescriptor<Solution_> entityDescriptor, List<String> variableNameIncludeList) {
        Objects.requireNonNull(entityDescriptor);
        var variableDescriptorList = entityDescriptor.getGenuineBasicVariableDescriptorList();
        if (variableNameIncludeList == null) {
            return variableDescriptorList;
        }

        return variableNameIncludeList.stream()
                .map(variableNameInclude -> variableDescriptorList.stream()
                        .filter(variableDescriptor -> variableDescriptor.getVariableName().equals(variableNameInclude))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "The config (%s) has a variableNameInclude (%s) which does not exist in the entity (%s)'s variableDescriptorList (%s)."
                                        .formatted(config, variableNameInclude, entityDescriptor.getEntityClass(),
                                                variableDescriptorList))))
                .toList();
    }
}
