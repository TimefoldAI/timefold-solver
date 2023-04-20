package ai.timefold.solver.examples.taskassigning.domain;

import ai.timefold.solver.examples.common.persistence.jackson.AbstractKeyDeserializer;

final class CustomerKeyDeserializer extends AbstractKeyDeserializer<Customer> {

    public CustomerKeyDeserializer() {
        super(Customer.class);
    }

    @Override
    protected Customer createInstance(long id) {
        return new Customer(id);
    }
}
