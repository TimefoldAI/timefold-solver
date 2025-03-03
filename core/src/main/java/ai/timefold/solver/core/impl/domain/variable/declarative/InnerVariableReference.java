package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.preview.api.domain.variable.declarative.VariableReference;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface InnerVariableReference<Solution_, Entity_, Value_> extends VariableReference<Entity_, Value_> {
    VariableId getVariableId();

    Value_ getValue(Object entity);

    @Nullable
    Object getSingleValueFromSingleParent(Object parent);

    @Nullable
    InnerVariableReference<Solution_, Entity_, ?> getParent();

    void processVariableReference(VariableReferenceGraph<Solution_> graph);

    void processObject(VariableReferenceGraph<Solution_> graph, Object object);

    void addReferences(DefaultShadowVariableFactory<Solution_> factory);

    boolean isNullValueValid();
}
