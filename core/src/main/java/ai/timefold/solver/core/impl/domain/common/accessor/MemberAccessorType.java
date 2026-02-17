package ai.timefold.solver.core.impl.domain.common.accessor;

public enum MemberAccessorType {
    FIELD_OR_READ_METHOD,
    FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
    FIELD_OR_GETTER_METHOD,
    FIELD_OR_GETTER_METHOD_WITH_SETTER(true),
    VOID_METHOD;

    private final boolean setterRequired;

    MemberAccessorType() {
        setterRequired = false;
    }

    MemberAccessorType(boolean setterRequired) {
        this.setterRequired = setterRequired;
    }

    public boolean isSetterRequired() {
        return setterRequired;
    }
}
