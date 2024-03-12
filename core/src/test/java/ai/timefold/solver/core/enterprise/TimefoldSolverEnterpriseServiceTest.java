package ai.timefold.solver.core.enterprise;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TimefoldSolverEnterpriseServiceTest {

    @Test
    void failsOnLoad() {
        Assertions.assertThatThrownBy(TimefoldSolverEnterpriseService::load)
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void solverVersion() {
        Assertions.assertThat(TimefoldSolverEnterpriseService.identifySolverVersion())
                .contains("Community Edition");
    }

}
