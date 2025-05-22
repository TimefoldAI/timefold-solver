package ai.timefold.solver.quarkus.testdomain.chained;

public class TestdataChainedQuarkusAnchor implements TestdataChainedQuarkusObject {

    private TestdataChainedQuarkusEntity next;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public TestdataChainedQuarkusEntity getNext() {
        return next;
    }

    @Override
    public void setNext(TestdataChainedQuarkusEntity next) {
        this.next = next;
    }

}
