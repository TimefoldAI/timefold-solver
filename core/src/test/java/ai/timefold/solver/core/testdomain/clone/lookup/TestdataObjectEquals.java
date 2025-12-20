package ai.timefold.solver.core.testdomain.clone.lookup;

public class TestdataObjectEquals {

    private final int id;

    public TestdataObjectEquals(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof TestdataObjectEquals other &&
                this.id == other.id;
    }

}
