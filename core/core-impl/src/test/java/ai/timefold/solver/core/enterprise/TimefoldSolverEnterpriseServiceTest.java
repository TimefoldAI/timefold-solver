package ai.timefold.solver.core.enterprise;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TimefoldSolverEnterpriseServiceTest {

    @Test
    void failsOnLoad() {
        Assertions.assertThat(TimefoldSolverEnterpriseService.load())
                .isNull();
    }

    @Test
    void banner() {
        Assertions.assertThat(TimefoldSolverEnterpriseService.getBanner())
                .contains("Community Edition");
    }

}
