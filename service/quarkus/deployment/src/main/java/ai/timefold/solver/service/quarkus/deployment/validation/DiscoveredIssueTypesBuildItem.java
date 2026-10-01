package ai.timefold.solver.service.quarkus.deployment.validation;

import java.util.List;

import ai.timefold.solver.service.definition.api.validation.IssueType;

import io.quarkus.builder.item.SimpleBuildItem;

final class DiscoveredIssueTypesBuildItem extends SimpleBuildItem {

    private final List<IssueType> issueTypes;

    public DiscoveredIssueTypesBuildItem(List<IssueType> issueTypes) {
        this.issueTypes = List.copyOf(issueTypes);
    }

    public List<IssueType> getIssueTypes() {
        return issueTypes;
    }
}
