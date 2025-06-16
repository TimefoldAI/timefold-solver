package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.lang.reflect.Field;

sealed interface ShallowCloningFieldCloner
        permits ShallowCloningPrimitiveFieldCloner, ShallowCloningReferenceFieldCloner {

    static ShallowCloningFieldCloner of(Field field) {
        Class<?> fieldType = field.getType();
        if (fieldType == boolean.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyBoolean);
        } else if (fieldType == byte.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyByte);
        } else if (fieldType == char.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyChar);
        } else if (fieldType == short.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyShort);
        } else if (fieldType == int.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyInt);
        } else if (fieldType == long.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyLong);
        } else if (fieldType == float.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyFloat);
        } else if (fieldType == double.class) {
            return new ShallowCloningPrimitiveFieldCloner(field, FieldCloningUtils::copyDouble);
        } else {
            return new ShallowCloningReferenceFieldCloner(field, FieldCloningUtils::copyObject);
        }

    }

    <C> void clone(C original, C clone);

}
