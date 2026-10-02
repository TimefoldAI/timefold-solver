package ai.timefold.solver.core.impl.domain.variable.violation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * Serves for detecting shadow variables' corruption. When a snapshot is created, it records the state of all shadow variables
 * of all entities. The {@link #createShadowVariablesViolationMessage} method takes a look at the shadow variables again,
 * compares their state with the recorded one and describes the difference in a violation message.
 */
public final class ShadowVariablesAssert {

    /**
     * Deterministic ordering of variable descriptors.
     */
    private static final Comparator<ShadowVariableDescriptor<?>> SHADOW_VARIABLE_DESCRIPTOR_COMPARATOR =
            Comparator
                    .<ShadowVariableDescriptor<?>> comparingInt(
                            variableDescriptor -> variableDescriptor.getEntityDescriptor().getOrdinal())
                    .thenComparingInt(VariableDescriptor::getOrdinal);

    private final List<ShadowVariableSnapshot> shadowVariableSnapshots;

    private ShadowVariablesAssert(List<ShadowVariableSnapshot> shadowVariableSnapshots) {
        this.shadowVariableSnapshots = shadowVariableSnapshots;
    }

    public static <Solution_> ShadowVariablesAssert takeSnapshot(
            SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ workingSolution) {
        List<ShadowVariableSnapshot> shadowVariableSnapshots = new ArrayList<>();
        solutionDescriptor.visitAllEntities(workingSolution,
                entity -> solutionDescriptor.findEntityDescriptorOrFail(entity.getClass())
                        .getShadowVariableDescriptors().stream()
                        .map(shadowVariableDescriptor -> ShadowVariableSnapshot.of(shadowVariableDescriptor, entity))
                        .forEach(shadowVariableSnapshots::add));
        return new ShadowVariablesAssert(shadowVariableSnapshots);
    }

    public static <Solution_> void resetShadowVariables(
            SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ workingSolution) {
        solutionDescriptor.visitAllEntities(workingSolution,
                entity -> solutionDescriptor.findEntityDescriptorOrFail(entity.getClass())
                        .getShadowVariableDescriptors()
                        .forEach(descriptor -> descriptor.setValue(entity, null)));
    }

    /**
     * Takes a look at the shadow variables of all entities and compares them against the recorded state. Every difference
     * is added to the violation message. The first N differences up to the {@code violationDisplayLimit} are displayed
     * in detail; the number of violations exceeding the display limit is reported at the end. The limit applies per each
     * shadow variable descriptor.
     * <p>
     * This method should be called after a forceful update of all shadow variables.
     *
     * @param violationDisplayLimit maximum number of violations reported per shadow variable descriptor
     * @return description of the violations or {@code null} if there are none
     */
    public String createShadowVariablesViolationMessage(long violationDisplayLimit) {
        var violationListMap = collectViolations();
        if (violationListMap.isEmpty()) {
            return null;
        }
        return format(violationListMap, violationDisplayLimit);
    }

    private Map<ShadowVariableDescriptor<?>, List<String>> collectViolations() {
        var violationListMap = new TreeMap<ShadowVariableDescriptor<?>, List<String>>(SHADOW_VARIABLE_DESCRIPTOR_COMPARATOR);
        for (var shadowVariableSnapshot : shadowVariableSnapshots) {
            shadowVariableSnapshot.validate(violationMessage -> violationListMap
                    .computeIfAbsent(shadowVariableSnapshot.getShadowVariableDescriptor(), k -> new ArrayList<>())
                    .add(violationMessage.indent(4)));
        }
        return violationListMap;
    }

    private String format(Map<ShadowVariableDescriptor<?>, List<String>> violationListMap, long violationDisplayLimit) {
        var message = new StringBuilder();
        violationListMap.forEach((shadowVariableDescriptor, violationList) -> {
            violationList.stream().limit(violationDisplayLimit).forEach(message::append);
            if (violationList.size() >= violationDisplayLimit) {
                message.append("  ... ").append(violationList.size() - violationDisplayLimit)
                        .append(" more\n");
            }
        });
        return message.toString();
    }
}
