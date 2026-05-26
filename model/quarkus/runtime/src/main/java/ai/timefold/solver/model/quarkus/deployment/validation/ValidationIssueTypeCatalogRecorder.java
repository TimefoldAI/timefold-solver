package ai.timefold.solver.model.quarkus.deployment.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.model.definition.api.validation.IssueType;
import ai.timefold.solver.model.definition.impl.validation.ValidationIssueTypeCatalog;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ValidationIssueTypeCatalogRecorder {

    public RuntimeValue<ValidationIssueTypeCatalog> createCatalog(List<IssueType> issueTypes) {
        Collection<IssueType> unmodifiable = Collections.unmodifiableList(issueTypes);
        return new RuntimeValue<>(new ValidationIssueTypeCatalog(unmodifiable));
    }
}
