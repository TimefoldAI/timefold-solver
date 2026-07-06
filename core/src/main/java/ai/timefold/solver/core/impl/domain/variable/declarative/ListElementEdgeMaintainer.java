package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Maintains the fan-in edges of a declarative shadow variable that is sourced
 * from the elements of a planning list variable, such as {@code "values[].endTime"}.
 * When the list variable changes, an edge {@code element.sourceVariable -> owner.targetVariable}
 * must be added or removed for every element that entered or left the changed range.
 *
 * @param sourceVariableId the declarative shadow variable on the element (or on a fact of the element)
 * @param targetVariableId the declarative shadow variable on the list variable's entity
 * @param chainFromElementToVariableEntity the fact accessors between the element and the entity
 *        declaring the source variable; typically empty
 */
@NullMarked
public record ListElementEdgeMaintainer(VariableMetaModel<?, ?, ?> sourceVariableId,
        VariableMetaModel<?, ?, ?> targetVariableId,
        List<MemberAccessor> chainFromElementToVariableEntity) {

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public @Nullable Object findSourceEntity(Object element) {
        var current = element;
        // Avoid creation of iterators on the hot path; the chain is typically empty.
        var chainLength = chainFromElementToVariableEntity.size();
        for (var i = 0; i < chainLength; i++) {
            current = chainFromElementToVariableEntity.get(i).executeGetter(current);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

}
