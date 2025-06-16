package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.lang.reflect.Field;
import java.util.Objects;

import ai.timefold.solver.core.api.function.TriConsumer;

/**
 * Copies values of primitive fields from one object to another.
 * Unlike {@link ShallowCloningReferenceFieldCloner}, this does not use method handles,
 * as we prefer to avoid boxing and unboxing of primitive types.
 */
final class ShallowCloningPrimitiveFieldCloner implements ShallowCloningFieldCloner {

    private final Field field;
    private final TriConsumer<Field, Object, Object> copyOperation;

    ShallowCloningPrimitiveFieldCloner(Field field, TriConsumer<Field, Object, Object> copyOperation) {
        field.setAccessible(true);
        this.field = Objects.requireNonNull(field);
        this.copyOperation = Objects.requireNonNull(copyOperation);
    }

    public <C> void clone(C original, C clone) {
        copyOperation.accept(field, original, clone);
    }

}
