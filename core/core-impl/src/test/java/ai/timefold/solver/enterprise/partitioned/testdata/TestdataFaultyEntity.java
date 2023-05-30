package ai.timefold.solver.enterprise.partitioned.testdata;

import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestdataFaultyEntity extends TestdataEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestdataFaultyEntity.class);

    public TestdataFaultyEntity() {
    }

    public TestdataFaultyEntity(String code) {
        super(code);
    }

    @Override
    public void setValue(TestdataValue value) {
        super.setValue(value);
        if (Thread.currentThread().getName().matches("Timefold-\\d+-PartThread-\\d+")) {
            LOGGER.info("Throwing exception on a partition thread.");
            throw new TestException();
        }
    }

    public static class TestException extends RuntimeException {

        public TestException() {
            super("Unexpected solver failure.");
        }
    }
}
