package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.preview.api.variable.provided.VariableReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract sealed class AbstractVariableReference<Entity_, Value_> implements VariableReference<Entity_, Value_>
        permits DefaultGroupVariableReference, DefaultSingleVariableReference {
    abstract VariableId getVariableId();

    abstract @Nullable Value_ getValue(@NonNull Object entity);

    abstract @Nullable Value_ getValueFromParent(@NonNull Object parent);

    abstract @Nullable AbstractVariableReference<Entity_, ?> getParent();

    abstract void processVariableReference(@NonNull VariableReferenceGraph graph);

    abstract void processObject(@NonNull VariableReferenceGraph graph, @NonNull Object object);

    abstract void addReferences(@NonNull DefaultShadowVariableFactory<?> factory);
}
