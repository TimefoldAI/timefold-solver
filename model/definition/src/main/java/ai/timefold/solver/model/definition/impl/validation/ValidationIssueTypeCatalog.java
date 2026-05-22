package ai.timefold.solver.model.definition.impl.validation;

import java.util.Collection;

import ai.timefold.solver.model.definition.api.validation.IssueType;

/**
 * Provides all supported {@link IssueType validation issue types}.
 */
public class ValidationIssueTypeCatalog {

    private final Collection<IssueType> issueTypes;

    public ValidationIssueTypeCatalog(Collection<IssueType> issueTypes) {
        this.issueTypes = issueTypes;
    }

    public Collection<IssueType> getIssueTypes() {
        return issueTypes;
    }
}
