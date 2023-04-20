package ai.timefold.solver.examples.nurserostering.domain;

import ai.timefold.solver.examples.common.persistence.jackson.AbstractKeyDeserializer;

final class ShiftDateKeyDeserializer extends AbstractKeyDeserializer<ShiftDate> {

    public ShiftDateKeyDeserializer() {
        super(ShiftDate.class);
    }

    @Override
    protected ShiftDate createInstance(long id) {
        return new ShiftDate(id);
    }
}
