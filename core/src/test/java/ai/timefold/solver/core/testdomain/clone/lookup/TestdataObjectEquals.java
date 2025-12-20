package ai.timefold.solver.core.testdomain.clone.lookup;

import java.util.Objects;

public class TestdataObjectEquals {

    private final Integer id;

    public TestdataObjectEquals(Integer id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TestdataObjectEquals other = (TestdataObjectEquals) obj;
        return Objects.equals(this.id, other.id);
    }

}
