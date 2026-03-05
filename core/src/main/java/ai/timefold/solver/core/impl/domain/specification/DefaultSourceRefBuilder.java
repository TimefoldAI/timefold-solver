package ai.timefold.solver.core.impl.domain.specification;

import ai.timefold.solver.core.api.domain.specification.SourceRefBuilder;

final class DefaultSourceRefBuilder implements SourceRefBuilder {

    private String sourceVariableName;

    @Override
    public SourceRefBuilder sourceVariable(String variableName) {
        this.sourceVariableName = variableName;
        return this;
    }

    String getSourceVariableName() {
        if (sourceVariableName == null) {
            throw new IllegalStateException("Source variable name is required. Call sourceVariable() before building.");
        }
        return sourceVariableName;
    }
}
