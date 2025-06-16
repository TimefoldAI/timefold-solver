package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.lang.reflect.Field;
import java.util.Objects;

import ai.timefold.solver.core.api.function.TriConsumer;
import ai.timefold.solver.core.impl.domain.common.accessor.FieldHandle;

/**
 * Copies the reference value from a field of one object to another.
 * Unlike {@link ShallowCloningPrimitiveFieldCloner},
 * this uses method handles for improved performance of field access.
 */
final class ShallowCloningReferenceFieldCloner implements ShallowCloningFieldCloner {

    private final FieldHandle fieldHandle;
    private final TriConsumer<FieldHandle, Object, Object> copyOperation;

    ShallowCloningReferenceFieldCloner(Field field, TriConsumer<FieldHandle, Object, Object> copyOperation) {
        this.fieldHandle = FieldHandle.of(field);
        this.copyOperation = Objects.requireNonNull(copyOperation);
    }

    public <C> void clone(C original, C clone) {
        copyOperation.accept(fieldHandle, original, clone);
    }

}
